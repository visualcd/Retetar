package com.ro.aminvatatro.core.units

/**
 * Culinary measurement unit with its [UnitCategory] and linear conversion factor to the category's
 * base unit (`G` for [UnitCategory.MASS], `ML` for [UnitCategory.VOLUME], `PIECE` for [UnitCategory.COUNT]).
 *
 * Units in [UnitCategory.OTHER] do not define a cross-unit conversion factor (`factorToBase == null`).
 */
enum class MeasurementUnit(
    val category: UnitCategory,
    val factorToBase: Double?,
) {
    /** Gram (base mass unit). */
    G(UnitCategory.MASS, 1.0),

    /** Kilogram (1000 g). */
    KG(UnitCategory.MASS, 1000.0),

    /** Ounce (≈ 28.3495 g). */
    OZ(UnitCategory.MASS, 28.3495),

    /** Pound (≈ 453.592 g). */
    LB(UnitCategory.MASS, 453.592),

    /** Milliliter (base volume unit). */
    ML(UnitCategory.VOLUME, 1.0),

    /** Liter (1000 ml). */
    L(UnitCategory.VOLUME, 1000.0),

    /** Teaspoon (5 ml). */
    TSP(UnitCategory.VOLUME, 5.0),

    /** Tablespoon (15 ml). */
    TBSP(UnitCategory.VOLUME, 15.0),

    /** Cup (240 ml). */
    CUP(UnitCategory.VOLUME, 240.0),

    /** Discrete count (piece). */
    PIECE(UnitCategory.COUNT, 1.0),

    /** Pinch (non-convertible, keeps numeric quantity). */
    PINCH(UnitCategory.OTHER, null),

    /** To taste (non-convertible, quantity must be `null`). */
    TO_TASTE(UnitCategory.OTHER, null);

    /** Canonical base unit for this unit's [category]. */
    val baseUnit: MeasurementUnit
        get() = when (category) {
            UnitCategory.MASS -> G
            UnitCategory.VOLUME -> ML
            UnitCategory.COUNT -> PIECE
            UnitCategory.OTHER -> this
        }

    /** `true` when this unit requires a positive numeric quantity; `false` only for [TO_TASTE]. */
    val requiresQuantity: Boolean
        get() = this != TO_TASTE
}
