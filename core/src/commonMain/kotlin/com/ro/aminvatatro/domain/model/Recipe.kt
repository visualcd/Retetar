package com.ro.aminvatatro.domain.model

import com.ro.aminvatatro.core.text.TextNormalizer
import com.ro.aminvatatro.core.units.MeasurementUnit

/**
 * Categorization label attached to one or more [Recipe]s.
 */
data class Tag(
    val id: String,
    val name: String,
)

/**
 * Single ingredient line inside a [Recipe].
 */
data class Ingredient(
    val id: String,
    val position: Int = 0,
    val name: String,
    val quantity: Double?,
    val unit: MeasurementUnit,
    val aisle: Aisle = Aisle.OTHER,
    val calories: Double? = null,
) {
    /** Canonical normalized name used for diacritic-insensitive search and aggregation. */
    val normalizedName: String
        get() = TextNormalizer.normalize(name)
}

/**
 * Single ordered preparation instruction inside a [Recipe].
 */
data class Step(
    val id: String,
    val position: Int = 0,
    val text: String,
)

/**
 * Core recipe aggregate containing metadata, ingredients, ordered steps, and tags.
 */
data class Recipe(
    val id: String,
    val title: String,
    val description: String = "",
    val baseServings: Int,
    val prepMinutes: Int = 0,
    val cookMinutes: Int = 0,
    val ingredients: List<Ingredient>,
    val steps: List<Step>,
    val tags: List<Tag> = emptyList(),
    val notes: String = "",
    val isFavorite: Boolean = false,
    val createdAt: Long = 0L,
    val updatedAt: Long = 0L,
    val caloriesTotal: Double? = null,
) {
    /** Total preparation plus cooking time in minutes. */
    val totalMinutes: Int
        get() = prepMinutes + cookMinutes

    /** Computed or stored total calories for the whole recipe. */
    val effectiveCaloriesTotal: Double?
        get() {
            if (caloriesTotal != null && caloriesTotal > 0.0) return caloriesTotal
            val ingredientCals = ingredients.mapNotNull { it.calories }
            return if (ingredientCals.isNotEmpty()) ingredientCals.sum() else null
        }

    /** Calories per serving for the base servings count. */
    val caloriesPerServing: Double?
        get() {
            val total = effectiveCaloriesTotal ?: return null
            if (baseServings <= 0) return null
            return total / baseServings
        }

    /** Canonical normalized title used for diacritic-insensitive search and duplicate detection. */
    val normalizedTitle: String
        get() = TextNormalizer.normalize(title)
}
