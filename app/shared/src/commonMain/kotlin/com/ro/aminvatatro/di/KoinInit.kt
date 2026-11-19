package com.ro.aminvatatro.di

import org.koin.core.context.startKoin
import org.koin.core.module.Module
import org.koin.dsl.KoinAppDeclaration
import org.koin.mp.KoinPlatformTools

/**
 * Idempotently initializes Koin with all shared `:core` modules, [presentationModule],
 * and optional [extraModules].
 */
fun initKoin(
    extraModules: List<Module> = emptyList(),
    appDeclaration: KoinAppDeclaration? = null,
) {
    if (KoinPlatformTools.defaultContext().getOrNull() == null) {
        startKoin {
            appDeclaration?.invoke(this)
            modules(coreModules(listOf(presentationModule) + extraModules))
        }
    }
}
