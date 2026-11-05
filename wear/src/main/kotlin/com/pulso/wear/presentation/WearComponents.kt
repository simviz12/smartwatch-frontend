package com.pulso.wear.presentation

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.Text
import com.pulso.wear.presentation.theme.WearColors
import com.pulso.wear.presentation.theme.WearIcons
import com.pulso.wear.presentation.theme.WearType
import kotlinx.coroutines.delay

// ───────────────────────── Motion ─────────────────────────

/** Items fade, rise and scale in one after another when a screen opens. */
fun Modifier.wearReveal(index: Int): Modifier = composed {
    val p = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        delay(index.coerceAtMost(6) * 60L)
        p.animateTo(1f, tween(460, easing = FastOutSlowInEasing))
    }
    graphicsLayer {
        alpha = p.value
        translationY = (1f - p.value) * 14.dp.toPx()
        val s = 0.94f + 0.06f * p.value
        scaleX = s
        scaleY = s
    }
}

/** Press feedback for wrist targets: the element sinks slightly while touched. */
fun Modifier.wearPress(interaction: MutableInteractionSource, pressed: Float = 0.94f): Modifier = composed {
    val isPressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(
        if (isPressed) pressed else 1f,
        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMediumLow),
        label = "press"
    )
    graphicsLayer {
        scaleX = scale
        scaleY = scale
    }
}

// ───────────────────────── Surfaces ─────────────────────────

/** Graphite glass: a soft top highlight fading into the surface, with a hairline border. */
val GlassBrush: Brush = Brush.verticalGradient(listOf(WearColors.SurfaceHigh, WearColors.Surface))

fun Modifier.glass(shape: Shape): Modifier =
    this.clip(shape).background(GlassBrush).border(1.dp, WearColors.Hairline, shape)

/** Tinted glass for status cards: a whisper of the data colour over graphite. */
fun Modifier.tintedGlass(tint: Color, shape: Shape): Modifier =
    this.clip(shape)
        .background(Brush.verticalGradient(listOf(tint.copy(alpha = 0.20f), WearColors.Surface)))
        .border(1.dp, tint.copy(alpha = 0.22f), shape)

// ───────────────────────── Buttons ─────────────────────────

/**
 * List action: glass card with a tinted icon disc, title, optional subtitle and chevron.
 * Replaces the old flat pills.
 */
@Composable
fun ActionCard(
    title: String,
    icon: ImageVector,
    tint: Color,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    subtitle: String? = null,
    trailing: (@Composable RowScope.() -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(26.dp)
    Row(
        modifier = modifier
            .wearPress(interaction)
            .fillMaxWidth()
            .heightIn(min = 56.dp)
            .glass(shape)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
            .padding(start = 8.dp, end = 12.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(tint.copy(alpha = 0.16f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(19.dp))
        }
        Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
            Text(title, style = WearType.label, maxLines = 1)
            if (subtitle != null) Text(subtitle, style = WearType.body, color = WearColors.Muted, maxLines = 1)
        }
        if (trailing != null) trailing() else {
            Icon(WearIcons.ChevronRight, contentDescription = null, tint = WearColors.Dim, modifier = Modifier.size(14.dp))
        }
    }
}

/**
 * Primary action that hugs the bottom curve of a round screen (Wear OS "edge button"):
 * white capsule, flat-ish top, deeply rounded bottom corners.
 */
@Composable
fun EdgeButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    enabled: Boolean = true,
    background: Color = WearColors.Primary,
    content: Color = WearColors.OnPrimary,
    height: Dp = 54.dp
) {
    val interaction = remember { MutableInteractionSource() }
    val shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp, bottomStart = 44.dp, bottomEnd = 44.dp)
    Row(
        modifier = modifier
            .wearPress(interaction, 0.95f)
            .fillMaxWidth(0.82f)
            .height(height)
            .clip(shape)
            .background(if (enabled) background else background.copy(alpha = 0.35f))
            .clickable(interactionSource = interaction, indication = LocalIndication.current, enabled = enabled, role = Role.Button, onClick = onClick),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (icon != null) Icon(icon, contentDescription = null, tint = content, modifier = Modifier.size(18.dp))
        Text(text, style = WearType.label.copy(fontSize = 15.sp), color = content, maxLines = 1)
    }
}

/** Circular glass button (or solid when [background] is given). */
@Composable
fun WearRoundButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 44.dp,
    background: Color? = null,
    tint: Color = WearColors.Text
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        Modifier
            .wearPress(interaction, 0.88f)
            .size(size)
            .then(if (background == null) Modifier.glass(CircleShape) else Modifier.clip(CircleShape).background(background))
            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.44f))
    }
}

/** Small status pill: dot + label, tinted with a data colour. */
@Composable
fun WearBadge(text: String, color: Color, modifier: Modifier = Modifier) {
    Row(
        modifier.clip(CircleShape).background(color.copy(alpha = 0.14f)).padding(horizontal = 9.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Box(Modifier.size(6.dp).clip(CircleShape).background(color))
        Text(text, style = WearType.body.copy(fontWeight = FontWeight.ExtraBold), color = color, maxLines = 1)
    }
}

/** Small uppercase heading used at the top of watch screens. */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier, color: Color = WearColors.Muted) {
    Text(text.uppercase(), style = WearType.kicker.copy(letterSpacing = 1.4.sp), color = color, modifier = modifier)
}
