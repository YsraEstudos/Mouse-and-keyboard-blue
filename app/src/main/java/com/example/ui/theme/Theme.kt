package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import com.example.data.ThemeMode

@Immutable
data class TronVisualStyle(
    val enabled: Boolean = false,
    val glowColor: Color = Color(0xFF00F0FF),
    val gridColor: Color = Color(0x1F00F0FF),
    val borderWidthDp: Float = 1.2f,
)

val LocalTronStyle = staticCompositionLocalOf { TronVisualStyle() }

@Composable
fun ControleBtTheme(
    themeMode: ThemeMode = ThemeMode.DARK,
    accentPalette: AccentPalette = AccentPalette.CYAN,
    tronStyleEnabled: Boolean = false,
    oledSaverEnabled: Boolean = false,
    content: @Composable () -> Unit,
) {
    val isDark = when {
        oledSaverEnabled || tronStyleEnabled -> true
        themeMode == ThemeMode.DARK -> true
        themeMode == ThemeMode.LIGHT -> false
        else -> isSystemInDarkTheme()
    }

    val colorScheme = when {
        oledSaverEnabled -> darkColorScheme(
            primary = accentPalette.primaryDark.copy(alpha = 0.82f),
            onPrimary = Color.Black,
            primaryContainer = Color(0xFF0A0D10),
            onPrimaryContainer = accentPalette.primaryDark,
            secondary = accentPalette.secondaryDark.copy(alpha = 0.7f),
            onSecondary = Color.Black,
            secondaryContainer = Color(0xFF080808),
            onSecondaryContainer = Color(0xFFBCC6D6),
            background = Color.Black,
            onBackground = Color(0xFFD5DCE6),
            surface = Color.Black,
            onSurface = Color(0xFFD5DCE6),
            surfaceVariant = Color(0xFF08090C),
            onSurfaceVariant = Color(0xFF7B879B),
            outline = Color(0xFF1F2633),
            outlineVariant = Color(0xFF151922),
            error = StatusError,
            onError = Color.White,
        )

        tronStyleEnabled -> darkColorScheme(
            primary = accentPalette.tronGlow,
            onPrimary = Color(0xFF020408),
            primaryContainer = accentPalette.tronGlow.copy(alpha = 0.16f),
            onPrimaryContainer = accentPalette.tronGlow,
            secondary = accentPalette.secondaryDark,
            onSecondary = Color(0xFF020408),
            secondaryContainer = TronSurfaceElevated,
            onSecondaryContainer = TronTextPrimary,
            background = TronBg,
            onBackground = TronTextPrimary,
            surface = TronSurface,
            onSurface = TronTextPrimary,
            surfaceVariant = TronSurfaceElevated,
            onSurfaceVariant = TronTextSecondary,
            outline = accentPalette.tronGlow.copy(alpha = 0.55f),
            outlineVariant = accentPalette.tronGlow.copy(alpha = 0.25f),
            error = StatusError,
            onError = Color.White,
        )

        isDark -> darkColorScheme(
            primary = accentPalette.primaryDark,
            onPrimary = Color(0xFF06101B),
            primaryContainer = accentPalette.primaryDark.copy(alpha = 0.18f),
            onPrimaryContainer = accentPalette.primaryDark,
            secondary = accentPalette.secondaryDark,
            onSecondary = Color(0xFF06101B),
            secondaryContainer = ObsidianSurfaceElevated,
            onSecondaryContainer = ObsidianTextPrimary,
            background = ObsidianBg,
            onBackground = ObsidianTextPrimary,
            surface = ObsidianSurface,
            onSurface = ObsidianTextPrimary,
            surfaceVariant = ObsidianSurfaceElevated,
            onSurfaceVariant = ObsidianTextSecondary,
            outline = ObsidianOutline,
            outlineVariant = ObsidianOutline.copy(alpha = 0.5f),
            error = StatusError,
            onError = Color.White,
        )

        else -> lightColorScheme(
            primary = accentPalette.primaryLight,
            onPrimary = Color.White,
            primaryContainer = accentPalette.primaryLight.copy(alpha = 0.13f),
            onPrimaryContainer = accentPalette.primaryLight,
            secondary = accentPalette.secondaryLight,
            onSecondary = Color.White,
            secondaryContainer = PorcelainSurfaceElevated,
            onSecondaryContainer = PorcelainTextPrimary,
            background = PorcelainBg,
            onBackground = PorcelainTextPrimary,
            surface = PorcelainSurface,
            onSurface = PorcelainTextPrimary,
            surfaceVariant = PorcelainSurfaceElevated,
            onSurfaceVariant = PorcelainTextSecondary,
            outline = PorcelainOutline,
            outlineVariant = PorcelainOutline.copy(alpha = 0.6f),
            error = StatusError,
            onError = Color.White,
        )
    }

    val tronVisual = TronVisualStyle(
        enabled = tronStyleEnabled && !oledSaverEnabled,
        glowColor = accentPalette.tronGlow,
        gridColor = accentPalette.tronGlow.copy(alpha = 0.12f),
        borderWidthDp = if (tronStyleEnabled) 1.3f else 1.0f,
    )

    CompositionLocalProvider(LocalTronStyle provides tronVisual) {
        MaterialTheme(
            colorScheme = colorScheme,
            typography = ControleBtTypography,
            content = content,
        )
    }
}
