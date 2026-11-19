package com.ro.aminvatatro.presentation.detail

import aminvatatro.app.shared.generated.resources.Res
import aminvatatro.app.shared.generated.resources.action_add
import aminvatatro.app.shared.generated.resources.action_back
import aminvatatro.app.shared.generated.resources.action_cancel
import aminvatatro.app.shared.generated.resources.action_delete
import aminvatatro.app.shared.generated.resources.action_edit
import aminvatatro.app.shared.generated.resources.action_toggle_favorite
import aminvatatro.app.shared.generated.resources.detail_add_to_plan
import aminvatatro.app.shared.generated.resources.detail_add_to_plan_success
import aminvatatro.app.shared.generated.resources.detail_add_to_plan_title
import aminvatatro.app.shared.generated.resources.detail_cook_time
import aminvatatro.app.shared.generated.resources.detail_date_label
import aminvatatro.app.shared.generated.resources.detail_delete_message
import aminvatatro.app.shared.generated.resources.detail_delete_message_with_plan
import aminvatatro.app.shared.generated.resources.detail_delete_title
import aminvatatro.app.shared.generated.resources.detail_ingredients
import aminvatatro.app.shared.generated.resources.detail_not_found_subtitle
import aminvatatro.app.shared.generated.resources.detail_not_found_title
import aminvatatro.app.shared.generated.resources.detail_notes
import aminvatatro.app.shared.generated.resources.detail_prep_time
import aminvatatro.app.shared.generated.resources.detail_servings
import aminvatatro.app.shared.generated.resources.detail_slot_label
import aminvatatro.app.shared.generated.resources.detail_steps
import aminvatatro.app.shared.generated.resources.detail_title
import aminvatatro.app.shared.generated.resources.detail_total_time
import aminvatatro.app.shared.generated.resources.error_generic
import aminvatatro.app.shared.generated.resources.loading
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.ui.components.ConfirmDialog
import com.ro.aminvatatro.ui.components.EmptyState
import com.ro.aminvatatro.ui.components.ErrorBanner
import com.ro.aminvatatro.ui.components.IngredientRow
import com.ro.aminvatatro.ui.components.ServingsStepper
import com.ro.aminvatatro.ui.components.TagChips
import com.ro.aminvatatro.ui.icons.AppIcons
import com.ro.aminvatatro.ui.resources.formatShortDayMonth
import com.ro.aminvatatro.ui.resources.labelRes
import com.ro.aminvatatro.ui.resources.shortLabelRes
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Recipe detail screen with live serving scaler, scaled ingredients, steps, favorite toggle,
 * quick "Add to plan" dialog, edit action, and delete confirmation showing affected plan entries.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun RecipeDetailScreen(
    viewModel: RecipeDetailViewModel,
    onNavigateBack: () -> Unit,
    onEditRecipe: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RecipeDetailUiEvent.Deleted -> onNavigateBack()
                RecipeDetailUiEvent.AddedToPlan -> {
                    snackbarHostState.showSnackbar(getString(Res.string.detail_add_to_plan_success))
                }
            }
        }
    }

    val recipe = state.recipe

    if (state.showDeleteDialog && recipe != null) {
        val message = if (state.affectedPlanEntriesCount > 0) {
            stringResource(
                Res.string.detail_delete_message_with_plan,
                recipe.title,
                state.affectedPlanEntriesCount,
            )
        } else {
            stringResource(Res.string.detail_delete_message, recipe.title)
        }
        ConfirmDialog(
            title = stringResource(Res.string.detail_delete_title),
            message = message,
            confirmLabel = stringResource(Res.string.action_delete),
            cancelLabel = stringResource(Res.string.action_cancel),
            isDestructive = true,
            onConfirm = viewModel::confirmDelete,
            onDismiss = viewModel::dismissDeleteDialog,
        )
    }

    if (state.showAddToPlanDialog && recipe != null) {
        AlertDialog(
            onDismissRequest = viewModel::dismissAddToPlanDialog,
            title = { Text(stringResource(Res.string.detail_add_to_plan_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(Res.string.detail_date_label), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.currentWeekDays.forEach { day ->
                            FilterChip(
                                selected = state.selectedPlanDate == day,
                                onClick = { viewModel.selectPlanDate(day) },
                                label = {
                                    Text("${stringResource(day.dayOfWeek.shortLabelRes())} ${day.formatShortDayMonth()}")
                                },
                            )
                        }
                    }
                    Text(stringResource(Res.string.detail_slot_label), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        MealSlot.entries.forEach { slot ->
                            FilterChip(
                                selected = state.selectedPlanSlot == slot,
                                onClick = { viewModel.selectPlanSlot(slot) },
                                label = { Text(stringResource(slot.labelRes())) },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmAddToPlan) {
                    Text(stringResource(Res.string.action_add))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAddToPlanDialog) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(recipe?.title ?: stringResource(Res.string.detail_title)) },
                navigationIcon = {
                    IconButton(
                        onClick = onNavigateBack,
                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                    ) {
                        Icon(
                            imageVector = AppIcons.ArrowBack,
                            contentDescription = stringResource(Res.string.action_back),
                        )
                    }
                },
                actions = {
                    if (recipe != null) {
                        IconButton(
                            onClick = viewModel::onToggleFavorite,
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
                        IconButton(
                            onClick = { onEditRecipe(recipe.id) },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Icon(
                                imageVector = AppIcons.Edit,
                                contentDescription = stringResource(Res.string.action_edit),
                            )
                        }
                        IconButton(
                            onClick = viewModel::requestDelete,
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Icon(
                                imageVector = AppIcons.Delete,
                                contentDescription = stringResource(Res.string.action_delete),
                            )
                        }
                    }
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        when {
            state.isLoading -> {
                val loadingDesc = stringResource(Res.string.loading)
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics { contentDescription = loadingDesc },
                    )
                }
            }

            recipe == null -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding),
                    contentAlignment = Alignment.Center,
                ) {
                    EmptyState(
                        icon = AppIcons.Recipes,
                        title = stringResource(Res.string.detail_not_found_title),
                        subtitle = stringResource(Res.string.detail_not_found_subtitle),
                    )
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    if (state.hasError) {
                        item(key = "error") {
                            ErrorBanner(message = stringResource(Res.string.error_generic))
                        }
                    }

                    item(key = "header") {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            if (recipe.description.isNotBlank()) {
                                Text(
                                    text = recipe.description,
                                    style = MaterialTheme.typography.bodyLarge,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            FlowRow(
                                horizontalArrangement = Arrangement.spacedBy(12.dp),
                                verticalArrangement = Arrangement.spacedBy(4.dp),
                            ) {
                                Text(
                                    text = stringResource(Res.string.detail_prep_time, recipe.prepMinutes),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Text(
                                    text = stringResource(Res.string.detail_cook_time, recipe.cookMinutes),
                                    style = MaterialTheme.typography.labelLarge,
                                )
                                Text(
                                    text = stringResource(Res.string.detail_total_time, recipe.totalMinutes),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                            if (recipe.tags.isNotEmpty()) {
                                TagChips(tags = recipe.tags.map { it.name })
                            }
                        }
                    }

                    item(key = "servings_and_plan") {
                        Card(modifier = Modifier.fillMaxWidth()) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                            ) {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.SpaceBetween,
                                ) {
                                    Text(
                                        text = stringResource(Res.string.detail_servings),
                                        style = MaterialTheme.typography.titleMedium,
                                    )
                                    ServingsStepper(
                                        servings = state.selectedServings,
                                        onServingsChange = viewModel::onServingsChange,
                                    )
                                }
                                Button(
                                    onClick = viewModel::openAddToPlanDialog,
                                    modifier = Modifier.fillMaxWidth(),
                                ) {
                                    Icon(imageVector = AppIcons.Planner, contentDescription = null)
                                    Spacer(Modifier.padding(horizontal = 4.dp))
                                    Text(stringResource(Res.string.detail_add_to_plan))
                                }
                            }
                        }
                    }

                    item(key = "ingredients_title") {
                        Text(
                            text = stringResource(Res.string.detail_ingredients),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    itemsIndexed(
                        items = state.scaledIngredients,
                        key = { _, scaled -> scaled.ingredient.id },
                    ) { index, scaled ->
                        IngredientRow(
                            name = scaled.ingredient.name,
                            quantity = scaled.quantity,
                            unit = scaled.unit,
                            aisle = scaled.ingredient.aisle,
                            language = state.language,
                        )
                        if (index < state.scaledIngredients.lastIndex) {
                            HorizontalDivider()
                        }
                    }

                    item(key = "steps_title") {
                        Text(
                            text = stringResource(Res.string.detail_steps),
                            style = MaterialTheme.typography.titleMedium,
                        )
                    }

                    itemsIndexed(
                        items = recipe.steps,
                        key = { _, step -> step.id },
                    ) { index, step ->
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp),
                            verticalAlignment = Alignment.Top,
                        ) {
                            Text(
                                text = "${index + 1}.",
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary,
                            )
                            Text(
                                text = step.text,
                                style = MaterialTheme.typography.bodyLarge,
                                modifier = Modifier.weight(1f),
                            )
                        }
                    }

                    if (recipe.notes.isNotBlank()) {
                        item(key = "notes") {
                            Card(modifier = Modifier.fillMaxWidth()) {
                                Column(
                                    modifier = Modifier.padding(14.dp),
                                    verticalArrangement = Arrangement.spacedBy(6.dp),
                                ) {
                                    Text(
                                        text = stringResource(Res.string.detail_notes),
                                        style = MaterialTheme.typography.titleSmall,
                                    )
                                    Text(
                                        text = recipe.notes,
                                        style = MaterialTheme.typography.bodyMedium,
                                    )
                                }
                            }
                        }
                    }

                    item(key = "bottom_spacer") {
                        Spacer(Modifier.height(16.dp))
                    }
                }
            }
        }
    }
}
