package com.ro.aminvatatro.data.local

import kotlinx.serialization.Serializable
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlin.test.assertTrue

class WebLocalStorageStoreTest {

    @Serializable
    private data class WebSpikePayload(
        val schemaVersion: Int,
        val label: String,
    )

    @Test
    fun persistsJsonAcrossFreshStoreInstancesInBrowserLocalStorage() {
        val key = "retetar.spike.phase0"
        val payload = WebSpikePayload(schemaVersion = 1, label = "Ciorbă de perișoare")
        val writer = createWebJsonKeyValueStore()

        try {
            assertTrue(writer.save(key, payload, WebSpikePayload.serializer()).isSuccess)

            val reader = createWebJsonKeyValueStore()
            val loaded = reader.load(key, WebSpikePayload.serializer()).getOrThrow()
            assertEquals(payload, loaded)
        } finally {
            writer.remove(key)
        }

        val afterDelete = createWebJsonKeyValueStore()
        assertNull(afterDelete.load(key, WebSpikePayload.serializer()).getOrThrow())
    }
}
