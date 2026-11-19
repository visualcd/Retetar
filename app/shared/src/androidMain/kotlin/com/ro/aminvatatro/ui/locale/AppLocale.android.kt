package com.ro.aminvatatro.ui.locale

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.key
import androidx.compose.ui.platform.LocalConfiguration
import com.ro.aminvatatro.domain.model.AppLanguage
import java.util.Locale

@Composable
actual fun ProvideAppLocale(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    val tag = when (language) {
        AppLanguage.RO -> "ro"
        AppLanguage.EN -> "en"
    }
    val locale = Locale.forLanguageTag(tag)
    Locale.setDefault(locale)
    val baseConfig = LocalConfiguration.current
    val updatedConfig = Configuration(baseConfig).apply {
        setLocale(locale)
    }
    CompositionLocalProvider(LocalConfiguration provides updatedConfig) {
        key(language) {
            content()
        }
    }
}
