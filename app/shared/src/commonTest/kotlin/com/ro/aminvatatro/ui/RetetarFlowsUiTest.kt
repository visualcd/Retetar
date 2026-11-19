package com.ro.aminvatatro.ui

import aminvatatro.app.shared.generated.resources.Res
import aminvatatro.app.shared.generated.resources.action_add
import aminvatatro.app.shared.generated.resources.action_decrease_servings
import aminvatatro.app.shared.generated.resources.action_increase_servings
import aminvatatro.app.shared.generated.resources.action_toggle_favorite
import aminvatatro.app.shared.generated.resources.editor_add_tag
import aminvatatro.app.shared.generated.resources.editor_field_title
import aminvatatro.app.shared.generated.resources.editor_ingredient_name
import aminvatatro.app.shared.generated.resources.editor_ingredient_quantity
import aminvatatro.app.shared.generated.resources.editor_step_placeholder
import aminvatatro.app.shared.generated.resources.editor_tag_placeholder
import aminvatatro.app.shared.generated.resources.picker_add_confirm
import aminvatatro.app.shared.generated.resources.planner_add_to_slot
import aminvatatro.app.shared.generated.resources.shopping_add_manual
import aminvatatro.app.shared.generated.resources.shopping_item_name
import aminvatatro.app.shared.generated.resources.shopping_regenerate
import aminvatatro.app.shared.generated.resources.slot_lunch
import aminvatatro.app.shared.generated.resources.validation_title_blank
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.width
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.assertHeightIsAtLeast
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertIsOff
import androidx.compose.ui.test.assertIsOn
import androidx.compose.ui.test.assertWidthIsAtLeast
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasSetTextAction
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performKeyInput
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.pressKey
import androidx.compose.ui.test.v2.runComposeUiTest
import androidx.compose.ui.unit.dp
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.AppLanguage
import com.ro.aminvatatro.domain.model.Ingredient
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.repository.AppSettings
import com.ro.aminvatatro.domain.usecase.plan.ChangeServings
import com.ro.aminvatatro.domain.usecase.plan.CopyDay
import com.ro.aminvatatro.domain.usecase.plan.RemovePlanEntry
import com.ro.aminvatatro.domain.usecase.plan.RepeatPreviousWeek
import com.ro.aminvatatro.domain.usecase.plan.SetPlanEntry
import com.ro.aminvatatro.domain.usecase.recipe.DeleteRecipe
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipe
import com.ro.aminvatatro.domain.usecase.recipe.GetRecipes
import com.ro.aminvatatro.domain.usecase.recipe.SaveRecipe
import com.ro.aminvatatro.domain.usecase.recipe.ScaleRecipe
import com.ro.aminvatatro.domain.usecase.recipe.ToggleFavorite
import com.ro.aminvatatro.domain.usecase.shopping.AddManualItem
import com.ro.aminvatatro.domain.usecase.shopping.ClearChecked
import com.ro.aminvatatro.domain.usecase.shopping.GenerateShoppingList
import com.ro.aminvatatro.domain.usecase.shopping.RemoveItem
import com.ro.aminvatatro.domain.usecase.shopping.ToggleItem
import com.ro.aminvatatro.presentation.FakeClock
import com.ro.aminvatatro.presentation.FakeMealPlanRepository
import com.ro.aminvatatro.presentation.FakeRecipeRepository
import com.ro.aminvatatro.presentation.FakeSettingsRepository
import com.ro.aminvatatro.presentation.FakeShoppingRepository
import com.ro.aminvatatro.presentation.SequenceIdGenerator
import com.ro.aminvatatro.presentation.detail.RecipeDetailScreen
import com.ro.aminvatatro.presentation.detail.RecipeDetailViewModel
import com.ro.aminvatatro.presentation.editor.RecipeEditorScreen
import com.ro.aminvatatro.presentation.editor.RecipeEditorViewModel
import com.ro.aminvatatro.presentation.picker.RecipePickerScreen
import com.ro.aminvatatro.presentation.picker.RecipePickerViewModel
import com.ro.aminvatatro.presentation.planner.PlannerScreen
import com.ro.aminvatatro.presentation.planner.PlannerViewModel
import com.ro.aminvatatro.presentation.recipes.RecipesScreen
import com.ro.aminvatatro.presentation.recipes.RecipesViewModel
import com.ro.aminvatatro.presentation.sampleRecipe
import com.ro.aminvatatro.presentation.shopping.ShoppingListScreen
import com.ro.aminvatatro.presentation.shopping.ShoppingListViewModel
import com.ro.aminvatatro.RetetarApp
import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.core.time.IdGenerator
import com.ro.aminvatatro.di.presentationModule
import com.ro.aminvatatro.di.useCaseModule
import com.ro.aminvatatro.domain.repository.MealPlanRepository
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.repository.SettingsRepository
import com.ro.aminvatatro.domain.repository.ShoppingRepository
import com.ro.aminvatatro.ui.navigation.LocalWindowWidthClass
import com.ro.aminvatatro.ui.navigation.WindowWidthClass
import com.ro.aminvatatro.ui.theme.RetetarTheme
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.stringResource
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull
import kotlin.test.assertTrue

@OptIn(ExperimentalTestApi::class)
class RetetarFlowsUiTest {

    @Test
    fun createRecipeFlowValidatesAndPersistsNewRecipe() = runComposeUiTest {
        val repo = FakeRecipeRepository()
        val vm = RecipeEditorViewModel(
            recipeId = null,
            getRecipe = GetRecipe(repo),
            saveRecipe = SaveRecipe(repo, FakeClock(), SequenceIdGenerator("rec")),
        )
        var savedId: String? = null
        var titleLabel = ""
        var titleBlankError = ""
        var ingredientNameLabel = ""
        var ingredientQtyLabel = ""
        var stepLabel = ""
        var tagPlaceholder = ""
        var addTagLabel = ""

        setContent {
            titleLabel = stringResource(Res.string.editor_field_title)
            titleBlankError = stringResource(Res.string.validation_title_blank)
            ingredientNameLabel = stringResource(Res.string.editor_ingredient_name)
            ingredientQtyLabel = stringResource(Res.string.editor_ingredient_quantity)
            stepLabel = stringResource(Res.string.editor_step_placeholder, 1)
            tagPlaceholder = stringResource(Res.string.editor_tag_placeholder)
            addTagLabel = stringResource(Res.string.editor_add_tag)

            RetetarTheme {
                CompositionLocalProvider(LocalWindowWidthClass provides WindowWidthClass.COMPACT) {
                    RecipeEditorScreen(
                        viewModel = vm,
                        onNavigateBack = {},
                        onSaved = { savedId = it },
                    )
                }
            }
        }

        // Attempt saving blank form -> inline validation error shown
        onNodeWithTag("editor_save_action").performClick()
        waitUntil(timeoutMillis = 3_000) {
            onAllNodesWithText(titleBlankError).fetchSemanticsNodes().isNotEmpty()
        }

        // Fill valid fields (scrolling LazyColumn so off-screen items compose)
        val editorList = onNodeWithTag("recipe_editor_list")
        onNode(hasText(titleLabel) and hasSetTextAction()).performTextInput("Omletă cu brânză")

        val ingNameMatcher = hasText(ingredientNameLabel) and hasSetTextAction()
        editorList.performScrollToNode(ingNameMatcher)
        onNode(ingNameMatcher).performTextInput("Ouă")

        val ingQtyMatcher = hasText(ingredientQtyLabel) and hasSetTextAction()
        editorList.performScrollToNode(ingQtyMatcher)
        onNode(ingQtyMatcher).performTextInput("3")

        val stepMatcher = hasText(stepLabel) and hasSetTextAction()
        editorList.performScrollToNode(stepMatcher)
        onNode(stepMatcher).performTextInput("Bate ouăle și prăjește în tigaie.")

        val tagMatcher = hasText(tagPlaceholder) and hasSetTextAction()
        editorList.performScrollToNode(tagMatcher)
        onNode(tagMatcher).performTextInput("Rapid")
        editorList.performScrollToNode(hasText(addTagLabel))
        onNodeWithText(addTagLabel).performClick()

        // Also verify keyboard Enter submits a second tag (Desktop/Web keyboard flow)
        onNode(tagMatcher).performTextInput("Mic dejun")
        onNode(hasText("Mic dejun") and hasSetTextAction()).performKeyInput { pressKey(Key.Enter) }

        // Save valid recipe
        onNodeWithTag("editor_save_action").performClick()
        waitForIdle()
        assertEquals(emptyList(), vm.uiState.value.validationErrors, "Unexpected validation errors: ${vm.uiState.value}")
        waitUntil(timeoutMillis = 3_000) { savedId != null }

        val stored = repo.recipesFlow.value.firstOrNull { it.id == savedId }
        assertNotNull(stored)
        assertEquals("Omletă cu brânză", stored.title)
        assertEquals(listOf("Ouă"), stored.ingredients.map { it.name })
        assertEquals(listOf("Rapid", "Mic dejun"), stored.tags.map { it.name })
    }

    @Test
    fun scaleServingsFlowRecalculatesIngredientQuantities() = runComposeUiTest {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val settingsRepo = FakeSettingsRepository(AppSettings(language = AppLanguage.RO))
        val clock = FakeClock()
        recipeRepo.recipesFlow.value = listOf(
            sampleRecipe(
                id = "r-scale",
                title = "Clătite",
                baseServings = 4,
                ingredients = listOf(
                    Ingredient("i1", 0, "Făină", 200.0, MeasurementUnit.G, Aisle.PANTRY),
                    Ingredient("i2", 1, "Lapte", 400.0, MeasurementUnit.ML, Aisle.DAIRY),
                ),
            ),
        )

        val vm = RecipeDetailViewModel(
            recipeId = "r-scale",
            getRecipe = GetRecipe(recipeRepo),
            scaleRecipe = ScaleRecipe(),
            toggleFavorite = ToggleFavorite(recipeRepo, clock),
            deleteRecipe = DeleteRecipe(recipeRepo),
            setPlanEntry = SetPlanEntry(planRepo, recipeRepo, SequenceIdGenerator("mp")),
            settingsRepository = settingsRepo,
            clock = clock,
        )

        var increaseDesc = ""
        var decreaseDesc = ""

        setContent {
            increaseDesc = stringResource(Res.string.action_increase_servings)
            decreaseDesc = stringResource(Res.string.action_decrease_servings)
            RetetarTheme {
                RecipeDetailScreen(
                    viewModel = vm,
                    onNavigateBack = {},
                    onEditRecipe = {},
                )
            }
        }

        waitUntil(timeoutMillis = 3_000) {
            onAllNodesWithText("200 g").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("400 ml").assertIsDisplayed()

        // Verify stepper touch targets >= 48dp and accessible descriptions
        onNodeWithContentDescription(increaseDesc)
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
            .performClick()
        onNodeWithContentDescription(increaseDesc).performClick() // 4 -> 6 servings

        waitUntil(timeoutMillis = 3_000) {
            onAllNodesWithText("300 g").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("600 ml").assertIsDisplayed()

        onNodeWithContentDescription(decreaseDesc)
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun planMealFlowSchedulesRecipeIntoSelectedSlot() = runComposeUiTest {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val clock = FakeClock(currentDate = LocalDate(2026, 3, 4)) // Week of Mon 2026-03-02
        val idGen = SequenceIdGenerator("mp")
        recipeRepo.recipesFlow.value = listOf(
            sampleRecipe(id = "r-soup", title = "Supă cremă de legume", baseServings = 4),
        )

        val plannerVm = PlannerViewModel(
            mealPlanRepository = planRepo,
            recipeRepository = recipeRepo,
            changeServings = ChangeServings(planRepo),
            removePlanEntry = RemovePlanEntry(planRepo),
            copyDay = CopyDay(planRepo, idGen),
            repeatPreviousWeek = RepeatPreviousWeek(planRepo, idGen),
            clock = clock,
        )

        var addLunchDesc = ""
        var confirmPickerLabel = ""

        setContent {
            val lunchLabel = stringResource(Res.string.slot_lunch)
            addLunchDesc = stringResource(Res.string.planner_add_to_slot, lunchLabel)
            confirmPickerLabel = stringResource(Res.string.picker_add_confirm)

            var pickerTarget by remember { mutableStateOf<Pair<LocalDate, MealSlot>?>(null) }
            RetetarTheme {
                CompositionLocalProvider(LocalWindowWidthClass provides WindowWidthClass.COMPACT) {
                    val target = pickerTarget
                    if (target == null) {
                        PlannerScreen(
                            viewModel = plannerVm,
                            onOpenRecipePicker = { d, s -> pickerTarget = d to s },
                            onOpenShoppingForWeek = {},
                        )
                    } else {
                        val pickerVm = remember(target) {
                            RecipePickerViewModel(
                                date = target.first,
                                slot = target.second,
                                getRecipes = GetRecipes(recipeRepo),
                                setPlanEntry = SetPlanEntry(planRepo, recipeRepo, idGen),
                            )
                        }
                        RecipePickerScreen(
                            viewModel = pickerVm,
                            onNavigateBack = { pickerTarget = null },
                        )
                    }
                }
            }
        }

        // Open recipe picker for Monday Lunch
        waitUntil(timeoutMillis = 3_000) {
            onAllNodesWithContentDescription(addLunchDesc).fetchSemanticsNodes().isNotEmpty()
        }
        onAllNodesWithContentDescription(addLunchDesc).onFirst().performClick()

        // Pick recipe and confirm
        waitUntil(timeoutMillis = 3_000) {
            onAllNodesWithText("Supă cremă de legume").fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithText("Supă cremă de legume").performClick()
        onNodeWithText(confirmPickerLabel).performClick()

        // Back on PlannerScreen: scheduled recipe is displayed in the slot
        waitUntil(timeoutMillis = 3_000) {
            planRepo.entriesFlow.value.isNotEmpty() &&
                onAllNodesWithText("Supă cremă de legume").fetchSemanticsNodes().isNotEmpty()
        }
        assertEquals(1, planRepo.entriesFlow.value.size)
        assertEquals(MealSlot.LUNCH, planRepo.entriesFlow.value.first().slot)
    }

    @Test
    fun generateShoppingListAndCheckItemFlowAggregatesAndToggles() = runComposeUiTest {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val shoppingRepo = FakeShoppingRepository()
        val settingsRepo = FakeSettingsRepository(AppSettings(language = AppLanguage.RO))
        val clock = FakeClock(currentDate = LocalDate(2026, 3, 4)) // Week of Mon 2026-03-02
        val idGen = SequenceIdGenerator("sh")

        recipeRepo.recipesFlow.value = listOf(
            sampleRecipe(
                id = "r1",
                title = "Clătite",
                baseServings = 4,
                ingredients = listOf(
                    Ingredient("i1", 0, "Făină", 500.0, MeasurementUnit.G, Aisle.PANTRY),
                ),
            ),
            sampleRecipe(
                id = "r2",
                title = "Pâine de casă",
                baseServings = 4,
                ingredients = listOf(
                    Ingredient("i2", 0, "faina", 1.0, MeasurementUnit.KG, Aisle.PANTRY),
                ),
            ),
        )
        planRepo.entriesFlow.value = listOf(
            MealPlanEntry("e1", LocalDate(2026, 3, 2), MealSlot.BREAKFAST, "r1", 4),
            MealPlanEntry("e2", LocalDate(2026, 3, 3), MealSlot.DINNER, "r2", 4),
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

        var regenerateLabel = ""
        var addManualLabel = ""
        var itemNameLabel = ""
        var addConfirmLabel = ""

        setContent {
            regenerateLabel = stringResource(Res.string.shopping_regenerate)
            addManualLabel = stringResource(Res.string.shopping_add_manual)
            itemNameLabel = stringResource(Res.string.shopping_item_name)
            addConfirmLabel = stringResource(Res.string.action_add)

            RetetarTheme {
                ShoppingListScreen(viewModel = vm)
            }
        }

        // Regenerate shopping list from the weekly plan
        onAllNodesWithText(regenerateLabel).onFirst().performClick()

        // 500 g + 1 kg -> 1,5 kg aggregated into a single row
        waitUntil(timeoutMillis = 3_000) {
            onAllNodesWithText("1,5 kg").fetchSemanticsNodes().isNotEmpty()
        }
        val flourRow = onNodeWithText("Făină")
        flourRow.assertIsOff()
        flourRow.performClick()
        waitUntil(timeoutMillis = 3_000) {
            shoppingRepo.itemsFlow.value.any { it.checked }
        }
        flourRow.assertIsOn()

        // Add manual item via dialog
        onNodeWithText(addManualLabel).performClick()
        onNode(hasText(itemNameLabel) and hasSetTextAction()).performTextInput("Șervețele")
        onNodeWithText(addConfirmLabel).performClick()

        waitUntil(timeoutMillis = 3_000) {
            onAllNodesWithText("Șervețele").fetchSemanticsNodes().isNotEmpty()
        }
        assertTrue(shoppingRepo.itemsFlow.value.any { it.name == "Șervețele" })
    }

    @Test
    fun forcedCompactAndExpandedLayoutsSwitchPlannerBetweenListAndGrid() = runComposeUiTest {
        val planRepo = FakeMealPlanRepository()
        val recipeRepo = FakeRecipeRepository(planRepo)
        val clock = FakeClock(currentDate = LocalDate(2026, 3, 4))
        val idGen = SequenceIdGenerator("mp")
        val plannerVm = PlannerViewModel(
            mealPlanRepository = planRepo,
            recipeRepository = recipeRepo,
            changeServings = ChangeServings(planRepo),
            removePlanEntry = RemovePlanEntry(planRepo),
            copyDay = CopyDay(planRepo, idGen),
            repeatPreviousWeek = RepeatPreviousWeek(planRepo, idGen),
            clock = clock,
        )

        var widthClass by mutableStateOf(WindowWidthClass.COMPACT)

        setContent {
            RetetarTheme {
                CompositionLocalProvider(LocalWindowWidthClass provides widthClass) {
                    Box(
                        modifier = if (widthClass == WindowWidthClass.COMPACT) {
                            Modifier.width(360.dp)
                        } else {
                            Modifier.fillMaxSize()
                        },
                    ) {
                        PlannerScreen(
                            viewModel = plannerVm,
                            onOpenRecipePicker = { _, _ -> },
                            onOpenShoppingForWeek = {},
                        )
                    }
                }
            }
        }

        // Narrow screen (COMPACT): vertical day list is active
        waitUntil(timeoutMillis = 3_000) {
            onAllNodesWithText("Săpt.", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                onAllNodesWithText("Week", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag("planner_compact_list").assertIsDisplayed()
        onNodeWithTag("planner_wide_grid").assertDoesNotExist()

        // Wide screen (EXPANDED): 7-column grid is active
        widthClass = WindowWidthClass.EXPANDED
        waitUntil(timeoutMillis = 3_000) {
            onAllNodesWithText("Săpt.", substring = true).fetchSemanticsNodes().isNotEmpty() ||
                onAllNodesWithText("Week", substring = true).fetchSemanticsNodes().isNotEmpty()
        }
        onNodeWithTag("planner_wide_grid").assertIsDisplayed()
        onNodeWithTag("planner_compact_list").assertDoesNotExist()
    }

    @Test
    fun recipesListAccessibleActionsMeetMinimum48dpTouchTarget() = runComposeUiTest {
        val repo = FakeRecipeRepository()
        repo.recipesFlow.value = listOf(sampleRecipe(id = "r1", title = "Clătite cu gem"))
        val vm = RecipesViewModel(
            getRecipes = GetRecipes(repo),
            toggleFavorite = ToggleFavorite(repo, FakeClock()),
            recipeRepository = repo,
        )
        var toggleFavDesc = ""

        setContent {
            toggleFavDesc = stringResource(Res.string.action_toggle_favorite)
            RetetarTheme {
                Box(modifier = Modifier.width(360.dp)) {
                    RecipesScreen(
                        viewModel = vm,
                        onOpenRecipe = {},
                        onCreateRecipe = {},
                    )
                }
            }
        }

        waitUntil(timeoutMillis = 3_000) {
            onAllNodesWithText("Clătite cu gem").fetchSemanticsNodes().isNotEmpty()
        }
        onNode(hasContentDescription(toggleFavDesc))
            .assertIsDisplayed()
            .assertWidthIsAtLeast(48.dp)
            .assertHeightIsAtLeast(48.dp)
    }

    @Test
    fun forcedCompactAndExpandedAppShellSwitchesBottomBarAndNavigationRail() = runComposeUiTest {
        val fakePlanRepo = FakeMealPlanRepository()
        val fakeRecipeRepo = FakeRecipeRepository(fakePlanRepo)
        val fakeShoppingRepo = FakeShoppingRepository()
        val fakeSettingsRepo = FakeSettingsRepository(AppSettings(language = AppLanguage.RO))
        val fakeClock = FakeClock()
        val fakeIdGen = SequenceIdGenerator("ui")

        stopKoin()
        startKoin {
            modules(
                module {
                    single<Clock> { fakeClock }
                    single<IdGenerator> { fakeIdGen }
                    single<RecipeRepository> { fakeRecipeRepo }
                    single<MealPlanRepository> { fakePlanRepo }
                    single<ShoppingRepository> { fakeShoppingRepo }
                    single<SettingsRepository> { fakeSettingsRepo }
                },
                useCaseModule,
                presentationModule,
            )
        }

        try {
            var widthClass by mutableStateOf(WindowWidthClass.COMPACT)
            setContent {
                RetetarApp(forcedWidthClass = widthClass)
            }

            waitUntil(timeoutMillis = 3_000) {
                onNodeWithTag("retetar_bottom_bar").fetchSemanticsNodeOrNull() != null
            }
            onNodeWithTag("retetar_bottom_bar").assertIsDisplayed()
            onNodeWithTag("retetar_nav_rail").assertDoesNotExist()

            widthClass = WindowWidthClass.EXPANDED
            waitUntil(timeoutMillis = 3_000) {
                onNodeWithTag("retetar_nav_rail").fetchSemanticsNodeOrNull() != null
            }
            onNodeWithTag("retetar_nav_rail").assertIsDisplayed()
            onNodeWithTag("retetar_bottom_bar").assertDoesNotExist()
        } finally {
            stopKoin()
        }
    }
}

private fun androidx.compose.ui.test.SemanticsNodeInteraction.fetchSemanticsNodeOrNull() =
    runCatching { fetchSemanticsNode() }.getOrNull()
