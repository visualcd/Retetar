package com.ro.aminvatatro.data.local

import android.content.Context
import androidx.sqlite.db.SupportSQLiteDatabase
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import com.ro.aminvatatro.data.local.db.RetetarDatabase

/**
 * Android [DriverFactory] backed by [AndroidSqliteDriver] with foreign key constraints enabled
 * on every opened connection (`PRAGMA foreign_keys = ON`).
 */
actual class DriverFactory(
    private val context: Context,
    private val databaseName: String? = DATABASE_NAME,
) {
    actual fun createDriver(): SqlDriver? = AndroidSqliteDriver(
        schema = RetetarDatabase.Schema,
        context = context.applicationContext,
        name = databaseName,
        callback = object : AndroidSqliteDriver.Callback(RetetarDatabase.Schema) {
            override fun onOpen(db: SupportSQLiteDatabase) {
                super.onOpen(db)
                db.setForeignKeyConstraintsEnabled(true)
            }
        },
    )
}
