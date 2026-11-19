package com.ro.aminvatatro.data.demo

import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.validation.RecipeValidator
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class DemoRecipesTest {

    private val allowedMetricUnits = setOf(
        MeasurementUnit.G,
        MeasurementUnit.KG,
        MeasurementUnit.ML,
        MeasurementUnit.L,
        MeasurementUnit.PIECE,
        MeasurementUnit.PINCH,
        MeasurementUnit.TO_TASTE,
    )

    @Test
    fun demoRecipesCountValidationMetricUnitsAndSharedIngredients() {
        val recipes = DemoRecipes.create()
        assertTrue(recipes.size in 8..10, "Expected 8-10 demo recipes, got ${recipes.size}")
        assertEquals(recipes.size, recipes.map { it.id }.toSet().size, "Demo recipe IDs must be unique")

        recipes.forEach { recipe ->
            val validation = RecipeValidator.validate(recipe)
            assertTrue(
                validation.isValid,
                "Demo recipe '${recipe.title}' failed validation: ${validation.errorsOrEmpty()}",
            )
            recipe.ingredients.forEach { ingredient ->
                assertTrue(
                    ingredient.unit in allowedMetricUnits,
                    "Demo recipe '${recipe.title}' uses non-metric unit ${ingredient.unit}",
                )
            }
        }

        // At least 3 recipes must share common ingredients (făină, ouă, ulei, ceapă)
        val requiredShared = listOf("faina", "oua", "ulei", "ceapa")
        requiredShared.forEach { normalized ->
            val count = recipes.count { recipe ->
                recipe.ingredients.any { it.normalizedName == normalized }
            }
            assertTrue(
                count >= 3,
                "Expected at least 3 demo recipes sharing '$normalized', found $count",
            )
        }
    }
}
