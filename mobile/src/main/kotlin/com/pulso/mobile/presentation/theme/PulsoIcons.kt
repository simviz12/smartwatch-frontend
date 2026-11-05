package com.pulso.mobile.presentation.theme

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/**
 * Icon set from the "Marea" design: 24-unit grid, rounded strokes.
 * Built from the exact SVG path data of the handoff so phone and watch match it.
 */
object PulsoIcons {
    private fun stroke(name: String, d: String, width: Float = 2f): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(
                pathData = PathParser().parsePathString(d).toNodes(),
                fill = null,
                stroke = SolidColor(Color.Black),
                strokeLineWidth = width,
                strokeLineCap = StrokeCap.Round,
                strokeLineJoin = StrokeJoin.Round
            )
            .build()

    private fun fill(name: String, d: String): ImageVector =
        ImageVector.Builder(name, 24.dp, 24.dp, 24f, 24f)
            .addPath(pathData = PathParser().parsePathString(d).toNodes(), fill = SolidColor(Color.Black))
            .build()

    val Home by lazy { stroke("home", "M3 10.5 12 3l9 7.5V20a1 1 0 0 1-1 1h-5v-6h-6v6H4a1 1 0 0 1-1-1z") }
    val History by lazy { stroke("history", "M3 3v18h18M7 15l4-4 3 3 5-6") }
    val Wind by lazy { stroke("wind", "M3 8h10a3 3 0 1 0-3-3M3 12h15a3 3 0 1 1-3 3M3 16h7") }
    val Spark by lazy { stroke("spark", "M12 3l1.9 5.1L19 10l-5.1 1.9L12 17l-1.9-5.1L5 10l5.1-1.9zM19 15l.8 2.2L22 18l-2.2.8L19 21l-.8-2.2L16 18l2.2-.8z") }
    val Sliders by lazy { stroke("sliders", "M4 6h9M17 6h3M4 12h3M11 12h9M4 18h11M19 18h1M15 4v4M9 10v4M17 16v4") }
    val Heart by lazy { stroke("heart", "M12 20s-7-4.4-7-10a4 4 0 0 1 7-2.6A4 4 0 0 1 19 10c0 5.6-7 10-7 10z") }
    val Wave by lazy { stroke("wave", "M3 12h4l2-5 4 10 2-5h6") }
    val Mic by lazy { stroke("mic", "M12 3a3 3 0 0 1 3 3v6a3 3 0 0 1-6 0V6a3 3 0 0 1 3-3zM5 11a7 7 0 0 0 14 0M12 18v3") }
    val Bolt by lazy { stroke("bolt", "M13 2 4 14h7l-1 8 9-12h-7z") }
    val Grid by lazy { stroke("grid", "M4 4h4v4H4zM10 4h4v4h-4zM16 4h4v4h-4zM4 10h4v4H4zM10 10h4v4h-4zM16 10h4v4h-4zM4 16h4v4H4zM10 16h4v4h-4zM16 16h4v4h-4z") }
    val Target by lazy { stroke("target", "M12 3a9 9 0 1 1 0 18 9 9 0 0 1 0-18zM12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8z") }
    val Flask by lazy { stroke("flask", "M9 3h6M10 3v6L4.5 18.5A1.7 1.7 0 0 0 6 21h12a1.7 1.7 0 0 0 1.5-2.5L14 9V3M7 15h10") }
    val Watch by lazy { stroke("watch", "M8 7h8a2 2 0 0 1 2 2v6a2 2 0 0 1-2 2H8a2 2 0 0 1-2-2V9a2 2 0 0 1 2-2zM9 7l1-4h4l1 4M9 17l1 4h4l1-4") }
    val Sync by lazy { stroke("sync", "M20 11a8 8 0 0 0-14.9-3M4 4v4h4M4 13a8 8 0 0 0 14.9 3M20 20v-4h-4") }
    val Moon by lazy { stroke("moon", "M20 14.5A8 8 0 1 1 9.5 4a6.5 6.5 0 0 0 10.5 10.5z") }
    val Sun by lazy { stroke("sun", "M12 8a4 4 0 1 1 0 8 4 4 0 0 1 0-8zM12 2v2M12 20v2M4.9 4.9l1.4 1.4M17.7 17.7l1.4 1.4M2 12h2M20 12h2M4.9 19.1l1.4-1.4M17.7 6.3l1.4-1.4") }
    val BellOff by lazy { stroke("bell_off", "M6 9a6 6 0 0 1 12 0c0 6 2 7 2 7H4s2-1 2-7M10 20h4M3 3l18 18") }
    val Vibration by lazy { stroke("vibration", "M8 4h8v16H8zM4 8v8M20 8v8") }
    val Globe by lazy { stroke("globe", "M12 3a9 9 0 1 1 0 18 9 9 0 0 1 0-18zM3 12h18M12 3c2.5 2.6 3.7 5.6 3.7 9s-1.2 6.4-3.7 9c-2.5-2.6-3.7-5.6-3.7-9S9.5 5.6 12 3z") }
    val Shield by lazy { stroke("shield", "M12 3l8 3v6c0 4.5-3.4 8-8 9-4.6-1-8-4.5-8-9V6z") }
    val Battery by lazy { stroke("battery", "M3 8h15a1 1 0 0 1 1 1v6a1 1 0 0 1-1 1H3a1 1 0 0 1-1-1V9a1 1 0 0 1 1-1zM22 11v2") }
    val Clock by lazy { stroke("clock", "M12 3a9 9 0 1 1 0 18 9 9 0 0 1 0-18zM12 7v5l3 2") }
    val Check by lazy { stroke("check", "M5 12.5 10 17 19 7", 2.4f) }
    val ChevronRight by lazy { stroke("chevron_right", "M9 6l6 6-6 6") }
    val ChevronLeft by lazy { stroke("chevron_left", "M15 6l-6 6 6 6", 2.2f) }
    val ArrowRight by lazy { stroke("arrow_right", "M5 12h14M13 6l6 6-6 6", 2.2f) }
    val ArrowUp by lazy { stroke("arrow_up", "M12 19V5M6 11l6-6 6 6", 2.2f) }
    val Info by lazy { stroke("info", "M12 3a9 9 0 1 1 0 18 9 9 0 0 1 0-18zM12 11v5M12 8h.01") }
    val Link by lazy { stroke("link", "M12 3a9 9 0 1 1 0 18 9 9 0 0 1 0-18zM8 12h8") }
    val Stop by lazy { fill("stop", "M7 7h10v10H7z") }
    val Play by lazy { fill("play", "M8 5l12 7-12 7z") }
    val Speaker by lazy { stroke("speaker", "M4 9h4l5-4v14l-5-4H4zM17 9a4 4 0 0 1 0 6") }
}
