package com.pulso.mobile.presentation.theme

import androidx.compose.ui.graphics.Color

// Light / Clean Modern Palette (Soft & Premium)
val LightBg = Color(0xFFF8FAFC)
val LightSurface = Color(0xFFFFFFFF)
val LightCard = Color(0xFFFFFFFF)
val LightTopBar = Color(0xFFFFFFFF)
val LightPrimary = Color(0xFF0284C7)
val LightAccent = Color(0xFF0EA5E9)
val LightTextPrimary = Color(0xFF0F172A)
val LightTextSecondary = Color(0xFF64748B)
val LightBorder = Color(0xFFE2E8F0)

// Dark / Clinical Modern Palette (Restful Deep Ocean)
val DarkBg = Color(0xFF070B10)
val DarkSurface = Color(0xFF0F172A)
val DarkCard = Color(0xFF131D2D)
val DarkTopBar = Color(0xFF0B1320)
val DarkPrimary = Color(0xFF38BDF8)
val DarkAccent = Color(0xFF0284C7)
val DarkTextPrimary = Color(0xFFF8FAFC)
val DarkTextSecondary = Color(0xFF94A3B8)
val DarkBorder = Color(0xFF1E293B)

data class PulsoThemeColors(
    val isLightMode: Boolean,
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

fun getPulsoTheme(isLightMode: Boolean): PulsoThemeColors {
    return if (isLightMode) {
        PulsoThemeColors(
            isLightMode = true,
            background = LightBg,
            surface = LightSurface,
            cardBackground = LightCard,
            topBarBackground = LightTopBar,
            primary = LightPrimary,
            onPrimary = Color.White,
            accent = LightAccent,
            textPrimary = LightTextPrimary,
            textSecondary = LightTextSecondary,
            border = LightBorder,
            success = Color(0xFF10B981),
            warning = Color(0xFFF59E0B),
            error = Color(0xFFEF4444)
        )
    } else {
        PulsoThemeColors(
            isLightMode = false,
            background = DarkBg,
            surface = DarkSurface,
            cardBackground = DarkCard,
            topBarBackground = DarkTopBar,
            primary = DarkPrimary,
            onPrimary = Color(0xFF020617),
            accent = DarkAccent,
            textPrimary = DarkTextPrimary,
            textSecondary = DarkTextSecondary,
            border = DarkBorder,
            success = Color(0xFF34D399),
            warning = Color(0xFFFBBF24),
            error = Color(0xFFFB7185)
        )
    }
}
