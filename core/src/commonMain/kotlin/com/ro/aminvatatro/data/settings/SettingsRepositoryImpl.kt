package com.ro.aminvatatro.data.settings

import com.ro.aminvatatro.domain.model.AppLanguage
import com.ro.aminvatatro.domain.model.AppTheme
import com.ro.aminvatatro.domain.model.UnitSystem
import com.ro.aminvatatro.domain.repository.AppSettings
import com.ro.aminvatatro.domain.repository.SettingsRepository
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Multiplatform [SettingsRepository] backed by `multiplatform-settings` [Settings].
 *
 * Maintains a hot [StateFlow] in memory synchronized on every update so all targets
 * (including Web `StorageSettings`) observe preference changes reactively.
 */
class SettingsRepositoryImpl(
    private val backingSettings: Settings,
) : SettingsRepository {

    private val state = MutableStateFlow(readSnapshot())
    override val settings: StateFlow<AppSettings> = state.asStateFlow()

    override suspend fun setTheme(theme: AppTheme) {
        backingSettings.putString(KEY_THEME, theme.name)
        state.value = state.value.copy(theme = theme)
    }

    override suspend fun setLanguage(language: AppLanguage) {
        backingSettings.putString(KEY_LANGUAGE, language.name)
        state.value = state.value.copy(language = language)
    }

    override suspend fun setUnitSystem(unitSystem: UnitSystem) {
        backingSettings.putString(KEY_UNIT_SYSTEM, unitSystem.name)
        state.value = state.value.copy(unitSystem = unitSystem)
    }

    override suspend fun setDemoDataLoaded(loaded: Boolean) {
        backingSettings.putBoolean(KEY_DEMO_DATA_LOADED, loaded)
        state.value = state.value.copy(demoDataLoaded = loaded)
    }

    private fun readSnapshot(): AppSettings {
        val theme = backingSettings.getStringOrNull(KEY_THEME)
            ?.let { raw -> AppTheme.entries.firstOrNull { it.name == raw } }
            ?: AppTheme.SYSTEM
        val language = backingSettings.getStringOrNull(KEY_LANGUAGE)
            ?.let { raw -> AppLanguage.entries.firstOrNull { it.name == raw } }
            ?: AppLanguage.RO
        val unitSystem = backingSettings.getStringOrNull(KEY_UNIT_SYSTEM)
            ?.let { raw -> UnitSystem.entries.firstOrNull { it.name == raw } }
            ?: UnitSystem.METRIC
        val demoDataLoaded = backingSettings.getBoolean(KEY_DEMO_DATA_LOADED, false)
        return AppSettings(
            theme = theme,
            language = language,
            unitSystem = unitSystem,
            demoDataLoaded = demoDataLoaded,
        )
    }

    companion object {
        private const val KEY_THEME = "retetar.settings.theme"
        private const val KEY_LANGUAGE = "retetar.settings.language"
        private const val KEY_UNIT_SYSTEM = "retetar.settings.unit_system"
        private const val KEY_DEMO_DATA_LOADED = "retetar.settings.demo_loaded"
    }
}
