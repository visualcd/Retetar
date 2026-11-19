package com.ro.aminvatatro.presentation

import app.cash.turbine.test
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipe
import com.ro.aminvatatro.domain.usecase.recipe.SaveRecipe
import com.ro.aminvatatro.domain.validation.RecipeValidationError
import com.ro.aminvatatro.presentation.editor.RecipeEditorUiEvent
import com.ro.aminvatatro.presentation.editor.RecipeEditorViewModel
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
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class RecipeEditorViewModelTest {

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
    fun saveInvalidRecipeSurfacesValidationErrorsThenSucceedsWhenFixed() = runTest(testDispatcher) {
        val repo = FakeRecipeRepository()
        val vm = RecipeEditorViewModel(
            recipeId = null,
            getRecipe = GetRecipe(repo),
            saveRecipe = SaveRecipe(repo, FakeClock(), SequenceIdGenerator("rec")),
        )

        // Save empty form -> validation errors
        vm.save()
        advanceUntilIdle()
        assertTrue(vm.uiState.value.validationErrors.contains(RecipeValidationError.TitleBlank))

        // Fill valid fields + test ingredient/step reordering & TO_TASTE clearing qty
        vm.onTitleChange("Omletă cu legume")
        vm.onBaseServingsChange("2")
        vm.onPrepMinutesChange("5")
        vm.onCookMinutesChange("10")
        vm.updateIngredient(0, name = "Ouă", quantityText = "3", unit = MeasurementUnit.PIECE, aisle = Aisle.DAIRY)
        vm.addIngredient()
        vm.updateIngredient(1, name = "Sare", quantityText = "5", unit = MeasurementUnit.TO_TASTE, aisle = Aisle.SPICES)
        assertEquals("", vm.uiState.value.ingredients[1].quantityText)
        vm.moveIngredientUp(1)
        assertEquals("Sare", vm.uiState.value.ingredients[0].name)
        vm.moveIngredientDown(0)
        assertEquals("Ouă", vm.uiState.value.ingredients[0].name)

        vm.updateStep(0, "Bate ouăle.")
        vm.addStep()
        vm.updateStep(1, "Prăjește în tigaie.")
        vm.moveStepUp(1)
        assertEquals("Prăjește în tigaie.", vm.uiState.value.steps[0].text)
        vm.moveStepDown(0)
        assertEquals("Bate ouăle.", vm.uiState.value.steps[0].text)

        vm.onTagInputChange("Mic dejun")
        vm.addTag()
        assertEquals(listOf("Mic dejun"), vm.uiState.value.tags)

        vm.events.test {
            vm.save()
            val event = awaitItem()
            assertTrue(event is RecipeEditorUiEvent.Saved)
            val saved = repo.getRecipeById(event.recipeId)
            assertNotNull(saved)
            assertEquals("Omletă cu legume", saved.title)
            assertEquals(2, saved.ingredients.size)
            assertEquals(2, saved.steps.size)
            cancelAndIgnoreRemainingEvents()
        }
        assertTrue(vm.uiState.value.validationErrors.isEmpty())
    }

    @Test
    fun editExistingRecipeLoadsDataAndPreservesCreatedAt() = runTest(testDispatcher) {
        val repo = FakeRecipeRepository()
        val clock = FakeClock(currentMillis = 9_999L)
        repo.recipesFlow.value = listOf(
            sampleRecipe(id = "r-existing", title = "Supă de pui", createdAt = 1_234L, updatedAt = 1_234L),
        )

        val vm = RecipeEditorViewModel(
            recipeId = "r-existing",
            getRecipe = GetRecipe(repo),
            saveRecipe = SaveRecipe(repo, clock, SequenceIdGenerator("rec")),
        )
        advanceUntilIdle()

        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.isEditing)
        assertEquals("Supă de pui", vm.uiState.value.title)

        vm.onTitleChange("Supă clară de pui")
        vm.events.test {
            vm.save()
            val event = awaitItem()
            assertEquals(RecipeEditorUiEvent.Saved("r-existing"), event)
            cancelAndIgnoreRemainingEvents()
        }

        val updated = repo.getRecipeById("r-existing")
        assertNotNull(updated)
        assertEquals("Supă clară de pui", updated.title)
        assertEquals(1_234L, updated.createdAt)
        assertEquals(9_999L, updated.updatedAt)
    }
}
