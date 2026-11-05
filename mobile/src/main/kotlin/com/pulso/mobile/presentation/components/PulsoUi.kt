package com.pulso.mobile.presentation.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.material3.ripple
import androidx.compose.runtime.remember
import androidx.compose.ui.draw.scale
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.pulso.mobile.presentation.i18n.tr
import com.pulso.mobile.presentation.theme.Pulso
import com.pulso.mobile.presentation.theme.PulsoIcons
import com.pulso.mobile.presentation.theme.PulsoType

// ───────────────────────── Surfaces ─────────────────────────

@Composable
fun PulsoCard(
    modifier: Modifier = Modifier,
    radius: Dp = 22.dp,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    background: Color = Pulso.colors.surface,
    border: Color? = Pulso.colors.outline,
    onClick: (() -> Unit)? = null,
    verticalArrangement: Arrangement.Vertical = Arrangement.spacedBy(12.dp),
    horizontalAlignment: Alignment.Horizontal = Alignment.Start,
    content: @Composable ColumnScope.() -> Unit
) {
    val shape = RoundedCornerShape(radius)
    val interaction = remember { MutableInteractionSource() }
    Column(
        modifier = modifier
            .then(if (onClick != null) Modifier.pressScale(interaction, 0.98f) else Modifier)
            .fillMaxWidth()
            .clip(shape)
            .background(background)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .then(if (onClick != null) Modifier.clickable(interactionSource = interaction, indication = ripple(), onClick = onClick) else Modifier)
            .padding(contentPadding),
        verticalArrangement = verticalArrangement,
        horizontalAlignment = horizontalAlignment,
        content = content
    )
}

@Composable
fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text = text.uppercase(),
        style = PulsoType.kicker.copy(fontWeight = FontWeight.ExtraBold),
        color = Pulso.colors.muted,
        modifier = modifier.padding(horizontal = 4.dp)
    )
}

@Composable
fun ScreenHeader(
    title: String,
    kicker: String? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp),
        verticalAlignment = Alignment.Bottom,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(2.dp), modifier = Modifier.weight(1f)) {
            if (kicker != null) {
                Text(kicker.uppercase(), style = PulsoType.kicker, color = Pulso.colors.muted, maxLines = 1, overflow = TextOverflow.Ellipsis)
            }
            Text(title, style = PulsoType.screenTitle, color = Pulso.colors.onSurface)
        }
        trailing?.invoke()
    }
}

@Composable
fun BackHeader(
    title: String,
    onBack: () -> Unit,
    trailing: (@Composable () -> Unit)? = null
) {
    val c = Pulso.colors
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .clickable(onClick = onBack, role = Role.Button)
                .semantics { contentDescription = "back" },
            contentAlignment = Alignment.Center
        ) {
            Icon(PulsoIcons.ChevronLeft, contentDescription = null, tint = c.onSurface, modifier = Modifier.size(22.dp))
        }
        Text(title, style = PulsoType.subScreenTitle, color = c.onSurface, modifier = Modifier.weight(1f))
        trailing?.invoke()
    }
}

@Composable
fun IconBadge(
    icon: ImageVector,
    tint: Color,
    background: Color,
    size: Dp = 42.dp,
    iconSize: Dp = 20.dp,
    radius: Dp = 14.dp
) {
    Box(
        modifier = Modifier.size(size).clip(RoundedCornerShape(radius)).background(background),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(iconSize))
    }
}

@Composable
fun IconCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: Dp = 44.dp,
    background: Color = Pulso.colors.surface,
    tint: Color = Pulso.colors.onSurface,
    border: Color? = Pulso.colors.outline
) {
    val interaction = remember { MutableInteractionSource() }
    Box(
        modifier = modifier
            .pressScale(interaction, 0.9f)
            .size(size)
            .clip(CircleShape)
            .background(background)
            .then(if (border != null) Modifier.border(1.dp, border, CircleShape) else Modifier)
            .clickable(interactionSource = interaction, indication = ripple(), onClick = onClick, role = Role.Button)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
    }
}

// ───────────────────────── Buttons ─────────────────────────

enum class PulsoButtonStyle { Primary, Tonal, Ghost, Danger, DangerGhost }

@Composable
fun PulsoButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: PulsoButtonStyle = PulsoButtonStyle.Primary,
    icon: ImageVector? = null,
    trailingIcon: ImageVector? = null,
    height: Dp = 52.dp,
    enabled: Boolean = true,
    loading: Boolean = false,
    textStyle: androidx.compose.ui.text.TextStyle = PulsoType.button
) {
    val c = Pulso.colors
    val (bg, fg, border) = when (style) {
        PulsoButtonStyle.Primary -> Triple(c.primary, c.onPrimary, null)
        PulsoButtonStyle.Tonal -> Triple(c.primarySoft, c.primary, null)
        PulsoButtonStyle.Ghost -> Triple(Color.Transparent, c.onSurface, c.outline)
        PulsoButtonStyle.Danger -> Triple(c.heart, c.bg, null)
        PulsoButtonStyle.DangerGhost -> Triple(Color.Transparent, c.heart, c.outline)
    }
    val shape = RoundedCornerShape(height / 2)
    val interaction = remember { MutableInteractionSource() }
    Row(
        modifier = modifier
            .pressScale(interaction)
            .heightIn(min = height)
            .height(height)
            .alpha(if (enabled) 1f else 0.45f)
            .clip(shape)
            .background(bg)
            .then(if (border != null) Modifier.border(1.dp, border, shape) else Modifier)
            .clickable(interactionSource = interaction, indication = ripple(), enabled = enabled && !loading, onClick = onClick, role = Role.Button)
            .padding(horizontal = 20.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (loading) {
            CircularProgressIndicator(color = fg, strokeWidth = 2.dp, modifier = Modifier.size(18.dp))
        } else if (icon != null) {
            Icon(icon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        }
        Text(text, style = textStyle, color = fg, maxLines = 1, overflow = TextOverflow.Ellipsis)
        if (trailingIcon != null) {
            Icon(trailingIcon, contentDescription = null, tint = fg, modifier = Modifier.size(20.dp))
        }
    }
}

/** Pill-shaped segmented control: track = surface2, selected segment = surface. */
@Composable
fun SegmentedControl(
    options: List<String>,
    selectedIndex: Int,
    onSelect: (Int) -> Unit,
    modifier: Modifier = Modifier,
    segmentHeight: Dp = 32.dp,
    fill: Boolean = false,
    icons: List<ImageVector>? = null,
    textStyle: androidx.compose.ui.text.TextStyle = PulsoType.chip.copy(fontSize = 12.sp)
) {
    val c = Pulso.colors
    Row(
        modifier = modifier
            .clip(CircleShape)
            .background(c.surface2)
            .padding(3.dp)
    ) {
        options.forEachIndexed { i, label ->
            val selected = i == selectedIndex
            val bg by animateColorAsState(if (selected) c.surface else Color.Transparent, tween(180), label = "seg")
            Row(
                modifier = Modifier
                    .then(if (fill) Modifier.weight(1f) else Modifier)
                    .height(segmentHeight)
                    .clip(CircleShape)
                    .background(bg)
                    .clickable(role = Role.Tab) { onSelect(i) }
                    .padding(horizontal = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                verticalAlignment = Alignment.CenterVertically
            ) {
                icons?.getOrNull(i)?.let {
                    Icon(it, contentDescription = null, tint = if (selected) c.onSurface else c.muted, modifier = Modifier.size(18.dp))
                }
                Text(
                    label,
                    style = textStyle.copy(fontWeight = if (selected) FontWeight.ExtraBold else FontWeight.Bold),
                    color = if (selected) c.onSurface else c.muted,
                    maxLines = 1
                )
            }
        }
    }
}

enum class ChipStyle { Accent, Inverse }

@Composable
fun PulsoChip(
    text: String,
    selected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    style: ChipStyle = ChipStyle.Accent,
    height: Dp = 40.dp
) {
    val c = Pulso.colors
    val (bg, fg, bd) = when {
        selected && style == ChipStyle.Accent -> Triple(c.primary, c.onPrimary, c.primary)
        selected -> Triple(c.onSurface, c.bg, c.onSurface)
        else -> Triple(Color.Transparent, c.onSurface, c.outline)
    }
    val interaction = remember { MutableInteractionSource() }
    val animatedBg by animateColorAsState(bg, tween(200), label = "chipBg")
    Box(
        modifier = modifier
            .pressScale(interaction, 0.94f)
            .height(height)
            .clip(CircleShape)
            .background(animatedBg)
            .border(1.dp, bd, CircleShape)
            .clickable(interactionSource = interaction, indication = ripple(), role = Role.RadioButton, onClick = onClick)
            .padding(horizontal = 14.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, style = PulsoType.chip, color = fg, maxLines = 1)
    }
}

@Composable
fun PulsoSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit, contentDescription: String) {
    val c = Pulso.colors
    val track by animateColorAsState(if (checked) c.primary else c.surface2, tween(180), label = "track")
    val knob by animateColorAsState(if (checked) c.onPrimary else c.muted, tween(180), label = "knob")
    val offset by animateDpAsState(if (checked) 20.dp else 0.dp, tween(180), label = "offset")
    Box(
        modifier = Modifier
            .width(52.dp)
            .height(32.dp)
            .clip(CircleShape)
            .background(track)
            .clickable(role = Role.Switch) { onCheckedChange(!checked) }
            .semantics { this.contentDescription = contentDescription }
            .padding(3.dp)
    ) {
        Box(Modifier.offset(x = offset).size(26.dp).clip(CircleShape).background(knob))
    }
}

@Composable
fun PulsoProgressBar(
    fraction: Float,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 6.dp,
    track: Color = Pulso.colors.surface2
) {
    Box(modifier.fillMaxWidth().height(height).clip(CircleShape).background(track)) {
        Box(
            Modifier
                .fillMaxWidth(animatedFraction(fraction))
                .height(height)
                .clip(CircleShape)
                .background(color)
        )
    }
}

@Composable
fun StatusPill(text: String, color: Color, background: Color, modifier: Modifier = Modifier, dot: Boolean = true) {
    Row(
        modifier = modifier.clip(CircleShape).background(background).padding(horizontal = 12.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        if (dot) Box(Modifier.size(8.dp).clip(CircleShape).background(color))
        Text(text, style = PulsoType.chip.copy(fontWeight = FontWeight.ExtraBold), color = color)
    }
}

/** Two-column row inside a list card: icon tile · label · trailing content. */
@Composable
fun ListRow(
    label: String,
    modifier: Modifier = Modifier,
    icon: ImageVector? = null,
    iconTint: Color = Pulso.colors.primary,
    subtitle: String? = null,
    showDivider: Boolean = true,
    onClick: (() -> Unit)? = null,
    minHeight: Dp = 56.dp,
    trailing: @Composable RowScope.() -> Unit = {}
) {
    val c = Pulso.colors
    Column(modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = minHeight)
                .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (icon != null) {
                IconBadge(icon, tint = iconTint, background = c.surface2, size = 34.dp, iconSize = 18.dp, radius = 11.dp)
            }
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(label, style = PulsoType.label, color = c.onSurface)
                if (subtitle != null) Text(subtitle, style = PulsoType.caption, color = c.muted)
            }
            trailing()
        }
        if (showDivider) Box(Modifier.fillMaxWidth().height(1.dp).background(c.outline))
    }
}

@Composable
fun Stepper(value: Int, onDecrement: () -> Unit, onIncrement: () -> Unit) {
    val c = Pulso.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        IconCircleButtonText("−", onDecrement)
        Text(
            "$value",
            style = PulsoType.number.copy(fontSize = 17.sp),
            color = c.onSurface,
            textAlign = TextAlign.Center,
            modifier = Modifier.width(26.dp)
        )
        IconCircleButtonText("+", onIncrement)
    }
}

@Composable
private fun IconCircleButtonText(symbol: String, onClick: () -> Unit) {
    val c = Pulso.colors
    Box(
        modifier = Modifier
            .size(36.dp)
            .clip(CircleShape)
            .border(1.dp, c.outline, CircleShape)
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = symbol },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, style = PulsoType.number.copy(fontSize = 18.sp), color = c.onSurface)
    }
}

@Composable
fun FeedbackText(text: String?, positive: Boolean = true, modifier: Modifier = Modifier) {
    val shown = com.pulso.mobile.presentation.i18n.localizeStatus(text) ?: return
    val c = Pulso.colors
    Text(
        shown,
        style = PulsoType.bodySmall,
        color = if (positive) c.primary else c.warn,
        textAlign = TextAlign.Center,
        modifier = modifier.fillMaxWidth()
    )
}

// ───────────────────────── Brand ─────────────────────────

/** The Pulso mark: a calm ring crossed by a heartbeat line. */
@Composable
fun PulsoLogo(size: Dp, modifier: Modifier = Modifier, circular: Boolean = false) {
    val c = Pulso.colors
    Canvas(modifier.size(size)) {
        val s = this.size.width / 108f
        val r = if (circular) this.size.width / 2 else 30f * s
        drawRoundRect(color = c.logoBg, cornerRadius = androidx.compose.ui.geometry.CornerRadius(r, r))
        drawCircle(color = c.primary, radius = 30f * s, style = Stroke(width = 8f * s))
        val p = Path().apply {
            moveTo(18f * s, 56f * s); lineTo(38f * s, 56f * s); lineTo(45f * s, 42f * s)
            lineTo(55f * s, 68f * s); lineTo(62f * s, 52f * s); lineTo(90f * s, 52f * s)
        }
        drawPath(p, color = c.heart, style = Stroke(width = 8f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

// ───────────────────────── Navigation ─────────────────────────

enum class NavDest { TODAY, HISTORY, BREATHE, ASSISTANT, SETTINGS }

@Composable
fun PulsoBottomBar(selected: NavDest, onSelect: (NavDest) -> Unit) {
    val c = Pulso.colors
    val items = listOf(
        Triple(NavDest.TODAY, PulsoIcons.Home, tr("Hoy", "Today")),
        Triple(NavDest.HISTORY, PulsoIcons.History, tr("Historial", "History")),
        Triple(NavDest.BREATHE, PulsoIcons.Wind, tr("Respirar", "Breathe")),
        Triple(NavDest.ASSISTANT, PulsoIcons.Spark, tr("Asistente", "Assistant")),
        Triple(NavDest.SETTINGS, PulsoIcons.Sliders, tr("Ajustes", "Settings"))
    )
    Column(Modifier.fillMaxWidth().background(c.surface)) {
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.outline))
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .windowInsetsPadding(WindowInsets.navigationBars)
                .padding(top = 10.dp, bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            items.forEach { (dest, icon, label) ->
                val active = dest == selected
                val pill by animateColorAsState(if (active) c.primarySoft else Color.Transparent, tween(200), label = "pill")
                Column(
                    modifier = Modifier
                        .width(72.dp)
                        .clip(RoundedCornerShape(16.dp))
                        .clickable(role = Role.Tab) { onSelect(dest) }
                        .padding(vertical = 2.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Box(
                        Modifier.width(56.dp).height(30.dp).clip(CircleShape).background(pill),
                        contentAlignment = Alignment.Center
                    ) {
                        val iconScale by animateFloatAsState(
                            if (active) 1.12f else 1f,
                            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow),
                            label = "navIcon"
                        )
                        Icon(icon, contentDescription = null, tint = if (active) c.primary else c.muted, modifier = Modifier.size(22.dp).scale(iconScale))
                    }
                    Text(
                        label,
                        style = PulsoType.caption.copy(fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold),
                        color = if (active) c.onSurface else c.muted,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

// ───────────────────────── Dialogs ─────────────────────────

/** Bottom-anchored confirmation sheet used for destructive actions. */
@Composable
fun PulsoConfirmSheet(
    icon: ImageVector,
    title: String,
    body: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
    secondaryLabel: String? = null,
    onSecondary: (() -> Unit)? = null
) {
    val c = Pulso.colors
    Dialog(onDismissRequest = onDismiss, properties = DialogProperties(usePlatformDefaultWidth = false)) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(c.scrim)
                .clickable(onClick = onDismiss)
                .padding(16.dp),
            contentAlignment = Alignment.BottomCenter
        ) {
            PulsoCard(
                radius = 28.dp,
                contentPadding = PaddingValues(24.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp),
                modifier = Modifier.clickable(enabled = false) {}
            ) {
                IconBadge(icon, tint = c.heart, background = c.heartSoft, size = 48.dp, iconSize = 24.dp, radius = 16.dp)
                Text(title, style = PulsoType.h2, color = c.onSurface)
                Text(body, style = PulsoType.bodySmall.copy(fontSize = 14.sp, lineHeight = 21.sp), color = c.muted)
                Spacer(Modifier.height(2.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    PulsoButton(tr("Cancelar", "Cancel"), onDismiss, Modifier.weight(1f), style = PulsoButtonStyle.Ghost, height = 50.dp)
                    PulsoButton(confirmLabel, onConfirm, Modifier.weight(1f), style = PulsoButtonStyle.Danger, height = 50.dp)
                }
                if (secondaryLabel != null && onSecondary != null) {
                    PulsoButton(secondaryLabel, onSecondary, Modifier.fillMaxWidth(), style = PulsoButtonStyle.DangerGhost, height = 48.dp)
                }
            }
        }
    }
}

// ───────────────────────── Charts ─────────────────────────

@Composable
fun Sparkline(values: List<Double>, color: Color, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        if (values.size < 2) return@Canvas
        val mn = values.min()
        val mx = values.max()
        val span = (mx - mn).takeIf { it > 0 } ?: 1.0
        val path = Path()
        values.forEachIndexed { i, v ->
            val x = i / (values.size - 1f) * size.width
            val y = size.height - ((v - mn) / span).toFloat() * (size.height - 4f) - 2f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        drawPath(path, color, style = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}

@Composable
fun LegendSwatch(label: String, fill: Color, border: Color, round: Boolean = false) {
    val c = Pulso.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
        val shape = if (round) CircleShape else RoundedCornerShape(3.dp)
        Box(
            Modifier
                .width(if (round) 10.dp else 14.dp)
                .height(10.dp)
                .clip(shape)
                .background(fill)
                .border(if (round) 2.dp else 1.dp, border, shape)
        )
        Text(label, style = PulsoType.caption, color = c.muted)
    }
}
