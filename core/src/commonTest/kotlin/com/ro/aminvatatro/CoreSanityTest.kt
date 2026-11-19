package com.ro.aminvatatro

import com.ro.aminvatatro.data.local.SettingsJsonKeyValueStore
import com.russhwolf.settings.MapSettings
import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class CoreSanityTest {

    @Serializable
    private data class SpikeRecord(
        val id: String,
        val title: String,
        val servings: Int,
    )

    @Test
    fun sanityCheck() {
        assertEquals("Hello, Retetar!", sayHello("Retetar"))
    }

    @Test
    fun jsonStoreRoundTripAndRemove() {
        val settings = MapSettings()
        val store = SettingsJsonKeyValueStore(settings)
        val record = SpikeRecord(id = "r-1", title = "Clătite", servings = 4)

        assertTrue(store.save("spike.record", record, SpikeRecord.serializer()).isSuccess)
        assertEquals(record, store.load("spike.record", SpikeRecord.serializer()).getOrThrow())

        assertTrue(store.remove("spike.record").isSuccess)
        assertNull(store.load("spike.record", SpikeRecord.serializer()).getOrThrow())
    }

    @Test
    fun jsonStoreReturnsFailureOnCorruptJsonWithoutCrashing() {
        val settings = MapSettings()
        settings.putString("spike.corrupt", "{not valid json")
        val store = SettingsJsonKeyValueStore(settings)

        val result = store.load("spike.corrupt", SpikeRecord.serializer())
        assertTrue(result.isFailure)
    }

    @Test
    fun jsonStoreRejectsBlankKey() {
        val store = SettingsJsonKeyValueStore(MapSettings())
        val record = SpikeRecord(id = "r-2", title = "Omletă", servings = 2)

        assertTrue(store.save("   ", record, SpikeRecord.serializer()).isFailure)
        assertTrue(store.load("", SpikeRecord.serializer()).isFailure)
        assertTrue(store.remove(" ").isFailure)
    }
}
