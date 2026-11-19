package com.ro.aminvatatro.core.units

import com.ro.aminvatatro.domain.model.AppLanguage
import kotlin.math.abs
import kotlin.math.round

/**
 * Formats culinary quantities in a human-friendly, locale-aware form without platform APIs:
 * - Maximum 2 decimal places, trailing fractional zeros removed.
 * - Locale decimal separator (`','` for [AppLanguage.RO], `'.'` for [AppLanguage.EN]).
 * - Common culinary fractions (`¼`, `⅓`, `½`, `⅔`, `¾`) for values in `(0, 1)` when close to exact.
 */
object QuantityFormatter {

    private const val FRACTION_TOLERANCE = 0.015

    private val commonFractions: List<Pair<Double, String>> = listOf(
        0.25 to "¼",
        1.0 / 3.0 to "⅓",
        0.5 to "½",
        2.0 / 3.0 to "⅔",
        0.75 to "¾",
    )

    /**
     * Formats [quantity] for display using [language] conventions.
     *
     * Returns an empty string for `null`, negative, or non-finite values.
     * When [useFractions] is `true` and [quantity] is strictly between `0` and `1` and within
     * [FRACTION_TOLERANCE] of a common fraction (`¼`, `⅓`, `½`, `⅔`, `¾`), the Unicode fraction
     * glyph is returned instead of a decimal number.
     */
    fun format(
        quantity: Double?,
        language: AppLanguage = AppLanguage.RO,
        useFractions: Boolean = true,
    ): String {
        if (quantity == null || !quantity.isFinite() || quantity < 0.0) return ""
        if (quantity == 0.0) return "0"

        if (useFractions && quantity < 1.0) {
            val matchedFraction = commonFractions.firstOrNull { (target, _) ->
                abs(quantity - target) <= FRACTION_TOLERANCE
            }
            if (matchedFraction != null) {
                return matchedFraction.second
            }
        }

        val scaledHundredths = round(quantity * 100.0).toLong()
        val wholePart = scaledHundredths / 100L
        val fractionalHundredths = (scaledHundredths % 100L).toInt()

        if (fractionalHundredths == 0) {
            return wholePart.toString()
        }

        val separator = decimalSeparatorFor(language)
        val fractionText = if (fractionalHundredths % 10 == 0) {
            (fractionalHundredths / 10).toString()
        } else {
            fractionalHundredths.toString().padStart(2, '0')
        }
        return "$wholePart$separator$fractionText"
    }

    /** Returns the decimal separator character for [language]. */
    fun decimalSeparatorFor(language: AppLanguage): Char = when (language) {
        AppLanguage.RO -> ','
        AppLanguage.EN -> '.'
    }
}
