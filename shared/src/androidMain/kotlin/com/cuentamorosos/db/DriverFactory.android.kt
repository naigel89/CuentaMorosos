package com.cuentamorosos.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.cuentamorosos.data.LogSanitizer

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val dbPath = context.getDatabasePath("cuentamorosos.db")

        if (dbPath.exists()) {
            ensureColumnsExist(dbPath)
            setSqlDelightVersion(dbPath)
        }

        return AndroidSqliteDriver(CuentaMorososDatabase.Schema, context, "cuentamorosos.db")
    }

    private fun ensureColumnsExist(dbPath: java.io.File) {
        try {
            SQLiteDatabase.openDatabase(
                dbPath.absolutePath, null,
                SQLiteDatabase.OPEN_READWRITE
            ).use { db ->
                val profileColumns = mutableSetOf<String>()
                db.rawQuery("PRAGMA table_info(CachedProfile)", null).use { cursor ->
                    while (cursor.moveToNext()) profileColumns.add(cursor.getString(1))
                }

                val allColumns = listOf(
                    "ownerId TEXT NOT NULL DEFAULT ''",
                    "photo_url TEXT NOT NULL DEFAULT ''",
                    "username TEXT NOT NULL DEFAULT ''",
                    "display_name TEXT NOT NULL DEFAULT ''",
                    "custom_names TEXT NOT NULL DEFAULT ''"
                )
                for (columnDef in allColumns) {
                    val columnName = columnDef.substringBefore(' ')
                    if (columnName !in profileColumns) {
                        LogSanitizer.log("DB", "Adding missing column: $columnName")
                        db.execSQL("ALTER TABLE CachedProfile ADD COLUMN $columnDef")
                    }
                }

                // Remove deprecated 'icon' column (removed in refactor 047fb4d).
                // Without this migration, the upsert with 11 VALUES fails on
                // tables that still have 12 columns (icon included), causing
                // profiles to never load after re-login.
                if ("icon" in profileColumns) {
                    LogSanitizer.log("DB", "Removing deprecated column: icon")
                    try {
                        db.execSQL("ALTER TABLE CachedProfile DROP COLUMN icon")
                    } catch (e: Exception) {
                        // Fallback for SQLite < 3.35.0 (API < 30):
                        // recreate the table without the icon column.
                        LogSanitizer.log("DB", "DROP COLUMN not supported, recreating table...")
                        db.execSQL("""
                            CREATE TABLE CachedProfile_new (
                                id TEXT NOT NULL PRIMARY KEY,
                                name TEXT NOT NULL,
                                email TEXT NOT NULL DEFAULT '',
                                isGhost INTEGER NOT NULL DEFAULT 0,
                                totalPendingEuros REAL NOT NULL DEFAULT 0.0,
                                updatedAt INTEGER NOT NULL,
                                ownerId TEXT NOT NULL DEFAULT '',
                                photo_url TEXT NOT NULL DEFAULT '',
                                username TEXT NOT NULL DEFAULT '',
                                display_name TEXT NOT NULL DEFAULT '',
                                custom_names TEXT NOT NULL DEFAULT ''
                            )
                        """)
                        db.execSQL("""
                            INSERT INTO CachedProfile_new
                            SELECT id, name, email, isGhost, totalPendingEuros,
                                   updatedAt, ownerId, photo_url, username,
                                   display_name, custom_names
                            FROM CachedProfile
                        """)
                        db.execSQL("DROP TABLE CachedProfile")
                        db.execSQL("ALTER TABLE CachedProfile_new RENAME TO CachedProfile")
                    }
                }

                val debtColumns = mutableListOf<String>()
                db.rawQuery("PRAGMA table_info(CachedDebt)", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        val colIndex = cursor.getInt(0)  // cid = column index (0-based)
                        val colName = cursor.getString(1)
                        debtColumns.add(colName)
                        LogSanitizer.log("DB", "CachedDebt column[$colIndex]: $colName")
                    }
                }

                val expectedDebtOrder = listOf("id", "eventId", "profileId", "creditorId", "amountEuros", "paid", "notes", "calculationMode", "updatedAt")
                val needsRecreate = debtColumns != expectedDebtOrder

                if (needsRecreate) {
                    LogSanitizer.log("DB", "CachedDebt column order is WRONG. Recreating table...")
                    LogSanitizer.log("DB", "  Current: $debtColumns")
                    LogSanitizer.log("DB", "  Expected: $expectedDebtOrder")

                    // 1. Rename old table
                    db.execSQL("ALTER TABLE CachedDebt RENAME TO CachedDebt_old")

                    // 2. Create new table with correct column order
                    db.execSQL("""
                        CREATE TABLE IF NOT EXISTS CachedDebt (
                            id TEXT NOT NULL PRIMARY KEY,
                            eventId TEXT NOT NULL,
                            profileId TEXT NOT NULL,
                            creditorId TEXT,
                            amountEuros REAL NOT NULL DEFAULT 0.0,
                            paid INTEGER NOT NULL DEFAULT 0,
                            notes TEXT NOT NULL DEFAULT '',
                            calculationMode TEXT,
                            updatedAt INTEGER NOT NULL
                        )
                    """)

                    // 3. Migrate data with explicit column mapping
                    val oldCols = debtColumns.joinToString(", ")
                    val newCols = expectedDebtOrder.joinToString(", ")
                    // Build column mapping: for each column in the new table,
                    // find the matching column name in the old table
                    val colMapping = expectedDebtOrder.joinToString(", ") { col ->
                        if (col in debtColumns) col else "NULL"
                    }
                    db.execSQL("""
                        INSERT INTO CachedDebt ($newCols)
                        SELECT $colMapping FROM CachedDebt_old
                    """)

                    // 4. Drop old table
                    db.execSQL("DROP TABLE CachedDebt_old")
                    LogSanitizer.log("DB", "CachedDebt table recreated successfully")
                } else {
                    LogSanitizer.log("DB", "CachedDebt column order is correct")
                }
            }
        } catch (e: Exception) {
            LogSanitizer.log("DB", "Failed to ensure columns: ${e.message}. Deleting database.")
            context.deleteDatabase("cuentamorosos.db")
        }
    }

    private fun setSqlDelightVersion(dbPath: java.io.File) {
        try {
            SQLiteDatabase.openDatabase(
                dbPath.absolutePath, null,
                SQLiteDatabase.OPEN_READWRITE
            ).use { db ->
                val expectedVersion = 1L
                db.execSQL("PRAGMA user_version = $expectedVersion")
                LogSanitizer.log("DB", "SQLDelight version set to $expectedVersion")
            }
        } catch (e: Exception) {
            LogSanitizer.log("DB", "Failed to set version: ${e.message}")
        }
    }
}
