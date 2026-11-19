package com.ro.aminvatatro.domain.usecase

import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.model.ShoppingItemSource
import com.ro.aminvatatro.domain.usecase.shopping.AddManualItem
import com.ro.aminvatatro.domain.usecase.shopping.AddManualItemResult
import com.ro.aminvatatro.domain.usecase.shopping.ClearChecked
import com.ro.aminvatatro.domain.usecase.shopping.GenerateShoppingList
import com.ro.aminvatatro.domain.usecase.shopping.RemoveItem
import com.ro.aminvatatro.domain.usecase.shopping.ToggleItem
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertIs
import kotlin.test.assertNotNull
import kotlin.test.assertNull
import kotlin.test.assertTrue

class ShoppingListUseCasesTest {

    private val weekMonday = LocalDate(2026, 3, 2)
    private val weekWednesday = LocalDate(2026, 3, 4)
    private val weekSunday = LocalDate(2026, 3, 8)
    private val nextWeekMonday = LocalDate(2026, 3, 9)

    @Test
    fun aggregatesUnitsDiacriticsVolumeSeparateCategoriesAndToTaste() = runTest {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val shoppingRepo = FakeShoppingRepository()
        val generate = GenerateShoppingList(planRepo, recipeRepo, shoppingRepo, SequenceIdGenerator("shop"))

        val recipeA = sampleRecipe(
            id = "rec-a",
            title = "Clătite",
            baseServings = 4,
            ingredients = listOf(
                Ingredient("a1", 0, "Făină", 500.0, MeasurementUnit.G, Aisle.PANTRY),
                Ingredient("a2", 1, "Lapte", 250.0, MeasurementUnit.ML, Aisle.DAIRY),
                Ingredient("a3", 2, "Ouă", 3.0, MeasurementUnit.PIECE, Aisle.DAIRY),
                Ingredient("a4", 3, "Ulei", 2.0, MeasurementUnit.TBSP, Aisle.PANTRY),
                Ingredient("a5", 4, "Sare", null, MeasurementUnit.TO_TASTE, Aisle.SPICES),
                Ingredient("a6", 5, "Piper", 1.0, MeasurementUnit.PINCH, Aisle.SPICES),
            ),
        )
        val recipeB = sampleRecipe(
            id = "rec-b",
            title = "Pâine de casă",
            baseServings = 2,
            ingredients = listOf(
                // Different case & no diacritics + different mass unit -> merges with Făină (500 g + 1 kg = 1.5 kg)
                Ingredient("b1", 0, "faina", 1.0, MeasurementUnit.KG, Aisle.OTHER),
                // Volume mixed units: 250 ml + 1 l = 1.25 l
                Ingredient("b2", 1, "Lapte", 1.0, MeasurementUnit.L, Aisle.DAIRY),
                // Same ingredient name "Ouă", but in MASS instead of COUNT -> MUST remain separate row!
                Ingredient("b3", 2, "ouă", 100.0, MeasurementUnit.G, Aisle.DAIRY),
                // Same unit (TBSP) across both recipes -> preserves TBSP (2 + 3 = 5 TBSP)
                Ingredient("b4", 3, "Ulei", 3.0, MeasurementUnit.TBSP, Aisle.PANTRY),
                // Second TO_TASTE sare -> MUST still appear once with null quantity
                Ingredient("b5", 4, "Sare", null, MeasurementUnit.TO_TASTE, Aisle.SPICES),
            ),
        )
        // Recipe scheduled next week -> must NOT affect this week's list
        val recipeNextWeek = sampleRecipe(
            id = "rec-next",
            title = "Săptămâna viitoare",
            baseServings = 1,
            ingredients = listOf(
                Ingredient("n1", 0, "Zahar", 999.0, MeasurementUnit.G, Aisle.PANTRY),
            ),
        )
        recipeRepo.saveRecipes(listOf(recipeA, recipeB, recipeNextWeek))

        planRepo.upsertEntry(MealPlanEntry("e1", weekMonday, MealSlot.BREAKFAST, "rec-a", servings = 4))
        planRepo.upsertEntry(MealPlanEntry("e2", weekSunday, MealSlot.DINNER, "rec-b", servings = 2))
        planRepo.upsertEntry(MealPlanEntry("e3", nextWeekMonday, MealSlot.LUNCH, "rec-next", servings = 1))

        // Pass Wednesday to confirm normalization to Monday
        val items = generate(weekWednesday)

        val flour = items.single { it.itemKey == "faina|MASS" }
        assertEquals(1.5, flour.quantity!!, 1e-6)
        assertEquals(MeasurementUnit.KG, flour.unit)
        assertEquals(Aisle.PANTRY, flour.aisle)
        assertEquals(weekMonday, flour.weekStart)

        val milk = items.single { it.itemKey == "lapte|VOLUME" }
        assertEquals(1.25, milk.quantity!!, 1e-6)
        assertEquals(MeasurementUnit.L, milk.unit)

        val oil = items.single { it.itemKey == "ulei|VOLUME" }
        assertEquals(5.0, oil.quantity!!, 1e-6)
        assertEquals(MeasurementUnit.TBSP, oil.unit)

        val eggsCount = items.single { it.itemKey == "oua|COUNT" }
        assertEquals(3.0, eggsCount.quantity!!, 1e-6)
        assertEquals(MeasurementUnit.PIECE, eggsCount.unit)

        val eggsMass = items.single { it.itemKey == "oua|MASS" }
        assertEquals(100.0, eggsMass.quantity!!, 1e-6)
        assertEquals(MeasurementUnit.G, eggsMass.unit)

        val salt = items.single { it.itemKey == "sare|TO_TASTE" }
        assertNull(salt.quantity)
        assertEquals(MeasurementUnit.TO_TASTE, salt.unit)

        val pepper = items.single { it.itemKey == "piper|PINCH" }
        assertEquals(1.0, pepper.quantity!!, 1e-6)
        assertEquals(MeasurementUnit.PINCH, pepper.unit)

        assertTrue(items.none { it.normalizedName == "zahar" })
    }

    @Test
    fun scalesIngredientsProportionallyBeforeAggregating() = runTest {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val shoppingRepo = FakeShoppingRepository()
        val generate = GenerateShoppingList(planRepo, recipeRepo, shoppingRepo, SequenceIdGenerator("s"))

        val recipe = sampleRecipe(
            id = "rec-scale",
            baseServings = 4,
            ingredients = listOf(
                Ingredient("i1", 0, "Paste", 400.0, MeasurementUnit.G, Aisle.PANTRY),
                Ingredient("i2", 1, "Ulei", 200.0, MeasurementUnit.ML, Aisle.PANTRY),
            ),
        )
        recipeRepo.saveRecipe(recipe)
        // Scheduled for 6 servings (factor 1.5) -> 600 g paste, 300 ml ulei
        planRepo.upsertEntry(MealPlanEntry("e1", weekMonday, MealSlot.LUNCH, "rec-scale", servings = 6))

        val items = generate(weekMonday)
        assertEquals(600.0, items.single { it.itemKey == "paste|MASS" }.quantity!!, 1e-6)
        assertEquals(300.0, items.single { it.itemKey == "ulei|VOLUME" }.quantity!!, 1e-6)
    }

    @Test
    fun regenerationPreservesCheckedAutoAndManualItemsAndDropsRemovedRecipeItems() = runTest {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val shoppingRepo = FakeShoppingRepository()
        val ids = SequenceIdGenerator("sh")

        val generate = GenerateShoppingList(planRepo, recipeRepo, shoppingRepo, ids)
        val toggleItem = ToggleItem(shoppingRepo)
        val addManualItem = AddManualItem(shoppingRepo, ids)

        val r1 = sampleRecipe(
            id = "r1",
            baseServings = 2,
            ingredients = listOf(
                Ingredient("i1", 0, "Făină", 300.0, MeasurementUnit.G, Aisle.PANTRY),
                Ingredient("i2", 1, "Lapte", 500.0, MeasurementUnit.ML, Aisle.DAIRY),
            ),
        )
        val r2 = sampleRecipe(
            id = "r2",
            baseServings = 2,
            ingredients = listOf(
                Ingredient("i3", 0, "Roșii", 4.0, MeasurementUnit.PIECE, Aisle.PRODUCE),
            ),
        )
        recipeRepo.saveRecipes(listOf(r1, r2))
        planRepo.upsertEntry(MealPlanEntry("e1", weekMonday, MealSlot.LUNCH, "r1", servings = 2))
        planRepo.upsertEntry(MealPlanEntry("e2", weekWednesday, MealSlot.DINNER, "r2", servings = 2))

        val initial = generate(weekMonday)
        val flourInitial = initial.single { it.itemKey == "faina|MASS" }
        assertNotNull(initial.firstOrNull { it.itemKey == "rosii|COUNT" })

        // Check flour and add a manual item
        toggleItem(flourInitial.id, checked = true)
        val manual = assertIs<AddManualItemResult.Success>(
            addManualItem(weekWednesday, name = "Șervețele", quantity = 2.0, unit = MeasurementUnit.PIECE, aisle = Aisle.OTHER),
        ).item

        // Change servings on r1 (2 -> 4) and remove r2 from plan
        planRepo.updateServings("e1", servings = 4)
        planRepo.deleteEntry("e2")

        val regenerated = generate(weekMonday)

        val flourAfter = regenerated.single { it.itemKey == "faina|MASS" }
        assertEquals(flourInitial.id, flourAfter.id)
        assertEquals(600.0, flourAfter.quantity!!, 1e-6)
        assertTrue(flourAfter.checked, "Checked state on AUTO item must survive regeneration")

        val milkAfter = regenerated.single { it.itemKey == "lapte|VOLUME" }
        assertEquals(1000.0, milkAfter.quantity!!, 1e-6)
        assertFalse(milkAfter.checked)

        assertTrue(
            regenerated.none { it.itemKey == "rosii|COUNT" },
            "AUTO items from removed recipes must disappear on regeneration",
        )

        val manualAfter = regenerated.single { it.id == manual.id }
        assertEquals(ShoppingItemSource.MANUAL, manualAfter.source)
        assertEquals("Șervețele", manualAfter.name)
        assertEquals(2.0, manualAfter.quantity)
    }

    @Test
    fun manualItemValidationRemoveAndClearCheckedWorkPerWeek() = runTest {
        val shoppingRepo = FakeShoppingRepository()
        val ids = SequenceIdGenerator("man")
        val addManualItem = AddManualItem(shoppingRepo, ids)
        val toggleItem = ToggleItem(shoppingRepo)
        val removeItem = RemoveItem(shoppingRepo)
        val clearChecked = ClearChecked(shoppingRepo)

        assertIs<AddManualItemResult.NameBlank>(addManualItem(weekMonday, name = "   "))
        assertIs<AddManualItemResult.NameTooLong>(addManualItem(weekMonday, name = "a".repeat(101)))
        assertIs<AddManualItemResult.InvalidQuantity>(addManualItem(weekMonday, name = "Pâine", quantity = 0.0))
        assertIs<AddManualItemResult.InvalidQuantity>(
            addManualItem(weekMonday, name = "Sare", quantity = 1.0, unit = MeasurementUnit.TO_TASTE),
        )

        val bread = assertIs<AddManualItemResult.Success>(
            addManualItem(weekWednesday, name = "Pâine", quantity = null, unit = MeasurementUnit.PIECE, aisle = Aisle.BAKERY),
        ).item
        val coffee = assertIs<AddManualItemResult.Success>(
            addManualItem(weekMonday, name = "Cafea", quantity = 250.0, unit = MeasurementUnit.G, aisle = Aisle.PANTRY),
        ).item
        val nextWeekItem = assertIs<AddManualItemResult.Success>(
            addManualItem(nextWeekMonday, name = "Ceai", quantity = 1.0, unit = MeasurementUnit.PIECE),
        ).item

        toggleItem(bread.id, checked = true)
        toggleItem(nextWeekItem.id, checked = true)
        clearChecked(weekSunday)

        assertEquals(listOf(coffee), shoppingRepo.getItemsForWeek(weekMonday))
        assertEquals(listOf(nextWeekItem.copy(checked = true)), shoppingRepo.getItemsForWeek(nextWeekMonday))

        removeItem(coffee.id)
        assertTrue(shoppingRepo.getItemsForWeek(weekMonday).isEmpty())
    }
}
