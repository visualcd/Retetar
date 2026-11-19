package com.ro.aminvatatro.core.units

/**
 * Physical or culinary category of a [MeasurementUnit].
 *
 * Conversions are strictly permitted only within the same category (`MASS` or `VOLUME`).
 */
enum class UnitCategory {
    /** Mass / weight units (base unit: grams). */
    MASS,

    /** Liquid / volume units (base unit: milliliters). */
    VOLUME,

    /** Discrete item counts (base unit: piece). */
    COUNT,

    /** Non-convertible culinary measures (`PINCH`, `TO_TASTE`). */
    OTHER,
}
