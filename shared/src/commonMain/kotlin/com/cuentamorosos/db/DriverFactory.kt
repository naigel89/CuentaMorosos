package com.cuentamorosos.db

import app.cash.sqldelight.db.SqlDriver

/** Platform-specific factory that creates and opens the SQLDelight [SqlDriver]. */
expect class DriverFactory {
    fun createDriver(): SqlDriver
}

/** Shared accessor for the single [CuentaMorososDatabase] instance. */
object DatabaseProvider {
    private var instance: CuentaMorososDatabase? = null

    fun getDatabase(factory: DriverFactory): CuentaMorososDatabase {
        return instance ?: CuentaMorososDatabase(factory.createDriver()).also { instance = it }
    }
}
