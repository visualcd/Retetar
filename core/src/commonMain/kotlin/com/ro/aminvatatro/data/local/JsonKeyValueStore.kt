package com.ro.aminvatatro.data.local

import com.russhwolf.settings.Settings
import kotlinx.serialization.KSerializer
import kotlinx.serialization.json.Json

/**
 * Minimal JSON key-value persistence abstraction used as the Web (`localStorage`) storage
 * foundation and testable across all targets with an in-memory [Settings] fake.
 */
interface JsonKeyValueStore {
    /** Serializes [value] as JSON and stores it under [key]. */
    fun <T> save(key: String, value: T, serializer: KSerializer<T>): Result<Unit>

    /** Loads and deserializes the JSON stored under [key], returning `null` if absent. */
    fun <T> load(key: String, serializer: KSerializer<T>): Result<T?>

    /** Removes the entry stored under [key], if present. */
    fun remove(key: String): Result<Unit>
}

/** Default JSON configuration shared by local JSON stores. */
val defaultLocalStoreJson: Json = Json {
    ignoreUnknownKeys = true
    encodeDefaults = true
}

/**
 * [JsonKeyValueStore] implementation backed by [Settings] (`localStorage` on Web via
 * `StorageSettings`, or `MapSettings` in unit tests).
 */
class SettingsJsonKeyValueStore(
    private val settings: Settings,
    private val json: Json = defaultLocalStoreJson,
) : JsonKeyValueStore {

    override fun <T> save(key: String, value: T, serializer: KSerializer<T>): Result<Unit> {
        if (key.isBlank()) {
            return Result.failure(IllegalArgumentException("Storage key must not be blank"))
        }
        return runCatching {
            val encoded = json.encodeToString(serializer, value)
            settings.putString(key, encoded)
        }
    }

    override fun <T> load(key: String, serializer: KSerializer<T>): Result<T?> {
        if (key.isBlank()) {
            return Result.failure(IllegalArgumentException("Storage key must not be blank"))
        }
        val raw = settings.getStringOrNull(key) ?: return Result.success(null)
        return runCatching {
            json.decodeFromString(serializer, raw)
        }
    }

    override fun remove(key: String): Result<Unit> {
        if (key.isBlank()) {
            return Result.failure(IllegalArgumentException("Storage key must not be blank"))
        }
        return runCatching {
            settings.remove(key)
        }
    }
}
