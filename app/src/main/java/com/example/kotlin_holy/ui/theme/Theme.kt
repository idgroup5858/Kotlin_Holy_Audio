package com.example.kotlin_holy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

val LocalHolyColors = staticCompositionLocalOf { DarkHolyColors }

/** Ranglarga qisqa yo'l: HolyTheme.colors.accent */
object HolyTheme {
    val colors: HolyColors
        @Composable get() = LocalHolyColors.current
}

/** Sozlamalardagi mavzu tanlovi */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun HolyTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = if (dark) DarkHolyColors else LightHolyColors

    val scheme = if (dark) {
        darkColorScheme(
            primary = colors.accent,
            onPrimary = colors.bg,
            background = colors.bg,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.inkSoft,
            outline = colors.line,
            error = colors.danger,
        )
    } else {
        lightColorScheme(
            primary = colors.accent,
            onPrimary = colors.surface,
            background = colors.bg,
            onBackground = colors.ink,
            surface = colors.surface,
            onSurface = colors.ink,
            surfaceVariant = colors.surface2,
            onSurfaceVariant = colors.inkSoft,
            outline = colors.line,
            error = colors.danger,
        )
    }

    CompositionLocalProvider(LocalHolyColors provides colors) {
        MaterialTheme(colorScheme = scheme, typography = HolyTypography, content = content)
    }
}
