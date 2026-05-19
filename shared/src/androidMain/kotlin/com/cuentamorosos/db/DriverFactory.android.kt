package com.cuentamorosos.db

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver

actual class DriverFactory(private val context: Context) {
    actual fun createDriver(): SqlDriver {
        val dbPath = context.getDatabasePath("cuentamorosos.db")

        if (dbPath.exists()) {
            addMissingExpenseColumns(dbPath)
            addMissingEventColumns(dbPath)
        }

        return AndroidSqliteDriver(CuentaMorososDatabase.Schema, context, "cuentamorosos.db")
    }

    /**
     * Safely adds the `state` column to CachedEvent for existing databases.
     * Also backfills state for existing events based on heuristic:
     * - lastCalculationMode IS NOT NULL → 'CALCULATED'
     * - participants IS NOT empty → 'OPEN'
     * - Otherwise → 'DRAFT'
     */
    private fun addMissingEventColumns(dbPath: java.io.File) {
        runCatching {
            SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                val existingColumns = mutableSetOf<String>()
                db.rawQuery("PRAGMA table_info(CachedEvent)", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        existingColumns.add(cursor.getString(1))
                    }
                }
                val needsColumn = "state" !in existingColumns
                if (needsColumn) {
                    db.execSQL("ALTER TABLE CachedEvent ADD COLUMN state TEXT NOT NULL DEFAULT 'DRAFT'")
                    // Backfill state for existing events
                    db.execSQL(
                        "UPDATE CachedEvent SET state = 'CALCULATED' " +
                            "WHERE lastCalculationMode IS NOT NULL AND lastCalculationMode != ''"
                    )
                    db.execSQL(
                        "UPDATE CachedEvent SET state = 'OPEN' " +
                            "WHERE (state = 'DRAFT' OR state IS NULL) " +
                            "AND participants IS NOT NULL AND participants != '' AND participants != '[]'"
                    )
                }
            }
        }
    }

    /**
     * Safely adds new columns to CachedExpense for existing databases.
     * Wrapped in try-catch so the app never crashes if columns already exist.
     */
    private fun addMissingExpenseColumns(dbPath: java.io.File) {
        val newColumns = listOf(
            "debtor_ids TEXT",
            "payer_contributions TEXT",
            "assigned_profile_ids TEXT",
            "profile_weights TEXT",
            "split_mode TEXT"
        )
        runCatching {
            SQLiteDatabase.openDatabase(dbPath.absolutePath, null, SQLiteDatabase.OPEN_READWRITE).use { db ->
                // Check which columns already exist
                val existingColumns = mutableSetOf<String>()
                db.rawQuery("PRAGMA table_info(CachedExpense)", null).use { cursor ->
                    while (cursor.moveToNext()) {
                        existingColumns.add(cursor.getString(1))
                    }
                }
                // Add only missing columns
                for (columnDef in newColumns) {
                    val columnName = columnDef.substringBefore(' ')
                    if (columnName !in existingColumns) {
                        db.execSQL("ALTER TABLE CachedExpense ADD COLUMN $columnDef")
                    }
                }
            }
        }
    }
}
