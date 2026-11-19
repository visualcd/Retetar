package com.ro.aminvatatro.presentation.picker

import aminvatatro.app.shared.generated.resources.Res
import aminvatatro.app.shared.generated.resources.action_back
import aminvatatro.app.shared.generated.resources.action_clear
import aminvatatro.app.shared.generated.resources.error_generic
import aminvatatro.app.shared.generated.resources.loading
import aminvatatro.app.shared.generated.resources.minutes_short
import aminvatatro.app.shared.generated.resources.picker_add_confirm
import aminvatatro.app.shared.generated.resources.picker_empty_subtitle
import aminvatatro.app.shared.generated.resources.picker_empty_title
import aminvatatro.app.shared.generated.resources.picker_search_placeholder
import aminvatatro.app.shared.generated.resources.picker_servings_label
import aminvatatro.app.shared.generated.resources.picker_subtitle
import aminvatatro.app.shared.generated.resources.picker_title
import aminvatatro.app.shared.generated.resources.servings_count
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ro.aminvatatro.ui.components.EmptyState
import com.ro.aminvatatro.ui.components.ErrorBanner
import com.ro.aminvatatro.ui.components.ServingsStepper
import com.ro.aminvatatro.ui.icons.AppIcons
import com.ro.aminvatatro.ui.resources.formatShortDayMonth
import com.ro.aminvatatro.ui.resources.fullLabelRes
import com.ro.aminvatatro.ui.resources.labelRes
import org.jetbrains.compose.resources.stringResource

/**
 * Secondary screen allowing the user to search for a recipe, choose servings,
 * and add it to a specific meal plan date and slot.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipePickerScreen(
    viewModel: RecipePickerViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val focusManager = LocalFocusManager.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                RecipePickerUiEvent.EntryAdded -> onNavigateBack()
            }
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(stringResource(Res.string.picker_title))
                        Text(
                            text = stringResource(
                                Res.string.picker_subtitle,
                                "${stringResource(state.date.dayOfWeek.fullLabelRes())} ${state.date.formatShortDayMonth()}",
                                stringResource(state.slot.labelRes()),
                            ),
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                },
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
            )
        },
        bottomBar = {
            val selected = state.selectedRecipe
            if (selected != null) {
                Surface(tonalElevation = 4.dp) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(10.dp),
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(Res.string.picker_servings_label),
                                style = MaterialTheme.typography.titleSmall,
                            )
                            ServingsStepper(
                                servings = state.servings,
                                onServingsChange = viewModel::onServingsChange,
                            )
                        }
                        Button(
                            onClick = viewModel::confirmSelection,
                            enabled = !state.isSaving,
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(stringResource(Res.string.picker_add_confirm))
                        }
                    }
                }
            }
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
                placeholder = { Text(stringResource(Res.string.picker_search_placeholder)) },
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

            Spacer(Modifier.height(10.dp))

            if (state.hasError) {
                ErrorBanner(message = stringResource(Res.string.error_generic))
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
                        EmptyState(
                            icon = AppIcons.Recipes,
                            title = stringResource(Res.string.picker_empty_title),
                            subtitle = stringResource(Res.string.picker_empty_subtitle),
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        items(items = state.recipes, key = { it.id }) { recipe ->
                            val isSelected = state.selectedRecipe?.id == recipe.id
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .selectable(
                                        selected = isSelected,
                                        role = Role.RadioButton,
                                        onClick = { viewModel.selectRecipe(recipe) },
                                    ),
                                border = if (isSelected) {
                                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                                } else {
                                    null
                                },
                                colors = if (isSelected) {
                                    CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.primaryContainer,
                                    )
                                } else {
                                    CardDefaults.cardColors()
                                },
                            ) {
                                Row(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(14.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = recipe.title,
                                            style = MaterialTheme.typography.titleMedium,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = "${stringResource(Res.string.servings_count, recipe.baseServings)} · ${stringResource(Res.string.minutes_short, recipe.totalMinutes)}",
                                            style = MaterialTheme.typography.labelMedium,
                                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                                        )
                                    }
                                    if (isSelected) {
                                        Icon(
                                            imageVector = AppIcons.Check,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
