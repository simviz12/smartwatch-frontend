package com.pulso.mobile.presentation.theme

import androidx.compose.ui.graphics.Color

// Light/Modern Palette (Calm & Premium)
val PulsoSlate950 = Color(0xFF020617)
val PulsoSlate900 = Color(0xFF0F172A)
val PulsoSlate800 = Color(0xFF1E293B)
val PulsoSlate700 = Color(0xFF334155)
val PulsoSlate400 = Color(0xFF94A3B8)
val PulsoSlate200 = Color(0xFFE2E8F0)
val PulsoSky500 = Color(0xFF0EA5E9)
val PulsoSky400 = Color(0xFF38BDF8)
val PulsoEmerald500 = Color(0xFF10B981)
val PulsoEmerald400 = Color(0xFF34D399)
val PulsoRose500 = Color(0xFFF43F5E)
val PulsoAmber400 = Color(0xFFFBBF24)

// High-Contrast Monochrome / B&W Palette
val BWBlack = Color(0xFF000000)
val BWDenseGray = Color(0xFF18181B)
val BWCard = Color(0xFF27272A)
val BWBorder = Color(0xFF52525B)
val BWTextMuted = Color(0xFFA1A1AA)
val BWWhite = Color(0xFFFFFFFF)

data class PulsoThemeColors(
    val isBlackAndWhite: Boolean,
    val background: Color,
    val surface: Color,
    val cardBackground: Color,
    val topBarBackground: Color,
    val primary: Color,
    val onPrimary: Color,
    val accent: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val border: Color,
    val success: Color,
    val warning: Color,
    val error: Color
)

fun getPulsoTheme(isBlackAndWhite: Boolean): PulsoThemeColors {
    return if (isBlackAndWhite) {
        PulsoThemeColors(
            isBlackAndWhite = true,
            background = BWBlack,
            surface = BWDenseGray,
            cardBackground = BWCard,
            topBarBackground = BWBlack,
            primary = BWWhite,
            onPrimary = BWBlack,
            accent = BWWhite,
            textPrimary = BWWhite,
            textSecondary = BWTextMuted,
            border = BWBorder,
            success = BWWhite,
            warning = BWWhite,
            error = BWWhite
        )
    } else {
        PulsoThemeColors(
            isBlackAndWhite = false,
            background = PulsoSlate950,
            surface = PulsoSlate900,
            cardBackground = PulsoSlate800,
            topBarBackground = PulsoSlate900,
            primary = PulsoSky400,
            onPrimary = PulsoSlate950,
            accent = PulsoSky500,
            textPrimary = Color.White,
            textSecondary = PulsoSlate400,
            border = PulsoSlate700,
            success = PulsoEmerald400,
            warning = PulsoAmber400,
            error = PulsoRose500
        )
    }
}
