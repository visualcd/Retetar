package com.ro.aminvatatro.presentation.shopping

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.core.time.IsoWeekInfo
import com.ro.aminvatatro.core.time.WeekUtils
import com.ro.aminvatatro.core.units.MeasurementUnit
import com.ro.aminvatatro.core.units.QuantityFormatter
import com.ro.aminvatatro.core.units.UnitConverter
import com.ro.aminvatatro.domain.model.Aisle
import com.ro.aminvatatro.domain.model.AppLanguage
import com.ro.aminvatatro.domain.model.ShoppingItem
import com.ro.aminvatatro.domain.model.UnitSystem
import com.ro.aminvatatro.domain.repository.SettingsRepository
import com.ro.aminvatatro.domain.repository.ShoppingRepository
import com.ro.aminvatatro.domain.usecase.shopping.AddManualItem
import com.ro.aminvatatro.domain.usecase.shopping.AddManualItemResult
import com.ro.aminvatatro.domain.usecase.shopping.ClearChecked
import com.ro.aminvatatro.domain.usecase.shopping.GenerateShoppingList
import com.ro.aminvatatro.domain.usecase.shopping.RemoveItem
import com.ro.aminvatatro.domain.usecase.shopping.ToggleItem
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
 * Typed validation errors for the Add Manual Item dialog.
 */
sealed interface ManualItemError {
    data object NameBlank : ManualItemError
    data class NameTooLong(val maxLength: Int) : ManualItemError
    data object InvalidQuantity : ManualItemError
}

/**
 * UI state for the weekly Shopping List screen.
 */
data class ShoppingListUiState(
    val isLoading: Boolean = true,
    val isBusy: Boolean = false,
    val weekStart: LocalDate,
    val weekEnd: LocalDate,
    val isoWeekInfo: IsoWeekInfo,
    val items: List<ShoppingItem> = emptyList(),
    val itemsByAisle: Map<Aisle, List<ShoppingItem>> = emptyMap(),
    val checkedCount: Int = 0,
    val language: AppLanguage = AppLanguage.RO,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val showAddManualDialog: Boolean = false,
    val manualName: String = "",
    val manualQuantityText: String = "",
    val manualUnit: MeasurementUnit = MeasurementUnit.PIECE,
    val manualAisle: Aisle = Aisle.OTHER,
    val manualError: ManualItemError? = null,
    val showClearCheckedDialog: Boolean = false,
    val hasError: Boolean = false,
)

/**
 * One-shot feedback events emitted by [ShoppingListViewModel].
 */
sealed interface ShoppingListUiEvent {
    data object Regenerated : ShoppingListUiEvent
}

/**
 * ViewModel managing weekly shopping list generation, aisle grouping, checking items,
 * manual item creation, clearing checked items, and week navigation.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class ShoppingListViewModel(
    private val shoppingRepository: ShoppingRepository,
    private val settingsRepository: SettingsRepository,
    private val generateShoppingList: GenerateShoppingList,
    private val toggleItem: ToggleItem,
    private val addManualItem: AddManualItem,
    private val removeItem: RemoveItem,
    private val clearChecked: ClearChecked,
    private val clock: Clock,
) : ViewModel() {

    private val selectedWeekStart = MutableStateFlow(WeekUtils.weekStartOf(clock.today()))

    private val _uiState = MutableStateFlow(
        ShoppingListUiState(
            weekStart = selectedWeekStart.value,
            weekEnd = WeekUtils.weekEndOf(selectedWeekStart.value),
            isoWeekInfo = WeekUtils.isoWeekInfo(selectedWeekStart.value),
            language = settingsRepository.settings.value.language,
            unitSystem = settingsRepository.settings.value.unitSystem,
        ),
    )
    val uiState: StateFlow<ShoppingListUiState> = _uiState.asStateFlow()

    private val _events = Channel<ShoppingListUiEvent>(Channel.BUFFERED)
    val events: Flow<ShoppingListUiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            selectedWeekStart
                .flatMapLatest { weekStart ->
                    combine(
                        shoppingRepository.observeItemsForWeek(weekStart),
                        settingsRepository.settings,
                    ) { items, settings ->
                        Triple(weekStart, items, settings)
                    }
                }
                .catch {
                    _uiState.update { it.copy(isLoading = false, hasError = true) }
                }
                .collect { (weekStart, items, settings) ->
                    val grouped = items
                        .groupBy { it.aisle }
                        .entries
                        .sortedBy { it.key.ordinal }
                        .associate { it.key to it.value }

                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            weekStart = weekStart,
                            weekEnd = WeekUtils.weekEndOf(weekStart),
                            isoWeekInfo = WeekUtils.isoWeekInfo(weekStart),
                            items = items,
                            itemsByAisle = grouped,
                            checkedCount = items.count { it.checked },
                            language = settings.language,
                            unitSystem = settings.unitSystem,
                            hasError = false,
                        )
                    }
                }
        }
    }

    fun selectWeek(date: LocalDate, autoRegenerate: Boolean = false) {
        val monday = WeekUtils.weekStartOf(date)
        selectedWeekStart.value = monday
        if (autoRegenerate) {
            regenerate()
        }
    }

    fun previousWeek() {
        selectedWeekStart.value = WeekUtils.previousWeek(selectedWeekStart.value)
    }

    fun nextWeek() {
        selectedWeekStart.value = WeekUtils.nextWeek(selectedWeekStart.value)
    }

    fun goToCurrentWeek() {
        selectedWeekStart.value = WeekUtils.weekStartOf(clock.today())
    }

    fun regenerate() {
        val weekStart = selectedWeekStart.value
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, hasError = false) }
            try {
                generateShoppingList(weekStart)
                _uiState.update { it.copy(isBusy = false) }
                _events.send(ShoppingListUiEvent.Regenerated)
            } catch (_: Throwable) {
                _uiState.update { it.copy(isBusy = false, hasError = true) }
            }
        }
    }

    fun onToggleItem(itemId: String, checked: Boolean) {
        viewModelScope.launch {
            toggleItem(itemId, checked)
        }
    }

    fun onRemoveItem(itemId: String) {
        viewModelScope.launch {
            removeItem(itemId)
        }
    }

    fun openAddManualDialog() {
        _uiState.update {
            it.copy(
                showAddManualDialog = true,
                manualName = "",
                manualQuantityText = "",
                manualUnit = MeasurementUnit.PIECE,
                manualAisle = Aisle.OTHER,
                manualError = null,
            )
        }
    }

    fun dismissAddManualDialog() {
        _uiState.update { it.copy(showAddManualDialog = false, manualError = null) }
    }

    fun onManualNameChange(value: String) {
        _uiState.update { it.copy(manualName = value, manualError = null) }
    }

    fun onManualQuantityChange(value: String) {
        _uiState.update { it.copy(manualQuantityText = value, manualError = null) }
    }

    fun onManualUnitChange(unit: MeasurementUnit) {
        _uiState.update {
            it.copy(
                manualUnit = unit,
                manualQuantityText = if (unit == MeasurementUnit.TO_TASTE) "" else it.manualQuantityText,
                manualError = null,
            )
        }
    }

    fun onManualAisleChange(aisle: Aisle) {
        _uiState.update { it.copy(manualAisle = aisle) }
    }

    fun submitManualItem() {
        val state = _uiState.value
        val cleanedQty = state.manualQuantityText.trim().replace(',', '.')
        val parsedQty: Double? = when {
            cleanedQty.isEmpty() -> null
            else -> cleanedQty.toDoubleOrNull() ?: Double.NaN
        }

        viewModelScope.launch {
            when (
                val result = addManualItem(
                    weekDate = state.weekStart,
                    name = state.manualName,
                    quantity = parsedQty,
                    unit = state.manualUnit,
                    aisle = state.manualAisle,
                )
            ) {
                is AddManualItemResult.Success -> {
                    _uiState.update { it.copy(showAddManualDialog = false, manualError = null) }
                }

                AddManualItemResult.NameBlank -> {
                    _uiState.update { it.copy(manualError = ManualItemError.NameBlank) }
                }

                is AddManualItemResult.NameTooLong -> {
                    _uiState.update { it.copy(manualError = ManualItemError.NameTooLong(result.maxLength)) }
                }

                AddManualItemResult.InvalidQuantity -> {
                    _uiState.update { it.copy(manualError = ManualItemError.InvalidQuantity) }
                }
            }
        }
    }

    fun requestClearChecked() {
        if (_uiState.value.checkedCount > 0) {
            _uiState.update { it.copy(showClearCheckedDialog = true) }
        }
    }

    fun dismissClearCheckedDialog() {
        _uiState.update { it.copy(showClearCheckedDialog = false) }
    }

    fun confirmClearChecked() {
        val weekStart = selectedWeekStart.value
        _uiState.update { it.copy(showClearCheckedDialog = false) }
        viewModelScope.launch {
            clearChecked(weekStart)
        }
    }
}

/**
 * Formats a grouped shopping list as plain text suitable for copying to the clipboard.
 */
fun formatShoppingListAsText(
    itemsByAisle: Map<Aisle, List<ShoppingItem>>,
    aisleLabel: (Aisle) -> String,
    unitLabel: (MeasurementUnit) -> String,
    language: AppLanguage,
    unitSystem: UnitSystem,
): String = buildString {
    itemsByAisle.entries.forEachIndexed { aisleIdx, (aisle, items) ->
        if (aisleIdx > 0) appendLine()
        appendLine(aisleLabel(aisle))
        items.forEach { item ->
            val mark = if (item.checked) "[x]" else "[ ]"
            val qtyAndUnit = formatShoppingQuantity(item, unitLabel, language, unitSystem)
            if (qtyAndUnit.isNotEmpty()) {
                appendLine("$mark ${item.name} - $qtyAndUnit")
            } else {
                appendLine("$mark ${item.name}")
            }
        }
    }
}.trimEnd()

/**
 * Formats a single [ShoppingItem]'s quantity and unit for display respecting [unitSystem] and [language].
 */
fun formatShoppingQuantity(
    item: ShoppingItem,
    unitLabel: (MeasurementUnit) -> String,
    language: AppLanguage,
    unitSystem: UnitSystem,
): String {
    if (item.unit == MeasurementUnit.TO_TASTE) return unitLabel(MeasurementUnit.TO_TASTE)
    val rawQty = item.quantity ?: return ""
    val (displayQty, displayUnit) = UnitConverter.convertForDisplaySystem(rawQty, item.unit, unitSystem)
    val formatted = QuantityFormatter.format(displayQty, language)
    return if (formatted.isEmpty()) "" else "$formatted ${unitLabel(displayUnit)}"
}
