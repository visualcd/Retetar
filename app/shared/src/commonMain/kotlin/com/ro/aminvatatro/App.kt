package com.ro.aminvatatro

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.rememberNavController
import com.ro.aminvatatro.di.initKoin
import com.ro.aminvatatro.domain.repository.SettingsRepository
import com.ro.aminvatatro.ui.locale.ProvideAppLocale
import com.ro.aminvatatro.ui.navigation.LocalWindowWidthClass
import com.ro.aminvatatro.ui.navigation.RetetarNavHost
import com.ro.aminvatatro.ui.navigation.WindowWidthClass
import com.ro.aminvatatro.ui.navigation.computeWindowWidthClass
import com.ro.aminvatatro.ui.theme.RetetarTheme
import org.koin.compose.koinInject

/**
 * Shared Compose Multiplatform entry point for Rețetar across Android, iOS, Desktop, and Web.
 */
@Composable
fun App() {
    RetetarApp()
}

/**
 * Root composable for Rețetar supporting an optional [forcedWidthClass] and [navController]
 * so adaptive layouts and flows can be verified deterministically in tests.
 */
@Composable
fun RetetarApp(
    forcedWidthClass: WindowWidthClass? = null,
    navController: NavHostController = rememberNavController(),
) {
    initKoin()
    val settingsRepository: SettingsRepository = koinInject()
    val settings by settingsRepository.settings.collectAsState()

    ProvideAppLocale(language = settings.language) {
        RetetarTheme(appTheme = settings.theme) {
            BoxWithConstraints(modifier = Modifier.fillMaxSize()) {
                val widthClass = forcedWidthClass ?: computeWindowWidthClass(maxWidth)
                CompositionLocalProvider(LocalWindowWidthClass provides widthClass) {
                    Surface(modifier = Modifier.fillMaxSize()) {
                        RetetarNavHost(navController = navController)
                    }
                }
            }
        }
    }
}
