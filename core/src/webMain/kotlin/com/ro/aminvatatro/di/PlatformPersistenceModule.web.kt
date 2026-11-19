package com.ro.aminvatatro.di

import com.ro.aminvatatro.data.local.JsonKeyValueStore
import com.ro.aminvatatro.data.local.createWebJsonKeyValueStore
import org.koin.core.module.Module
import org.koin.dsl.module

/**
 * Web (`js` / `wasmJs`) persistence Koin module using `localStorage` via [JsonKeyValueStore]
 * and [jsonPersistenceModule].
 */
actual fun platformPersistenceModule(): Module = module {
    includes(jsonPersistenceModule)
    single<JsonKeyValueStore> { createWebJsonKeyValueStore(json = get()) }
}
