package com.ro.aminvatatro.ui.locale

import androidx.compose.runtime.Composable
import androidx.compose.runtime.key
import com.ro.aminvatatro.domain.model.AppLanguage

@Composable
actual fun ProvideAppLocale(
    language: AppLanguage,
    content: @Composable () -> Unit,
) {
    key(language) {
        content()
    }
}
