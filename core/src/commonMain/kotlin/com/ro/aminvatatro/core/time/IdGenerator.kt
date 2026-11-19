package com.ro.aminvatatro.core.time

import kotlin.uuid.ExperimentalUuidApi
import kotlin.uuid.Uuid

/**
 * Injectable identifier generator for domain entities so IDs are deterministic in tests.
 */
interface IdGenerator {
    /** Generates a new unique identifier string (UUID format in production). */
    fun nextId(): String
}

/**
 * Default [IdGenerator] producing RFC 4122 random UUID v4 strings via Kotlin stdlib [Uuid].
 */
class UuidIdGenerator : IdGenerator {
    @OptIn(ExperimentalUuidApi::class)
    override fun nextId(): String = Uuid.random().toString()
}
