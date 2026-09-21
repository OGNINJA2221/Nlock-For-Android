package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import com.example.data.model.AppThemeMode

private val GlassBlueColorScheme = darkColorScheme(
    primary = GlassAccentCyan,
    onPrimary = Color.Black,
    primaryContainer = GlassPrimaryBlue,
    onPrimaryContainer = Color.White,
    secondary = Color.White,
    onSecondary = Color.Black,
    surface = GlassWhiteLow,
    onSurface = TextPrimary,
    background = GlassBackgroundDeep,
    onBackground = TextPrimary
)

private val DarkGlassColorScheme = darkColorScheme(
    primary = GlassAccentCyan,
    onPrimary = Color.Black,
    primaryContainer = Color(0xFF0F172A),
    surface = Color(0x33000000),
    onSurface = Color.White,
    background = Color(0xFF070B12),
    onBackground = Color.White
)

private val PureWhiteGlassColorScheme = lightColorScheme(
    primary = Color(0xFF0066FF),
    onPrimary = Color.White,
    surface = Color(0xAAFFFFFF),
    onSurface = Color(0xFF1E293B),
    background = Color(0xFFF0F4F8),
    onBackground = Color(0xFF0F172A)
)

@Composable
fun NLockTheme(
    themeMode: AppThemeMode = AppThemeMode.DEFAULT_GLASS_BLUE,
    content: @Composable () -> Unit
) {
    val systemInDark = isSystemInDarkTheme()
    val colorScheme = when (themeMode) {
        AppThemeMode.DEFAULT_GLASS_BLUE -> GlassBlueColorScheme
        AppThemeMode.DARK_GLASS -> DarkGlassColorScheme
        AppThemeMode.PURE_WHITE_GLASS -> PureWhiteGlassColorScheme
        AppThemeMode.AUTO -> if (systemInDark) DarkGlassColorScheme else GlassBlueColorScheme
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}

// Backward compatibility alias
@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    NLockTheme(content = content)
}
