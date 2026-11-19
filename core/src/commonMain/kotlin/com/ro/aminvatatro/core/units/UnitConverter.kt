package com.ro.aminvatatro.core.units

import com.ro.aminvatatro.domain.model.UnitSystem

/**
 * Pure domain unit conversion, scaling, display-unit promotion, and shopping-key helpers.
 */
object UnitConverter {

    private const val METRIC_PROMOTION_THRESHOLD = 1000.0
    private const val POUND_THRESHOLD_IN_OUNCES = 16.0
    private const val CUP_THRESHOLD_IN_ML = 120.0
    private const val TBSP_THRESHOLD_IN_ML = 15.0

    /**
     * Converts [quantity] from [from] to [to].
     *
     * Conversions are strictly limited to units in the same [UnitCategory] with defined
     * conversion factors. Cross-category conversions (e.g. `MASS` ↔ `VOLUME`), conversions
     * involving [MeasurementUnit.TO_TASTE], negative, or non-finite quantities return `null`.
     */
    fun convert(
        quantity: Double,
        from: MeasurementUnit,
        to: MeasurementUnit,
    ): Double? {
        if (!quantity.isFinite() || quantity < 0.0) return null
        if (from == MeasurementUnit.TO_TASTE || to == MeasurementUnit.TO_TASTE) return null
        if (from == to) return quantity
        if (from.category != to.category) return null

        val fromFactor = from.factorToBase ?: return null
        val toFactor = to.factorToBase ?: return null
        return (quantity * fromFactor) / toFactor
    }

    /**
     * Converts [quantity] expressed in [unit] to its category's base unit (`G` for mass,
     * `ML` for volume, `PIECE` for count; [MeasurementUnit.PINCH] stays as-is).
     *
     * Returns `null` for [MeasurementUnit.TO_TASTE] or invalid (negative / non-finite) values.
     */
    fun toBaseUnit(quantity: Double, unit: MeasurementUnit): Double? {
        if (!quantity.isFinite() || quantity < 0.0) return null
        if (unit == MeasurementUnit.TO_TASTE) return null
        val factor = unit.factorToBase ?: return quantity
        return quantity * factor
    }

    /**
     * Scales an ingredient [quantity] proportionally from [baseServings] to [targetServings].
     *
     * Returns `null` for [MeasurementUnit.TO_TASTE] or when [quantity] is `null`.
     *
     * @throws IllegalArgumentException if [baseServings] or [targetServings] is not positive,
     * or if a non-null [quantity] is negative or non-finite.
     */
    fun scaleQuantity(
        quantity: Double?,
        unit: MeasurementUnit,
        baseServings: Int,
        targetServings: Int,
    ): Double? {
        require(baseServings > 0) { "baseServings must be > 0, got $baseServings" }
        require(targetServings > 0) { "targetServings must be > 0, got $targetServings" }
        if (unit == MeasurementUnit.TO_TASTE || quantity == null) return null
        require(quantity.isFinite() && quantity >= 0.0) { "quantity must be finite and >= 0, got $quantity" }
        return quantity * (targetServings.toDouble() / baseServings.toDouble())
    }

    /**
     * Promotes a base-unit quantity (`G` or `ML`) to `KG` or `L` when `>= 1000`.
     */
    fun promoteMetricBaseUnit(
        baseQuantity: Double,
        category: UnitCategory,
    ): Pair<Double, MeasurementUnit> {
        require(baseQuantity.isFinite() && baseQuantity >= 0.0) {
            "baseQuantity must be finite and >= 0, got $baseQuantity"
        }
        return when (category) {
            UnitCategory.MASS -> if (baseQuantity >= METRIC_PROMOTION_THRESHOLD) {
                (baseQuantity / MeasurementUnit.KG.factorToBase!!) to MeasurementUnit.KG
            } else {
                baseQuantity to MeasurementUnit.G
            }

            UnitCategory.VOLUME -> if (baseQuantity >= METRIC_PROMOTION_THRESHOLD) {
                (baseQuantity / MeasurementUnit.L.factorToBase!!) to MeasurementUnit.L
            } else {
                baseQuantity to MeasurementUnit.ML
            }

            UnitCategory.COUNT -> baseQuantity to MeasurementUnit.PIECE
            UnitCategory.OTHER -> baseQuantity to MeasurementUnit.PINCH
        }
    }

    /**
     * Converts [quantity] and [unit] to a friendly representation in the requested [system]
     * for display purposes. Non-convertible units (`PIECE`, `PINCH`, `TO_TASTE`) are returned unchanged.
     */
    fun convertForDisplaySystem(
        quantity: Double,
        unit: MeasurementUnit,
        system: UnitSystem,
    ): Pair<Double, MeasurementUnit> {
        if (!quantity.isFinite() || quantity < 0.0) return quantity to unit
        return when (unit.category) {
            UnitCategory.MASS -> {
                val grams = toBaseUnit(quantity, unit) ?: return quantity to unit
                when (system) {
                    UnitSystem.METRIC -> promoteMetricBaseUnit(grams, UnitCategory.MASS)
                    UnitSystem.IMPERIAL -> {
                        val ounces = grams / MeasurementUnit.OZ.factorToBase!!
                        if (ounces >= POUND_THRESHOLD_IN_OUNCES) {
                            (grams / MeasurementUnit.LB.factorToBase!!) to MeasurementUnit.LB
                        } else {
                            ounces to MeasurementUnit.OZ
                        }
                    }
                }
            }

            UnitCategory.VOLUME -> {
                val ml = toBaseUnit(quantity, unit) ?: return quantity to unit
                when (system) {
                    UnitSystem.METRIC -> promoteMetricBaseUnit(ml, UnitCategory.VOLUME)
                    UnitSystem.IMPERIAL -> when {
                        ml >= CUP_THRESHOLD_IN_ML -> (ml / MeasurementUnit.CUP.factorToBase!!) to MeasurementUnit.CUP
                        ml >= TBSP_THRESHOLD_IN_ML -> (ml / MeasurementUnit.TBSP.factorToBase!!) to MeasurementUnit.TBSP
                        else -> (ml / MeasurementUnit.TSP.factorToBase!!) to MeasurementUnit.TSP
                    }
                }
            }

            UnitCategory.COUNT, UnitCategory.OTHER -> quantity to unit
        }
    }

    /**
     * Computes the canonical shopping-list aggregation key (`itemKey`) for an ingredient.
     *
     * - `MASS`, `VOLUME`, `COUNT`: `"$normalizedName|${unit.category.name}"`
     * - `OTHER`: `"$normalizedName|${unit.name}"` (e.g. `"sare|TO_TASTE"`)
     */
    fun shoppingItemKey(normalizedName: String, unit: MeasurementUnit): String = when (unit.category) {
        UnitCategory.MASS, UnitCategory.VOLUME, UnitCategory.COUNT -> "$normalizedName|${unit.category.name}"
        UnitCategory.OTHER -> "$normalizedName|${unit.name}"
    }
}
