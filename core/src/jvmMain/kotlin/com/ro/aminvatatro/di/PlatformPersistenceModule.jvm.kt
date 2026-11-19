package com.ro.aminvatatro.di

import com.ro.aminvatatro.data.local.DriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Desktop (JVM) persistence Koin module using [DriverFactory] (`JdbcSqliteDriver`) and [sqlPersistenceModule].
 */
actual fun platformPersistenceModule(): Module = module {
    includes(sqlPersistenceModule)
    single { DriverFactory() }
}
