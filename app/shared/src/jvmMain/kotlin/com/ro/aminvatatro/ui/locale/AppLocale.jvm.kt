package com.ro.aminvatatro.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
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
    Locale.setDefault(Locale.forLanguageTag(tag))
    key(language) {
        content()
    }
}
