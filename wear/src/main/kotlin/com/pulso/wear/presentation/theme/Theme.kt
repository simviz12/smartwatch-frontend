package com.pulso.wear.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.compose.runtime.Composable

val DarkBackground = Color(0xFF000000)
val PrimaryTeal = Color(0xFF00E5FF)
val PrimaryVariant = Color(0xFF00B0FF)
val SecondaryOrange = Color(0xFFFF9100)
val SurfaceDark = Color(0xFF1E1E1E)
val TextPrimary = Color(0xFFFFFFFF)
val TextSecondary = Color(0xFFB0BEC5)
val SuccessGreen = Color(0xFF00E676)
val ErrorRed = Color(0xFFFF5252)

val PulsoWearColors = Colors(
    primary = PrimaryTeal,
    primaryVariant = PrimaryVariant,
    secondary = SecondaryOrange,
    background = DarkBackground,
    surface = SurfaceDark,
    onPrimary = Color.Black,
    onSecondary = Color.Black,
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
