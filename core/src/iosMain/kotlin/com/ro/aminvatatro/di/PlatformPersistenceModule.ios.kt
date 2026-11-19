package com.ro.aminvatatro.di

import com.ro.aminvatatro.data.local.DriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * iOS persistence Koin module using [DriverFactory] (`NativeSqliteDriver`) and [sqlPersistenceModule].
 */
actual fun platformPersistenceModule(): Module = module {
    includes(sqlPersistenceModule)
    single { DriverFactory() }
}
