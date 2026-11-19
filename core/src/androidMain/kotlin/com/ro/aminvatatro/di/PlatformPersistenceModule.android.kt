package com.ro.aminvatatro.di

import com.ro.aminvatatro.data.local.DriverFactory
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Android persistence Koin module using [DriverFactory] (resolved with Koin's `androidContext`)
 * and [sqlPersistenceModule].
 */
actual fun platformPersistenceModule(): Module = module {
    includes(sqlPersistenceModule)
    single { DriverFactory(context = get()) }
}
