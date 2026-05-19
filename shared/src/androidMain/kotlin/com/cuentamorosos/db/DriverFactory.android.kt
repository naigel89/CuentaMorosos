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
     * Always backfills state for existing events based on heuristic:
     * - lastCalculationMode IS NOT NULL → 'CALCULATED'
     * - participants OR memberIds IS NOT empty → 'OPEN'
     * - Otherwise → 'DRAFT'
     *
     * Backfill runs on every launch until all events have a non-DRAFT state
     * where appropriate (safe because CALCULATED/OPEN are idempotent).
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
                if ("state" !in existingColumns) {
                    println("[Migration] Adding state column to CachedEvent")
                    db.execSQL("ALTER TABLE CachedEvent ADD COLUMN state TEXT NOT NULL DEFAULT 'DRAFT'")
                }
                // Always backfill — safe because it only upgrades DRAFT → OPEN/CALCULATED
                db.execSQL(
                    "UPDATE CachedEvent SET state = 'CALCULATED' " +
                        "WHERE state = 'DRAFT' " +
                        "AND lastCalculationMode IS NOT NULL AND lastCalculationMode != ''"
                )
                val calculatedCount = db.rawQuery("SELECT COUNT(*) FROM CachedEvent WHERE state = 'CALCULATED'", null).use {
                    if (it.moveToNext()) it.getLong(0) else 0
                }
                db.execSQL(
                    "UPDATE CachedEvent SET state = 'OPEN' " +
                        "WHERE state = 'DRAFT' " +
                        "AND (participants IS NOT NULL AND participants != '' AND participants != '[]' " +
                        "     OR memberIds IS NOT NULL AND memberIds != '' AND memberIds != '[]')"
                )
                val openCount = db.rawQuery("SELECT COUNT(*) FROM CachedEvent WHERE state = 'OPEN'", null).use {
                    if (it.moveToNext()) it.getLong(0) else 0
                }
                val draftCount = db.rawQuery("SELECT COUNT(*) FROM CachedEvent WHERE state = 'DRAFT'", null).use {
                    if (it.moveToNext()) it.getLong(0) else 0
                }
                println("[Migration] Event state backfill complete: CALCULATED=$calculatedCount, OPEN=$openCount, DRAFT=$draftCount")
            }
        }.onFailure { e ->
            println("[Migration] Event state backfill failed: ${e.message}")
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
