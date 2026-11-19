package com.ro.aminvatatro.domain.usecase.recipe

import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.core.units.UnitConverter
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.UnitSystem
import com.ro.aminvatatro.domain.validation.RecipeValidator

/**
 * Ingredient line with its quantity proportionally scaled for a target serving count
 * and optionally converted to a preferred display [UnitSystem].
 */
data class ScaledIngredient(
    val ingredient: Ingredient,
    val quantity: Double?,
    val unit: MeasurementUnit,
)

/**
 * Outcome of scaling a [Recipe] to a target number of servings.
 */
sealed interface ScaleRecipeResult {
    /** Scaling succeeded. */
    data class Success(
        val recipe: Recipe,
        val targetServings: Int,
        val ingredients: List<ScaledIngredient>,
    ) : ScaleRecipeResult

    /** Either [actual] target servings or recipe base servings were outside valid bounds. */
    data class InvalidServings(
        val min: Int,
        val max: Int,
        val actual: Int,
    ) : ScaleRecipeResult
}

/**
 * Proportionally scales all ingredient quantities of a [Recipe] from its `baseServings`
 * to a requested `targetServings` without rounding (rounding occurs only at display time).
 */
class ScaleRecipe {
    /**
     * Scales [recipe] to [targetServings], optionally adapting mass/volume units to
     * [displayUnitSystem] when non-null.
     */
    operator fun invoke(
        recipe: Recipe,
        targetServings: Int,
        displayUnitSystem: UnitSystem? = null,
    ): ScaleRecipeResult {
        if (targetServings !in RecipeValidator.MIN_SERVINGS..RecipeValidator.MAX_SERVINGS) {
            return ScaleRecipeResult.InvalidServings(
                min = RecipeValidator.MIN_SERVINGS,
                max = RecipeValidator.MAX_SERVINGS,
                actual = targetServings,
            )
        }
        if (recipe.baseServings !in RecipeValidator.MIN_SERVINGS..RecipeValidator.MAX_SERVINGS) {
            return ScaleRecipeResult.InvalidServings(
                min = RecipeValidator.MIN_SERVINGS,
                max = RecipeValidator.MAX_SERVINGS,
                actual = recipe.baseServings,
            )
        }

        val scaledIngredients = recipe.ingredients.map { ingredient ->
            val rawScaled = UnitConverter.scaleQuantity(
                quantity = ingredient.quantity,
                unit = ingredient.unit,
                baseServings = recipe.baseServings,
                targetServings = targetServings,
            )
            val (finalQuantity, finalUnit) = if (rawScaled != null && displayUnitSystem != null) {
                UnitConverter.convertForDisplaySystem(rawScaled, ingredient.unit, displayUnitSystem)
            } else {
                rawScaled to ingredient.unit
            }
            ScaledIngredient(
                ingredient = ingredient,
                quantity = finalQuantity,
                unit = finalUnit,
            )
        }

        return ScaleRecipeResult.Success(
            recipe = recipe,
            targetServings = targetServings,
            ingredients = scaledIngredients,
        )
    }
}
