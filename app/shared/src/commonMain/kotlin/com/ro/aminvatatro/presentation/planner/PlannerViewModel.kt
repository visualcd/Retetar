package com.ro.aminvatatro.presentation.planner

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.core.time.IsoWeekInfo
import com.ro.aminvatatro.core.time.WeekUtils
import com.ro.aminvatatro.domain.model.MealPlanEntry
import com.ro.aminvatatro.domain.model.MealSlot
import com.ro.aminvatatro.domain.repository.MealPlanRepository
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.usecase.plan.ChangeServings
import com.ro.aminvatatro.domain.usecase.plan.CopyDay
import com.ro.aminvatatro.domain.usecase.plan.CopyDayResult
import com.ro.aminvatatro.domain.usecase.plan.RemovePlanEntry
import com.ro.aminvatatro.domain.usecase.plan.RepeatPreviousWeek
import com.ro.aminvatatro.domain.usecase.plan.RepeatPreviousWeekResult
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.datetime.LocalDate

/**
 * Display model combining a scheduled [MealPlanEntry] with its resolved recipe title.
 */
data class PlannerEntryItem(
    val entry: MealPlanEntry,
    val recipeTitle: String,
)

/**
 * UI state for the Weekly Meal Planner screen.
 */
data class PlannerUiState(
    val isLoading: Boolean = true,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val weekDays: List<LocalDate>,
    val isoWeekInfo: IsoWeekInfo,
    val entriesByDateAndSlot: Map<LocalDate, Map<MealSlot, List<PlannerEntryItem>>> = emptyMap(),
    val editingEntry: PlannerEntryItem? = null,
    val editingServings: Int = 1,
    val showCopyDayDialog: Boolean = false,
    val copySourceDate: LocalDate = weekStart,
    val copyTargetDate: LocalDate = weekDays.getOrElse(1) { weekStart },
    val hasError: Boolean = false,
)

/**
 * One-shot feedback and navigation events emitted by [PlannerViewModel].
 */
sealed interface PlannerUiEvent {
    data object RepeatPreviousWeekEmpty : PlannerUiEvent
    data class RepeatPreviousWeekDone(val count: Int) : PlannerUiEvent
    data object CopyDaySameDate : PlannerUiEvent
    data object CopyDaySourceEmpty : PlannerUiEvent
    data class CopyDayDone(val count: Int) : PlannerUiEvent
    data class OpenRecipePicker(val date: LocalDate, val slot: MealSlot) : PlannerUiEvent
    data class OpenShoppingForWeek(val weekStart: LocalDate) : PlannerUiEvent
}

/**
 * ViewModel managing week navigation, slot inspection, serving edits, day copy,
 * and repeating the previous week.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class PlannerViewModel(
    private val mealPlanRepository: MealPlanRepository,
    private val recipeRepository: RecipeRepository,
    private val changeServings: ChangeServings,
    private val removePlanEntry: RemovePlanEntry,
    private val copyDay: CopyDay,
    private val repeatPreviousWeek: RepeatPreviousWeek,
    private val clock: Clock,
) : ViewModel() {

    private val selectedWeekStart = MutableStateFlow(WeekUtils.weekStartOf(clock.today()))

    private val _uiState = MutableStateFlow(buildInitialState(selectedWeekStart.value))
    val uiState: StateFlow<PlannerUiState> = _uiState.asStateFlow()

    private val _events = Channel<PlannerUiEvent>(Channel.BUFFERED)
    val events: Flow<PlannerUiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            selectedWeekStart
                .flatMapLatest { weekStart ->
                    val weekEnd = WeekUtils.weekEndOf(weekStart)
                    combine(
                        mealPlanRepository.observeEntriesForRange(weekStart, weekEnd),
                        recipeRepository.observeRecipes(),
                    ) { entries, recipes ->
                        Triple(weekStart, entries, recipes)
                    }
                }
                .catch {
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
                }
                .collect { (weekStart, entries, recipes) ->
                    val titlesById = recipes.associate { it.id to it.title }
                    val weekDays = WeekUtils.weekDaysOf(weekStart)
                    val grouped = entries
                        .groupBy { it.date }
                        .mapValues { (_, dayEntries) ->
                            dayEntries
                                .groupBy { it.slot }
                                .mapValues { (_, slotEntries) ->
                                    slotEntries.map { entry ->
                                        PlannerEntryItem(
                                            entry = entry,
                                            recipeTitle = titlesById[entry.recipeId] ?: entry.recipeId,
                                        )
                                    }
                                }
                        }

                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            weekStart = weekStart,
                            weekEnd = WeekUtils.weekEndOf(weekStart),
                            weekDays = weekDays,
                            isoWeekInfo = WeekUtils.isoWeekInfo(weekStart),
                            entriesByDateAndSlot = grouped,
                            hasError = false,
                        )
                    }
                }
        }
    }

    fun previousWeek() {
        val prev = WeekUtils.previousWeek(selectedWeekStart.value)
        selectedWeekStart.value = prev
    }

    fun nextWeek() {
        val next = WeekUtils.nextWeek(selectedWeekStart.value)
        selectedWeekStart.value = next
    }

    fun goToCurrentWeek() {
        selectedWeekStart.value = WeekUtils.weekStartOf(clock.today())
    }

    fun onAddEntryClicked(date: LocalDate, slot: MealSlot) {
        viewModelScope.launch {
            _events.send(PlannerUiEvent.OpenRecipePicker(date, slot))
        }
    }

    fun onEditEntryServings(item: PlannerEntryItem) {
        _uiState.update {
            it.copy(editingEntry = item, editingServings = item.entry.servings)
        }
    }

    fun onEditingServingsChange(servings: Int) {
        _uiState.update { it.copy(editingServings = servings) }
    }

    fun dismissEditServingsDialog() {
        _uiState.update { it.copy(editingEntry = null) }
    }

    fun confirmEditServings() {
        val target = _uiState.value.editingEntry ?: return
        val newServings = _uiState.value.editingServings
        _uiState.update { it.copy(editingEntry = null) }
        viewModelScope.launch {
            changeServings(target.entry.id, newServings)
        }
    }

    fun onRemoveEntry(entryId: String) {
        viewModelScope.launch {
            removePlanEntry(entryId)
        }
    }

    fun openCopyDayDialog() {
        val days = _uiState.value.weekDays
        _uiState.update {
            it.copy(
                showCopyDayDialog = true,
                copySourceDate = days.first(),
                copyTargetDate = days.getOrElse(1) { days.first() },
            )
        }
    }

    fun dismissCopyDayDialog() {
        _uiState.update { it.copy(showCopyDayDialog = false) }
    }

    fun setCopySourceDate(date: LocalDate) {
        _uiState.update { it.copy(copySourceDate = date) }
    }

    fun setCopyTargetDate(date: LocalDate) {
        _uiState.update { it.copy(copyTargetDate = date) }
    }

    fun confirmCopyDay() {
        val state = _uiState.value
        _uiState.update { it.copy(showCopyDayDialog = false) }
        viewModelScope.launch {
            when (val result = copyDay(state.copySourceDate, state.copyTargetDate)) {
                CopyDayResult.SameDate -> _events.send(PlannerUiEvent.CopyDaySameDate)
                CopyDayResult.SourceEmpty -> _events.send(PlannerUiEvent.CopyDaySourceEmpty)
                is CopyDayResult.Copied -> _events.send(PlannerUiEvent.CopyDayDone(result.copiedEntries.size))
            }
        }
    }

    fun onRepeatPreviousWeek() {
        val weekStart = selectedWeekStart.value
        viewModelScope.launch {
            when (val result = repeatPreviousWeek(weekStart)) {
                RepeatPreviousWeekResult.PreviousWeekEmpty -> {
                    _events.send(PlannerUiEvent.RepeatPreviousWeekEmpty)
                }

                is RepeatPreviousWeekResult.Repeated -> {
                    _events.send(PlannerUiEvent.RepeatPreviousWeekDone(result.copiedEntries.size))
                }
            }
        }
    }

    fun onOpenShoppingList() {
        viewModelScope.launch {
            _events.send(PlannerUiEvent.OpenShoppingForWeek(selectedWeekStart.value))
        }
    }

    private fun buildInitialState(weekStart: LocalDate): PlannerUiState {
        val days = WeekUtils.weekDaysOf(weekStart)
        return PlannerUiState(
            isLoading = true,
            weekStart = weekStart,
            weekEnd = WeekUtils.weekEndOf(weekStart),
            weekDays = days,
            isoWeekInfo = WeekUtils.isoWeekInfo(weekStart),
            copySourceDate = days.first(),
            copyTargetDate = days.getOrElse(1) { days.first() },
        )
    }
}
