@file:OptIn(androidx.compose.ui.text.ExperimentalTextApi::class)

package com.pulso.mobile.presentation.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import com.pulso.mobile.R
import com.pulso.mobile.presentation.i18n.AppLanguage
import com.pulso.mobile.presentation.i18n.LocalAppLanguage

/**
 * "Marea" design system tokens (phone). Mirrors the Claude Design handoff:
 * dark is the default, light is a themed variant. Every tension level is
 * always paired with text + icon; color is never the only signal.
 */
@Immutable
data class PulsoColors(
    val isLight: Boolean,
    val bg: Color,
    val surface: Color,
    val surface2: Color,
    val outline: Color,
    val onSurface: Color,
    val muted: Color,
    val primary: Color,
    val onPrimary: Color,
    val primarySoft: Color,
    val heart: Color,
    val heartSoft: Color,
    val hrv: Color,
    val hrvSoft: Color,
    val warn: Color,
    val warnSoft: Color,
    val logoBg: Color,
    val scrim: Color
)

val DarkPulsoColors = PulsoColors(
    isLight = false,
    bg = Color(0xFF0A0F14),
    surface = Color(0xFF121A22),
    surface2 = Color(0xFF18232D),
    outline = Color(0xFF22303B),
    onSurface = Color(0xFFEAF1F5),
    muted = Color(0xFF93A4B1),
    primary = Color(0xFF3DD6B5),
    onPrimary = Color(0xFF04221C),
    primarySoft = Color(0xFF3DD6B5).copy(alpha = 0.14f),
    heart = Color(0xFFFF7A66),
    heartSoft = Color(0xFFFF7A66).copy(alpha = 0.14f),
    hrv = Color(0xFF9AA6FF),
    hrvSoft = Color(0xFF9AA6FF).copy(alpha = 0.14f),
    warn = Color(0xFFF5B94A),
    warnSoft = Color(0xFFF5B94A).copy(alpha = 0.14f),
    logoBg = Color(0xFF0F1E22),
    scrim = Color(0xFF04080C).copy(alpha = 0.62f)
)

val LightPulsoColors = PulsoColors(
    isLight = true,
    bg = Color(0xFFF3F6F7),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFEAF0F2),
    outline = Color(0xFFDCE4E8),
    onSurface = Color(0xFF0C1A22),
    muted = Color(0xFF52646E),
    primary = Color(0xFF0B7D69),
    onPrimary = Color(0xFFFFFFFF),
    primarySoft = Color(0xFF0B7D69).copy(alpha = 0.10f),
    heart = Color(0xFFC2462E),
    heartSoft = Color(0xFFC2462E).copy(alpha = 0.09f),
    hrv = Color(0xFF4A55C9),
    hrvSoft = Color(0xFF4A55C9).copy(alpha = 0.09f),
    warn = Color(0xFF9A6200),
    warnSoft = Color(0xFF9A6200).copy(alpha = 0.10f),
    logoBg = Color(0xFFE6F6F2),
    scrim = Color(0xFF04080C).copy(alpha = 0.45f)
)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

val LocalPulsoColors = staticCompositionLocalOf { DarkPulsoColors }

private fun manrope(weight: Int) = Font(
    R.font.manrope_variable,
    FontWeight(weight),
    variationSettings = FontVariation.Settings(FontVariation.weight(weight))
)

val Manrope = FontFamily(manrope(400), manrope(500), manrope(600), manrope(700), manrope(800))

/** Type scale from the design system (sizes in sp, weights 400–800). */
object PulsoType {
    private val base = TextStyle(fontFamily = Manrope)
    private const val TABULAR = "tnum"

    val display = base.copy(fontSize = 64.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.03).em, fontFeatureSettings = TABULAR)
    val screenTitle = base.copy(fontSize = 30.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em, lineHeight = 36.sp)
    val subScreenTitle = base.copy(fontSize = 24.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.01).em)
    val h1 = base.copy(fontSize = 28.sp, fontWeight = FontWeight.Bold, letterSpacing = (-0.01).em)
    val h2 = base.copy(fontSize = 20.sp, fontWeight = FontWeight.ExtraBold)
    val title = base.copy(fontSize = 16.sp, fontWeight = FontWeight.ExtraBold)
    val label = base.copy(fontSize = 15.sp, fontWeight = FontWeight.Bold)
    val body = base.copy(fontSize = 15.sp, fontWeight = FontWeight.Medium, lineHeight = 22.sp)
    val bodySmall = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Medium, lineHeight = 19.sp)
    val caption = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Medium)
    val kicker = base.copy(fontSize = 12.sp, fontWeight = FontWeight.Bold, letterSpacing = 0.08.em)
    val button = base.copy(fontSize = 15.sp, fontWeight = FontWeight.ExtraBold)
    val chip = base.copy(fontSize = 13.sp, fontWeight = FontWeight.Bold)
    val metric = base.copy(fontSize = 34.sp, fontWeight = FontWeight.ExtraBold, letterSpacing = (-0.02).em, fontFeatureSettings = TABULAR)
    val number = base.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = TABULAR)
}

object Pulso {
    val colors: PulsoColors
        @Composable @ReadOnlyComposable get() = LocalPulsoColors.current
}

@Composable
fun PulsoTheme(
    colors: PulsoColors,
    language: AppLanguage,
    content: @Composable () -> Unit
) {
    val scheme = if (colors.isLight) {
        lightColorScheme(
            primary = colors.primary, onPrimary = colors.onPrimary,
            background = colors.bg, onBackground = colors.onSurface,
            surface = colors.surface, onSurface = colors.onSurface,
            surfaceVariant = colors.surface2, onSurfaceVariant = colors.muted,
            outline = colors.outline, error = colors.heart
        )
    } else {
        darkColorScheme(
            primary = colors.primary, onPrimary = colors.onPrimary,
            background = colors.bg, onBackground = colors.onSurface,
            surface = colors.surface, onSurface = colors.onSurface,
            surfaceVariant = colors.surface2, onSurfaceVariant = colors.muted,
            outline = colors.outline, error = colors.heart
        )
    }
    val body = PulsoType.body
    val typography = Typography(
        displayLarge = PulsoType.display,
        headlineLarge = PulsoType.screenTitle,
        headlineMedium = PulsoType.h1,
        headlineSmall = PulsoType.h2,
        titleLarge = PulsoType.h2,
        titleMedium = PulsoType.title,
        titleSmall = PulsoType.label,
        bodyLarge = body,
        bodyMedium = PulsoType.bodySmall,
        bodySmall = PulsoType.caption,
        labelLarge = PulsoType.button,
        labelMedium = PulsoType.chip,
        labelSmall = PulsoType.caption
    )
    CompositionLocalProvider(
        LocalPulsoColors provides colors,
        LocalAppLanguage provides language
    ) {
        MaterialTheme(colorScheme = scheme, typography = typography, content = content)
    }
}
