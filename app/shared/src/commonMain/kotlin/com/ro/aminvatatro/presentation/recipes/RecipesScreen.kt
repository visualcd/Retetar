package com.ro.aminvatatro.presentation.recipes

import aminvatatro.app.shared.generated.resources.Res
import aminvatatro.app.shared.generated.resources.action_add_to_shopping_list
import aminvatatro.app.shared.generated.resources.action_clear
import aminvatatro.app.shared.generated.resources.action_toggle_favorite
import aminvatatro.app.shared.generated.resources.recipe_added_to_shopping_success
import aminvatatro.app.shared.generated.resources.error_generic
import aminvatatro.app.shared.generated.resources.loading
import aminvatatro.app.shared.generated.resources.minutes_short
import aminvatatro.app.shared.generated.resources.recipes_clear_filters
import aminvatatro.app.shared.generated.resources.recipes_empty_subtitle
import aminvatatro.app.shared.generated.resources.recipes_empty_title
import aminvatatro.app.shared.generated.resources.recipes_filter_favorites
import aminvatatro.app.shared.generated.resources.recipes_filter_time_15
import aminvatatro.app.shared.generated.resources.recipes_filter_time_30
import aminvatatro.app.shared.generated.resources.recipes_filter_time_60
import aminvatatro.app.shared.generated.resources.recipes_filter_time_all
import aminvatatro.app.shared.generated.resources.recipes_new
import aminvatatro.app.shared.generated.resources.recipes_no_matches_subtitle
import aminvatatro.app.shared.generated.resources.recipes_no_matches_title
import aminvatatro.app.shared.generated.resources.recipes_search_placeholder
import aminvatatro.app.shared.generated.resources.recipes_title
import aminvatatro.app.shared.generated.resources.servings_count
import aminvatatro.app.shared.generated.resources.action_cancel
import aminvatatro.app.shared.generated.resources.recipes_calorie_accept
import aminvatatro.app.shared.generated.resources.recipes_calorie_badge
import aminvatatro.app.shared.generated.resources.recipes_calorie_btn_generate
import aminvatatro.app.shared.generated.resources.recipes_calorie_dialog_desc
import aminvatatro.app.shared.generated.resources.recipes_calorie_dialog_title
import aminvatatro.app.shared.generated.resources.recipes_calorie_target_label
import aminvatatro.app.shared.generated.resources.recipes_generate_by_calories
import androidx.compose.foundation.clickable
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedCard
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.remember
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.ui.components.EmptyState
import com.ro.aminvatatro.ui.components.ErrorBanner
import com.ro.aminvatatro.ui.components.TagChips
import com.ro.aminvatatro.ui.icons.AppIcons
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Main recipes screen with diacritic-insensitive search, favorite/time/tag filters,
 * and loading/empty/error/list states.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipesScreen(
    viewModel: RecipesViewModel,
    onOpenRecipe: (String) -> Unit,
    onCreateRecipe: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RecipesUiEvent.OpenRecipe -> onOpenRecipe(event.recipeId)
                RecipesUiEvent.CreateRecipe -> onCreateRecipe()
                is RecipesUiEvent.RecipeSaved -> onOpenRecipe(event.recipeId)
                RecipesUiEvent.RecipeAddedToShopping -> {
                    snackbarHostState.showSnackbar(getString(Res.string.recipe_added_to_shopping_success))
                }
            }
        }
    }

    if (state.showCalorieDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCalorieTargetDialog,
            title = { Text(stringResource(Res.string.recipes_calorie_dialog_title)) },
            text = {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    Text(
                        text = stringResource(Res.string.recipes_calorie_dialog_desc),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        OutlinedTextField(
                            value = state.calorieTargetInput,
                            onValueChange = viewModel::onCalorieTargetInputChange,
                            label = { Text(stringResource(Res.string.recipes_calorie_target_label)) },
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                            modifier = Modifier.weight(1f),
                        )
                        Button(
                            onClick = viewModel::generateSuggestionsForCurrentTarget,
                            enabled = !state.isGeneratingSuggestions,
                        ) {
                            Text(stringResource(Res.string.recipes_calorie_btn_generate))
                        }
                    }

                    if (state.isGeneratingSuggestions) {
                        Box(
                            modifier = Modifier.fillMaxWidth().padding(16.dp),
                            contentAlignment = Alignment.Center,
                        ) {
                            CircularProgressIndicator()
                        }
                    }

                    state.calorieGenerationMessage?.let { msg ->
                        Text(
                            text = msg,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    if (state.calorieSuggestions.isNotEmpty()) {
                        LazyColumn(
                            modifier = Modifier.fillMaxWidth().height(260.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp),
                        ) {
                            items(state.calorieSuggestions) { suggestion ->
                                ElevatedCard(modifier = Modifier.fillMaxWidth()) {
                                    Column(
                                        modifier = Modifier.padding(10.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp),
                                    ) {
                                        Row(
                                            modifier = Modifier.fillMaxWidth(),
                                            horizontalArrangement = Arrangement.SpaceBetween,
                                            verticalAlignment = Alignment.CenterVertically,
                                        ) {
                                            Text(
                                                text = suggestion.recipe.title,
                                                style = MaterialTheme.typography.titleSmall,
                                                modifier = Modifier.weight(1f),
                                            )
                                            Text(
                                                text = stringResource(
                                                    Res.string.recipes_calorie_badge,
                                                    suggestion.caloriesPerServing.toInt(),
                                                ),
                                                style = MaterialTheme.typography.labelMedium,
                                                color = MaterialTheme.colorScheme.primary,
                                            )
                                        }
                                        if (suggestion.recipe.description.isNotBlank()) {
                                            Text(
                                                text = suggestion.recipe.description,
                                                style = MaterialTheme.typography.bodySmall,
                                                maxLines = 2,
                                                overflow = TextOverflow.Ellipsis,
                                            )
                                        }
                                        Button(
                                            onClick = { viewModel.acceptSuggestion(suggestion) },
                                            modifier = Modifier.fillMaxWidth(),
                                        ) {
                                            Text(stringResource(Res.string.recipes_calorie_accept))
                                        }
                                    }
                                }
                            }
                        }
                    }
                }
            },
            confirmButton = {},
            dismissButton = {
                TextButton(onClick = viewModel::dismissCalorieTargetDialog) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.recipes_title)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
        floatingActionButton = {
            ExtendedFloatingActionButton(
                onClick = viewModel::onCreateRecipeClicked,
                icon = {
                    Icon(
                        imageVector = AppIcons.Add,
                        contentDescription = stringResource(Res.string.recipes_new),
                    )
                },
                text = { Text(stringResource(Res.string.recipes_new)) },
            )
        },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            OutlinedTextField(
                value = state.query,
                onValueChange = viewModel::onQueryChange,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
                keyboardActions = KeyboardActions(onSearch = { focusManager.clearFocus() }),
                placeholder = { Text(stringResource(Res.string.recipes_search_placeholder)) },
                leadingIcon = { Icon(imageVector = AppIcons.Search, contentDescription = null) },
                trailingIcon = if (state.query.isNotEmpty()) {
                    {
                        IconButton(
                            onClick = { viewModel.onQueryChange("") },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Icon(
                                imageVector = AppIcons.Close,
                                contentDescription = stringResource(Res.string.action_clear),
                            )
                        }
                    }
                } else {
                    null
                },
                modifier = Modifier.fillMaxWidth(),
            )

            Spacer(Modifier.height(8.dp))

            FlowRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                FilterChip(
                    selected = state.favoritesOnly,
                    onClick = viewModel::toggleFavoritesFilter,
                    label = { Text(stringResource(Res.string.recipes_filter_favorites)) },
                )
                TimeFilterChip(
                    label = stringResource(Res.string.recipes_filter_time_all),
                    selected = state.maxTotalMinutes == null,
                    onClick = { viewModel.setMaxTotalMinutes(null) },
                )
                TimeFilterChip(
                    label = stringResource(Res.string.recipes_filter_time_15),
                    selected = state.maxTotalMinutes == 15,
                    onClick = { viewModel.setMaxTotalMinutes(15) },
                )
                TimeFilterChip(
                    label = stringResource(Res.string.recipes_filter_time_30),
                    selected = state.maxTotalMinutes == 30,
                    onClick = { viewModel.setMaxTotalMinutes(30) },
                )
                TimeFilterChip(
                    label = stringResource(Res.string.recipes_filter_time_60),
                    selected = state.maxTotalMinutes == 60,
                    onClick = { viewModel.setMaxTotalMinutes(60) },
                )
                FilterChip(
                    selected = state.showCalorieDialog,
                    onClick = viewModel::openCalorieTargetDialog,
                    label = { Text(stringResource(Res.string.recipes_generate_by_calories)) },
                )
                if (state.hasActiveFilters) {
                    TextButton(onClick = viewModel::clearFilters) {
                        Text(stringResource(Res.string.recipes_clear_filters))
                    }
                }
            }

            if (state.availableTags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                TagChips(
                    tags = state.availableTags.map { it.name },
                    selectedTags = state.selectedTags,
                    onTagClick = viewModel::toggleTagFilter,
                )
            }

            Spacer(Modifier.height(8.dp))

            if (state.hasError) {
                ErrorBanner(
                    message = stringResource(Res.string.error_generic),
                    onRetry = viewModel::retry,
                )
                Spacer(Modifier.height(8.dp))
            }

            when {
                state.isLoading -> {
                    val loadingDesc = stringResource(Res.string.loading)
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.semantics { contentDescription = loadingDesc },
                        )
                    }
                }

                state.recipes.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        if (state.hasActiveFilters) {
                            EmptyState(
                                icon = AppIcons.Search,
                                title = stringResource(Res.string.recipes_no_matches_title),
                                subtitle = stringResource(Res.string.recipes_no_matches_subtitle),
                                actionLabel = stringResource(Res.string.recipes_clear_filters),
                                onAction = viewModel::clearFilters,
                            )
                        } else {
                            EmptyState(
                                icon = AppIcons.Recipes,
                                title = stringResource(Res.string.recipes_empty_title),
                                subtitle = stringResource(Res.string.recipes_empty_subtitle),
                                actionLabel = stringResource(Res.string.recipes_new),
                                onAction = viewModel::onCreateRecipeClicked,
                            )
                        }
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 88.dp, top = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        items(items = state.recipes, key = { it.id }) { recipe ->
                            RecipeCard(
                                recipe = recipe,
                                onClick = { viewModel.onRecipeClicked(recipe.id) },
                                onToggleFavorite = { viewModel.onToggleFavorite(recipe.id) },
                                onAddToShoppingList = { viewModel.onAddRecipeToShoppingList(recipe.id) },
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun TimeFilterChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}

@Composable
private fun RecipeCard(
    recipe: Recipe,
    onClick: () -> Unit,
    onToggleFavorite: () -> Unit,
    onAddToShoppingList: () -> Unit,
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = recipe.title,
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                IconButton(
                    onClick = onAddToShoppingList,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = AppIcons.Shopping,
                        contentDescription = stringResource(Res.string.action_add_to_shopping_list),
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                IconButton(
                    onClick = onToggleFavorite,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = if (recipe.isFavorite) AppIcons.FavoriteFilled else AppIcons.FavoriteBorder,
                        contentDescription = stringResource(Res.string.action_toggle_favorite),
                        tint = if (recipe.isFavorite) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        },
                    )
                }
            }
            if (recipe.description.isNotBlank()) {
                Text(
                    text = recipe.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    text = stringResource(Res.string.servings_count, recipe.baseServings),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.primary,
                )
                Text(
                    text = stringResource(Res.string.minutes_short, recipe.totalMinutes),
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val calsPerServing = recipe.caloriesPerServing
                if (calsPerServing != null && calsPerServing > 0.0) {
                    Text(
                        text = stringResource(Res.string.recipes_calorie_badge, calsPerServing.toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.secondary,
                    )
                }
            }
            if (recipe.tags.isNotEmpty()) {
                TagChips(tags = recipe.tags.map { it.name })
            }
        }
    }
}
