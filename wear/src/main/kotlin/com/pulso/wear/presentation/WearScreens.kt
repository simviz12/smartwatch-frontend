package com.pulso.wear.presentation

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.LocalIndication
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
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
import androidx.compose.runtime.mutableStateOf
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
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
import kotlin.math.cos
import kotlin.math.sin

fun planShortLabel(planName: String): String = when (planName) {
    "Cuadrada 4-4-4-4" -> "4-4-4-4"
    "Relajante 4-7-8" -> "4-7-8"
    else -> "4-6"
}

/** A heart that beats at the measured rate (72 bpm when unknown). */
@Composable
private fun beatScale(bpm: Double?, amplitude: Float = 0.16f): Float {
    val period = (60_000f / (bpm?.toFloat() ?: 72f)).toInt().coerceIn(300, 2000)
    val t = rememberInfiniteTransition(label = "beat")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(period, easing = LinearEasing)), label = "beatP")
    return 1f + amplitude * (1f - (p * 5f).coerceAtMost(1f))
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
    val bpm = uiState.liveHeartRate.bpm
    val snooze = uiState.config?.stressConfig?.snoozeUntil?.takeIf { it > System.currentTimeMillis() }
    val timeFmt = remember { SimpleDateFormat("H:mm", Locale("es", "CO")) }

    Scaffold(
        timeText = { TimeText() },
        vignette = { Vignette(vignettePosition = VignettePosition.TopAndBottom) },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().background(WearColors.Bg),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            item { Kicker("Pulso", Modifier.padding(top = 14.dp).wearReveal(0)) }

            item {
                if (consent) {
                    LiveHeroCard(bpm = bpm, onClick = onOpenLivePulse, modifier = Modifier.wearReveal(1))
                } else {
                    Column(
                        Modifier.wearReveal(1).fillMaxWidth().tintedGlass(WearColors.Warn, RoundedCornerShape(26.dp)).padding(14.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(3.dp)
                    ) {
                        Text("Vinculación pendiente", style = WearType.label, color = WearColors.Warn)
                        Text("Acepta el consentimiento en tu celular", style = WearType.body, color = WearColors.Muted, textAlign = TextAlign.Center)
                    }
                }
            }

            item {
                BreathingHeroButton(
                    subtitle = "${planShortLabel(breathing.preferredPlanName)} · ${breathing.durationMinutes} min",
                    onClick = onOpenBreathing,
                    modifier = Modifier.wearReveal(2)
                )
            }
            item {
                ActionCard("Hablar con IA", WearIcons.Mic, WearColors.Hrv, onTalk, Modifier.wearReveal(3), subtitle = "Te escucho")
            }
            item {
                ActionCard(
                    "Pausar avisos", WearIcons.BellOff, WearColors.Warn, onOpenPause, Modifier.wearReveal(4),
                    subtitle = snooze?.let { "Hasta las ${timeFmt.format(Date(it))}" } ?: "Avisos activos"
                )
            }
            item {
                ActionCard(
                    "Sensores", WearIcons.Watch, WearColors.Calm, onOpenSensors, Modifier.wearReveal(5),
                    subtitle = "${uiState.totalWindowsLogged} mediciones"
                )
            }
            uiState.statusMessage?.let { msg ->
                item {
                    Text(msg, style = WearType.body, color = WearColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 12.dp))
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
        }
    }
}

/** Status card: beating heart, live bpm and a "live" badge on tinted glass. */
@Composable
private fun LiveHeroCard(bpm: Double?, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val shown by animateIntAsState(bpm?.toInt() ?: 0, tween(600), label = "bpm")
    val heart = beatScale(bpm)
    Row(
        modifier
            .wearPress(interaction, 0.96f)
            .fillMaxWidth()
            .tintedGlass(WearColors.Heart, RoundedCornerShape(28.dp))
            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(38.dp).scale(if (bpm != null) heart else 1f).clip(CircleShape).background(WearColors.Heart.copy(alpha = 0.16f)))
            Icon(WearIcons.HeartFilled, null, tint = WearColors.Heart, modifier = Modifier.size(18.dp).scale(if (bpm != null) heart else 1f))
        }
        Column(Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                Text(if (bpm != null) "$shown" else "--", style = WearType.bigNumber.copy(fontSize = 28.sp, lineHeight = 30.sp))
                Text("lpm", style = WearType.body, color = WearColors.Muted, modifier = Modifier.padding(bottom = 4.dp))
            }
            Text(if (bpm != null) "Pulso en vivo" else "Buscando pulso…", style = WearType.body, color = WearColors.Muted)
        }
        Icon(WearIcons.ChevronRight, null, tint = WearColors.Dim, modifier = Modifier.size(14.dp))
    }
}

/** The main action: a white capsule card with a dark play disc. */
@Composable
private fun BreathingHeroButton(subtitle: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val interaction = remember { MutableInteractionSource() }
    val t = rememberInfiniteTransition(label = "calm")
    val breath by t.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(2600), RepeatMode.Reverse), label = "breath")
    Row(
        modifier
            .wearPress(interaction, 0.95f)
            .fillMaxWidth()
            .heightIn(min = 62.dp)
            .clip(RoundedCornerShape(31.dp))
            .background(WearColors.Primary)
            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.Button, onClick = onClick)
            .padding(start = 16.dp, end = 8.dp, top = 8.dp, bottom = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Column(Modifier.weight(1f)) {
            Text("Calma guiada", style = WearType.title.copy(fontSize = 15.sp), color = WearColors.OnPrimary)
            Text(subtitle, style = WearType.body, color = WearColors.OnPrimary.copy(alpha = 0.6f))
        }
        Box(Modifier.size(42.dp), contentAlignment = Alignment.Center) {
            Box(Modifier.size(42.dp).scale(breath).clip(CircleShape).background(WearColors.OnPrimary.copy(alpha = 0.08f)))
            Box(Modifier.size(34.dp).clip(CircleShape).background(WearColors.OnPrimary), contentAlignment = Alignment.Center) {
                Icon(WearIcons.Wind, null, tint = WearColors.Primary, modifier = Modifier.size(18.dp))
            }
        }
    }
}

// ───────────────────────── Live pulse ─────────────────────────

@Composable
fun LivePulseScreen(uiState: MainUiState) {
    val hr = uiState.liveHeartRate
    val bpm = hr.bpm
    val shown by animateIntAsState(bpm?.toInt() ?: 0, tween(700), label = "bpm")
    val heart = beatScale(bpm, 0.22f)
    val t = rememberInfiniteTransition(label = "ecg")
    val scroll by t.animateFloat(0f, 1f, infiniteRepeatable(tween((60_000f / (bpm?.toFloat() ?: 72f)).toInt().coerceIn(300, 2000), easing = LinearEasing)), label = "scroll")
    val fraction by animateFloatAsState(bpm?.let { ((it - 40) / 100.0).toFloat().coerceIn(0f, 1f) } ?: 0f, tween(800), label = "marker")

    Box(
        Modifier.fillMaxSize().background(WearColors.Bg)
            .semantics { contentDescription = bpm?.let { "${it.toInt()} latidos por minuto" } ?: "Sin lectura de pulso" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.fillMaxSize()) {
            // Soft glow that swells with each beat.
            drawCircle(
                Brush.radialGradient(
                    listOf(WearColors.Heart.copy(alpha = if (bpm != null) 0.22f * heart else 0.08f), Color.Transparent),
                    center = center, radius = size.minDimension * 0.42f
                ),
                radius = size.minDimension * 0.42f
            )
            val stroke = 5.dp.toPx()
            val inset = 8.dp.toPx()
            val arc = Size(size.width - inset * 2, size.height - inset * 2)
            val topLeft = Offset(inset, inset)
            drawArc(WearColors.Track, 135f, 270f, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            // Typical resting band 60–90 bpm on the 40–140 dial.
            drawArc(WearColors.Calm.copy(alpha = 0.55f), 135f + 270f * 0.2f, 270f * 0.3f, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round))
            if (bpm != null) {
                drawArc(
                    Brush.sweepGradient(listOf(WearColors.Heart.copy(alpha = 0.1f), WearColors.Heart), center),
                    135f, 270f * fraction, false, topLeft, arc, style = Stroke(stroke, cap = StrokeCap.Round)
                )
                val angle = Math.toRadians((135f + 270f * fraction).toDouble())
                val r = arc.width / 2
                val marker = Offset(center.x + (r * cos(angle)).toFloat(), center.y + (r * sin(angle)).toFloat())
                drawCircle(WearColors.Heart.copy(alpha = 0.35f), 9.dp.toPx(), marker)
                drawCircle(WearColors.Text, 4.5.dp.toPx(), marker)
            }
            // ECG trace with a fading tail.
            val w = size.width * 0.5f
            val x0 = (size.width - w) / 2
            val baseY = size.height * 0.83f
            val amp = 13.dp.toPx()
            val seg = 30.dp.toPx()
            val path = Path()
            var x = 0f
            while (x <= w) {
                val p = ((x + scroll * seg) % seg) / seg
                val shape = when {
                    p < 0.12f -> p / 0.12f
                    p < 0.3f -> 1 - (p - 0.12f) / 0.18f * 0.75f
                    p < 0.36f -> 0.25f + (p - 0.3f) / 0.06f * 0.15f
                    else -> 0.4f * (1 - (p - 0.36f) / 0.64f)
                }
                if (x == 0f) path.moveTo(x0 + x, baseY - shape * amp) else path.lineTo(x0 + x, baseY - shape * amp)
                x += 2.dp.toPx()
            }
            if (bpm != null) {
                drawPath(
                    path,
                    Brush.horizontalGradient(listOf(Color.Transparent, WearColors.Heart, WearColors.Heart.copy(alpha = 0.6f), Color.Transparent), startX = x0, endX = x0 + w),
                    style = Stroke(2.2.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round)
                )
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.padding(bottom = 20.dp)) {
            Icon(WearIcons.HeartFilled, null, tint = WearColors.Heart, modifier = Modifier.size(20.dp).scale(if (bpm != null) heart else 1f))
            Text(if (bpm != null) "$shown" else "--", style = WearType.hero)
            Text("LPM", style = WearType.kicker.copy(letterSpacing = 2.sp), color = WearColors.Muted)
            Spacer(Modifier.height(8.dp))
            WearBadge(
                when {
                    !hr.onBody -> "Fuera de la muñeca"
                    bpm == null -> "Buscando pulso…"
                    bpm in 60.0..90.0 -> "En rango de reposo"
                    else -> "Fuera del reposo"
                },
                when {
                    bpm == null || !hr.onBody -> WearColors.Warn
                    bpm in 60.0..90.0 -> WearColors.Calm
                    else -> WearColors.Heart
                }
            )
        }
        Text("40", style = WearType.body, color = WearColors.Dim, modifier = Modifier.align(Alignment.BottomStart).padding(start = 42.dp, bottom = 28.dp))
        Text("140", style = WearType.body, color = WearColors.Dim, modifier = Modifier.align(Alignment.BottomEnd).padding(end = 36.dp, bottom = 28.dp))
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
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            item {
                Box(Modifier.padding(top = 14.dp).size(40.dp).wearReveal(0).clip(CircleShape).background(WearColors.Warn.copy(alpha = 0.16f)), contentAlignment = Alignment.Center) {
                    Icon(WearIcons.BellOff, null, tint = WearColors.Warn, modifier = Modifier.size(20.dp))
                }
            }
            item { Text("Pausar avisos", style = WearType.title, modifier = Modifier.wearReveal(0)) }
            item {
                Text(
                    active?.let { "Pausados hasta las ${fmt.format(Date(it))}" } ?: "Los avisos están activos",
                    style = WearType.body, color = WearColors.Muted, modifier = Modifier.wearReveal(1)
                )
            }
            options.forEachIndexed { i, (label, mins) ->
                item {
                    val on = i == selected
                    val interaction = remember { MutableInteractionSource() }
                    val shape = RoundedCornerShape(26.dp)
                    Row(
                        Modifier.wearReveal(2 + i).wearPress(interaction).fillMaxWidth().height(52.dp)
                            .then(if (on) Modifier.clip(shape).background(WearColors.Warn) else Modifier.glass(shape))
                            .clickable(interactionSource = interaction, indication = LocalIndication.current, role = Role.RadioButton) {
                                selected = i
                                onPause(if (mins > 0) mins else minutesUntilTomorrowMorning())
                            }
                            .padding(start = 18.dp, end = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(label, style = WearType.label.copy(fontSize = 15.sp), color = if (on) WearColors.OnWarn else WearColors.Text, modifier = Modifier.weight(1f))
                        Box(
                            Modifier.size(26.dp).clip(CircleShape).background(if (on) WearColors.OnWarn else WearColors.Surface2),
                            contentAlignment = Alignment.Center
                        ) {
                            if (on) Icon(WearIcons.Check, null, tint = WearColors.Warn, modifier = Modifier.size(14.dp))
                        }
                    }
                }
            }
            item { Spacer(Modifier.height(18.dp)) }
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
    val ring by animateFloatAsState((battery ?: 0) / 100f, tween(1200), label = "battery")
    val hr = uiState.liveHeartRate
    var demo by remember { mutableStateOf(com.pulso.wear.data.glance.GlanceStore.isDemo(context)) }

    Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        Box(Modifier.fillMaxSize().background(WearColors.Bg)) {
            Canvas(Modifier.fillMaxSize().padding(5.dp)) {
                val stroke = 3.dp.toPx()
                val arc = Size(size.width - stroke, size.height - stroke)
                drawArc(WearColors.Track, 0f, 360f, false, Offset(stroke / 2, stroke / 2), arc, style = Stroke(stroke))
                if (battery != null) {
                    drawArc(WearColors.Text, -90f, 360f * ring, false, Offset(stroke / 2, stroke / 2), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                }
            }
            ScalingLazyColumn(
                modifier = Modifier.fillMaxSize(),
                state = listState,
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                item { Kicker("Sensores", Modifier.padding(top = 16.dp).wearReveal(0)) }
                item {
                    Column(Modifier.wearReveal(1), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatCell("Pulso", if (hr.bpm != null) "Activo" else "—", if (hr.bpm != null) WearColors.Calm else WearColors.Warn, Modifier.weight(1f))
                            StatCell("Batería", battery?.let { "$it %" } ?: "—", WearColors.Calm, Modifier.weight(1f))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            StatCell("Hoy", "${uiState.totalWindowsLogged}", WearColors.Hrv, Modifier.weight(1f))
                            StatCell("Celular", if (uiState.connectedNode != null) "Sí" else "No", if (uiState.connectedNode != null) WearColors.Calm else WearColors.Warn, Modifier.weight(1f))
                        }
                    }
                }
                item {
                    Row(Modifier.wearReveal(2), verticalAlignment = Alignment.CenterVertically) {
                        val interaction = remember { MutableInteractionSource() }
                        Row(
                            Modifier.wearPress(interaction).height(48.dp).clip(CircleShape).background(WearColors.Primary)
                                .clickable(interactionSource = interaction, indication = LocalIndication.current, enabled = !uiState.isSyncing, role = Role.Button, onClick = onSync)
                                .padding(horizontal = 18.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            Icon(WearIcons.Sync, null, tint = WearColors.OnPrimary, modifier = Modifier.size(18.dp).rotate(if (uiState.isSyncing) angle else 0f))
                            Text(if (uiState.isSyncing) "Enviando…" else "Sincronizar", style = WearType.label, color = WearColors.OnPrimary)
                        }
                    }
                }
                item {
                    ActionCard(
                        "Modo demo", WearIcons.Spark, WearColors.Hrv,
                        onClick = {
                            demo = !demo
                            com.pulso.wear.data.glance.GlanceStore.setDemo(context, demo)
                        },
                        modifier = Modifier.wearReveal(3),
                        subtitle = "Esfera y tile de ejemplo"
                    ) {
                        val knob by animateFloatAsState(if (demo) 1f else 0f, tween(220), label = "knob")
                        Box(
                            Modifier.width(38.dp).height(22.dp).clip(CircleShape).background(if (demo) WearColors.Calm else WearColors.Track).padding(3.dp)
                        ) {
                            Box(
                                Modifier.padding(start = (16 * knob).dp).size(16.dp).clip(CircleShape)
                                    .background(if (demo) Color(0xFF06201A) else WearColors.Muted)
                            )
                        }
                    }
                }
                item {
                    Text(
                        "Guardar informe",
                        style = WearType.body.copy(fontWeight = FontWeight.ExtraBold),
                        color = WearColors.Muted,
                        modifier = Modifier.wearReveal(4).clip(CircleShape).clickable(onClick = onSaveReport).padding(horizontal = 14.dp, vertical = 10.dp)
                    )
                }
                uiState.statusMessage?.let { item { Text(it, style = WearType.body, color = WearColors.Muted, textAlign = TextAlign.Center) } }
                item { Spacer(Modifier.height(18.dp)) }
            }
        }
    }
}

@Composable
private fun StatCell(label: String, value: String, dot: Color, modifier: Modifier) {
    Column(
        modifier.glass(RoundedCornerShape(20.dp)).padding(horizontal = 11.dp, vertical = 8.dp),
        verticalArrangement = Arrangement.spacedBy(1.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            Box(Modifier.size(6.dp).clip(CircleShape).background(dot))
            Text(label, style = WearType.body, color = WearColors.Muted)
        }
        Text(value, style = WearType.label.copy(fontSize = 15.sp, fontFeatureSettings = "tnum"))
    }
}
