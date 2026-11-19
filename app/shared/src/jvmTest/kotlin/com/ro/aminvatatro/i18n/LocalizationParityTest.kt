package com.ro.aminvatatro.i18n

import java.io.File
import javax.xml.parsers.DocumentBuilderFactory
import org.w3c.dom.Element
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Verifies complete key parity, non-empty values, and matching format placeholders across
 * default Romanian (`values/strings.xml`), explicit Romanian (`values-ro/strings.xml`),
 * and explicit English (`values-en/strings.xml`) resources.
 */
class LocalizationParityTest {

    private val placeholderRegex = Regex("""%\d+\$[sd]""")

    @Test
    fun romanianAndEnglishResourcesHaveMatchingKeysNonEmptyValuesAndPlaceholders() {
        val defaultFile = resolveResourceFile("values/strings.xml")
        val roFile = resolveResourceFile("values-ro/strings.xml")
        val enFile = resolveResourceFile("values-en/strings.xml")

        assertTrue(defaultFile.exists(), "Missing default strings.xml at ${defaultFile.absolutePath}")
        assertTrue(roFile.exists(), "Missing RO strings.xml at ${roFile.absolutePath}")
        assertTrue(enFile.exists(), "Missing EN strings.xml at ${enFile.absolutePath}")

        val defaultStrings = parseStringsXml(defaultFile)
        val roStrings = parseStringsXml(roFile)
        val enStrings = parseStringsXml(enFile)

        assertTrue(defaultStrings.isNotEmpty(), "Default strings.xml must not be empty")
        assertEquals(defaultStrings.keys, roStrings.keys, "Keys mismatch between values and values-ro")
        assertEquals(defaultStrings.keys, enStrings.keys, "Keys mismatch between values and values-en")

        defaultStrings.forEach { (key, defaultValue) ->
            val roValue = roStrings.getValue(key)
            val enValue = enStrings.getValue(key)
            assertTrue(defaultValue.isNotBlank(), "Default string '$key' is blank")
            assertTrue(roValue.isNotBlank(), "RO string '$key' is blank")
            assertTrue(enValue.isNotBlank(), "EN string '$key' is blank")

            val defaultPlaceholders = placeholderRegex.findAll(defaultValue).map { it.value }.sorted().toList()
            val roPlaceholders = placeholderRegex.findAll(roValue).map { it.value }.sorted().toList()
            val enPlaceholders = placeholderRegex.findAll(enValue).map { it.value }.sorted().toList()
            assertEquals(defaultPlaceholders, roPlaceholders, "Placeholder mismatch for '$key' (default vs RO)")
            assertEquals(defaultPlaceholders, enPlaceholders, "Placeholder mismatch for '$key' (default vs EN)")
        }

        // Default and RO resources must contain standard Romanian diacritics (ă, â, î, ș, ț)
        val allDefaultText = defaultStrings.values.joinToString(" ").lowercase()
        val allRoText = roStrings.values.joinToString(" ").lowercase()
        assertTrue(
            listOf('ă', 'â', 'î', 'ș', 'ț').all { it in allDefaultText },
            "Expected default strings.xml to be in Romanian with diacritics (ă, â, î, ș, ț)",
        )
        assertTrue(
            listOf('ă', 'â', 'î', 'ș', 'ț').all { it in allRoText },
            "Expected values-ro/strings.xml to contain Romanian diacritics (ă, â, î, ș, ț)",
        )

        // EN resources must genuinely differ from RO for user-visible navigation/settings labels
        assertNotEquals(roStrings.getValue("nav_recipes"), enStrings.getValue("nav_recipes"))
        assertNotEquals(roStrings.getValue("settings_title"), enStrings.getValue("settings_title"))
    }

    private fun resolveResourceFile(relativePath: String): File {
        val candidates = listOf(
            File("src/commonMain/composeResources/$relativePath"),
            File("app/shared/src/commonMain/composeResources/$relativePath"),
        )
        return candidates.firstOrNull { it.exists() } ?: candidates.first()
    }

    private fun parseStringsXml(file: File): Map<String, String> {
        val factory = DocumentBuilderFactory.newInstance()
        val document = factory.newDocumentBuilder().parse(file)
        val nodes = document.getElementsByTagName("string")
        val result = linkedMapOf<String, String>()
        for (i in 0 until nodes.length) {
            val element = nodes.item(i) as Element
            val name = element.getAttribute("name")
            assertTrue(name.isNotBlank(), "Unnamed <string> entry in ${file.name}")
            assertTrue(name !in result, "Duplicate <string name=\"$name\"> in ${file.name}")
            result[name] = element.textContent
        }
        return result
    }
}
