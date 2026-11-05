package com.pulso.wear.presentation.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.liveRegion
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
import com.pulso.core.breathing.BreathingPlan
import com.pulso.core.breathing.BreathingSessionSnapshot
import com.pulso.core.breathing.BreathingSessionState
import com.pulso.core.breathing.TimelineSegmentType
import com.pulso.wear.data.breathing.BreathingDisplayMode
import com.pulso.wear.presentation.WearRoundButton
import com.pulso.wear.presentation.theme.WearColors
import com.pulso.wear.presentation.theme.WearIcons
import com.pulso.wear.presentation.theme.WearType
import kotlin.math.PI
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.sin

private data class WearPlan(val id: String, val pattern: String, val name: String)

private val WEAR_PLANS = listOf(
    WearPlan("Relajante 4-7-8", "4-7-8", "Relajar"),
    WearPlan("Cuadrada 4-4-4-4", "4-4-4-4", "Caja"),
    WearPlan("Calma 4-6", "4-6", "Calma")
)

@Composable
fun BreathingSessionScreen(
    snapshot: BreathingSessionSnapshot?,
    displayMode: BreathingDisplayMode,
    liveHr: Double?,
    plan: BreathingPlan?,
    hrBefore: Double?,
    defaultPlanName: String,
    defaultMinutes: Int,
    onToggleDisplayMode: () -> Unit,
    onStartSession: (plan: BreathingPlan, minutes: Int, mode: BreathingDisplayMode) -> Unit,
    onPauseResume: () -> Unit,
    onStopSession: () -> Unit,
    onSaveFeeling: (String) -> Unit,
    onFinishScreen: () -> Unit
) {
    var showFeelingPrompt by remember { mutableStateOf(false) }
    // Only ask "how do you feel" for a session that actually ran while this screen was open;
    // a finished snapshot left over from an earlier session must show the setup instead.
    var sawActiveSession by remember { mutableStateOf(false) }
    val rawState = snapshot?.state ?: BreathingSessionState.IDLE
    val active = rawState == BreathingSessionState.RUNNING || rawState == BreathingSessionState.PAUSED || rawState == BreathingSessionState.PREPARING
    val state = if (active || sawActiveSession) rawState else BreathingSessionState.IDLE

    LaunchedEffect(rawState) {
        if (active) sawActiveSession = true
        val ended = rawState == BreathingSessionState.COMPLETED || rawState == BreathingSessionState.STOPPED || rawState == BreathingSessionState.INTERRUPTED
        if (ended && sawActiveSession) showFeelingPrompt = true
    }

    Box(Modifier.fillMaxSize().background(WearColors.Bg), contentAlignment = Alignment.Center) {
        when {
            showFeelingPrompt -> FeelingScreen(
                liveHr = liveHr,
                hrBefore = hrBefore,
                completedCycles = snapshot?.currentCycle ?: 0,
                totalCycles = snapshot?.totalCycles ?: 0,
                onSelect = { onSaveFeeling(it); showFeelingPrompt = false; onFinishScreen() },
                onSkip = { showFeelingPrompt = false; onFinishScreen() }
            )
            state == BreathingSessionState.IDLE -> BreathingSetupScreen(
                defaultPlanName = defaultPlanName,
                defaultMinutes = defaultMinutes,
                displayMode = displayMode,
                onToggleDisplayMode = onToggleDisplayMode,
                onStart = { planId, minutes ->
                    val base = when (planId) {
                        "Cuadrada 4-4-4-4" -> BreathingPlan.cuadrada4444()
                        "Relajante 4-7-8" -> BreathingPlan.relajante478()
                        else -> BreathingPlan.calma46()
                    }
                    onStartSession(BreathingPlan.withTargetDuration(base, minutes), minutes, displayMode)
                }
            )
            displayMode == BreathingDisplayMode.VIBRATION_ONLY -> VibrationOnlyScreen(snapshot, onStop = onStopSession)
            else -> GuidedBreathingScreen(snapshot, plan, onPauseResume = onPauseResume, onStop = onStopSession)
        }
    }
}

// ───────────────────────── Setup ─────────────────────────

@Composable
private fun BreathingSetupScreen(
    defaultPlanName: String,
    defaultMinutes: Int,
    displayMode: BreathingDisplayMode,
    onToggleDisplayMode: () -> Unit,
    onStart: (planId: String, minutes: Int) -> Unit
) {
    var planIndex by rememberSaveable { mutableIntStateOf(WEAR_PLANS.indexOfFirst { it.id == defaultPlanName }.coerceAtLeast(0)) }
    var minutes by rememberSaveable { mutableIntStateOf(defaultMinutes.coerceIn(1, 10)) }
    val vibrationOnly = displayMode == BreathingDisplayMode.VIBRATION_ONLY
    val plan = WEAR_PLANS[planIndex]
    val listState = rememberScalingLazyListState()

    Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().background(WearColors.Bg),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item { Text("CALMA GUIADA", style = WearType.kicker, color = WearColors.Primary, modifier = Modifier.padding(top = 12.dp)) }
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    WearRoundButton(WearIcons.ChevronLeft, "Patrón anterior", { planIndex = (planIndex + WEAR_PLANS.size - 1) % WEAR_PLANS.size }, size = 36.dp)
                    Column(Modifier.width(96.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(plan.pattern, style = WearType.bigNumber.copy(fontSize = 26.sp), maxLines = 1)
                        Text(plan.name, style = WearType.body, color = WearColors.Muted)
                    }
                    WearRoundButton(WearIcons.ChevronRight, "Patrón siguiente", { planIndex = (planIndex + 1) % WEAR_PLANS.size }, size = 36.dp)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    WEAR_PLANS.indices.forEach { i ->
                        Box(Modifier.height(5.dp).width(if (i == planIndex) 14.dp else 5.dp).clip(CircleShape).background(if (i == planIndex) WearColors.Primary else WearColors.Track))
                    }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(WearColors.Surface).padding(horizontal = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    StepButton("−") { minutes = (minutes - 1).coerceAtLeast(1) }
                    Text("$minutes min", style = WearType.label.copy(fontSize = 17.sp, fontFeatureSettings = "tnum"))
                    StepButton("+") { minutes = (minutes + 1).coerceAtMost(10) }
                }
            }
            item {
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(WearColors.Surface)
                        .clickable(role = Role.Switch, onClick = onToggleDisplayMode)
                        .padding(start = 14.dp, end = 8.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Icon(WearIcons.Vibration, null, tint = WearColors.Hrv, modifier = Modifier.size(18.dp))
                    Text("Solo vibración", style = WearType.label.copy(fontSize = 13.sp), modifier = Modifier.weight(1f))
                    Box(
                        Modifier.width(36.dp).height(22.dp).clip(CircleShape).background(if (vibrationOnly) WearColors.Primary else WearColors.Track).padding(3.dp),
                        contentAlignment = if (vibrationOnly) Alignment.CenterEnd else Alignment.CenterStart
                    ) {
                        Box(Modifier.size(16.dp).clip(CircleShape).background(if (vibrationOnly) WearColors.OnPrimary else WearColors.Muted))
                    }
                }
            }
            item {
                WearRoundButton(WearIcons.Play, "Empezar", { onStart(plan.id, minutes) }, size = 52.dp, background = WearColors.Primary, tint = WearColors.OnPrimary)
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun StepButton(symbol: String, onClick: () -> Unit) {
    Box(
        Modifier.size(38.dp).clip(CircleShape).background(WearColors.Surface2).clickable(role = Role.Button, onClick = onClick)
            .semantics { contentDescription = symbol },
        contentAlignment = Alignment.Center
    ) {
        Text(symbol, style = WearType.title.copy(fontSize = 20.sp))
    }
}

// ───────────────────────── Guided (visual) ─────────────────────────

private fun segmentMs(plan: BreathingPlan?, type: TimelineSegmentType?): Long = when (type) {
    TimelineSegmentType.INHALE -> plan?.inhaleMs
    TimelineSegmentType.HOLD_AFTER_INHALE -> plan?.holdAfterInhaleMs
    TimelineSegmentType.EXHALE -> plan?.exhaleMs
    TimelineSegmentType.HOLD_AFTER_EXHALE -> plan?.holdAfterExhaleMs
    else -> null
} ?: 4000L

private fun phaseLabel(type: TimelineSegmentType?): String = when (type) {
    TimelineSegmentType.START_TAP, null -> "Prepárate"
    TimelineSegmentType.INHALE -> "Inhala"
    TimelineSegmentType.HOLD_AFTER_INHALE, TimelineSegmentType.HOLD_AFTER_EXHALE -> "Sostén"
    TimelineSegmentType.EXHALE -> "Exhala"
    TimelineSegmentType.END_DOUBLE_TAP -> "Listo"
}

private fun planPattern(plan: BreathingPlan?): String {
    if (plan == null) return ""
    val parts = listOf(plan.inhaleMs, plan.holdAfterInhaleMs, plan.exhaleMs, plan.holdAfterExhaleMs).filter { it > 0 }
    return parts.joinToString("-") { (it / 1000).toString() }
}

@Composable
private fun GuidedBreathingScreen(snapshot: BreathingSessionSnapshot?, plan: BreathingPlan?, onPauseResume: () -> Unit, onStop: () -> Unit) {
    val type = snapshot?.currentSegmentType
    val progress = snapshot?.segmentProgress ?: 0f
    val paused = snapshot?.state == BreathingSessionState.PAUSED
    val ease = (0.5 - 0.5 * cos(PI * progress)).toFloat()
    val target = when (type) {
        TimelineSegmentType.INHALE -> 0.45f + 0.55f * ease
        TimelineSegmentType.HOLD_AFTER_INHALE -> 1f
        TimelineSegmentType.EXHALE -> 1f - 0.55f * ease
        TimelineSegmentType.HOLD_AFTER_EXHALE -> 0.45f
        else -> 0.7f
    }
    val scale by animateFloatAsState(target, tween(120, easing = LinearEasing), label = "scale")
    val rot = rememberInfiniteTransition(label = "rot")
    val rotation by rot.animateFloat(0f, 360f, infiniteRepeatable(tween(60_000, easing = LinearEasing), RepeatMode.Restart), label = "r")
    val count = ceil((1f - progress) * segmentMs(plan, type) / 1000.0).toInt().coerceAtLeast(1)
    val total = snapshot?.totalDurationMs ?: 0L
    val elapsed = snapshot?.elapsedTimeMs ?: 0L
    val left = ((total - elapsed) / 1000).coerceAtLeast(0)
    val petals = listOf(WearColors.Primary, WearColors.Sky, WearColors.Hrv, WearColors.Primary, WearColors.Sky, WearColors.Hrv)

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize().padding(3.dp)) {
            val stroke = 4.dp.toPx()
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(WearColors.Surface, 0f, 360f, false, Offset(stroke / 2, stroke / 2), arc, style = Stroke(stroke))
            val frac = if (total > 0) (elapsed / total.toFloat()).coerceIn(0f, 1f) else 0f
            drawArc(WearColors.Primary, -90f, 360f * frac, false, Offset(stroke / 2, stroke / 2), arc, style = Stroke(stroke, cap = StrokeCap.Round))

            val center = Offset(size.width / 2, size.height / 2)
            val petalR = size.width * 0.19f * (0.55f + 0.45f * scale)
            val dist = size.width * 0.11f * scale
            petals.forEachIndexed { i, col ->
                val a = (i * 60 + rotation) * PI / 180
                drawCircle(col.copy(alpha = 0.34f), petalR, Offset(center.x + (cos(a) * dist).toFloat(), center.y + (sin(a) * dist).toFloat()))
            }
            drawCircle(Color.Black.copy(alpha = 0.35f), size.width * 0.14f * (0.6f + 0.4f * scale), center)
        }
        Text(
            "${planPattern(plan)} · ciclo ${snapshot?.currentCycle ?: 1} de ${snapshot?.totalCycles ?: 1}",
            style = WearType.body.copy(fontWeight = FontWeight.ExtraBold),
            color = WearColors.Muted,
            modifier = Modifier.align(Alignment.TopCenter).padding(top = 18.dp)
        )
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }) {
            Text(if (paused) "En pausa" else phaseLabel(type), style = WearType.title)
            Text("$count", style = WearType.bigNumber)
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 12.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(2.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                WearRoundButton(if (paused) WearIcons.Play else WearIcons.Pause, if (paused) "Reanudar" else "Pausar", onPauseResume, size = 36.dp)
                if (paused) WearRoundButton(WearIcons.Stop, "Terminar", onStop, size = 36.dp, tint = WearColors.Heart)
            }
            Text("%d:%02d".format(left / 60, left % 60), style = WearType.time, color = WearColors.Muted)
        }
    }
}

// ───────────────────────── Vibration only ─────────────────────────

@Composable
private fun VibrationOnlyScreen(snapshot: BreathingSessionSnapshot?, onStop: () -> Unit) {
    val type = snapshot?.currentSegmentType
    val t = rememberInfiniteTransition(label = "rings")
    val p by t.animateFloat(0f, 1f, infiniteRepeatable(tween(1700, easing = LinearEasing), RepeatMode.Restart), label = "p")
    val buzz by t.animateFloat(1f, 1.08f, infiniteRepeatable(tween(140), RepeatMode.Reverse), label = "buzz")
    val holding = type == TimelineSegmentType.HOLD_AFTER_INHALE || type == TimelineSegmentType.HOLD_AFTER_EXHALE
    val marks = listOf(TimelineSegmentType.INHALE to 3, TimelineSegmentType.HOLD_AFTER_INHALE to 4, TimelineSegmentType.EXHALE to 3)

    Box(
        Modifier.fillMaxSize().pointerInput(Unit) { detectTapGestures(onTap = { onStop() }) }
            .semantics { contentDescription = "Toca para terminar" },
        contentAlignment = Alignment.Center
    ) {
        Canvas(Modifier.size(110.dp)) {
            for (i in 0 until 3) {
                val k = (p + i / 3f) % 1f
                drawCircle(
                    WearColors.Hrv.copy(alpha = if (holding) 0.08f else 0.55f * (1 - k)),
                    radius = size.width / 2 * (0.5f + k * 1.1f),
                    style = Stroke(1.5.dp.toPx())
                )
            }
        }
        Text("SOLO VIBRACIÓN", style = WearType.kicker, color = WearColors.Hrv, modifier = Modifier.align(Alignment.TopCenter).padding(top = 22.dp))
        Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Box(
                Modifier.size(48.dp).scale(if (holding) 1f else buzz).clip(CircleShape).background(WearColors.Hrv.copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(WearIcons.VibrationStrong, null, tint = WearColors.Hrv, modifier = Modifier.size(24.dp))
            }
            Text(phaseLabel(type), style = WearType.title.copy(fontSize = 16.sp))
        }
        Column(
            Modifier.align(Alignment.BottomCenter).padding(bottom = 28.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically) {
                marks.forEach { (seg, n) ->
                    val on = seg == type || (seg == TimelineSegmentType.HOLD_AFTER_INHALE && type == TimelineSegmentType.HOLD_AFTER_EXHALE)
                    repeat(n) {
                        Box(
                            Modifier.height(4.dp).width(
                                when (seg) { TimelineSegmentType.INHALE -> 13.dp; TimelineSegmentType.EXHALE -> 9.dp; else -> 4.dp }
                            ).clip(CircleShape).background(if (on) WearColors.Hrv else WearColors.Track)
                        )
                    }
                }
            }
            Text("Cierra los ojos. Toca para terminar.", style = WearType.body, color = WearColors.Dim, textAlign = TextAlign.Center, modifier = Modifier.width(140.dp))
        }
    }
}

// ───────────────────────── How do you feel ─────────────────────────

@Composable
private fun FeelingScreen(
    liveHr: Double?,
    hrBefore: Double?,
    completedCycles: Int,
    totalCycles: Int,
    onSelect: (String) -> Unit,
    onSkip: () -> Unit
) {
    val delta = if (liveHr != null && hrBefore != null) (liveHr - hrBefore).toInt() else null
    val listState = rememberScalingLazyListState()
    Scaffold(positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().background(WearColors.Bg),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp), modifier = Modifier.padding(top = 12.dp)) {
                    Icon(WearIcons.Check, null, tint = WearColors.Primary, modifier = Modifier.size(16.dp))
                    val finished = totalCycles == 0 || completedCycles >= totalCycles
                    Text(if (finished) "Sesión completa" else "Sesión terminada", style = WearType.label.copy(fontSize = 13.sp), color = WearColors.Primary)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    if (delta != null) {
                        Chip((if (delta > 0) "+" else if (delta < 0) "−" else "") + "${kotlin.math.abs(delta)} lpm", WearColors.Heart)
                    }
                    if (totalCycles > 0) Chip("$completedCycles/$totalCycles ciclos", WearColors.Hrv)
                }
            }
            item { Text("¿Cómo te sientes?", style = WearType.title, textAlign = TextAlign.Center) }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    Face("Mejor", WearColors.Primary, Mouth.SMILE) { onSelect("BETTER") }
                    Face("Igual", WearColors.Hrv, Mouth.FLAT) { onSelect("SAME") }
                    Face("Peor", WearColors.Warn, Mouth.FROWN) { onSelect("WORSE") }
                }
            }
            item {
                Text(
                    "Omitir", style = WearType.body.copy(fontWeight = FontWeight.ExtraBold), color = WearColors.Muted,
                    modifier = Modifier.clip(CircleShape).clickable(onClick = onSkip).padding(horizontal = 14.dp, vertical = 10.dp)
                )
            }
        }
    }
}

@Composable
private fun Chip(text: String, color: Color) {
    Text(
        text,
        style = WearType.body.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
        color = color,
        modifier = Modifier.clip(CircleShape).background(color.copy(alpha = 0.16f)).padding(horizontal = 8.dp, vertical = 4.dp)
    )
}

private enum class Mouth { SMILE, FLAT, FROWN }

@Composable
private fun Face(label: String, color: Color, mouth: Mouth, onClick: () -> Unit) {
    Column(
        Modifier.clip(RoundedCornerShape(16.dp)).clickable(role = Role.Button, onClick = onClick).padding(2.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Box(
            Modifier.size(44.dp).clip(CircleShape).background(color.copy(alpha = 0.16f)).border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Canvas(Modifier.size(26.dp)) {
                val s = size.width / 24f
                val st = Stroke(2 * s, cap = StrokeCap.Round, join = StrokeJoin.Round)
                drawCircle(color, 9.5f * s, style = st)
                drawCircle(color, 1.2f * s, Offset(8.6f * s, 9.6f * s))
                drawCircle(color, 1.2f * s, Offset(15.4f * s, 9.6f * s))
                val p = Path()
                when (mouth) {
                    Mouth.SMILE -> { p.moveTo(8 * s, 14.5f * s); p.cubicTo(9 * s, 16 * s, 10.4f * s, 16.7f * s, 12 * s, 16.7f * s); p.cubicTo(13.6f * s, 16.7f * s, 15 * s, 16 * s, 16 * s, 14.5f * s) }
                    Mouth.FLAT -> { p.moveTo(8.5f * s, 15.5f * s); p.lineTo(15.5f * s, 15.5f * s) }
                    Mouth.FROWN -> { p.moveTo(8 * s, 16.5f * s); p.cubicTo(9 * s, 15 * s, 10.4f * s, 14.3f * s, 12 * s, 14.3f * s); p.cubicTo(13.6f * s, 14.3f * s, 15 * s, 15 * s, 16 * s, 16.5f * s) }
                }
                drawPath(p, color, style = st)
            }
        }
        Text(label, style = WearType.body.copy(fontWeight = FontWeight.ExtraBold))
    }
}
