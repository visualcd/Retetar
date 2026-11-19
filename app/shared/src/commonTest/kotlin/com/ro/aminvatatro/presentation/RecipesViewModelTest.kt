package com.ro.aminvatatro.presentation

import app.cash.turbine.test
import com.ro.aminvatatro.domain.model.Tag
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipes
import com.ro.aminvatatro.domain.usecase.recipe.ToggleFavorite
import com.ro.aminvatatro.presentation.recipes.RecipesUiEvent
import com.ro.aminvatatro.presentation.recipes.RecipesViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecipesViewModelTest {

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
    fun searchFiltersAndFavoriteToggleUpdateState() = runTest(testDispatcher) {
        val repo = FakeRecipeRepository()
        val clock = FakeClock()
        val r1 = sampleRecipe(
            id = "r1",
            title = "Clătite cu brânză",
            prepMinutes = 10,
            cookMinutes = 15,
            tags = listOf(Tag("t1", "Desert"), Tag("t2", "Rapid")),
            isFavorite = true,
            updatedAt = 2_000L,
        )
        val r2 = sampleRecipe(
            id = "r2",
            title = "Sarmale în foi de varză",
            prepMinutes = 40,
            cookMinutes = 120,
            tags = listOf(Tag("t3", "Tradițional")),
            isFavorite = false,
            updatedAt = 1_000L,
        )
        repo.recipesFlow.value = listOf(r1, r2)

        val vm = RecipesViewModel(
            getRecipes = GetRecipes(repo),
            toggleFavorite = ToggleFavorite(repo, clock),
            recipeRepository = repo,
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertEquals(2, vm.uiState.value.recipes.size)
        assertEquals(3, vm.uiState.value.availableTags.size)

        // Diacritic-insensitive search
        vm.onQueryChange("clatite")
        advanceUntilIdle()
        assertEquals(listOf("r1"), vm.uiState.value.recipes.map { it.id })
        assertTrue(vm.uiState.value.hasActiveFilters)

        // Clear filters restores both
        vm.clearFilters()
        advanceUntilIdle()
        assertEquals(2, vm.uiState.value.recipes.size)
        assertFalse(vm.uiState.value.hasActiveFilters)

        // Filter by time <= 30 min
        vm.setMaxTotalMinutes(30)
        advanceUntilIdle()
        assertEquals(listOf("r1"), vm.uiState.value.recipes.map { it.id })

        // Filter by tag + favorites
        vm.clearFilters()
        vm.toggleTagFilter("Tradițional")
        advanceUntilIdle()
        assertEquals(listOf("r2"), vm.uiState.value.recipes.map { it.id })

        vm.toggleFavoritesFilter()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.recipes.isEmpty())

        // Toggling favorite on r2 makes it match
        vm.onToggleFavorite("r2")
        advanceUntilIdle()
        assertEquals(listOf("r2"), vm.uiState.value.recipes.map { it.id })
    }

    @Test
    fun navigationEventsAreEmittedViaTurbine() = runTest(testDispatcher) {
        val repo = FakeRecipeRepository()
        val vm = RecipesViewModel(
            getRecipes = GetRecipes(repo),
            toggleFavorite = ToggleFavorite(repo, FakeClock()),
            recipeRepository = repo,
        )

        vm.events.test {
            vm.onRecipeClicked("rec-42")
            assertEquals(RecipesUiEvent.OpenRecipe("rec-42"), awaitItem())

            vm.onCreateRecipeClicked()
            assertEquals(RecipesUiEvent.CreateRecipe, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }
}
