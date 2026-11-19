package com.ro.aminvatatro.presentation.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.ro.aminvatatro.core.time.Clock
import com.ro.aminvatatro.data.demo.DemoRecipes
import com.ro.aminvatatro.domain.model.AppLanguage
import com.ro.aminvatatro.domain.model.AppTheme
import com.ro.aminvatatro.domain.model.Recipe
import com.ro.aminvatatro.domain.model.UnitSystem
import com.ro.aminvatatro.domain.repository.AppSettings
import com.ro.aminvatatro.domain.repository.RecipeRepository
import com.ro.aminvatatro.domain.repository.SettingsRepository
import com.ro.aminvatatro.domain.usecase.importexport.DuplicatePolicy
import com.ro.aminvatatro.domain.usecase.importexport.ExportRecipes
import com.ro.aminvatatro.domain.usecase.importexport.ImportRecipes
import com.ro.aminvatatro.domain.usecase.importexport.ImportRecipesResult
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * UI state for the Settings screen covering theme, language, unit system, demo data,
 * and JSON recipe import/export.
 */
data class SettingsUiState(
    val settings: AppSettings = AppSettings(),
    val recipes: List<Recipe> = emptyList(),
    val selectedRecipeIdsForExport: Set<String> = emptySet(),
    val showRecipeSelection: Boolean = false,
    val exportedJson: String = "",
    val exportedCount: Int? = null,
    val exportedSchemaVersion: Int = 1,
    val importJsonInput: String = "",
    val duplicatePolicy: DuplicatePolicy = DuplicatePolicy.SKIP,
    val isBusy: Boolean = false,
    val error: SettingsError? = null,
)

/**
 * Typed error states displayed on the Settings screen.
 */
sealed interface SettingsError {
    data object Generic : SettingsError
    data class InvalidJson(val reason: String) : SettingsError
    data class UnsupportedSchemaVersion(val found: Int, val supported: Int) : SettingsError
    data class ImportValidationFailed(val errorCount: Int) : SettingsError
}

/**
 * One-shot feedback events emitted by [SettingsViewModel].
 */
sealed interface SettingsUiEvent {
    data class DemoLoaded(val count: Int) : SettingsUiEvent
    data object DemoCleared : SettingsUiEvent
    data class ExportReady(val count: Int, val schemaVersion: Int) : SettingsUiEvent
    data class ImportSucceeded(val imported: Int, val skipped: Int, val duplicated: Int) : SettingsUiEvent
}

/**
 * ViewModel managing user preferences, demo recipes lifecycle, and JSON import/export.
 */
class SettingsViewModel(
    private val settingsRepository: SettingsRepository,
    private val recipeRepository: RecipeRepository,
    private val exportRecipes: ExportRecipes,
    private val importRecipes: ImportRecipes,
    private val clock: Clock,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SettingsUiState(settings = settingsRepository.settings.value))
    val uiState: StateFlow<SettingsUiState> = _uiState.asStateFlow()

    private val _events = Channel<SettingsUiEvent>(Channel.BUFFERED)
    val events: Flow<SettingsUiEvent> = _events.receiveAsFlow()

    init {
        viewModelScope.launch {
            settingsRepository.settings.collect { settings ->
                _uiState.update { it.copy(settings = settings) }
            }
        }
        viewModelScope.launch {
            recipeRepository.observeRecipes()
                .catch { _uiState.update { it.copy(error = SettingsError.Generic) } }
                .collect { recipes ->
                    _uiState.update { state ->
                        val validIds = recipes.map { it.id }.toSet()
                        state.copy(
                            recipes = recipes,
                            selectedRecipeIdsForExport = state.selectedRecipeIdsForExport.intersect(validIds),
                        )
                    }
                }
        }
    }

    fun setTheme(theme: AppTheme) {
        viewModelScope.launch { settingsRepository.setTheme(theme) }
    }

    fun setLanguage(language: AppLanguage) {
        viewModelScope.launch { settingsRepository.setLanguage(language) }
    }

    fun setUnitSystem(unitSystem: UnitSystem) {
        viewModelScope.launch { settingsRepository.setUnitSystem(unitSystem) }
    }

    fun loadDemoData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null) }
            try {
                val demoRecipes = DemoRecipes.create(clock.nowEpochMillis())
                recipeRepository.saveRecipes(demoRecipes)
                settingsRepository.setDemoDataLoaded(true)
                _uiState.update { it.copy(isBusy = false) }
                _events.send(SettingsUiEvent.DemoLoaded(demoRecipes.size))
            } catch (_: Throwable) {
                _uiState.update { it.copy(isBusy = false, error = SettingsError.Generic) }
            }
        }
    }

    fun clearDemoData() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null) }
            try {
                val existing = recipeRepository.getAllRecipes()
                existing
                    .filter { it.id.startsWith(DemoRecipes.DEMO_ID_PREFIX) }
                    .forEach { recipeRepository.deleteRecipe(it.id) }
                settingsRepository.setDemoDataLoaded(false)
                _uiState.update { it.copy(isBusy = false) }
                _events.send(SettingsUiEvent.DemoCleared)
            } catch (_: Throwable) {
                _uiState.update { it.copy(isBusy = false, error = SettingsError.Generic) }
            }
        }
    }

    fun toggleRecipeSelectionPanel() {
        _uiState.update { it.copy(showRecipeSelection = !it.showRecipeSelection) }
    }

    fun toggleRecipeForExport(recipeId: String) {
        _uiState.update { state ->
            val updated = if (recipeId in state.selectedRecipeIdsForExport) {
                state.selectedRecipeIdsForExport - recipeId
            } else {
                state.selectedRecipeIdsForExport + recipeId
            }
            state.copy(selectedRecipeIdsForExport = updated)
        }
    }

    fun exportAll() = runExport(recipeIds = null)

    fun exportSelected() {
        val selected = _uiState.value.selectedRecipeIdsForExport
        if (selected.isNotEmpty()) {
            runExport(recipeIds = selected)
        }
    }

    private fun runExport(recipeIds: Set<String>?) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null) }
            try {
                val result = exportRecipes(recipeIds)
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        exportedJson = result.json,
                        exportedCount = result.recipeCount,
                        exportedSchemaVersion = result.schemaVersion,
                    )
                }
                _events.send(SettingsUiEvent.ExportReady(result.recipeCount, result.schemaVersion))
            } catch (_: Throwable) {
                _uiState.update { it.copy(isBusy = false, error = SettingsError.Generic) }
            }
        }
    }

    fun onImportJsonChanged(value: String) {
        _uiState.update { it.copy(importJsonInput = value, error = null) }
    }

    fun setDuplicatePolicy(policy: DuplicatePolicy) {
        _uiState.update { it.copy(duplicatePolicy = policy) }
    }

    fun importFromJson() {
        viewModelScope.launch {
            val raw = _uiState.value.importJsonInput
            val policy = _uiState.value.duplicatePolicy
            _uiState.update { it.copy(isBusy = true, error = null) }
            when (val result = importRecipes(raw, policy)) {
                is ImportRecipesResult.Success -> {
                    _uiState.update { it.copy(isBusy = false, importJsonInput = "") }
                    _events.send(
                        SettingsUiEvent.ImportSucceeded(
                            imported = result.importedCount,
                            skipped = result.skippedCount,
                            duplicated = result.duplicatedCount,
                        ),
                    )
                }

                is ImportRecipesResult.InvalidJson -> {
                    _uiState.update {
                        it.copy(isBusy = false, error = SettingsError.InvalidJson(result.reason))
                    }
                }

                is ImportRecipesResult.UnsupportedSchemaVersion -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            error = SettingsError.UnsupportedSchemaVersion(
                                found = result.found,
                                supported = result.supported,
                            ),
                        )
                    }
                }

                is ImportRecipesResult.ValidationFailed -> {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            error = SettingsError.ImportValidationFailed(result.errors.size),
                        )
                    }
                }
            }
        }
    }

    fun dismissError() {
        _uiState.update { it.copy(error = null) }
    }
}
