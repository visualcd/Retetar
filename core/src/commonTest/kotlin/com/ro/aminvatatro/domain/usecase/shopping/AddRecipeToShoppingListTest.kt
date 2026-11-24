package com.ro.aminvatatro.domain.usecase.shopping

import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.ShoppingItem
import com.ro.aminvatatro.domain.model.ShoppingItemSource
import com.ro.aminvatatro.domain.usecase.FakeClock
import com.ro.aminvatatro.domain.usecase.FakeShoppingRepository
import com.ro.aminvatatro.domain.usecase.SequenceIdGenerator
import com.ro.aminvatatro.domain.usecase.sampleRecipe
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDate
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AddRecipeToShoppingListTest {

    private val targetDate = LocalDate(2026, 3, 4) // Wednesday
    private val weekMonday = LocalDate(2026, 3, 2)

    @Test
    fun addsRecipeIngredientsToEmptyShoppingListWithProperScaling() = runTest {
        val shoppingRepo = FakeShoppingRepository()
        val clock = FakeClock(currentDate = targetDate)
        val idGen = SequenceIdGenerator("shop")
        val addRecipeToShoppingList = AddRecipeToShoppingList(shoppingRepo, idGen, clock)

        val recipe = sampleRecipe(
            id = "r1",
            title = "Omletă",
            baseServings = 2,
            ingredients = listOf(
                Ingredient("i1", 0, "Ouă", 4.0, MeasurementUnit.PIECE, Aisle.DAIRY),
                Ingredient("i2", 1, "Lapte", 100.0, MeasurementUnit.ML, Aisle.DAIRY),
                Ingredient("i3", 2, "Sare", null, MeasurementUnit.TO_TASTE, Aisle.SPICES),
            ),
        )

        // Scale from 2 to 4 servings
        val result = addRecipeToShoppingList(recipe, servings = 4, targetDate = targetDate)
        assertEquals(3, result.size)

        val oua = result.first { it.normalizedName == "oua" }
        assertEquals(8.0, oua.quantity)
        assertEquals(MeasurementUnit.PIECE, oua.unit)
        assertEquals(Aisle.DAIRY, oua.aisle)
        assertFalse(oua.checked)
        assertEquals(ShoppingItemSource.AUTO, oua.source)

        val lapte = result.first { it.normalizedName == "lapte" }
        assertEquals(200.0, lapte.quantity)
        assertEquals(MeasurementUnit.ML, lapte.unit)

        val sare = result.first { it.normalizedName == "sare" }
        assertNull(sare.quantity)
        assertEquals(MeasurementUnit.TO_TASTE, sare.unit)
    }

    @Test
    fun mergesWithExistingItemsPreservingCheckedStateAndManualItems() = runTest {
        val shoppingRepo = FakeShoppingRepository()
        val clock = FakeClock(currentDate = targetDate)
        val idGen = SequenceIdGenerator("shop")
        val addRecipeToShoppingList = AddRecipeToShoppingList(shoppingRepo, idGen, clock)

        // Pre-populate with a checked item and a manual item
        val existingOua = ShoppingItem(
            id = "existing-1",
            weekStart = weekMonday,
            itemKey = "oua|COUNT",
            name = "Ouă",
            normalizedName = "oua",
            quantity = 2.0,
            unit = MeasurementUnit.PIECE,
            aisle = Aisle.DAIRY,
            source = ShoppingItemSource.AUTO,
            checked = true,
        )
        val manualItem = ShoppingItem(
            id = "manual-1",
            weekStart = weekMonday,
            itemKey = "ciocolata|OTHER",
            name = "Ciocolată",
            normalizedName = "ciocolata",
            quantity = 1.0,
            unit = MeasurementUnit.PIECE,
            aisle = Aisle.PANTRY,
            source = ShoppingItemSource.MANUAL,
            checked = false,
        )
        shoppingRepo.replaceItemsForWeek(weekMonday, listOf(existingOua, manualItem))

        val recipe = sampleRecipe(
            id = "r2",
            title = "Prăjitură",
            baseServings = 1,
            ingredients = listOf(
                Ingredient("i1", 0, "ouă", 3.0, MeasurementUnit.PIECE, Aisle.DAIRY),
                Ingredient("i2", 1, "Făină", 600.0, MeasurementUnit.G, Aisle.PANTRY),
            ),
        )

        val result = addRecipeToShoppingList(recipe, servings = 1, targetDate = targetDate)
        assertEquals(3, result.size)

        val oua = result.first { it.normalizedName == "oua" }
        assertEquals("existing-1", oua.id)
        assertEquals(5.0, oua.quantity) // 2 existing + 3 new = 5
        assertTrue(oua.checked, "Checked state of existing item must be preserved")

        val manual = result.first { it.id == "manual-1" }
        assertEquals(ShoppingItemSource.MANUAL, manual.source)

        val faina = result.first { it.normalizedName == "faina" }
        assertEquals(600.0, faina.quantity)
        assertEquals(MeasurementUnit.G, faina.unit)
    }

    @Test
    fun promotesGramsToKilogramsOnAggregation() = runTest {
        val shoppingRepo = FakeShoppingRepository()
        val clock = FakeClock(currentDate = targetDate)
        val idGen = SequenceIdGenerator("shop")
        val addRecipeToShoppingList = AddRecipeToShoppingList(shoppingRepo, idGen, clock)

        val existingFaina = ShoppingItem(
            id = "faina-1",
            weekStart = weekMonday,
            itemKey = "faina|MASS",
            name = "Făină",
            normalizedName = "faina",
            quantity = 500.0,
            unit = MeasurementUnit.G,
            aisle = Aisle.PANTRY,
            source = ShoppingItemSource.AUTO,
            checked = false,
        )
        shoppingRepo.replaceItemsForWeek(weekMonday, listOf(existingFaina))

        val recipe = sampleRecipe(
            id = "r3",
            title = "Cozonac",
            baseServings = 1,
            ingredients = listOf(
                Ingredient("i1", 0, "faina", 700.0, MeasurementUnit.G, Aisle.PANTRY),
            ),
        )

        val result = addRecipeToShoppingList(recipe, servings = 1, targetDate = targetDate)
        val faina = result.first { it.normalizedName == "faina" }
        // 500g + 700g = 1200g -> promoted to 1.2 kg
        assertEquals(1.2, faina.quantity)
        assertEquals(MeasurementUnit.KG, faina.unit)
    }
}
