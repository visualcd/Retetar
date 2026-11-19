package com.ro.aminvatatro.core.text

/**
 * Pure Kotlin text normalizer for diacritic-insensitive search and ingredient aggregation.
 *
 * Rules:
 * - Lowercase
 * - Remove Romanian diacritics (`ă`, `â`, `î`, `ș`, `ț`, plus legacy cedilla variants `ş`, `ţ`),
 *   common Latin accents, and Unicode combining diacritical marks (NFD support)
 * - Collapse multiple whitespace characters into a single space
 * - Trim leading and trailing whitespace
 */
object TextNormalizer {

    private val diacriticMap: Map<Char, Char> = mapOf(
        // Romanian (NFC lowercase & uppercase, including cedilla variants)
        'ă' to 'a', 'Ă' to 'a',
        'â' to 'a', 'Â' to 'a',
        'î' to 'i', 'Î' to 'i',
        'ș' to 's', 'Ș' to 's',
        'ş' to 's', 'Ş' to 's',
        'ț' to 't', 'Ț' to 't',
        'ţ' to 't', 'Ţ' to 't',
        // Common Latin accents
        'á' to 'a', 'Á' to 'a',
        'à' to 'a', 'À' to 'a',
        'ä' to 'a', 'Ä' to 'a',
        'ã' to 'a', 'Ã' to 'a',
        'é' to 'e', 'É' to 'e',
        'è' to 'e', 'È' to 'e',
        'ê' to 'e', 'Ê' to 'e',
        'ë' to 'e', 'Ë' to 'e',
        'í' to 'i', 'Í' to 'i',
        'ì' to 'i', 'Ì' to 'i',
        'ï' to 'i', 'Ï' to 'i',
        'ó' to 'o', 'Ó' to 'o',
        'ò' to 'o', 'Ò' to 'o',
        'ô' to 'o', 'Ô' to 'o',
        'ö' to 'o', 'Ö' to 'o',
        'õ' to 'o', 'Õ' to 'o',
        'ú' to 'u', 'Ú' to 'u',
        'ù' to 'u', 'Ù' to 'u',
        'û' to 'u', 'Û' to 'u',
        'ü' to 'u', 'Ü' to 'u',
        'ç' to 'c', 'Ç' to 'c',
        'ñ' to 'n', 'Ñ' to 'n',
    )

    /**
     * Normalizes [input] into a canonical lowercase, ASCII-folded, single-spaced string.
     */
    fun normalize(input: String): String {
        if (input.isEmpty()) return ""
        val result = StringBuilder(input.length)
        var pendingSpace = false

        for (ch in input) {
            // Skip Unicode combining diacritical marks (e.g., NFD decomposed characters)
            if (ch in '\u0300'..'\u036F') continue

            if (ch.isWhitespace()) {
                if (result.isNotEmpty()) {
                    pendingSpace = true
                }
            } else {
                if (pendingSpace) {
                    result.append(' ')
                    pendingSpace = false
                }
                val mapped = diacriticMap[ch] ?: ch.lowercaseChar()
                result.append(mapped)
            }
        }
        return result.toString()
    }
}
