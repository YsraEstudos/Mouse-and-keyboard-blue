package com.example.ui.theme

import androidx.compose.ui.graphics.Color

// Precision Obsidian Dark surfaces
val ObsidianBg = Color(0xFF0A0D14)
val ObsidianSurface = Color(0xFF121722)
val ObsidianSurfaceElevated = Color(0xFF1A2232)
val ObsidianOutline = Color(0xFF2B3548)
val ObsidianTextPrimary = Color(0xFFF2F5FA)
val ObsidianTextSecondary = Color(0xFF9AA7BD)

// Crisp Porcelain Light surfaces
val PorcelainBg = Color(0xFFF5F7FB)
val PorcelainSurface = Color(0xFFFFFFFF)
val PorcelainSurfaceElevated = Color(0xFFEAF0F8)
val PorcelainOutline = Color(0xFFCBD5E4)
val PorcelainTextPrimary = Color(0xFF0E1420)
val PorcelainTextSecondary = Color(0xFF536178)

// Tron Pitch-Black surfaces
val TronBg = Color(0xFF020408)
val TronSurface = Color(0xFF050A12)
val TronSurfaceElevated = Color(0xFF091322)
val TronTextPrimary = Color(0xFFE6FAFF)
val TronTextSecondary = Color(0xFF7BB9CC)

// 6 Curated Accent Palettes (Cor do tema)
enum class AccentPalette(
    val id: String,
    val label: String,
    val primaryDark: Color,
    val primaryLight: Color,
    val secondaryDark: Color,
    val secondaryLight: Color,
    val tronGlow: Color,
) {
    CYAN(
        id = "cyan",
        label = "Ciano",
        primaryDark = Color(0xFF22D3EE),
        primaryLight = Color(0xFF0891B2),
        secondaryDark = Color(0xFF38BDF8),
        secondaryLight = Color(0xFF0284C7),
        tronGlow = Color(0xFF00F0FF),
    ),
    ELECTRIC_BLUE(
        id = "blue",
        label = "Azul",
        primaryDark = Color(0xFF4F8CFF),
        primaryLight = Color(0xFF2563EB),
        secondaryDark = Color(0xFF7BA7FF),
        secondaryLight = Color(0xFF1D4ED8),
        tronGlow = Color(0xFF2979FF),
    ),
    EMERALD(
        id = "emerald",
        label = "Esmeralda",
        primaryDark = Color(0xFF34D399),
        primaryLight = Color(0xFF059669),
        secondaryDark = Color(0xFF6EE7B7),
        secondaryLight = Color(0xFF047857),
        tronGlow = Color(0xFF00FF9D),
    ),
    AMBER(
        id = "amber",
        label = "Âmbar",
        primaryDark = Color(0xFFFBBF24),
        primaryLight = Color(0xFFD97706),
        secondaryDark = Color(0xFFFCD34D),
        secondaryLight = Color(0xFFB45309),
        tronGlow = Color(0xFFFFB800),
    ),
    CORAL(
        id = "coral",
        label = "Coral",
        primaryDark = Color(0xFFFB7185),
        primaryLight = Color(0xFFE11D48),
        secondaryDark = Color(0xFFFDA4AF),
        secondaryLight = Color(0xFFBE123C),
        tronGlow = Color(0xFFFF2E63),
    ),
    VIOLET(
        id = "violet",
        label = "Violeta",
        primaryDark = Color(0xFFA78BFA),
        primaryLight = Color(0xFF7C3AED),
        secondaryDark = Color(0xFFC4B5FD),
        secondaryLight = Color(0xFF6D28D9),
        tronGlow = Color(0xFFBC6CFF),
    );

    companion object {
        fun fromId(id: String): AccentPalette = entries.find { it.id == id } ?: CYAN
    }
}

val StatusConnected = Color(0xFF22C55E)
val StatusWarning = Color(0xFFF59E0B)
val StatusError = Color(0xFFEF4444)
