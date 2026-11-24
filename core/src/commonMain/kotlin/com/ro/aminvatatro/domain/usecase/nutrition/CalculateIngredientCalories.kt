package com.ro.aminvatatro.domain.usecase.nutrition

import com.ro.aminvatatro.core.nutrition.NutritionDatabase
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.data.remote.OpenFoodFactsClient

/**
 * Use case to automatically calculate the estimated calories for an ingredient given its name,
 * quantity, and unit, combining OpenFoodFacts API with the local offline nutritional database.
 */
class CalculateIngredientCalories(
    private val openFoodFactsClient: OpenFoodFactsClient,
) {
    /**
     * Calculates the estimated calories for [name], [quantity], and [unit].
     *
     * 1. Checks local built-in database first for instant offline calculation.
     * 2. If not found in local database or [preferOnline] is true, tries querying OpenFoodFacts.
     * 3. Falls back gracefully to whichever source yields a valid result.
     */
    suspend operator fun invoke(
        name: String,
        quantity: Double?,
        unit: MeasurementUnit,
        preferOnline: Boolean = false,
    ): Double? {
        if (quantity == null || !quantity.isFinite() || quantity <= 0.0) return null
        if (unit == MeasurementUnit.TO_TASTE) return 0.0

        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) return null

        if (!preferOnline) {
            val localCalc = NutritionDatabase.calculateCalories(trimmedName, quantity, unit)
            if (localCalc != null) return localCalc
        }

        // Try OpenFoodFacts
        val offResult = openFoodFactsClient.searchKcalPer100g(trimmedName)
        if (offResult != null) {
            val fromOff = NutritionDatabase.calculateCalories(
                name = trimmedName,
                quantity = quantity,
                unit = unit,
                kcalPer100gOverride = offResult.kcalPer100g,
            )
            if (fromOff != null) return fromOff
        }

        // Final fallback to local
        return NutritionDatabase.calculateCalories(trimmedName, quantity, unit)
    }
}
