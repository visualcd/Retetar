package com.ro.aminvatatro.core.text

import kotlin.test.Test
import kotlin.test.assertEquals

class TextNormalizerTest {

    @Test
    fun normalizesRomanianDiacriticsAndCase() {
        assertEquals("faina", TextNormalizer.normalize("Făină"))
        assertEquals("faina", TextNormalizer.normalize("FAINĂ"))
        assertEquals("faina", TextNormalizer.normalize("făină"))
        assertEquals("faina", TextNormalizer.normalize("faina"))
        assertEquals("aaist aaist", TextNormalizer.normalize("ăâîșț ĂÂÎȘȚ"))
    }

    @Test
    fun normalizesLegacyCedillaVariantsAndDecomposedUnicode() {
        assertEquals("st st", TextNormalizer.normalize("şţ ŞŢ"))
        assertEquals("ciorba de perisoare", TextNormalizer.normalize("Ciorbă de perișoare"))
        assertEquals("ciorba de perisoare", TextNormalizer.normalize("Ciorbă de perişoare"))
        // NFD decomposed form: a + combining breve (\u0306)
        assertEquals("faina", TextNormalizer.normalize("Fa\u0306ina\u0306"))
    }

    @Test
    fun trimsAndCollapsesMultipleWhitespace() {
        assertEquals(
            "supa crema de legume",
            TextNormalizer.normalize("   Supă   cremă\t\tde \n  legume   "),
        )
        assertEquals("", TextNormalizer.normalize(""))
        assertEquals("", TextNormalizer.normalize("   \t\n  "))
    }

    @Test
    fun normalizesCommonLatinAccents() {
        assertEquals("creme brulee", TextNormalizer.normalize("Crème brûlée"))
        assertEquals("jalapeno", TextNormalizer.normalize("Jalapeño"))
    }
}
