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
