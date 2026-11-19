package com.ro.aminvatatro.ui.navigation

import aminvatatro.app.shared.generated.resources.Res
import aminvatatro.app.shared.generated.resources.nav_planner
import aminvatatro.app.shared.generated.resources.nav_recipes
import aminvatatro.app.shared.generated.resources.nav_settings
import aminvatatro.app.shared.generated.resources.nav_shopping
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.consumeWindowInsets
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Icon
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationRail
import androidx.compose.material3.NavigationRailItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.testTag
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import androidx.savedstate.read
import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.domain.model.MealSlot
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
import com.ro.aminvatatro.presentation.settings.SettingsScreen
import com.ro.aminvatatro.presentation.settings.SettingsViewModel
import com.ro.aminvatatro.presentation.shopping.ShoppingListScreen
import com.ro.aminvatatro.presentation.shopping.ShoppingListViewModel
import com.ro.aminvatatro.ui.icons.AppIcons
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.StringResource
import org.jetbrains.compose.resources.stringResource
import org.koin.compose.koinInject
import org.koin.compose.viewmodel.koinViewModel
import org.koin.core.parameter.parametersOf

/**
 * Navigation route constants and helpers for all Rețetar destinations.
 */
object RetetarRoutes {
    const val RECIPES: String = "recipes"
    const val PLANNER: String = "planner"
    const val SHOPPING_BASE: String = "shopping"
    const val SHOPPING_PATTERN: String = "shopping?weekStart={weekStart}&autoGenerate={autoGenerate}"
    const val SETTINGS: String = "settings"

    const val RECIPE_DETAIL_PATTERN: String = "recipe_detail/{recipeId}"
    const val RECIPE_EDITOR_PATTERN: String = "recipe_editor?recipeId={recipeId}"
    const val RECIPE_PICKER_PATTERN: String = "recipe_picker/{date}/{slot}"

    fun recipeDetail(recipeId: String): String = "recipe_detail/$recipeId"

    fun recipeEditor(recipeId: String? = null): String =
        if (recipeId.isNullOrBlank()) "recipe_editor" else "recipe_editor?recipeId=$recipeId"

    fun recipePicker(date: LocalDate, slot: MealSlot): String = "recipe_picker/$date/${slot.name}"

    fun shopping(weekStart: LocalDate? = null, autoGenerate: Boolean = false): String =
        if (weekStart == null) {
            SHOPPING_BASE
        } else {
            "$SHOPPING_BASE?weekStart=$weekStart&autoGenerate=$autoGenerate"
        }
}

/**
 * Primary top-level destinations shown in the adaptive bottom bar / navigation rail.
 */
enum class TopLevelDestination(
    val route: String,
    val matchPrefix: String,
    val labelRes: StringResource,
    val icon: ImageVector,
) {
    RECIPES(
        route = RetetarRoutes.RECIPES,
        matchPrefix = RetetarRoutes.RECIPES,
        labelRes = Res.string.nav_recipes,
        icon = AppIcons.Recipes,
    ),
    PLANNER(
        route = RetetarRoutes.PLANNER,
        matchPrefix = RetetarRoutes.PLANNER,
        labelRes = Res.string.nav_planner,
        icon = AppIcons.Planner,
    ),
    SHOPPING(
        route = RetetarRoutes.SHOPPING_BASE,
        matchPrefix = RetetarRoutes.SHOPPING_BASE,
        labelRes = Res.string.nav_shopping,
        icon = AppIcons.Shopping,
    ),
    SETTINGS(
        route = RetetarRoutes.SETTINGS,
        matchPrefix = RetetarRoutes.SETTINGS,
        labelRes = Res.string.nav_settings,
        icon = AppIcons.Settings,
    ),
}

/**
 * Adaptive root navigation shell for Rețetar.
 *
 * Uses a bottom [NavigationBar] on compact screens and a persistent [NavigationRail]
 * on medium/expanded screens across top-level destinations.
 */
@Composable
fun RetetarNavHost(
    modifier: Modifier = Modifier,
    navController: NavHostController = rememberNavController(),
) {
    val widthClass = LocalWindowWidthClass.current
    val backStackEntry by navController.currentBackStackEntryAsState()
    val currentRoute = backStackEntry?.destination?.route
    val currentTopLevel = TopLevelDestination.entries.firstOrNull { destination ->
        currentRoute == destination.route || currentRoute?.startsWith("${destination.matchPrefix}?") == true
    }

    val navigateToTopLevel: (TopLevelDestination) -> Unit = { destination ->
        navController.navigate(destination.route) {
            popUpTo(navController.graph.findStartDestination().id) {
                saveState = true
            }
            launchSingleTop = true
            restoreState = true
        }
    }

    if (widthClass == WindowWidthClass.COMPACT) {
        Scaffold(
            modifier = modifier.fillMaxSize(),
            bottomBar = {
                if (currentTopLevel != null) {
                    NavigationBar(modifier = Modifier.testTag("retetar_bottom_bar")) {
                        TopLevelDestination.entries.forEach { destination ->
                            val label = stringResource(destination.labelRes)
                            NavigationBarItem(
                                selected = currentTopLevel == destination,
                                onClick = { navigateToTopLevel(destination) },
                                icon = {
                                    Icon(
                                        imageVector = destination.icon,
                                        contentDescription = label,
                                    )
                                },
                                label = { Text(label) },
                            )
                        }
                    }
                }
            },
        ) { innerPadding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .consumeWindowInsets(innerPadding),
            ) {
                RetetarNavGraph(navController = navController)
            }
        }
    } else {
        Row(modifier = modifier.fillMaxSize()) {
            if (currentTopLevel != null) {
                NavigationRail(
                    modifier = Modifier
                        .fillMaxHeight()
                        .testTag("retetar_nav_rail"),
                ) {
                    TopLevelDestination.entries.forEach { destination ->
                        val label = stringResource(destination.labelRes)
                        NavigationRailItem(
                            selected = currentTopLevel == destination,
                            onClick = { navigateToTopLevel(destination) },
                            icon = {
                                Icon(
                                    imageVector = destination.icon,
                                    contentDescription = label,
                                )
                            },
                            label = { Text(label) },
                        )
                    }
                }
            }
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxHeight(),
            ) {
                RetetarNavGraph(navController = navController)
            }
        }
    }
}

@Composable
private fun RetetarNavGraph(
    navController: NavHostController,
) {
    val clock: Clock = koinInject()

    NavHost(
        navController = navController,
        startDestination = RetetarRoutes.RECIPES,
        modifier = Modifier.fillMaxSize(),
    ) {
        composable(route = RetetarRoutes.RECIPES) {
            val viewModel: RecipesViewModel = koinViewModel()
            RecipesScreen(
                viewModel = viewModel,
                onOpenRecipe = { recipeId ->
                    navController.navigate(RetetarRoutes.recipeDetail(recipeId))
                },
                onCreateRecipe = {
                    navController.navigate(RetetarRoutes.recipeEditor())
                },
            )
        }

        composable(
            route = RetetarRoutes.RECIPE_DETAIL_PATTERN,
            arguments = listOf(
                navArgument("recipeId") { type = NavType.StringType },
            ),
        ) { entry ->
            val recipeId = entry.arguments?.read { getStringOrNull("recipeId") }.orEmpty()
            val viewModel: RecipeDetailViewModel = koinViewModel(
                key = "recipe_detail_$recipeId",
                parameters = { parametersOf(recipeId) },
            )
            RecipeDetailScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onEditRecipe = { id ->
                    navController.navigate(RetetarRoutes.recipeEditor(id))
                },
            )
        }

        composable(
            route = RetetarRoutes.RECIPE_EDITOR_PATTERN,
            arguments = listOf(
                navArgument("recipeId") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val recipeId = entry.arguments?.read { getStringOrNull("recipeId") }
            val viewModel: RecipeEditorViewModel = koinViewModel(
                key = "recipe_editor_${recipeId ?: "new"}",
                parameters = {
                    if (recipeId != null) parametersOf(recipeId) else parametersOf()
                },
            )
            RecipeEditorScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
                onSaved = { savedId ->
                    if (recipeId != null) {
                        navController.popBackStack()
                    } else {
                        navController.navigate(RetetarRoutes.recipeDetail(savedId)) {
                            popUpTo(RetetarRoutes.RECIPES)
                        }
                    }
                },
            )
        }

        composable(route = RetetarRoutes.PLANNER) {
            val viewModel: PlannerViewModel = koinViewModel()
            PlannerScreen(
                viewModel = viewModel,
                onOpenRecipePicker = { date, slot ->
                    navController.navigate(RetetarRoutes.recipePicker(date, slot))
                },
                onOpenShoppingForWeek = { weekStart ->
                    navController.navigate(
                        RetetarRoutes.shopping(weekStart = weekStart, autoGenerate = true),
                    ) {
                        popUpTo(navController.graph.findStartDestination().id) {
                            saveState = true
                        }
                        launchSingleTop = true
                    }
                },
            )
        }

        composable(
            route = RetetarRoutes.RECIPE_PICKER_PATTERN,
            arguments = listOf(
                navArgument("date") { type = NavType.StringType },
                navArgument("slot") { type = NavType.StringType },
            ),
        ) { entry ->
            val dateArg = entry.arguments?.read { getStringOrNull("date") }
            val slotArg = entry.arguments?.read { getStringOrNull("slot") }
            val date = dateArg?.let { runCatching { LocalDate.parse(it) }.getOrNull() } ?: clock.today()
            val slot = MealSlot.entries.firstOrNull { it.name == slotArg } ?: MealSlot.LUNCH
            val viewModel: RecipePickerViewModel = koinViewModel(
                key = "recipe_picker_${date}_${slot.name}",
                parameters = { parametersOf(date, slot) },
            )
            RecipePickerScreen(
                viewModel = viewModel,
                onNavigateBack = { navController.popBackStack() },
            )
        }

        composable(
            route = RetetarRoutes.SHOPPING_PATTERN,
            arguments = listOf(
                navArgument("weekStart") {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
                navArgument("autoGenerate") {
                    type = NavType.BoolType
                    defaultValue = false
                },
            ),
        ) { entry ->
            val viewModel: ShoppingListViewModel = koinViewModel()
            val weekStartArg = entry.arguments?.read { getStringOrNull("weekStart") }
            val autoGenerateArg = entry.arguments?.read { getBooleanOrNull("autoGenerate") } ?: false
            var handledRequest by rememberSaveable { mutableStateOf<String?>(null) }
            val requestKey = if (weekStartArg != null) "$weekStartArg:$autoGenerateArg" else null

            LaunchedEffect(requestKey) {
                if (weekStartArg != null && requestKey != null && handledRequest != requestKey) {
                    handledRequest = requestKey
                    val parsedDate = runCatching { LocalDate.parse(weekStartArg) }.getOrNull()
                    if (parsedDate != null) {
                        viewModel.selectWeek(parsedDate, autoRegenerate = autoGenerateArg)
                    }
                }
            }

            ShoppingListScreen(viewModel = viewModel)
        }

        composable(route = RetetarRoutes.SETTINGS) {
            val viewModel: SettingsViewModel = koinViewModel()
            SettingsScreen(viewModel = viewModel)
        }
    }
}
