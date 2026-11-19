package com.ro.aminvatatro.data.local

import app.cash.sqldelight.db.SqlDriver

/** Default SQLite database file name used on Android, iOS, and Desktop (JVM). */
const val DATABASE_NAME: String = "retetar.db"

/**
 * Platform-specific SQLDelight driver factory.
 *
 * - Android, iOS, and Desktop (JVM) return an open [SqlDriver] with `PRAGMA foreign_keys = ON`.
 * - Web (`js` and `wasmJs`) returns `null`, because Web uses the `localStorage` JSON persistence fallback.
 */
expect class DriverFactory {
    /** Creates the platform [SqlDriver], or returns `null` on targets using JSON fallback storage. */
    fun createDriver(): SqlDriver?
}
