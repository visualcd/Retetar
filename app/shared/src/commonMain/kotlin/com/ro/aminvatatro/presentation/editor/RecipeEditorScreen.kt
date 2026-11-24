package com.ro.aminvatatro.presentation.editor

import aminvatatro.app.shared.generated.resources.Res
import aminvatatro.app.shared.generated.resources.action_back
import aminvatatro.app.shared.generated.resources.action_move_down
import aminvatatro.app.shared.generated.resources.action_move_up
import aminvatatro.app.shared.generated.resources.action_remove
import aminvatatro.app.shared.generated.resources.action_save
import aminvatatro.app.shared.generated.resources.editor_add_ingredient
import aminvatatro.app.shared.generated.resources.editor_add_step
import aminvatatro.app.shared.generated.resources.editor_add_tag
import aminvatatro.app.shared.generated.resources.editor_edit_title
import aminvatatro.app.shared.generated.resources.editor_field_cook_minutes
import aminvatatro.app.shared.generated.resources.editor_field_description
import aminvatatro.app.shared.generated.resources.editor_field_notes
import aminvatatro.app.shared.generated.resources.editor_field_prep_minutes
import aminvatatro.app.shared.generated.resources.editor_field_servings
import aminvatatro.app.shared.generated.resources.editor_field_title
import aminvatatro.app.shared.generated.resources.editor_calories_badge
import aminvatatro.app.shared.generated.resources.editor_ingredient_calories
import aminvatatro.app.shared.generated.resources.editor_ingredient_aisle
import aminvatatro.app.shared.generated.resources.editor_ingredient_name
import aminvatatro.app.shared.generated.resources.editor_ingredient_quantity
import aminvatatro.app.shared.generated.resources.editor_ingredient_unit
import aminvatatro.app.shared.generated.resources.editor_new_title
import aminvatatro.app.shared.generated.resources.editor_section_ingredients
import aminvatatro.app.shared.generated.resources.editor_section_steps
import aminvatatro.app.shared.generated.resources.editor_section_tags
import aminvatatro.app.shared.generated.resources.editor_step_placeholder
import aminvatatro.app.shared.generated.resources.editor_tag_placeholder
import aminvatatro.app.shared.generated.resources.error_generic
import aminvatatro.app.shared.generated.resources.loading
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
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.validation.RecipeValidationError
import com.ro.aminvatatro.ui.components.ErrorBanner
import com.ro.aminvatatro.ui.components.TagChips
import com.ro.aminvatatro.ui.icons.AppIcons
import com.ro.aminvatatro.ui.navigation.LocalWindowWidthClass
import com.ro.aminvatatro.ui.navigation.WindowWidthClass
import com.ro.aminvatatro.ui.resources.labelRes
import com.ro.aminvatatro.ui.resources.toMessage
import org.jetbrains.compose.resources.stringResource

/**
 * Screen for creating or editing a recipe with inline validation, dynamic ingredient/step lists,
 * up/down reordering, and tag chips.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun RecipeEditorScreen(
    viewModel: RecipeEditorViewModel,
    onNavigateBack: () -> Unit,
    onSaved: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val widthClass = LocalWindowWidthClass.current
    val focusManager = LocalFocusManager.current

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is RecipeEditorUiEvent.Saved -> onSaved(event.recipeId)
            }
        }
    }

    val titleError = state.validationErrors.firstOrNull {
        it is RecipeValidationError.TitleBlank || it is RecipeValidationError.TitleTooLong
    }
    val servingsError = state.validationErrors.firstOrNull { it is RecipeValidationError.InvalidServings }
    val prepError = state.validationErrors.firstOrNull { it is RecipeValidationError.InvalidPrepMinutes }
    val cookError = state.validationErrors.firstOrNull { it is RecipeValidationError.InvalidCookMinutes }
    val emptyIngredientsError = state.validationErrors.firstOrNull { it is RecipeValidationError.EmptyIngredients }
    val emptyStepsError = state.validationErrors.firstOrNull { it is RecipeValidationError.EmptySteps }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(
                            if (state.isEditing) Res.string.editor_edit_title else Res.string.editor_new_title,
                        ),
                    )
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
                actions = {
                    TextButton(
                        onClick = viewModel::save,
                        enabled = !state.isLoading && !state.isSaving,
                        modifier = Modifier.testTag("editor_save_action"),
                    ) {
                        Text(stringResource(Res.string.action_save))
                    }
                },
            )
        },
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

            state.loadError -> {
                Box(
                    modifier = Modifier.fillMaxSize().padding(innerPadding).padding(16.dp),
                    contentAlignment = Alignment.TopCenter,
                ) {
                    ErrorBanner(message = stringResource(Res.string.error_generic))
                }
            }

            else -> {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .testTag("recipe_editor_list"),
                    contentPadding = PaddingValues(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    item(key = "basic_fields") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            OutlinedTextField(
                                value = state.title,
                                onValueChange = viewModel::onTitleChange,
                                label = { Text(stringResource(Res.string.editor_field_title)) },
                                singleLine = true,
                                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                                keyboardActions = KeyboardActions(
                                    onNext = { focusManager.moveFocus(FocusDirection.Next) },
                                ),
                                isError = titleError != null,
                                supportingText = titleError?.let { err -> { Text(err.toMessage()) } },
                                modifier = Modifier.fillMaxWidth(),
                            )

                            OutlinedTextField(
                                value = state.description,
                                onValueChange = viewModel::onDescriptionChange,
                                label = { Text(stringResource(Res.string.editor_field_description)) },
                                maxLines = 3,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            if (widthClass == WindowWidthClass.COMPACT) {
                                OutlinedTextField(
                                    value = state.baseServingsText,
                                    onValueChange = viewModel::onBaseServingsChange,
                                    label = { Text(stringResource(Res.string.editor_field_servings)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(
                                        keyboardType = KeyboardType.Number,
                                        imeAction = ImeAction.Next,
                                    ),
                                    keyboardActions = KeyboardActions(
                                        onNext = { focusManager.moveFocus(FocusDirection.Next) },
                                    ),
                                    isError = servingsError != null,
                                    supportingText = servingsError?.let { err -> { Text(err.toMessage()) } },
                                    modifier = Modifier.fillMaxWidth(),
                                )
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedTextField(
                                        value = state.prepMinutesText,
                                        onValueChange = viewModel::onPrepMinutesChange,
                                        label = { Text(stringResource(Res.string.editor_field_prep_minutes)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next,
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Next) },
                                        ),
                                        isError = prepError != null,
                                        supportingText = prepError?.let { err -> { Text(err.toMessage()) } },
                                        modifier = Modifier.weight(1f),
                                    )
                                    OutlinedTextField(
                                        value = state.cookMinutesText,
                                        onValueChange = viewModel::onCookMinutesChange,
                                        label = { Text(stringResource(Res.string.editor_field_cook_minutes)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next,
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Next) },
                                        ),
                                        isError = cookError != null,
                                        supportingText = cookError?.let { err -> { Text(err.toMessage()) } },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            } else {
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                                ) {
                                    OutlinedTextField(
                                        value = state.baseServingsText,
                                        onValueChange = viewModel::onBaseServingsChange,
                                        label = { Text(stringResource(Res.string.editor_field_servings)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next,
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Next) },
                                        ),
                                        isError = servingsError != null,
                                        supportingText = servingsError?.let { err -> { Text(err.toMessage()) } },
                                        modifier = Modifier.weight(1f),
                                    )
                                    OutlinedTextField(
                                        value = state.prepMinutesText,
                                        onValueChange = viewModel::onPrepMinutesChange,
                                        label = { Text(stringResource(Res.string.editor_field_prep_minutes)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next,
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Next) },
                                        ),
                                        isError = prepError != null,
                                        supportingText = prepError?.let { err -> { Text(err.toMessage()) } },
                                        modifier = Modifier.weight(1f),
                                    )
                                    OutlinedTextField(
                                        value = state.cookMinutesText,
                                        onValueChange = viewModel::onCookMinutesChange,
                                        label = { Text(stringResource(Res.string.editor_field_cook_minutes)) },
                                        singleLine = true,
                                        keyboardOptions = KeyboardOptions(
                                            keyboardType = KeyboardType.Number,
                                            imeAction = ImeAction.Next,
                                        ),
                                        keyboardActions = KeyboardActions(
                                            onNext = { focusManager.moveFocus(FocusDirection.Next) },
                                        ),
                                        isError = cookError != null,
                                        supportingText = cookError?.let { err -> { Text(err.toMessage()) } },
                                        modifier = Modifier.weight(1f),
                                    )
                                }
                            }

                            val totalCals = state.totalCalories
                            val perServing = state.caloriesPerServing
                            if (totalCals != null && perServing != null && totalCals > 0.0) {
                                Text(
                                    text = stringResource(
                                        Res.string.editor_calories_badge,
                                        totalCals.toInt(),
                                        perServing.toInt(),
                                    ),
                                    style = MaterialTheme.typography.labelLarge,
                                    color = MaterialTheme.colorScheme.primary,
                                )
                            }
                        }
                    }

                    item(key = "ingredients_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(Res.string.editor_section_ingredients),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            OutlinedButton(onClick = viewModel::addIngredient) {
                                Icon(imageVector = AppIcons.Add, contentDescription = null)
                                Spacer(Modifier.padding(horizontal = 2.dp))
                                Text(stringResource(Res.string.editor_add_ingredient))
                            }
                        }
                        if (emptyIngredientsError != null) {
                            Spacer(Modifier.height(6.dp))
                            ErrorBanner(message = emptyIngredientsError.toMessage())
                        }
                    }

                    itemsIndexed(
                        items = state.ingredients,
                        key = { _, item -> "ing_${item.localKey}" },
                    ) { index, draft ->
                        val nameError = state.validationErrors.firstOrNull {
                            it is RecipeValidationError.IngredientNameBlank && it.index == index
                        }
                        val qtyError = state.validationErrors.firstOrNull {
                            (it is RecipeValidationError.InvalidIngredientQuantity && it.index == index) ||
                                (it is RecipeValidationError.QuantityNotAllowedForToTaste && it.index == index)
                        }

                        IngredientDraftCard(
                            draft = draft,
                            canMoveUp = index > 0,
                            canMoveDown = index < state.ingredients.lastIndex,
                            nameError = nameError,
                            qtyError = qtyError,
                            onNameChange = { viewModel.updateIngredient(index, name = it) },
                            onQuantityChange = { viewModel.updateIngredient(index, quantityText = it) },
                            onUnitChange = { viewModel.updateIngredient(index, unit = it) },
                            onAisleChange = { viewModel.updateIngredient(index, aisle = it) },
                            onMoveUp = { viewModel.moveIngredientUp(index) },
                            onMoveDown = { viewModel.moveIngredientDown(index) },
                            onRemove = { viewModel.removeIngredient(index) },
                        )
                    }

                    item(key = "steps_header") {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween,
                        ) {
                            Text(
                                text = stringResource(Res.string.editor_section_steps),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            OutlinedButton(onClick = viewModel::addStep) {
                                Icon(imageVector = AppIcons.Add, contentDescription = null)
                                Spacer(Modifier.padding(horizontal = 2.dp))
                                Text(stringResource(Res.string.editor_add_step))
                            }
                        }
                        if (emptyStepsError != null) {
                            Spacer(Modifier.height(6.dp))
                            ErrorBanner(message = emptyStepsError.toMessage())
                        }
                    }

                    itemsIndexed(
                        items = state.steps,
                        key = { _, item -> "step_${item.localKey}" },
                    ) { index, draft ->
                        val stepError = state.validationErrors.firstOrNull {
                            it is RecipeValidationError.StepTextBlank && it.index == index
                        }
                        StepDraftCard(
                            index = index,
                            draft = draft,
                            canMoveUp = index > 0,
                            canMoveDown = index < state.steps.lastIndex,
                            error = stepError,
                            onTextChange = { viewModel.updateStep(index, it) },
                            onMoveUp = { viewModel.moveStepUp(index) },
                            onMoveDown = { viewModel.moveStepDown(index) },
                            onRemove = { viewModel.removeStep(index) },
                        )
                    }

                    item(key = "tags_and_notes") {
                        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                            Text(
                                text = stringResource(Res.string.editor_section_tags),
                                style = MaterialTheme.typography.titleMedium,
                            )
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(8.dp),
                            ) {
                                OutlinedTextField(
                                    value = state.tagInput,
                                    onValueChange = viewModel::onTagInputChange,
                                    placeholder = { Text(stringResource(Res.string.editor_tag_placeholder)) },
                                    singleLine = true,
                                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                                    keyboardActions = KeyboardActions(onDone = { viewModel.addTag() }),
                                    modifier = Modifier
                                        .weight(1f)
                                        .onPreviewKeyEvent { keyEvent ->
                                            if (keyEvent.type == KeyEventType.KeyUp &&
                                                (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                            ) {
                                                viewModel.addTag()
                                                true
                                            } else {
                                                false
                                            }
                                        },
                                )
                                OutlinedButton(onClick = viewModel::addTag) {
                                    Text(stringResource(Res.string.editor_add_tag))
                                }
                            }
                            TagChips(
                                tags = state.tags,
                                onRemoveTag = viewModel::removeTag,
                            )

                            OutlinedTextField(
                                value = state.notes,
                                onValueChange = viewModel::onNotesChange,
                                label = { Text(stringResource(Res.string.editor_field_notes)) },
                                minLines = 2,
                                maxLines = 5,
                                modifier = Modifier.fillMaxWidth(),
                            )

                            Spacer(Modifier.height(8.dp))
                            Button(
                                onClick = viewModel::save,
                                enabled = !state.isSaving,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text(stringResource(Res.string.action_save))
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IngredientDraftCard(
    draft: IngredientDraft,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    nameError: RecipeValidationError?,
    qtyError: RecipeValidationError?,
    onNameChange: (String) -> Unit,
    onQuantityChange: (String) -> Unit,
    onUnitChange: (MeasurementUnit) -> Unit,
    onAisleChange: (Aisle) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    var unitMenuOpen by remember { mutableStateOf(false) }
    var aisleMenuOpen by remember { mutableStateOf(false) }

    Card(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                OutlinedTextField(
                    value = draft.name,
                    onValueChange = onNameChange,
                    label = { Text(stringResource(Res.string.editor_ingredient_name)) },
                    singleLine = true,
                    isError = nameError != null,
                    supportingText = nameError?.let { err -> { Text(err.toMessage()) } },
                    modifier = Modifier.weight(1f),
                )
                ReorderAndRemoveButtons(
                    canMoveUp = canMoveUp,
                    canMoveDown = canMoveDown,
                    onMoveUp = onMoveUp,
                    onMoveDown = onMoveDown,
                    onRemove = onRemove,
                )
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                OutlinedTextField(
                    value = draft.quantityText,
                    onValueChange = onQuantityChange,
                    label = { Text(stringResource(Res.string.editor_ingredient_quantity)) },
                    enabled = draft.unit != MeasurementUnit.TO_TASTE,
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    isError = qtyError != null,
                    supportingText = qtyError?.let { err -> { Text(err.toMessage()) } },
                    modifier = Modifier.weight(1f),
                )

                Box {
                    OutlinedButton(onClick = { unitMenuOpen = true }) {
                        Text(
                            "${stringResource(Res.string.editor_ingredient_unit)}: ${stringResource(draft.unit.labelRes())}",
                        )
                    }
                    DropdownMenu(
                        expanded = unitMenuOpen,
                        onDismissRequest = { unitMenuOpen = false },
                    ) {
                        MeasurementUnit.entries.forEach { unit ->
                            DropdownMenuItem(
                                text = { Text(stringResource(unit.labelRes())) },
                                onClick = {
                                    unitMenuOpen = false
                                    onUnitChange(unit)
                                },
                            )
                        }
                    }
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Box {
                    OutlinedButton(onClick = { aisleMenuOpen = true }) {
                        Text(
                            "${stringResource(Res.string.editor_ingredient_aisle)}: ${stringResource(draft.aisle.labelRes())}",
                        )
                    }
                    DropdownMenu(
                        expanded = aisleMenuOpen,
                        onDismissRequest = { aisleMenuOpen = false },
                    ) {
                        Aisle.entries.forEach { aisle ->
                            DropdownMenuItem(
                                text = { Text(stringResource(aisle.labelRes())) },
                                onClick = {
                                    aisleMenuOpen = false
                                    onAisleChange(aisle)
                                },
                            )
                        }
                    }
                }

                if (draft.isEstimatingCalories) {
                    CircularProgressIndicator(modifier = Modifier.padding(end = 8.dp))
                } else if (draft.calories != null && draft.calories > 0.0) {
                    Text(
                        text = stringResource(Res.string.editor_ingredient_calories, draft.calories.toInt()),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(end = 8.dp),
                    )
                }
            }
        }
    }
}

@Composable
private fun StepDraftCard(
    index: Int,
    draft: StepDraft,
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    error: RecipeValidationError?,
    onTextChange: (String) -> Unit,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.padding(12.dp).fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            OutlinedTextField(
                value = draft.text,
                onValueChange = onTextChange,
                label = { Text(stringResource(Res.string.editor_step_placeholder, index + 1)) },
                minLines = 2,
                maxLines = 4,
                isError = error != null,
                supportingText = error?.let { err -> { Text(err.toMessage()) } },
                modifier = Modifier.weight(1f),
            )
            ReorderAndRemoveButtons(
                canMoveUp = canMoveUp,
                canMoveDown = canMoveDown,
                onMoveUp = onMoveUp,
                onMoveDown = onMoveDown,
                onRemove = onRemove,
            )
        }
    }
}

@Composable
private fun ReorderAndRemoveButtons(
    canMoveUp: Boolean,
    canMoveDown: Boolean,
    onMoveUp: () -> Unit,
    onMoveDown: () -> Unit,
    onRemove: () -> Unit,
) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        IconButton(
            onClick = onMoveUp,
            enabled = canMoveUp,
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Icon(
                imageVector = AppIcons.ArrowUp,
                contentDescription = stringResource(Res.string.action_move_up),
            )
        }
        IconButton(
            onClick = onMoveDown,
            enabled = canMoveDown,
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Icon(
                imageVector = AppIcons.ArrowDown,
                contentDescription = stringResource(Res.string.action_move_down),
            )
        }
        IconButton(
            onClick = onRemove,
            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
        ) {
            Icon(
                imageVector = AppIcons.Delete,
                contentDescription = stringResource(Res.string.action_remove),
            )
        }
    }
}
