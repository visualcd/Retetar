package com.ro.aminvatatro.data.local

import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import com.ro.aminvatatro.data.local.db.RetetarDatabase
import java.io.File
import java.util.Properties

/**
 * Computes the default persistent SQLite JDBC URL inside the user's home directory (`~/.retetar/retetar.db`).
 */
fun defaultJvmDatabaseUrl(fileName: String = DATABASE_NAME): String {
    val home = System.getProperty("user.home").orEmpty().ifBlank { "." }
    val dir = File(home, ".retetar")
    if (!dir.exists()) {
        dir.mkdirs()
    }
    val dbFile = File(dir, fileName)
    return "jdbc:sqlite:${dbFile.absolutePath}"
}

/**
 * Desktop (JVM) [DriverFactory] backed by [JdbcSqliteDriver] with `foreign_keys = true`
 * (`PRAGMA foreign_keys = ON`) enabled on every connection.
 */
actual class DriverFactory(
    private val databaseUrl: String = defaultJvmDatabaseUrl(),
) {
    actual fun createDriver(): SqlDriver? {
        val properties = Properties().apply {
            put("foreign_keys", "true")
        }
        return JdbcSqliteDriver(
            url = databaseUrl,
            properties = properties,
            schema = RetetarDatabase.Schema,
        )
    }

    companion object {
        /** Creates an in-memory JVM [DriverFactory] suitable for integration tests. */
        fun inMemory(): DriverFactory = DriverFactory(databaseUrl = JdbcSqliteDriver.IN_MEMORY)
    }
}
