package com.example.kotlin_holy.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.Dp

val LocalHolyColors = staticCompositionLocalOf { DarkHolyColors }
val LocalAppearance = staticCompositionLocalOf { Appearance() }

/** Ranglarga qisqa yo'l: HolyTheme.colors.accent, burchak: HolyTheme.shape */
object HolyTheme {
    val colors: HolyColors
        @Composable get() = LocalHolyColors.current

    val appearance: Appearance
        @Composable get() = LocalAppearance.current

    /** Veb nusxadagi yagona --radius-md: kartalar, tugmalar, tablar, inputlar */
    val radius: Dp
        @Composable get() = LocalAppearance.current.radiusDp

    val shape: Shape
        @Composable get() = RoundedCornerShape(LocalAppearance.current.radiusDp)
}

/** Yorug' / qorong'i / tizim rejimi — veb nusxadagidek standart: qorong'i */
enum class ThemeMode { SYSTEM, LIGHT, DARK }

@Composable
fun HolyTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    appearance: Appearance = Appearance(),
    content: @Composable () -> Unit,
) {
    val dark = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }
    val colors = remember(appearance, dark) { holyColors(appearance, dark) }

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

    CompositionLocalProvider(
        LocalHolyColors provides colors,
        LocalAppearance provides appearance,
    ) {
        MaterialTheme(colorScheme = scheme, typography = HolyTypography) {
            CompositionLocalProvider(
                LocalTextStyle provides HolyTypography.bodyMedium.copy(color = colors.ink),
                content = content,
            )
        }
    }
}
