package com.ro.aminvatatro.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.ro.aminvatatro.domain.model.AppTheme

private val LightColorScheme = lightColorScheme(
    primary = Color(0xFF8C4A27),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDBCA),
    onPrimaryContainer = Color(0xFF331200),
    secondary = Color(0xFF4F6544),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD1EBC1),
    onSecondaryContainer = Color(0xFF0E2007),
    tertiary = Color(0xFF6B5D2F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFF5E1A7),
    onTertiaryContainer = Color(0xFF231B00),
    background = Color(0xFFFFF8F5),
    onBackground = Color(0xFF221A16),
    surface = Color(0xFFFFF8F5),
    onSurface = Color(0xFF221A16),
    surfaceVariant = Color(0xFFF4DED4),
    onSurfaceVariant = Color(0xFF52443D),
    outline = Color(0xFF85736B),
    error = Color(0xFFBA1A1A),
    onError = Color(0xFFFFFFFF),
    errorContainer = Color(0xFFFFDAD6),
    onErrorContainer = Color(0xFF410002),
)

private val DarkColorScheme = darkColorScheme(
    primary = Color(0xFFFFB690),
    onPrimary = Color(0xFF532200),
    primaryContainer = Color(0xFF703712),
    onPrimaryContainer = Color(0xFFFFDBCA),
    secondary = Color(0xFFB5CFA6),
    onSecondary = Color(0xFF223619),
    secondaryContainer = Color(0xFF384D2E),
    onSecondaryContainer = Color(0xFFD1EBC1),
    tertiary = Color(0xFFD8C58D),
    onTertiary = Color(0xFF3A2F05),
    tertiaryContainer = Color(0xFF52461A),
    onTertiaryContainer = Color(0xFFF5E1A7),
    background = Color(0xFF1A120E),
    onBackground = Color(0xFFF0DFD8),
    surface = Color(0xFF1A120E),
    onSurface = Color(0xFFF0DFD8),
    surfaceVariant = Color(0xFF52443D),
    onSurfaceVariant = Color(0xFFD7C2B8),
    outline = Color(0xFFA08D84),
    error = Color(0xFFFFB4AB),
    onError = Color(0xFF690005),
    errorContainer = Color(0xFF93000A),
    onErrorContainer = Color(0xFFFFDAD6),
)

/**
 * Material 3 theme for Rețetar supporting system, light, and dark modes.
 */
@Composable
fun RetetarTheme(
    appTheme: AppTheme = AppTheme.SYSTEM,
    content: @Composable () -> Unit,
) {
    val darkTheme = when (appTheme) {
        AppTheme.SYSTEM -> isSystemInDarkTheme()
        AppTheme.LIGHT -> false
        AppTheme.DARK -> true
    }

    MaterialTheme(
        colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme,
        typography = Typography(),
        content = content,
    )
}
