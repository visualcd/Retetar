package com.ro.aminvatatro.core.units

import com.ro.aminvatatro.domain.model.AppLanguage
import kotlin.test.Test
import kotlin.test.assertEquals

class QuantityFormatterTest {

    @Test
    fun formatsDecimalsWithLocaleSpecificSeparator() {
        assertEquals("1,5", QuantityFormatter.format(1.5, AppLanguage.RO))
        assertEquals("1.5", QuantityFormatter.format(1.5, AppLanguage.EN))
        assertEquals("2,25", QuantityFormatter.format(2.25, AppLanguage.RO))
        assertEquals("2.25", QuantityFormatter.format(2.25, AppLanguage.EN))
        assertEquals("0,15", QuantityFormatter.format(0.15, AppLanguage.RO))
        assertEquals("0.15", QuantityFormatter.format(0.15, AppLanguage.EN))
    }

    @Test
    fun formatsCommonFractionsWhenCloseToExact() {
        assertEquals("¼", QuantityFormatter.format(0.25, AppLanguage.RO))
        assertEquals("⅓", QuantityFormatter.format(0.33, AppLanguage.RO))
        assertEquals("⅓", QuantityFormatter.format(1.0 / 3.0, AppLanguage.EN))
        assertEquals("½", QuantityFormatter.format(0.5, AppLanguage.RO))
        assertEquals("⅔", QuantityFormatter.format(0.67, AppLanguage.RO))
        assertEquals("⅔", QuantityFormatter.format(2.0 / 3.0, AppLanguage.EN))
        assertEquals("¾", QuantityFormatter.format(0.75, AppLanguage.RO))
    }

    @Test
    fun disablesFractionsWhenRequested() {
        assertEquals("0,5", QuantityFormatter.format(0.5, AppLanguage.RO, useFractions = false))
        assertEquals("0.5", QuantityFormatter.format(0.5, AppLanguage.EN, useFractions = false))
        assertEquals("0,33", QuantityFormatter.format(0.33, AppLanguage.RO, useFractions = false))
    }

    @Test
    fun trimsTrailingZerosAndRoundsToAtMostTwoDecimals() {
        assertEquals("2", QuantityFormatter.format(2.0, AppLanguage.RO))
        assertEquals("1,2", QuantityFormatter.format(1.20, AppLanguage.RO))
        assertEquals("1,23", QuantityFormatter.format(1.234, AppLanguage.RO))
        assertEquals("1,24", QuantityFormatter.format(1.236, AppLanguage.RO))
        assertEquals("2", QuantityFormatter.format(1.999, AppLanguage.RO))
        assertEquals("0,05", QuantityFormatter.format(0.05, AppLanguage.RO))
        assertEquals("0,13", QuantityFormatter.format(0.126, AppLanguage.RO))
    }

    @Test
    fun handlesNullZeroNegativeAndNonFiniteGracefully() {
        assertEquals("", QuantityFormatter.format(null, AppLanguage.RO))
        assertEquals("0", QuantityFormatter.format(0.0, AppLanguage.RO))
        assertEquals("", QuantityFormatter.format(-1.5, AppLanguage.RO))
        assertEquals("", QuantityFormatter.format(Double.NaN, AppLanguage.RO))
        assertEquals("", QuantityFormatter.format(Double.POSITIVE_INFINITY, AppLanguage.EN))

        assertEquals(',', QuantityFormatter.decimalSeparatorFor(AppLanguage.RO))
        assertEquals('.', QuantityFormatter.decimalSeparatorFor(AppLanguage.EN))
    }
}
