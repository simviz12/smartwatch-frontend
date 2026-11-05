package com.pulso.wear.presentation

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.ui.composed
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import androidx.wear.compose.material.Vignette
import androidx.wear.compose.material.VignettePosition
import com.pulso.core.breathing.BreathingConfig
import com.pulso.wear.presentation.theme.WearColors
import com.pulso.wear.presentation.theme.WearIcons
import com.pulso.wear.presentation.theme.WearType
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

// ───────────────────────── Shared pieces ─────────────────────────

/** Press feedback for wrist targets: the element sinks slightly while touched. */
fun Modifier.wearPress(interaction: androidx.compose.foundation.interaction.MutableInteractionSource): Modifier = composed {
    val pressed by interaction.collectIsPressedAsState()
    val scale by animateFloatAsState(if (pressed) 0.94f else 1f, spring(dampingRatio = Spring.DampingRatioMediumBouncy), label = "press")
    this.scale(scale)
}

@Composable
fun WearPill(
    modifier: Modifier = Modifier,
    background: Color = WearColors.Surface,
    height: Dp = 52.dp,
    onClick: () -> Unit,
    content: @Composable RowScope.() -> Unit
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Row(
        modifier = modifier
            .wearPress(interaction)
            .fillMaxWidth()
            .heightIn(min = height)
            .clip(RoundedCornerShape(height / 2))
            .background(background)
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) { content() }
}

@Composable
fun WearRoundButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit,
    size: Dp = 40.dp,
    background: Color = WearColors.Surface,
    tint: Color = WearColors.Text
) {
    val interaction = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    Box(
        Modifier
            .wearPress(interaction)
            .size(size)
            .clip(CircleShape)
            .background(background)
            .clickable(interactionSource = interaction, indication = androidx.compose.foundation.LocalIndication.current, role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(size * 0.46f))
    }
}

@Composable
fun WearBadge(text: String, color: Color) {
    Text(
        text,
        style = WearType.body.copy(fontSize = 12.sp, fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
        color = color,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

fun planShortLabel(planName: String): String = when (planName) {
    "Cuadrada 4-4-4-4" -> "4-4-4-4"
    "Relajante 4-7-8" -> "4-7-8"
    else -> "4-6"
}

// ───────────────────────── Home ─────────────────────────

@Composable
fun HomeScreen(
    uiState: MainUiState,
    onOpenBreathing: () -> Unit,
    onOpenLivePulse: () -> Unit,
    onTalk: () -> Unit,
    onOpenPause: () -> Unit,
    onOpenSensors: () -> Unit
) {
    val listState = rememberScalingLazyListState()
    val consent = uiState.config?.collectionEnabled == true
    val breathing = uiState.config?.breathingConfig ?: BreathingConfig()
    val bpm = uiState.liveHeartRate.bpm?.toInt()

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().background(WearColors.Bg),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Text("PULSO", style = WearType.kicker, color = WearColors.Primary, modifier = Modifier.padding(top = 12.dp))
            }
            item {
                Row(
                    Modifier.fillMaxWidth().clip(RoundedCornerShape(30.dp)).background(WearColors.Primary)
                        .clickable(role = Role.Button, onClick = onOpenBreathing)
                        .padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    val calm = rememberInfiniteTransition(label = "calm")
                    val breath by calm.animateFloat(0.9f, 1.08f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "breath")
                    Box(Modifier.size(34.dp).scale(breath).clip(CircleShape).background(WearColors.OnPrimary.copy(alpha = 0.14f)), contentAlignment = Alignment.Center) {
                        Icon(WearIcons.Wind, null, tint = WearColors.OnPrimary, modifier = Modifier.size(20.dp))
                    }
                    Column {
                        Text("Calma guiada", style = WearType.title.copy(fontSize = 15.sp), color = WearColors.OnPrimary)
                        Text(
                            "${planShortLabel(breathing.preferredPlanName)} · ${breathing.durationMinutes} min",
                            style = WearType.body, color = WearColors.OnPrimary.copy(alpha = 0.8f)
                        )
                    }
                }
            }
            item {
                if (consent) {
                    WearPill(onClick = onOpenLivePulse) {
                        Icon(WearIcons.HeartFilled, null, tint = WearColors.Heart, modifier = Modifier.size(20.dp))
                        Column(Modifier.weight(1f)) {
                            Text(if (bpm != null) "$bpm lpm" else "-- lpm", style = WearType.label.copy(fontFeatureSettings = "tnum"))
                            Text("Pulso en vivo", style = WearType.body, color = WearColors.Muted)
                        }
                        Box(Modifier.size(8.dp).clip(CircleShape).background(if (bpm != null) WearColors.Heart else WearColors.Track))
                    }
                } else {
                    Column(
                        Modifier.fillMaxWidth().clip(RoundedCornerShape(24.dp)).background(WearColors.Warn.copy(alpha = 0.12f)).padding(12.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(2.dp)
                    ) {
                        Text("Vinculación pendiente", style = WearType.label.copy(fontSize = 13.sp), color = WearColors.Warn)
                        Text("Acepta el consentimiento en tu celular", style = WearType.body, color = WearColors.Text, textAlign = TextAlign.Center)
                    }
                }
            }
            item {
                WearPill(onClick = onTalk) {
                    Icon(WearIcons.Mic, null, tint = WearColors.Hrv, modifier = Modifier.size(20.dp))
                    Text("Hablar con IA", style = WearType.label)
                }
            }
            item {
                WearPill(onClick = onOpenPause) {
                    Icon(WearIcons.BellOff, null, tint = WearColors.Warn, modifier = Modifier.size(20.dp))
                    Text("Pausar avisos", style = WearType.label)
                }
            }
            item {
                WearPill(onClick = onOpenSensors) {
                    Icon(WearIcons.Watch, null, tint = WearColors.Primary, modifier = Modifier.size(20.dp))
                    Column(Modifier.weight(1f)) {
                        Text("Sensores", style = WearType.label)
                        Text("${uiState.totalWindowsLogged} mediciones", style = WearType.body, color = WearColors.Muted)
                    }
                }
            }
            uiState.statusMessage?.let { msg ->
                item {
                    Text(msg, style = WearType.body, color = WearColors.Primary, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ───────────────────────── Live pulse ─────────────────────────

@Composable
fun LivePulseScreen(uiState: MainUiState) {
    val hr = uiState.liveHeartRate
    val bpm = hr.bpm
    val period = (60_000f / (bpm?.toFloat() ?: 72f)).toInt().coerceIn(300, 2000)
    val t = rememberInfiniteTransition(label = "beat")
    val beat by t.animateFloat(0f, 1f, infiniteRepeatable(tween(period, easing = LinearEasing), RepeatMode.Restart), label = "beat")
    val scroll by t.animateFloat(0f, 1f, infiniteRepeatable(tween(period, easing = LinearEasing), RepeatMode.Restart), label = "wave")
    val heartScale = 1f + 0.18f * (1f - (beat * 6f).coerceAtMost(1f))
    val fraction = bpm?.let { ((it - 40) / 100.0).toFloat().coerceIn(0f, 1f) }

    Box(
        Modifier.fillMaxSize().background(WearColors.Bg)
            .semantics { contentDescription = bpm?.let { "${it.toInt()} latidos por minuto" } ?: "Sin lectura de pulso" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize().padding(6.dp)) {
            val stroke = 6.dp.toPx()
            val inset = stroke / 2
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(WearColors.Surface, 135f, 270f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            // Typical resting band 60–90 bpm, shown as a soft guide on the 40–140 dial.
            drawArc(WearColors.Primary.copy(alpha = 0.45f), 135f + 270f * 0.2f, 270f * 0.3f, false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
            if (fraction != null) {
                val angle = Math.toRadians((135f + 270f * fraction).toDouble())
                val r = (size.width - stroke) / 2
                val center = Offset(size.width / 2 + (r * kotlin.math.cos(angle)).toFloat(), size.height / 2 + (r * kotlin.math.sin(angle)).toFloat())
                drawCircle(Color.Black, 8.dp.toPx(), center)
                drawCircle(WearColors.Heart, 5.5.dp.toPx(), center)
            }
            // Heartbeat trace along the bottom
            val w = size.width * 0.45f
            val x0 = (size.width - w) / 2
            val baseY = size.height * 0.84f
            val amp = 14.dp.toPx()
            val segment = 32.dp.toPx()
            val path = androidx.compose.ui.graphics.Path()
            var x = 0f
            while (x <= w) {
                val p = (((x + scroll * segment) % segment) / segment)
                val shape = when {
                    p < 0.12f -> p / 0.12f
                    p < 0.3f -> 1 - (p - 0.12f) / 0.18f * 0.75f
                    p < 0.36f -> 0.25f + (p - 0.3f) / 0.06f * 0.15f
                    else -> 0.4f * (1 - (p - 0.36f) / 0.64f)
                }
                val px = x0 + x
                val py = baseY - shape * amp
                if (x == 0f) path.moveTo(px, py) else path.lineTo(px, py)
                x += 2.dp.toPx()
            }
            if (bpm != null) drawPath(path, WearColors.Heart, style = Stroke(2.dp.toPx(), cap = StrokeCap.Round))
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 18.dp)) {
            Icon(WearIcons.HeartFilled, null, tint = WearColors.Heart, modifier = Modifier.size(22.dp).scale(if (bpm != null) heartScale else 1f))
            Text(bpm?.toInt()?.toString() ?: "--", style = WearType.hero)
            Text("LPM", style = WearType.kicker.copy(letterSpacing = 1.6.sp), color = WearColors.Muted)
            Spacer(Modifier.height(6.dp))
            WearBadge(
                when {
                    !hr.onBody -> "Fuera de la muñeca"
                    bpm == null -> "Buscando pulso…"
                    else -> "Sensor ${hr.availability.lowercase()}"
                },
                if (bpm != null && hr.onBody) WearColors.Primary else WearColors.Warn
            )
        }
        Text("40", style = WearType.body, color = WearColors.Muted, modifier = Modifier.align(Alignment.BottomStart).padding(start = 40.dp, bottom = 26.dp))
        Text("140", style = WearType.body, color = WearColors.Muted, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 34.dp, bottom = 26.dp))
    }
}

// ───────────────────────── Pause alerts ─────────────────────────

private fun minutesUntilTomorrowMorning(): Long {
    val now = Calendar.getInstance()
    val target = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 7); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        if (!after(now)) add(Calendar.DAY_OF_YEAR, 1)
    }
    return ((target.timeInMillis - now.timeInMillis) / 60_000L).coerceAtLeast(1)
}

@Composable
fun PauseAlertsScreen(snoozeUntil: Long?, onPause: (minutes: Long) -> Unit) {
    var selected by rememberSaveable { mutableIntStateOf(-1) }
    val options = listOf("30 min" to 30L, "1 hora" to 60L, "Hasta mañana" to -1L)
    val active = snoozeUntil?.takeIf { it > System.currentTimeMillis() }
    val fmt = remember { SimpleDateFormat("h:mm a", Locale("es", "CO")) }
    val listState = rememberScalingLazyListState()

    Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().background(WearColors.Bg),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(top = 12.dp)) {
                    Icon(WearIcons.BellOff, null, tint = WearColors.Warn, modifier = Modifier.size(20.dp))
                    Text("Pausar avisos", style = WearType.title)
                }
            }
            item {
                Text(
                    active?.let { "Pausados hasta ${fmt.format(Date(it))}" } ?: "Avisos activos",
                    style = WearType.body, color = WearColors.Muted
                )
            }
            options.forEachIndexed { i, (label, mins) ->
                item {
                    val on = i == selected
                    Row(
                        Modifier.fillMaxWidth().height(52.dp).clip(CircleShape)
                            .background(if (on) WearColors.Warn else WearColors.Surface)
                            .clickable(role = Role.RadioButton) {
                                selected = i
                                onPause(if (mins > 0) mins else minutesUntilTomorrowMorning())
                            }
                            .padding(start = 18.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = WearType.label.copy(fontSize = 15.sp), color = if (on) WearColors.OnWarn else WearColors.Text, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(24.dp).clip(CircleShape)
                                .background(if (on) WearColors.OnWarn else Color.Transparent)
                                .border(2.5.dp, if (on) WearColors.OnWarn else WearColors.Ring, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (on) Icon(WearIcons.Check, null, tint = WearColors.Warn, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(16.dp)) }
        }
    }
}

// ───────────────────────── Sensors report ─────────────────────────

private fun batteryPercent(context: Context): Int? = runCatching {
    val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED)) ?: return null
    val level = intent.getIntExtra(BatteryManager.EXTRA_LEVEL, -1)
    val scale = intent.getIntExtra(BatteryManager.EXTRA_SCALE, -1)
    if (level < 0 || scale <= 0) null else level * 100 / scale
}.getOrNull()

@Composable
fun SensorsScreen(uiState: MainUiState, onSync: () -> Unit, onSaveReport: () -> Unit) {
    val context = LocalContext.current
    val battery = remember { batteryPercent(context) }
    val listState = rememberScalingLazyListState()
    val t = rememberInfiniteTransition(label = "spin")
    val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing)), label = "angle")
    val hr = uiState.liveHeartRate

    Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        Box(Modifier.fillMaxSize().background(WearColors.Bg)) {
            Canvas(Modifier.fillMaxSize().padding(4.dp)) {
                val stroke = 4.dp.toPx()
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(WearColors.Surface, 0f, 360f, false, Offset(stroke / 2, stroke / 2), arc, style = Stroke(stroke))
                battery?.let {
                    drawArc(WearColors.Primary, -90f, 360f * it / 100f, false, Offset(stroke / 2, stroke / 2), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                item { Text("SENSORES", style = WearType.kicker, color = WearColors.Primary, modifier = Modifier.padding(top = 14.dp)) }
                item {
                    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatCell("Pulso", if (hr.bpm != null) "Activo" else "—", if (hr.bpm != null) WearColors.Primary else WearColors.Warn, Modifier.weight(1f))
                            StatCell("Batería", battery?.let { "$it %" } ?: "—", WearColors.Primary, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatCell("Hoy", "${uiState.totalWindowsLogged}", WearColors.Hrv, Modifier.weight(1f))
                            StatCell("Celular", if (uiState.connectedNode != null) "Sí" else "No", if (uiState.connectedNode != null) WearColors.Primary else WearColors.Warn, Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Row(
                        Modifier.height(48.dp).clip(CircleShape).background(WearColors.Primary)
                            .clickable(enabled = !uiState.isSyncing, role = Role.Button, onClick = onSync)
                            .padding(horizontal = 18.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Icon(WearIcons.Sync, null, tint = WearColors.OnPrimary, modifier = Modifier.size(18.dp).rotate(if (uiState.isSyncing) angle else 0f))
                        Text(if (uiState.isSyncing) "Enviando…" else "Sincronizar", style = WearType.label, color = WearColors.OnPrimary)
                    }
                }
                item {
                    var demo by remember { androidx.compose.runtime.mutableStateOf(com.pulso.wear.data.glance.GlanceStore.isDemo(context)) }
                    Row(
                        Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(WearColors.Surface)
                            .clickable(role = Role.Switch) {
                                demo = !demo
                                com.pulso.wear.data.glance.GlanceStore.setDemo(context, demo)
                            }
                            .padding(start = 14.dp, end = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text("Modo demo", style = WearType.label.copy(fontSize = 13.sp))
                            Text("Esfera y tile de ejemplo", style = WearType.body.copy(fontSize = 11.sp), color = WearColors.Muted)
                        }
                        Box(
                            Modifier.width(36.dp).height(22.dp).clip(CircleShape).background(if (demo) WearColors.Primary else WearColors.Track).padding(3.dp),
                            contentAlignment = if (demo) Alignment.CenterEnd else Alignment.CenterStart
                        ) {
                            Box(Modifier.size(16.dp).clip(CircleShape).background(if (demo) WearColors.OnPrimary else WearColors.Muted))
                        }
                    }
                }
                item {
                    Text(
                        "Guardar informe",
                        style = WearType.body.copy(fontWeight = androidx.compose.ui.text.font.FontWeight.ExtraBold),
                        color = WearColors.Muted,
                        modifier = Modifier.clip(CircleShape).clickable(onClick = onSaveReport).padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
                uiState.statusMessage?.let { item { Text(it, style = WearType.body, color = WearColors.Primary, textAlign = TextAlign.Center) } }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, dot: Color, modifier: Modifier) {
    Column(
        modifier.clip(RoundedCornerShape(18.dp)).background(WearColors.Surface).padding(horizontal = 10.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
            Text(label, style = WearType.body, color = WearColors.Muted)
        }
        Text(value, style = WearType.label.copy(fontSize = 15.sp, fontFeatureSettings = "tnum"))
    }
}

