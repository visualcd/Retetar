package com.ro.aminvatatro.data.local

import app.cash.sqldelight.db.SqlDriver

/**
 * Web (`js` / `wasmJs`) [DriverFactory].
 *
 * Always returns `null` because Web persistence uses the JSON `localStorage` fallback
 * decided in Phase 0.
 */
actual class DriverFactory {
    actual fun createDriver(): SqlDriver? = null
}
