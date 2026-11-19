package com.ro.aminvatatro.di

import android.content.Context
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module

/**
 * Idempotently initializes Koin on Android, registering the application [context].
 */
fun initKoinForAndroid(
    context: Context,
    extraModules: List<Module> = emptyList(),
) {
    initKoin(extraModules = extraModules) {
        androidContext(context.applicationContext)
    }
}
