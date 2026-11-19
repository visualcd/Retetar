package com.ro.aminvatatro.presentation.planner

import aminvatatro.app.shared.generated.resources.Res
import aminvatatro.app.shared.generated.resources.action_cancel
import aminvatatro.app.shared.generated.resources.action_confirm
import aminvatatro.app.shared.generated.resources.action_remove
import aminvatatro.app.shared.generated.resources.action_save
import aminvatatro.app.shared.generated.resources.error_generic
import aminvatatro.app.shared.generated.resources.loading
import aminvatatro.app.shared.generated.resources.planner_add_to_slot
import aminvatatro.app.shared.generated.resources.planner_copy_day
import aminvatatro.app.shared.generated.resources.planner_copy_day_title
import aminvatatro.app.shared.generated.resources.planner_copy_done
import aminvatatro.app.shared.generated.resources.planner_copy_empty
import aminvatatro.app.shared.generated.resources.planner_copy_from
import aminvatatro.app.shared.generated.resources.planner_copy_same_day
import aminvatatro.app.shared.generated.resources.planner_copy_to
import aminvatatro.app.shared.generated.resources.planner_edit_servings_title
import aminvatatro.app.shared.generated.resources.planner_empty_slot
import aminvatatro.app.shared.generated.resources.planner_generate_shopping
import aminvatatro.app.shared.generated.resources.planner_next_week
import aminvatatro.app.shared.generated.resources.planner_prev_week
import aminvatatro.app.shared.generated.resources.planner_repeat_done
import aminvatatro.app.shared.generated.resources.planner_repeat_empty
import aminvatatro.app.shared.generated.resources.planner_repeat_week
import aminvatatro.app.shared.generated.resources.planner_title
import aminvatatro.app.shared.generated.resources.planner_today
import aminvatatro.app.shared.generated.resources.planner_week_label
import aminvatatro.app.shared.generated.resources.servings_count
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.ui.components.ErrorBanner
import com.ro.aminvatatro.ui.components.ServingsStepper
import com.ro.aminvatatro.ui.icons.AppIcons
import com.ro.aminvatatro.ui.navigation.LocalWindowWidthClass
import com.ro.aminvatatro.ui.navigation.WindowWidthClass
import com.ro.aminvatatro.ui.resources.formatShortDayMonth
import com.ro.aminvatatro.ui.resources.fullLabelRes
import com.ro.aminvatatro.ui.resources.labelRes
import com.ro.aminvatatro.ui.resources.shortLabelRes
import kotlinx.datetime.LocalDate
import org.jetbrains.compose.resources.getString
import org.jetbrains.compose.resources.stringResource

/**
 * Adaptive weekly meal planner screen: vertical list of days on compact widths,
 * 7-column weekly grid on medium/expanded widths.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalLayoutApi::class)
@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel,
    onOpenRecipePicker: (LocalDate, MealSlot) -> Unit,
    onOpenShoppingForWeek: (LocalDate) -> Unit,
    modifier: Modifier = Modifier,
) {
    val state by viewModel.uiState.collectAsState()
    val widthClass = LocalWindowWidthClass.current
    val snackbarHostState = remember { SnackbarHostState() }

    LaunchedEffect(viewModel) {
        viewModel.events.collect { event ->
            when (event) {
                is PlannerUiEvent.OpenRecipePicker -> onOpenRecipePicker(event.date, event.slot)
                is PlannerUiEvent.OpenShoppingForWeek -> onOpenShoppingForWeek(event.weekStart)
                PlannerUiEvent.RepeatPreviousWeekEmpty -> {
                    snackbarHostState.showSnackbar(getString(Res.string.planner_repeat_empty))
                }

                is PlannerUiEvent.RepeatPreviousWeekDone -> {
                    snackbarHostState.showSnackbar(getString(Res.string.planner_repeat_done, event.count))
                }

                PlannerUiEvent.CopyDaySameDate -> {
                    snackbarHostState.showSnackbar(getString(Res.string.planner_copy_same_day))
                }

                PlannerUiEvent.CopyDaySourceEmpty -> {
                    snackbarHostState.showSnackbar(getString(Res.string.planner_copy_empty))
                }

                is PlannerUiEvent.CopyDayDone -> {
                    snackbarHostState.showSnackbar(getString(Res.string.planner_copy_done, event.count))
                }
            }
        }
    }

    state.editingEntry?.let { editing ->
        AlertDialog(
            onDismissRequest = viewModel::dismissEditServingsDialog,
            title = { Text(stringResource(Res.string.planner_edit_servings_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(editing.recipeTitle, style = MaterialTheme.typography.titleSmall)
                    ServingsStepper(
                        servings = state.editingServings,
                        onServingsChange = viewModel::onEditingServingsChange,
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmEditServings) {
                    Text(stringResource(Res.string.action_save))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissEditServingsDialog) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    if (state.showCopyDayDialog) {
        AlertDialog(
            onDismissRequest = viewModel::dismissCopyDayDialog,
            title = { Text(stringResource(Res.string.planner_copy_day_title)) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(stringResource(Res.string.planner_copy_from), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.weekDays.forEach { day ->
                            FilterChip(
                                selected = state.copySourceDate == day,
                                onClick = { viewModel.setCopySourceDate(day) },
                                label = {
                                    Text("${stringResource(day.dayOfWeek.shortLabelRes())} ${day.formatShortDayMonth()}")
                                },
                            )
                        }
                    }
                    Text(stringResource(Res.string.planner_copy_to), style = MaterialTheme.typography.labelLarge)
                    FlowRow(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        state.weekDays.forEach { day ->
                            FilterChip(
                                selected = state.copyTargetDate == day,
                                onClick = { viewModel.setCopyTargetDate(day) },
                                label = {
                                    Text("${stringResource(day.dayOfWeek.shortLabelRes())} ${day.formatShortDayMonth()}")
                                },
                            )
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = viewModel::confirmCopyDay) {
                    Text(stringResource(Res.string.action_confirm))
                }
            },
            dismissButton = {
                TextButton(onClick = viewModel::dismissCopyDayDialog) {
                    Text(stringResource(Res.string.action_cancel))
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(title = { Text(stringResource(Res.string.planner_title)) })
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(horizontal = 12.dp),
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
                OutlinedButton(onClick = viewModel::goToCurrentWeek) {
                    Text(stringResource(Res.string.planner_today))
                }
                OutlinedButton(onClick = viewModel::openCopyDayDialog) {
                    Text(stringResource(Res.string.planner_copy_day))
                }
                OutlinedButton(onClick = viewModel::onRepeatPreviousWeek) {
                    Text(stringResource(Res.string.planner_repeat_week))
                }
                OutlinedButton(onClick = viewModel::onOpenShoppingList) {
                    Text(stringResource(Res.string.planner_generate_shopping))
                }
            }

            Spacer(Modifier.height(8.dp))

            if (state.hasError) {
                ErrorBanner(message = stringResource(Res.string.error_generic))
                Spacer(Modifier.height(8.dp))
            }

            if (state.isLoading) {
                val loadingDesc = stringResource(Res.string.loading)
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator(
                        modifier = Modifier.semantics { contentDescription = loadingDesc },
                    )
                }
            } else if (widthClass == WindowWidthClass.COMPACT) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("planner_compact_list"),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                ) {
                    items(items = state.weekDays, key = { it.toString() }) { day ->
                        DayPlanCard(
                            date = day,
                            slots = state.entriesByDateAndSlot[day].orEmpty(),
                            onAddToSlot = { slot -> viewModel.onAddEntryClicked(day, slot) },
                            onEditServings = viewModel::onEditEntryServings,
                            onRemoveEntry = viewModel::onRemoveEntry,
                            modifier = Modifier.fillMaxWidth(),
                        )
                    }
                }
            } else {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .testTag("planner_wide_grid")
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 16.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                    state.weekDays.forEach { day ->
                        DayPlanCard(
                            date = day,
                            slots = state.entriesByDateAndSlot[day].orEmpty(),
                            onAddToSlot = { slot -> viewModel.onAddEntryClicked(day, slot) },
                            onEditServings = viewModel::onEditEntryServings,
                            onRemoveEntry = viewModel::onRemoveEntry,
                            modifier = Modifier
                                .width(220.dp)
                                .fillMaxHeight()
                                .verticalScroll(rememberScrollState()),
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun DayPlanCard(
    date: LocalDate,
    slots: Map<MealSlot, List<PlannerEntryItem>>,
    onAddToSlot: (MealSlot) -> Unit,
    onEditServings: (PlannerEntryItem) -> Unit,
    onRemoveEntry: (String) -> Unit,
    modifier: Modifier = Modifier,
) {
    Card(modifier = modifier) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "${stringResource(date.dayOfWeek.fullLabelRes())} · ${date.formatShortDayMonth()}",
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
            )
            HorizontalDivider()

            MealSlot.entries.forEach { slot ->
                val slotLabel = stringResource(slot.labelRes())
                val entries = slots[slot].orEmpty()
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                    ) {
                        Text(
                            text = slotLabel,
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                        IconButton(
                            onClick = { onAddToSlot(slot) },
                            modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                        ) {
                            Icon(
                                imageVector = AppIcons.Add,
                                contentDescription = stringResource(Res.string.planner_add_to_slot, slotLabel),
                            )
                        }
                    }

                    if (entries.isEmpty()) {
                        Text(
                            text = stringResource(Res.string.planner_empty_slot),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    } else {
                        entries.forEach { item ->
                            Surface(
                                shape = MaterialTheme.shapes.small,
                                color = MaterialTheme.colorScheme.secondaryContainer,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .defaultMinSize(minHeight = 48.dp)
                                    .clickable { onEditServings(item) },
                            ) {
                                Row(
                                    modifier = Modifier.padding(start = 10.dp, top = 4.dp, bottom = 4.dp, end = 2.dp),
                                    verticalAlignment = Alignment.CenterVertically,
                                ) {
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = item.recipeTitle,
                                            style = MaterialTheme.typography.bodyMedium,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                            maxLines = 2,
                                            overflow = TextOverflow.Ellipsis,
                                        )
                                        Text(
                                            text = stringResource(Res.string.servings_count, item.entry.servings),
                                            style = MaterialTheme.typography.labelSmall,
                                            color = MaterialTheme.colorScheme.onSecondaryContainer,
                                        )
                                    }
                                    IconButton(
                                        onClick = { onRemoveEntry(item.entry.id) },
                                        modifier = Modifier.defaultMinSize(minWidth = 48.dp, minHeight = 48.dp),
                                    ) {
                                        Icon(
                                            imageVector = AppIcons.Close,
                                            contentDescription = stringResource(Res.string.action_remove),
                                            tint = MaterialTheme.colorScheme.onSecondaryContainer,
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
