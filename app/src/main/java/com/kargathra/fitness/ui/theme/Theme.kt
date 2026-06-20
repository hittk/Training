package com.kargathra.fitness.ui.theme

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Kargathra is dark by design — navy is the identity — so we use a single
// dark scheme regardless of the system setting.
private val KargathraColors = darkColorScheme(
    primary = Gold,
    onPrimary = NavyDeep,
    primaryContainer = NavyElevated,
    onPrimaryContainer = OffWhite,
    secondary = GoldBright,
    onSecondary = NavyDeep,
    background = NavyDeep,
    onBackground = OffWhite,
    surface = NavySurface,
    onSurface = OffWhite,
    surfaceVariant = NavyElevated,
    onSurfaceVariant = MutedText,
    outline = NavyLine,
    error = DangerRed,
    onError = NavyDeep
)

@Composable
fun KargathraTheme(
    @Suppress("UNUSED_PARAMETER") darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as Activity).window
            window.statusBarColor = NavyDeep.toArgb()
            window.navigationBarColor = NavyDeep.toArgb()
            WindowCompat.getInsetsController(window, view)
                .isAppearanceLightStatusBars = false
        }
    }
    MaterialTheme(
        colorScheme = KargathraColors,
        typography = KargathraType,
        content = content
    )
}
