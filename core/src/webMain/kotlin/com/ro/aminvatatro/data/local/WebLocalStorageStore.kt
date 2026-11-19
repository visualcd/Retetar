package com.ro.aminvatatro.data.local

import com.russhwolf.settings.Settings
import kotlinx.serialization.json.Json

/**
 * Creates a [JsonKeyValueStore] persisted in browser `localStorage` for Web targets (`js` and `wasmJs`).
 *
 * `Settings()` from `multiplatform-settings-no-arg` resolves to `StorageSettings()` on both
 * Kotlin/JS and Kotlin/WasmJS, delegating to `window.localStorage`.
 */
fun createWebJsonKeyValueStore(
    json: Json = defaultLocalStoreJson,
): JsonKeyValueStore = SettingsJsonKeyValueStore(
    settings = Settings(),
    json = json,
)
