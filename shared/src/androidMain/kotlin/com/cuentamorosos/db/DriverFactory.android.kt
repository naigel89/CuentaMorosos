package com.cuentamorosos.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

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
                        println("[DB] Adding missing column: $columnName")
                        db.execSQL("ALTER TABLE CachedProfile ADD COLUMN $columnDef")
                    }
                }

                // Remove deprecated 'icon' column (removed in refactor 047fb4d).
                // Without this migration, the upsert with 11 VALUES fails on
                // tables that still have 12 columns (icon included), causing
                // profiles to never load after re-login.
                if ("icon" in profileColumns) {
                    println("[DB] Removing deprecated column: icon")
                    try {
                        db.execSQL("ALTER TABLE CachedProfile DROP COLUMN icon")
                    } catch (e: Exception) {
                        // Fallback for SQLite < 3.35.0 (API < 30):
                        // recreate the table without the icon column.
                        println("[DB] DROP COLUMN not supported, recreating table...")
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
            }
        } catch (e: Exception) {
            println("[DB] Failed to ensure columns: ${e.message}. Deleting database.")
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
                println("[DB] SQLDelight version set to $expectedVersion")
            }
        } catch (e: Exception) {
            println("[DB] Failed to set version: ${e.message}")
        }
    }
}
