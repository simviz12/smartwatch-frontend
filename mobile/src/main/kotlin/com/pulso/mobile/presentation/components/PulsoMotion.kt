package com.pulso.mobile.presentation.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * Motion language for Pulso: calm, short and purposeful.
 * All animations go through Compose, so they respect the system "remove animations" setting.
 */
object PulsoMotion {
    const val STAGGER_MS = 55L
    const val ENTER_MS = 420
    val easing = FastOutSlowInEasing
}

/**
 * Fades and lifts content in when it first appears. [index] staggers sibling cards
 * so a screen "settles" from top to bottom instead of popping in at once.
 */
fun Modifier.reveal(index: Int = 0): Modifier = composed {
    val progress = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(8) * PulsoMotion.STAGGER_MS)
        progress.animateTo(1f, tween(PulsoMotion.ENTER_MS, easing = PulsoMotion.easing))
    }
    val lift = 18.dp
    graphicsLayer {
        val p = progress.value
        alpha = p
        translationY = (1f - p) * lift.toPx()
        val s = 0.985f + 0.015f * p
        scaleX = s
        scaleY = s
    }
}

/** Subtle press feedback: the element sinks slightly while touched. */
fun Modifier.pressScale(interaction: MutableInteractionSource, pressedScale: Float = 0.96f): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        targetValue = if (pressed) pressedScale else 1f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium),
        label = "press"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

/** A number that counts up/down to its new value, for metrics and stats. */
@Composable
fun AnimatedNumber(value: Int?, style: TextStyle, color: Color, modifier: Modifier = Modifier, placeholder: String = "--") {
    if (value == null) {
        Text(placeholder, style = style, color = color, modifier = modifier)
        return
    }
    val shown by animateIntAsState(value, tween(700, easing = PulsoMotion.easing), label = "number")
    Text(shown.toString(), style = style, color = color, modifier = modifier)
}

/** Animates a 0..1 fraction (progress bars, gauges) from its previous value. */
@Composable
fun animatedFraction(target: Float, durationMs: Int = 700): Float {
    val v by animateFloatAsState(target.coerceIn(0f, 1f), tween(durationMs, easing = PulsoMotion.easing), label = "fraction")
    return v
}

/** 0→1 once on first composition (and again when [key] changes): drives "draw-in" chart animations. */
@Composable
fun drawInProgress(key: Any?, durationMs: Int = 900): Float {
    val p = remember(key) { Animatable(0f) }
    LaunchedEffect(key) { p.animateTo(1f, tween(durationMs, easing = PulsoMotion.easing)) }
    return p.value
}
