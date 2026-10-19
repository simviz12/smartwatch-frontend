package com.pulso.wear.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.compose.runtime.Composable

val DarkBackground = Color(0xFF0A0F14)
val DarkSurface = Color(0xFF131C24)
val DarkSurfaceSubtle = Color(0xFF1B2733)
val DarkSurfaceBorder = Color(0xFF263544)

val PrimaryTeal = Color(0xFF2DD4BF)
val PrimaryTealVariant = Color(0xFF0F766E)
val CalmAmber = Color(0xFFFBBF24)
val CalmAmberVariant = Color(0xFFB45309)
val ReliefGreen = Color(0xFF34D399)
val ReliefGreenVariant = Color(0xFF047857)
val AlertRose = Color(0xFFFB7185)
val AlertRoseVariant = Color(0xFFBE123C)
val OceanBlue = Color(0xFF38BDF8)
val OceanBlueVariant = Color(0xFF0284C7)

val TextPrimary = Color(0xFFF1F5F9)
val TextSecondary = Color(0xFF94A3B8)
val TextTertiary = Color(0xFF64748B)

val PulsoWearColors = Colors(
    primary = PrimaryTeal,
    primaryVariant = PrimaryTealVariant,
    secondary = OceanBlue,
    background = DarkBackground,
    surface = DarkSurface,
    onPrimary = Color(0xFF042F2E),
    onSecondary = Color(0xFF082F49),
    onBackground = TextPrimary,
    onSurface = TextPrimary
)

@Composable
fun PulsoWearTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colors = PulsoWearColors,
        content = content
    )
}
