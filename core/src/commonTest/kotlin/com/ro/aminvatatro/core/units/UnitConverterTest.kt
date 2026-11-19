package com.ro.aminvatatro.core.units

import com.ro.aminvatatro.domain.model.UnitSystem
import kotlin.math.abs
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class UnitConverterTest {

    private fun assertApprox(expected: Double, actual: Double?, tolerance: Double = 1e-4) {
        assertNotNull(actual, "Expected $expected but got null")
        assertTrue(
            abs(expected - actual) <= tolerance,
            "Expected $expected ± $tolerance, got $actual",
        )
    }

    @Test
    fun massConversionsWithinCategory() {
        assertApprox(1000.0, UnitConverter.convert(1.0, MeasurementUnit.KG, MeasurementUnit.G))
        assertApprox(0.5, UnitConverter.convert(500.0, MeasurementUnit.G, MeasurementUnit.KG))
        assertApprox(28.3495, UnitConverter.convert(1.0, MeasurementUnit.OZ, MeasurementUnit.G))
        assertApprox(453.592, UnitConverter.convert(1.0, MeasurementUnit.LB, MeasurementUnit.G))
        assertApprox(16.0, UnitConverter.convert(1.0, MeasurementUnit.LB, MeasurementUnit.OZ), tolerance = 1e-3)
    }

    @Test
    fun volumeConversionsWithinCategory() {
        assertApprox(1.0, UnitConverter.convert(3.0, MeasurementUnit.TSP, MeasurementUnit.TBSP))
        assertApprox(15.0, UnitConverter.convert(1.0, MeasurementUnit.TBSP, MeasurementUnit.ML))
        assertApprox(5.0, UnitConverter.convert(1.0, MeasurementUnit.TSP, MeasurementUnit.ML))
        assertApprox(240.0, UnitConverter.convert(1.0, MeasurementUnit.CUP, MeasurementUnit.ML))
        assertApprox(16.0, UnitConverter.convert(1.0, MeasurementUnit.CUP, MeasurementUnit.TBSP))
        assertApprox(1000.0, UnitConverter.convert(1.0, MeasurementUnit.L, MeasurementUnit.ML))
        assertApprox(0.25, UnitConverter.convert(250.0, MeasurementUnit.ML, MeasurementUnit.L))
    }

    @Test
    fun identityAndCrossCategoryConversions() {
        assertApprox(3.0, UnitConverter.convert(3.0, MeasurementUnit.PIECE, MeasurementUnit.PIECE))
        assertApprox(2.0, UnitConverter.convert(2.0, MeasurementUnit.PINCH, MeasurementUnit.PINCH))
        assertApprox(100.0, UnitConverter.convert(100.0, MeasurementUnit.G, MeasurementUnit.G))

        // Cross-category conversions must return null (no density assumptions)
        assertNull(UnitConverter.convert(100.0, MeasurementUnit.G, MeasurementUnit.ML))
        assertNull(UnitConverter.convert(1.0, MeasurementUnit.KG, MeasurementUnit.L))
        assertNull(UnitConverter.convert(1.0, MeasurementUnit.CUP, MeasurementUnit.G))
        assertNull(UnitConverter.convert(2.0, MeasurementUnit.PIECE, MeasurementUnit.G))
        assertNull(UnitConverter.convert(1.0, MeasurementUnit.PINCH, MeasurementUnit.G))
        assertNull(UnitConverter.convert(1.0, MeasurementUnit.PINCH, MeasurementUnit.TSP))

        // TO_TASTE cannot be converted
        assertNull(UnitConverter.convert(1.0, MeasurementUnit.TO_TASTE, MeasurementUnit.TO_TASTE))
        assertNull(UnitConverter.convert(1.0, MeasurementUnit.TO_TASTE, MeasurementUnit.G))
        assertNull(UnitConverter.convert(1.0, MeasurementUnit.PINCH, MeasurementUnit.TO_TASTE))

        // Invalid quantities return null
        assertNull(UnitConverter.convert(-1.0, MeasurementUnit.G, MeasurementUnit.KG))
        assertNull(UnitConverter.convert(Double.NaN, MeasurementUnit.G, MeasurementUnit.KG))
        assertNull(UnitConverter.convert(Double.POSITIVE_INFINITY, MeasurementUnit.G, MeasurementUnit.KG))
    }

    @Test
    fun toBaseUnitHandlesAllCategoriesAndInvalidValues() {
        assertApprox(1000.0, UnitConverter.toBaseUnit(1.0, MeasurementUnit.KG))
        assertApprox(240.0, UnitConverter.toBaseUnit(1.0, MeasurementUnit.CUP))
        assertApprox(4.0, UnitConverter.toBaseUnit(4.0, MeasurementUnit.PIECE))
        assertApprox(2.0, UnitConverter.toBaseUnit(2.0, MeasurementUnit.PINCH))
        assertNull(UnitConverter.toBaseUnit(1.0, MeasurementUnit.TO_TASTE))
        assertNull(UnitConverter.toBaseUnit(-5.0, MeasurementUnit.G))
        assertNull(UnitConverter.toBaseUnit(Double.NaN, MeasurementUnit.ML))
    }

    @Test
    fun scalingCoversServingsFractionalPiecesPinchAndToTaste() {
        // 4 -> 6 servings
        assertApprox(300.0, UnitConverter.scaleQuantity(200.0, MeasurementUnit.G, 4, 6))
        // 0.5 pieces
        assertApprox(0.5, UnitConverter.scaleQuantity(1.0, MeasurementUnit.PIECE, 4, 2))
        assertApprox(0.75, UnitConverter.scaleQuantity(0.5, MeasurementUnit.PIECE, 4, 6))
        // PINCH scales numerically
        assertApprox(1.5, UnitConverter.scaleQuantity(1.0, MeasurementUnit.PINCH, 4, 6))
        // TO_TASTE and null stay null
        assertNull(UnitConverter.scaleQuantity(null, MeasurementUnit.TO_TASTE, 4, 6))
        assertNull(UnitConverter.scaleQuantity(1.0, MeasurementUnit.TO_TASTE, 4, 6))
        assertNull(UnitConverter.scaleQuantity(null, MeasurementUnit.G, 4, 6))

        // Invalid arguments throw IllegalArgumentException
        assertFailsWith<IllegalArgumentException> {
            UnitConverter.scaleQuantity(100.0, MeasurementUnit.G, 0, 4)
        }
        assertFailsWith<IllegalArgumentException> {
            UnitConverter.scaleQuantity(100.0, MeasurementUnit.G, 4, 0)
        }
        assertFailsWith<IllegalArgumentException> {
            UnitConverter.scaleQuantity(-1.0, MeasurementUnit.G, 4, 6)
        }
        assertFailsWith<IllegalArgumentException> {
            UnitConverter.scaleQuantity(Double.NaN, MeasurementUnit.G, 4, 6)
        }
    }

    @Test
    fun promoteMetricBaseUnitAndDisplaySystemConversion() {
        val (gQty, gUnit) = UnitConverter.promoteMetricBaseUnit(999.0, UnitCategory.MASS)
        assertApprox(999.0, gQty)
        assertEquals(MeasurementUnit.G, gUnit)

        val (kgQty, kgUnit) = UnitConverter.promoteMetricBaseUnit(1500.0, UnitCategory.MASS)
        assertApprox(1.5, kgQty)
        assertEquals(MeasurementUnit.KG, kgUnit)

        val (mlQty, mlUnit) = UnitConverter.promoteMetricBaseUnit(500.0, UnitCategory.VOLUME)
        assertApprox(500.0, mlQty)
        assertEquals(MeasurementUnit.ML, mlUnit)

        val (lQty, lUnit) = UnitConverter.promoteMetricBaseUnit(1000.0, UnitCategory.VOLUME)
        assertApprox(1.0, lQty)
        assertEquals(MeasurementUnit.L, lUnit)

        assertEquals(MeasurementUnit.PIECE, UnitConverter.promoteMetricBaseUnit(3.0, UnitCategory.COUNT).second)
        assertEquals(MeasurementUnit.PINCH, UnitConverter.promoteMetricBaseUnit(2.0, UnitCategory.OTHER).second)

        // Imperial conversions
        val (lbQty, lbUnit) = UnitConverter.convertForDisplaySystem(500.0, MeasurementUnit.G, UnitSystem.IMPERIAL)
        assertEquals(MeasurementUnit.LB, lbUnit)
        assertApprox(500.0 / 453.592, lbQty)

        val (ozQty, ozUnit) = UnitConverter.convertForDisplaySystem(100.0, MeasurementUnit.G, UnitSystem.IMPERIAL)
        assertEquals(MeasurementUnit.OZ, ozUnit)
        assertApprox(100.0 / 28.3495, ozQty)

        val (cupQty, cupUnit) = UnitConverter.convertForDisplaySystem(240.0, MeasurementUnit.ML, UnitSystem.IMPERIAL)
        assertEquals(MeasurementUnit.CUP, cupUnit)
        assertApprox(1.0, cupQty)

        val (tbspQty, tbspUnit) = UnitConverter.convertForDisplaySystem(30.0, MeasurementUnit.ML, UnitSystem.IMPERIAL)
        assertEquals(MeasurementUnit.TBSP, tbspUnit)
        assertApprox(2.0, tbspQty)

        val (tspQty, tspUnit) = UnitConverter.convertForDisplaySystem(10.0, MeasurementUnit.ML, UnitSystem.IMPERIAL)
        assertEquals(MeasurementUnit.TSP, tspUnit)
        assertApprox(2.0, tspQty)

        // Metric display & non-convertible passthrough
        val (metricQty, metricUnit) = UnitConverter.convertForDisplaySystem(1.0, MeasurementUnit.LB, UnitSystem.METRIC)
        assertEquals(MeasurementUnit.G, metricUnit)
        assertApprox(453.592, metricQty)

        val (metricVolQty, metricVolUnit) = UnitConverter.convertForDisplaySystem(1.0, MeasurementUnit.CUP, UnitSystem.METRIC)
        assertEquals(MeasurementUnit.ML, metricVolUnit)
        assertApprox(240.0, metricVolQty)

        val pieceResult = UnitConverter.convertForDisplaySystem(3.0, MeasurementUnit.PIECE, UnitSystem.IMPERIAL)
        assertEquals(3.0, pieceResult.first)
        assertEquals(MeasurementUnit.PIECE, pieceResult.second)

        val pinchResult = UnitConverter.convertForDisplaySystem(1.0, MeasurementUnit.PINCH, UnitSystem.METRIC)
        assertEquals(1.0, pinchResult.first)
        assertEquals(MeasurementUnit.PINCH, pinchResult.second)

        val invalidResult = UnitConverter.convertForDisplaySystem(-1.0, MeasurementUnit.G, UnitSystem.METRIC)
        assertEquals(-1.0, invalidResult.first)
        assertEquals(MeasurementUnit.G, invalidResult.second)
    }

    @Test
    fun shoppingItemKeyFollowsCategoryAndOtherRules() {
        assertEquals("faina|MASS", UnitConverter.shoppingItemKey("faina", MeasurementUnit.G))
        assertEquals("faina|MASS", UnitConverter.shoppingItemKey("faina", MeasurementUnit.KG))
        assertEquals("lapte|VOLUME", UnitConverter.shoppingItemKey("lapte", MeasurementUnit.ML))
        assertEquals("lapte|VOLUME", UnitConverter.shoppingItemKey("lapte", MeasurementUnit.CUP))
        assertEquals("oua|COUNT", UnitConverter.shoppingItemKey("oua", MeasurementUnit.PIECE))
        assertEquals("sare|TO_TASTE", UnitConverter.shoppingItemKey("sare", MeasurementUnit.TO_TASTE))
        assertEquals("piper|PINCH", UnitConverter.shoppingItemKey("piper", MeasurementUnit.PINCH))
    }

    @Test
    fun measurementUnitMetadataIsConsistent() {
        assertEquals(MeasurementUnit.G, MeasurementUnit.KG.baseUnit)
        assertEquals(MeasurementUnit.ML, MeasurementUnit.CUP.baseUnit)
        assertEquals(MeasurementUnit.PIECE, MeasurementUnit.PIECE.baseUnit)
        assertEquals(MeasurementUnit.PINCH, MeasurementUnit.PINCH.baseUnit)
        assertEquals(MeasurementUnit.TO_TASTE, MeasurementUnit.TO_TASTE.baseUnit)

        assertTrue(MeasurementUnit.G.requiresQuantity)
        assertTrue(MeasurementUnit.PINCH.requiresQuantity)
        assertFalse(MeasurementUnit.TO_TASTE.requiresQuantity)
    }
}
