package com.ro.aminvatatro.domain.repository

import com.ro.aminvatatro.domain.model.AppLanguage
import com.ro.aminvatatro.domain.model.AppTheme
import com.ro.aminvatatro.domain.model.UnitSystem
import kotlinx.coroutines.flow.StateFlow

/**
 * Snapshot of persisted user preferences.
 */
data class AppSettings(
    val theme: AppTheme = AppTheme.SYSTEM,
    val language: AppLanguage = AppLanguage.RO,
    val unitSystem: UnitSystem = UnitSystem.METRIC,
    val demoDataLoaded: Boolean = false,
)

/**
 * Persistence contract for user settings.
 */
interface SettingsRepository {
    /** Hot reactive stream reflecting the latest persisted [AppSettings]. */
    val settings: StateFlow<AppSettings>

    /** Persists the preferred [theme]. */
    suspend fun setTheme(theme: AppTheme)

    /** Persists the preferred [language]. */
    suspend fun setLanguage(language: AppLanguage)

    /** Persists the preferred display [unitSystem]. */
    suspend fun setUnitSystem(unitSystem: UnitSystem)

    /** Persists whether demo recipes are currently loaded. */
    suspend fun setDemoDataLoaded(loaded: Boolean)
}
