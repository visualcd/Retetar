package com.ro.aminvatatro.ui.locale

import androidx.compose.runtime.Composable
import com.ro.aminvatatro.domain.model.AppLanguage

/**
 * Applies the selected [AppLanguage] to the Compose resource environment where supported
 * (Android and Desktop JVM at runtime; iOS and Web follow the platform/browser locale).
 */
@Composable
expect fun ProvideAppLocale(
    language: AppLanguage,
    content: @Composable () -> Unit,
)
