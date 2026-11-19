package com.ro.aminvatatro.di

import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.presentation.detail.RecipeDetailViewModel
import com.ro.aminvatatro.presentation.editor.RecipeEditorViewModel
import com.ro.aminvatatro.presentation.picker.RecipePickerViewModel
import com.ro.aminvatatro.presentation.planner.PlannerViewModel
import com.ro.aminvatatro.presentation.recipes.RecipesViewModel
import com.ro.aminvatatro.presentation.settings.SettingsViewModel
import com.ro.aminvatatro.presentation.shopping.ShoppingListViewModel
import kotlinx.datetime.LocalDate
import org.koin.core.module.Module
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

/**
 * Koin module registering all presentation-layer KMP ViewModels.
 */
val presentationModule: Module = module {
    viewModel {
        RecipesViewModel(
            getRecipes = get(),
            toggleFavorite = get(),
            recipeRepository = get(),
        )
    }
    viewModel { (recipeId: String) ->
        RecipeDetailViewModel(
            recipeId = recipeId,
            getRecipe = get(),
            scaleRecipe = get(),
            toggleFavorite = get(),
            deleteRecipe = get(),
            setPlanEntry = get(),
            settingsRepository = get(),
            clock = get(),
        )
    }
    viewModel { params ->
        RecipeEditorViewModel(
            recipeId = params.getOrNull<String>(),
            getRecipe = get(),
            saveRecipe = get(),
        )
    }
    viewModel {
        PlannerViewModel(
            mealPlanRepository = get(),
            recipeRepository = get(),
            changeServings = get(),
            removePlanEntry = get(),
            copyDay = get(),
            repeatPreviousWeek = get(),
            clock = get(),
        )
    }
    viewModel { (date: LocalDate, slot: MealSlot) ->
        RecipePickerViewModel(
            date = date,
            slot = slot,
            getRecipes = get(),
            setPlanEntry = get(),
        )
    }
    viewModel {
        ShoppingListViewModel(
            shoppingRepository = get(),
            settingsRepository = get(),
            generateShoppingList = get(),
            toggleItem = get(),
            addManualItem = get(),
            removeItem = get(),
            clearChecked = get(),
            clock = get(),
        )
    }
    viewModel {
        SettingsViewModel(
            settingsRepository = get(),
            recipeRepository = get(),
            exportRecipes = get(),
            importRecipes = get(),
            clock = get(),
        )
    }
}
