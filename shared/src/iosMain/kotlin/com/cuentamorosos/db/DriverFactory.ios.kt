package com.cuentamorosos.db

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver

actual class DriverFactory {
    actual fun createDriver(): SqlDriver =
        NativeSqliteDriver(CuentaMorososDatabase.Schema, "cuentamorosos.db")
}
