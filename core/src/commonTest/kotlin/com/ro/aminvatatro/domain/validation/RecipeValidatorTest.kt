package com.ro.aminvatatro.domain.validation

import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.Step
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertTrue

class RecipeValidatorTest {

    private fun validRecipe(
        title: String = "Clătite",
        baseServings: Int = 4,
        prepMinutes: Int = 10,
        cookMinutes: Int = 15,
        ingredients: List<Ingredient> = listOf(
            Ingredient(
                id = "ing-1",
                position = 0,
                name = "Făină",
                quantity = 200.0,
                unit = MeasurementUnit.G,
                aisle = Aisle.PANTRY,
            ),
        ),
        steps: List<Step> = listOf(
            Step(id = "step-1", position = 0, text = "Amestecă ingredientele."),
        ),
    ): Recipe = Recipe(
        id = "rec-1",
        title = title,
        baseServings = baseServings,
        prepMinutes = prepMinutes,
        cookMinutes = cookMinutes,
        ingredients = ingredients,
        steps = steps,
    )

    @Test
    fun acceptsValidRecipe() {
        val result = RecipeValidator.validate(validRecipe())
        assertEquals(ValidationResult.Valid, result)
        assertTrue(result.isValid)
        assertTrue(result.errorsOrEmpty().isEmpty())
    }

    @Test
    fun validatesTitleBoundaries() {
        // Min boundary: 0 (invalid) vs 1 (valid)
        val blank = RecipeValidator.validate(validRecipe(title = "   "))
        assertFalse(blank.isValid)
        assertTrue(RecipeValidationError.TitleBlank in blank.errorsOrEmpty())

        assertTrue(RecipeValidator.validate(validRecipe(title = "A")).isValid)

        // Max boundary: 100 (valid) vs 101 (invalid)
        assertTrue(RecipeValidator.validate(validRecipe(title = "a".repeat(100))).isValid)
        assertTrue(RecipeValidator.validate(validRecipe(title = "  " + "a".repeat(100) + "  ")).isValid)

        val tooLong = RecipeValidator.validate(validRecipe(title = "a".repeat(101)))
        assertIs<ValidationResult.Invalid>(tooLong)
        assertTrue(
            RecipeValidationError.TitleTooLong(maxLength = 100, actualLength = 101) in tooLong.errors,
        )
    }

    @Test
    fun validatesServingsBoundaries() {
        assertFalse(RecipeValidator.validate(validRecipe(baseServings = 0)).isValid)
        assertFalse(RecipeValidator.validate(validRecipe(baseServings = -2)).isValid)
        assertTrue(RecipeValidator.validate(validRecipe(baseServings = 1)).isValid)
        assertTrue(RecipeValidator.validate(validRecipe(baseServings = 100)).isValid)

        val overMax = RecipeValidator.validate(validRecipe(baseServings = 101))
        assertTrue(
            RecipeValidationError.InvalidServings(min = 1, max = 100, actual = 101) in overMax.errorsOrEmpty(),
        )
    }

    @Test
    fun validatesPrepAndCookTimeBoundaries() {
        assertTrue(RecipeValidator.validate(validRecipe(prepMinutes = 0, cookMinutes = 0)).isValid)
        assertTrue(RecipeValidator.validate(validRecipe(prepMinutes = 1440, cookMinutes = 1440)).isValid)

        val negPrep = RecipeValidator.validate(validRecipe(prepMinutes = -1))
        assertTrue(
            RecipeValidationError.InvalidPrepMinutes(min = 0, max = 1440, actual = -1) in negPrep.errorsOrEmpty(),
        )
        val overPrep = RecipeValidator.validate(validRecipe(prepMinutes = 1441))
        assertTrue(
            RecipeValidationError.InvalidPrepMinutes(min = 0, max = 1440, actual = 1441) in overPrep.errorsOrEmpty(),
        )

        val negCook = RecipeValidator.validate(validRecipe(cookMinutes = -1))
        assertTrue(
            RecipeValidationError.InvalidCookMinutes(min = 0, max = 1440, actual = -1) in negCook.errorsOrEmpty(),
        )
        val overCook = RecipeValidator.validate(validRecipe(cookMinutes = 1441))
        assertTrue(
            RecipeValidationError.InvalidCookMinutes(min = 0, max = 1440, actual = 1441) in overCook.errorsOrEmpty(),
        )
    }

    @Test
    fun validatesIngredientsListAndQuantities() {
        val emptyIngs = RecipeValidator.validate(validRecipe(ingredients = emptyList()))
        assertTrue(RecipeValidationError.EmptyIngredients in emptyIngs.errorsOrEmpty())

        val blankName = RecipeValidator.validate(
            validRecipe(
                ingredients = listOf(Ingredient("1", 0, "  ", 100.0, MeasurementUnit.G)),
            ),
        )
        assertTrue(RecipeValidationError.IngredientNameBlank(0) in blankName.errorsOrEmpty())

        // Quantity > 0 required for non-TO_TASTE units
        val nullQty = RecipeValidator.validate(
            validRecipe(
                ingredients = listOf(Ingredient("1", 0, "Făină", null, MeasurementUnit.G)),
            ),
        )
        assertTrue(RecipeValidationError.InvalidIngredientQuantity(0) in nullQty.errorsOrEmpty())

        val zeroQty = RecipeValidator.validate(
            validRecipe(
                ingredients = listOf(Ingredient("1", 0, "Făină", 0.0, MeasurementUnit.G)),
            ),
        )
        assertTrue(RecipeValidationError.InvalidIngredientQuantity(0) in zeroQty.errorsOrEmpty())

        val negQty = RecipeValidator.validate(
            validRecipe(
                ingredients = listOf(Ingredient("1", 0, "Făină", -10.0, MeasurementUnit.G)),
            ),
        )
        assertTrue(RecipeValidationError.InvalidIngredientQuantity(0) in negQty.errorsOrEmpty())

        val nanQty = RecipeValidator.validate(
            validRecipe(
                ingredients = listOf(Ingredient("1", 0, "Făină", Double.NaN, MeasurementUnit.G)),
            ),
        )
        assertTrue(RecipeValidationError.InvalidIngredientQuantity(0) in nanQty.errorsOrEmpty())

        // PINCH requires positive quantity
        assertTrue(
            RecipeValidator.validate(
                validRecipe(ingredients = listOf(Ingredient("1", 0, "Sare", 1.0, MeasurementUnit.PINCH))),
            ).isValid,
        )
        assertFalse(
            RecipeValidator.validate(
                validRecipe(ingredients = listOf(Ingredient("1", 0, "Sare", null, MeasurementUnit.PINCH))),
            ).isValid,
        )

        // TO_TASTE allows null quantity, rejects non-null quantity
        assertTrue(
            RecipeValidator.validate(
                validRecipe(ingredients = listOf(Ingredient("1", 0, "Piper", null, MeasurementUnit.TO_TASTE))),
            ).isValid,
        )
        val toTasteWithQty = RecipeValidator.validate(
            validRecipe(ingredients = listOf(Ingredient("1", 0, "Piper", 1.0, MeasurementUnit.TO_TASTE))),
        )
        assertTrue(RecipeValidationError.QuantityNotAllowedForToTaste(0) in toTasteWithQty.errorsOrEmpty())
    }

    @Test
    fun validatesStepsAndCollectsMultipleErrors() {
        val emptySteps = RecipeValidator.validate(validRecipe(steps = emptyList()))
        assertTrue(RecipeValidationError.EmptySteps in emptySteps.errorsOrEmpty())

        val blankStep = RecipeValidator.validate(
            validRecipe(
                steps = listOf(
                    Step("s1", 0, "Fierbe apa."),
                    Step("s2", 1, "   "),
                ),
            ),
        )
        assertTrue(RecipeValidationError.StepTextBlank(1) in blankStep.errorsOrEmpty())

        val multiInvalid = RecipeValidator.validate(
            validRecipe(
                title = "",
                baseServings = 0,
                prepMinutes = -5,
                cookMinutes = 2000,
                ingredients = emptyList(),
                steps = emptyList(),
            ),
        )
        assertEquals(6, multiInvalid.errorsOrEmpty().size)
    }
}
