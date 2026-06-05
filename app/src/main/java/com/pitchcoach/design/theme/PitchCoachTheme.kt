package com.pitchcoach.design.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

private val PitchLightColors = lightColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFD8EAFE),
    onPrimaryContainer = Color(0xFF00325F),
    secondary = Color(0xFF1F8A4C),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFDDF6E7),
    onSecondaryContainer = Color(0xFF093C20),
    tertiary = Color(0xFFC4365A),
    onTertiary = Color.White,
    tertiaryContainer = Color(0xFFFFE0E8),
    onTertiaryContainer = Color(0xFF5D1029),
    background = Color(0xFFFAF9F5),
    onBackground = Color(0xFF10110F),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF10110F),
    surfaceVariant = Color(0xFFECEAE3),
    onSurfaceVariant = Color(0xFF5E625C),
    outline = Color(0xFF7B8078),
    error = Color(0xFFFF3B30),
)

private val PitchDarkColors = darkColorScheme(
    primary = Color(0xFF0A84FF),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFF093963),
    onPrimaryContainer = Color(0xFFD9EAFF),
    secondary = Color(0xFF30D158),
    onSecondary = Color(0xFF05250F),
    secondaryContainer = Color(0xFF153D20),
    onSecondaryContainer = Color(0xFFDDF6E7),
    tertiary = Color(0xFFFF375F),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFF5B1428),
    onTertiaryContainer = Color(0xFFFFE0E8),
    background = Color(0xFF0D0E0D),
    onBackground = Color(0xFFF4F1EA),
    surface = Color(0xFF171816),
    onSurface = Color(0xFFF4F1EA),
    surfaceVariant = Color(0xFF2A2B28),
    onSurfaceVariant = Color(0xFFB8B8B0),
    outline = Color(0xFF777B73),
    error = Color(0xFFFF453A),
)

@Composable
fun PitchCoachTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit,
) {
    val colors = if (darkTheme) PitchDarkColors else PitchLightColors
    ConfigureSystemBars(colors)

    MaterialTheme(
        colorScheme = colors,
        typography = PitchCoachTypography,
        shapes = PitchCoachShapes,
        content = content,
    )
}

@Composable
private fun ConfigureSystemBars(colors: ColorScheme) {
    val view = LocalView.current
    if (view.isInEditMode) return

    SideEffect {
        val window = (view.context as? Activity)?.window ?: return@SideEffect
        window.statusBarColor = colors.background.toArgb()
        window.navigationBarColor = colors.background.toArgb()
        val useDarkIcons = colors.background.luminance() > 0.5f
        WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = useDarkIcons
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowCompat.getInsetsController(window, view).isAppearanceLightNavigationBars = useDarkIcons
        }
    }
}
