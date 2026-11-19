package com.ro.aminvatatro.presentation.shopping

import aminvatatro.app.shared.generated.resources.Res
import aminvatatro.app.shared.generated.resources.action_add
import aminvatatro.app.shared.generated.resources.action_cancel
import aminvatatro.app.shared.generated.resources.action_delete
import aminvatatro.app.shared.generated.resources.action_remove
import aminvatatro.app.shared.generated.resources.editor_ingredient_aisle
import aminvatatro.app.shared.generated.resources.editor_ingredient_unit
import aminvatatro.app.shared.generated.resources.error_generic
import aminvatatro.app.shared.generated.resources.loading
import aminvatatro.app.shared.generated.resources.planner_next_week
import aminvatatro.app.shared.generated.resources.planner_prev_week
import aminvatatro.app.shared.generated.resources.planner_today
import aminvatatro.app.shared.generated.resources.planner_week_label
import aminvatatro.app.shared.generated.resources.shopping_add_dialog_title
import aminvatatro.app.shared.generated.resources.shopping_add_manual
import aminvatatro.app.shared.generated.resources.shopping_clear_checked
import aminvatatro.app.shared.generated.resources.shopping_clear_confirm_message
import aminvatatro.app.shared.generated.resources.shopping_clear_confirm_title
import aminvatatro.app.shared.generated.resources.shopping_copied
import aminvatatro.app.shared.generated.resources.shopping_copy_list
import aminvatatro.app.shared.generated.resources.shopping_empty_subtitle
import aminvatatro.app.shared.generated.resources.shopping_empty_title
import aminvatatro.app.shared.generated.resources.shopping_error_invalid_quantity
import aminvatatro.app.shared.generated.resources.shopping_error_name_blank
import aminvatatro.app.shared.generated.resources.shopping_error_name_too_long
import aminvatatro.app.shared.generated.resources.shopping_item_name
import aminvatatro.app.shared.generated.resources.shopping_item_quantity_optional
import aminvatatro.app.shared.generated.resources.shopping_manual_badge
import aminvatatro.app.shared.generated.resources.shopping_regenerate
import aminvatatro.app.shared.generated.resources.shopping_regenerated
import aminvatatro.app.shared.generated.resources.shopping_title
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
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusDirection
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.platform.LocalClipboardManager
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.ShoppingItem
import com.ro.aminvatatro.domain.model.ShoppingItemSource
import com.ro.aminvatatro.ui.components.ConfirmDialog
import com.ro.aminvatatro.ui.components.EmptyState
import com.ro.aminvatatro.ui.components.ErrorBanner
import com.ro.aminvatatro.ui.icons.AppIcons
import com.ro.aminvatatro.ui.resources.formatShortDayMonth
import com.ro.aminvatatro.ui.resources.labelRes
import kotlinx.coroutines.launch
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Weekly shopping list screen with aisle grouping, checkable items, manual item creation,
 * clear-checked confirmation, regeneration from the meal plan, and plain-text clipboard copy.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun ShoppingListScreen(
    viewModel: ShoppingListViewModel,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    @Suppress("DEPRECATION")
    val clipboardManager = LocalClipboardManager.current

    val aisleLabels = Aisle.entries.associateWith { stringResource(it.labelRes()) }
    val unitLabels = MeasurementUnit.entries.associateWith { stringResource(it.labelRes()) }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                ShoppingListUiEvent.Regenerated -> {
                    snackbarHostState.showSnackbar(getString(Res.string.shopping_regenerated))
                }
            }
        }
    }

    if (state.showClearCheckedDialog) {
        ConfirmDialog(
            title = stringResource(Res.string.shopping_clear_confirm_title),
            message = stringResource(Res.string.shopping_clear_confirm_message, state.checkedCount),
            confirmLabel = stringResource(Res.string.action_delete),
            cancelLabel = stringResource(Res.string.action_cancel),
            isDestructive = true,
            onConfirm = viewModel::confirmClearChecked,
            onDismiss = viewModel::dismissClearCheckedDialog,
        )
    }

    if (state.showAddManualDialog) {
        var unitMenuOpen by remember { mutableStateOf(false) }
        var aisleMenuOpen by remember { mutableStateOf(false) }

        val errorMessage = when (val err = state.manualError) {
            null -> null
            ManualItemError.NameBlank -> stringResource(Res.string.shopping_error_name_blank)
            is ManualItemError.NameTooLong -> stringResource(Res.string.shopping_error_name_too_long, err.maxLength)
            ManualItemError.InvalidQuantity -> stringResource(Res.string.shopping_error_invalid_quantity)
        }

        AlertDialog(
            onDismissRequest = viewModel::dismissAddManualDialog,
            title = { Text(stringResource(Res.string.shopping_add_dialog_title)) },
            text = {
                val dialogFocusManager = LocalFocusManager.current
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    OutlinedTextField(
                        value = state.manualName,
                        onValueChange = viewModel::onManualNameChange,
                        label = { Text(stringResource(Res.string.shopping_item_name)) },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(imeAction = ImeAction.Next),
                        keyboardActions = KeyboardActions(
                            onNext = { dialogFocusManager.moveFocus(FocusDirection.Next) },
                        ),
                        isError = state.manualError is ManualItemError.NameBlank ||
                            state.manualError is ManualItemError.NameTooLong,
                        modifier = Modifier.fillMaxWidth(),
                    )
                    OutlinedTextField(
                        value = state.manualQuantityText,
                        onValueChange = viewModel::onManualQuantityChange,
                        label = { Text(stringResource(Res.string.shopping_item_quantity_optional)) },
                        enabled = state.manualUnit != MeasurementUnit.TO_TASTE,
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Decimal,
                            imeAction = ImeAction.Done,
                        ),
                        keyboardActions = KeyboardActions(onDone = { viewModel.submitManualItem() }),
                        isError = state.manualError is ManualItemError.InvalidQuantity,
                        modifier = Modifier
                            .fillMaxWidth()
                            .onPreviewKeyEvent { keyEvent ->
                                if (keyEvent.type == KeyEventType.KeyUp &&
                                    (keyEvent.key == Key.Enter || keyEvent.key == Key.NumPadEnter)
                                ) {
                                    viewModel.submitManualItem()
                                    true
                                } else {
                                    false
                                }
                            },
                    )
                    FlowRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp),
                    ) {
                        Box {
                            OutlinedButton(onClick = { unitMenuOpen = true }) {
                                Text(
                                    "${stringResource(Res.string.editor_ingredient_unit)}: ${unitLabels[state.manualUnit].orEmpty()}",
                                )
                            }
                            DropdownMenu(
                                expanded = unitMenuOpen,
                                onDismissRequest = { unitMenuOpen = false },
                            ) {
                                MeasurementUnit.entries.forEach { unit ->
                                    DropdownMenuItem(
                                        text = { Text(unitLabels[unit].orEmpty()) },
                                        onClick = {
                                            unitMenuOpen = false
                                            viewModel.onManualUnitChange(unit)
                                        },
                                    )
                                }
                            }
                        }
                        Box {
                            OutlinedButton(onClick = { aisleMenuOpen = true }) {
                                Text(
                                    "${stringResource(Res.string.editor_ingredient_aisle)}: ${aisleLabels[state.manualAisle].orEmpty()}",
                                )
                            }
                            DropdownMenu(
                                expanded = aisleMenuOpen,
                                onDismissRequest = { aisleMenuOpen = false },
                            ) {
                                Aisle.entries.forEach { aisle ->
                                    DropdownMenuItem(
                                        text = { Text(aisleLabels[aisle].orEmpty()) },
                                        onClick = {
                                            aisleMenuOpen = false
                                            viewModel.onManualAisleChange(aisle)
                                        },
                                    )
                                }
                            }
                        }
                    }
                    if (errorMessage != null) {
                        Text(
                            text = errorMessage,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::submitManualItem) {
                    Text(stringResource(Res.string.action_add))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissAddManualDialog) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.shopping_title)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 16.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                IconButton(
                    onClick = viewModel::previousWeek,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = AppIcons.ChevronLeft,
                        contentDescription = stringResource(Res.string.planner_prev_week),
                    )
                }
                Text(
                    text = stringResource(
                        Res.string.planner_week_label,
                        state.isoWeekInfo.weekNumber,
                        state.weekStart.formatShortDayMonth(),
                        state.weekEnd.formatShortDayMonth(),
                    ),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
                IconButton(
                    onClick = viewModel::nextWeek,
                    modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                ) {
                    Icon(
                        imageVector = AppIcons.ChevronRight,
                        contentDescription = stringResource(Res.string.planner_next_week),
                    )
                }
            }

            FlowRow(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Button(
                    onClick = viewModel::regenerate,
                    enabled = !state.isBusy,
                ) {
                    Icon(imageVector = AppIcons.Refresh, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 2.dp))
                    Text(stringResource(Res.string.shopping_regenerate))
                }
                OutlinedButton(onClick = viewModel::openAddManualDialog) {
                    Icon(imageVector = AppIcons.Add, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 2.dp))
                    Text(stringResource(Res.string.shopping_add_manual))
                }
                OutlinedButton(
                    onClick = viewModel::requestClearChecked,
                    enabled = state.checkedCount > 0,
                ) {
                    Text(stringResource(Res.string.shopping_clear_checked))
                }
                OutlinedButton(
                    onClick = {
                        val plain = formatShoppingListAsText(
                            itemsByAisle = state.itemsByAisle,
                            aisleLabel = { aisleLabels[it].orEmpty() },
                            unitLabel = { unitLabels[it].orEmpty() },
                            language = state.language,
                            unitSystem = state.unitSystem,
                        )
                        clipboardManager.setText(AnnotatedString(plain))
                        coroutineScope.launch {
                            snackbarHostState.showSnackbar(getString(Res.string.shopping_copied))
                        }
                    },
                    enabled = state.items.isNotEmpty(),
                ) {
                    Icon(imageVector = AppIcons.Copy, contentDescription = null)
                    Spacer(Modifier.padding(horizontal = 2.dp))
                    Text(stringResource(Res.string.shopping_copy_list))
                }
                TextButton(onClick = viewModel::goToCurrentWeek) {
                    Text(stringResource(Res.string.planner_today))
                }
            }

            if (state.isBusy) {
                Spacer(Modifier.height(6.dp))
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            Spacer(Modifier.height(8.dp))

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

                state.items.isEmpty() -> {
                    Box(
                        modifier = Modifier.fillMaxSize(),
                        contentAlignment = Alignment.Center,
                    ) {
                        EmptyState(
                            icon = AppIcons.Shopping,
                            title = stringResource(Res.string.shopping_empty_title),
                            subtitle = stringResource(Res.string.shopping_empty_subtitle),
                            actionLabel = stringResource(Res.string.shopping_regenerate),
                            onAction = viewModel::regenerate,
                        )
                    }
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier.fillMaxSize(),
                        contentPadding = PaddingValues(bottom = 24.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        state.itemsByAisle.forEach { (aisle, aisleItems) ->
                            item(key = "aisle_${aisle.name}") {
                                Text(
                                    text = aisleLabels[aisle].orEmpty(),
                                    style = MaterialTheme.typography.titleSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                                )
                            }
                            items(items = aisleItems, key = { it.id }) { item ->
                                ShoppingItemRow(
                                    item = item,
                                    formattedQuantity = formatShoppingQuantity(
                                        item = item,
                                        unitLabel = { unitLabels[it].orEmpty() },
                                        language = state.language,
                                        unitSystem = state.unitSystem,
                                    ),
                                    onToggle = { checked -> viewModel.onToggleItem(item.id, checked) },
                                    onRemove = { viewModel.onRemoveItem(item.id) },
                                )
                                HorizontalDivider()
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ShoppingItemRow(
    item: ShoppingItem,
    formattedQuantity: String,
    onToggle: (Boolean) -> Unit,
    onRemove: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .defaultMinSize(minHeight = 48.dp)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Row(
            modifier = Modifier
                .weight(1f)
                .defaultMinSize(minHeight = 48.dp)
                .toggleable(
                    value = item.checked,
                    role = Role.Checkbox,
                    onValueChange = onToggle,
                ),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Checkbox(
                checked = item.checked,
                onCheckedChange = null,
            )
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.bodyLarge,
                        textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None,
                        color = if (item.checked) {
                            MaterialTheme.colorScheme.onSurfaceVariant
                        } else {
                            MaterialTheme.colorScheme.onSurface
                        },
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (item.source == ShoppingItemSource.MANUAL) {
                        Surface(
                            shape = MaterialTheme.shapes.extraSmall,
                            color = MaterialTheme.colorScheme.tertiaryContainer,
                        ) {
                            Text(
                                text = stringResource(Res.string.shopping_manual_badge),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onTertiaryContainer,
                                modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp),
                            )
                        }
                    }
                }
                if (formattedQuantity.isNotEmpty()) {
                    Text(
                        text = formattedQuantity,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
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
