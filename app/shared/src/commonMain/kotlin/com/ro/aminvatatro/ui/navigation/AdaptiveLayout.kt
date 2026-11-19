package com.ro.aminvatatro.ui.navigation

import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Material 3 window width size classes used to switch between compact (bottom bar / vertical list)
 * and wider (navigation rail / 7-column planner grid) layouts.
 */
enum class WindowWidthClass {
    /** Phone portrait (`< 600.dp`). */
    COMPACT,

    /** Large phone landscape or small tablet (`600.dp ..< 840.dp`). */
    MEDIUM,

    /** Tablet landscape, desktop, or wide browser (`>= 840.dp`). */
    EXPANDED,
}

/** Computes the Material 3 [WindowWidthClass] for the available [width]. */
fun computeWindowWidthClass(width: Dp): WindowWidthClass = when {
    width < 600.dp -> WindowWidthClass.COMPACT
    width < 840.dp -> WindowWidthClass.MEDIUM
    else -> WindowWidthClass.EXPANDED
}

/** Provides the active [WindowWidthClass] down the Compose tree. */
val LocalWindowWidthClass = staticCompositionLocalOf { WindowWidthClass.COMPACT }
