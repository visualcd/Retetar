package com.ro.aminvatatro.presentation

import app.cash.turbine.test
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.AppLanguage
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.model.UnitSystem
import com.ro.aminvatatro.domain.usecase.shopping.AddManualItem
import com.ro.aminvatatro.domain.usecase.shopping.ClearChecked
import com.ro.aminvatatro.domain.usecase.shopping.GenerateShoppingList
import com.ro.aminvatatro.domain.usecase.shopping.RemoveItem
import com.ro.aminvatatro.domain.usecase.shopping.ToggleItem
import com.ro.aminvatatro.presentation.shopping.ManualItemError
import com.ro.aminvatatro.presentation.shopping.ShoppingListUiEvent
import com.ro.aminvatatro.presentation.shopping.ShoppingListViewModel
import com.ro.aminvatatro.presentation.shopping.formatShoppingListAsText
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlinx.datetime.LocalDate
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun regenerateToggleManualItemClearCheckedAndFormatTextWork() = runTest(testDispatcher) {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val shoppingRepo = FakeShoppingRepository()
        val settingsRepo = FakeSettingsRepository()
        val clock = FakeClock(currentDate = LocalDate(2026, 3, 4)) // Week starting 2026-03-02
        val idGen = SequenceIdGenerator("sh")

        recipeRepo.recipesFlow.value = listOf(sampleRecipe(id = "r1", baseServings = 4))
        planRepo.entriesFlow.value = listOf(
            MealPlanEntry("e1", LocalDate(2026, 3, 2), MealSlot.LUNCH, "r1", 4),
        )

        val vm = ShoppingListViewModel(
            shoppingRepository = shoppingRepo,
            settingsRepository = settingsRepo,
            generateShoppingList = GenerateShoppingList(planRepo, recipeRepo, shoppingRepo, idGen),
            toggleItem = ToggleItem(shoppingRepo),
            addManualItem = AddManualItem(shoppingRepo, idGen),
            removeItem = RemoveItem(shoppingRepo),
            clearChecked = ClearChecked(shoppingRepo),
            clock = clock,
        )
        advanceUntilIdle()

        vm.events.test {
            vm.regenerate()
            assertEquals(ShoppingListUiEvent.Regenerated, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
        advanceUntilIdle()

        assertEquals(3, vm.uiState.value.items.size)
        assertTrue(vm.uiState.value.itemsByAisle.containsKey(Aisle.PANTRY))
        assertTrue(vm.uiState.value.itemsByAisle.containsKey(Aisle.DAIRY))

        // Toggle first item checked
        val firstItem = vm.uiState.value.items.first()
        vm.onToggleItem(firstItem.id, true)
        advanceUntilIdle()
        assertEquals(1, vm.uiState.value.checkedCount)

        // Plain-text formatting includes aisle header and [x] marker
        val formatted = formatShoppingListAsText(
            itemsByAisle = vm.uiState.value.itemsByAisle,
            aisleLabel = { it.name },
            unitLabel = { it.name.lowercase() },
            language = AppLanguage.RO,
            unitSystem = UnitSystem.METRIC,
        )
        assertTrue(formatted.contains("[x]"))
        assertTrue(formatted.contains("[ ]"))

        // Manual item validation & creation
        vm.openAddManualDialog()
        vm.onManualNameChange("   ")
        vm.submitManualItem()
        advanceUntilIdle()
        assertEquals(ManualItemError.NameBlank, vm.uiState.value.manualError)

        vm.onManualNameChange("Șervețele")
        vm.onManualQuantityChange("2")
        vm.onManualUnitChange(MeasurementUnit.PIECE)
        vm.onManualAisleChange(Aisle.OTHER)
        vm.submitManualItem()
        advanceUntilIdle()
        assertFalse(vm.uiState.value.showAddManualDialog)
        assertEquals(4, vm.uiState.value.items.size)

        // Clear checked removes only the checked item
        vm.requestClearChecked()
        assertTrue(vm.uiState.value.showClearCheckedDialog)
        vm.confirmClearChecked()
        advanceUntilIdle()
        assertEquals(0, vm.uiState.value.checkedCount)
        assertEquals(3, vm.uiState.value.items.size)
    }
}
