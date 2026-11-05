@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.pulso.wear.presentation.theme

import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Colors
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Typography
import com.pulso.wear.R

/**
 * "Marea Noche" tokens for Wear OS: monochrome and premium. Pure black for OLED,
 * graphite glass surfaces with hairline borders, white primary actions with black text,
 * and colour reserved for data (calm, pulse, HRV, alert) in soft pastels.
 * Tap targets ≥ 48 dp and 12 sp minimum text.
 */
object WearColors {
    val Bg = Color(0xFF000000)
    val Surface = Color(0xFF15171B)
    val SurfaceHigh = Color(0xFF1E2127)
    val Surface2 = Color(0xFF24282F)
    val Track = Color(0xFF2B3038)
    val Ring = Color(0xFF3B414B)
    val Hairline = Color(0x1FFFFFFF)
    val Text = Color(0xFFF4F6F8)
    val Muted = Color(0xFF9198A4)
    val Dim = Color(0xFF6E7582)

    /** Primary action: white capsule with near-black text. */
    val Primary = Color(0xFFF4F6F8)
    val OnPrimary = Color(0xFF0B0D10)

    /** Data colours (pastel, never the only signal: always paired with text). */
    val Calm = Color(0xFF8FEBD3)
    val Heart = Color(0xFFFF8A7D)
    val Hrv = Color(0xFFB4BBFF)
    val Sky = Color(0xFF9AD8FF)
    val Warn = Color(0xFFFFCF7A)
    val OnWarn = Color(0xFF221800)
}

private fun manrope(weight: Int) = Font(
    R.font.manrope_variable,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val Manrope = FontFamily(manrope(500), manrope(600), manrope(700), manrope(800))

object WearType {
    private val base = TextStyle(fontFamily = Manrope, color = WearColors.Text)
    val hero = base.copy(fontSize = 60.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.04).em, fontFeatureSettings = "tnum", lineHeight = 60.sp)
    val bigNumber = base.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum", lineHeight = 36.sp)
    val title = base.copy(fontSize = 17.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 20.sp)
    val label = base.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, lineHeight = 17.sp)
    val body = base.copy(fontSize = 12.sp, fontWeight = FontWeight.SemiBold, lineHeight = 15.sp)
    val kicker = base.copy(fontSize = 12.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = 0.08.em)
    val time = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum")
}

private val PulsoWearColors = Colors(
    primary = WearColors.Calm,
    primaryVariant = Color(0xFF2E7D6B),
    secondary = WearColors.Hrv,
    background = WearColors.Bg,
    surface = WearColors.Surface,
    error = WearColors.Heart,
    onPrimary = Color(0xFF06201A),
    onSecondary = Color(0xFF0B1240),
    onBackground = WearColors.Text,
    onSurface = WearColors.Text,
    onSurfaceVariant = WearColors.Muted
)

@Composable
fun PulsoWearTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colors = PulsoWearColors,
        typography = Typography(defaultFontFamily = Manrope),
        content = content
    )
}

/** Same 24-grid icon paths as the phone, so both apps read as one system. */
object WearIcons {
    private fun stroke(name: String, d: String, width: Float = 2.2f) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(PathParser().parsePathString(d).toNodes(), fill = null, stroke = SolidColor(Color.Black),
                strokeLineWidth = width, strokeLineCap = StrokeCap.Round, strokeLineJoin = StrokeJoin.Round)
            .build()

    private fun fill(name: String, d: String) =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.Black))
            .build()

    val Wind by lazy { stroke("wind", "M3 8h10a3 3 0 1 0-3-3M3 12h15a3 3 0 1 1-3 3M3 16h7") }
    val HeartFilled by lazy { fill("heart", "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z") }
    val Mic by lazy { stroke("mic", "M12 3a3 3 0 0 1 3 3v6a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3zM5 11a7 7 0 0 0 14 0M12 18v3") }
    val BellOff by lazy { stroke("bell_off", "M6 9a6 6 0 0 1 12 0c0 6 2 7 2 7H4s2-1 2-7M10 20h4M3 3l18 18") }
    val Sync by lazy { stroke("sync", "M20 11a8 8 0 0 0-14.9-3M4 4v4h4M4 13a8 8 0 0 0 14.9 3M20 20v-4h-4", 2.6f) }
    val Vibration by lazy { stroke("vibration", "M8 4h8v16H8zM4 8v8M20 8v8") }
    val VibrationStrong by lazy { stroke("vibration_strong", "M8 4h8v16H8zM4 8v8M20 8v8M1.5 10v4M22.5 10v4", 2f) }
    val Check by lazy { stroke("check", "M5 12.5 10 17 19 7", 3f) }
    val ChevronLeft by lazy { stroke("chevron_left", "M15 6l-6 6 6 6", 2.6f) }
    val ChevronRight by lazy { stroke("chevron_right", "M9 6l6 6-6 6", 2.6f) }
    val Play by lazy { fill("play", "M8 5l12 7-12 7z") }
    val Pause by lazy { fill("pause", "M7 5h3.5v14H7zM13.5 5H17v14h-3.5z") }
    val Stop by lazy { fill("stop", "M6 8a2 2 0 0 1 2-2h8a2 2 0 0 1 2 2v8a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2z") }
    val Spark by lazy { fill("spark", "M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9z") }
    val Watch by lazy { stroke("watch", "M8 7h8a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2zM9 7l1-4h4l1 4M9 17l1 4h4l1-4") }
    val Wave by lazy { stroke("wave", "M3 12h4l2-5 4 10 2-5h6", 2.6f) }
    val Bolt by lazy { fill("bolt", "M13 2 4 14h7l-1 8 9-12h-7z") }
    val Speaker by lazy { stroke("speaker", "M4 9h4l5-4v14l-5-4H4zM17 9a4 4 0 0 1 0 6") }
}
