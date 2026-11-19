package com.ro.aminvatatro.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.native.NativeSqliteDriver
import com.ro.aminvatatro.data.local.db.RetetarDatabase

/**
 * iOS [DriverFactory] backed by [NativeSqliteDriver] with `foreignKeyConstraints = true`
 * (`PRAGMA foreign_keys = ON`).
 */
actual class DriverFactory(
    private val databaseName: String = DATABASE_NAME,
) {
    actual fun createDriver(): SqlDriver? = NativeSqliteDriver(
        schema = RetetarDatabase.Schema,
        name = databaseName,
        onConfiguration = { config ->
            config.copy(
                extendedConfig = config.extendedConfig.copy(foreignKeyConstraints = true),
            )
        },
    )
}
