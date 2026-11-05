package com.pulso.mobile.presentation.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.withFrameMillis
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulso.core.breathing.BreathingConfig
import com.pulso.core.breathing.BreathingPlan
import com.pulso.mobile.data.local.PhoneBreathingSessionEntity
import com.pulso.mobile.domain.breathing.BreathingWeeklySummary
import com.pulso.mobile.presentation.components.reveal
import com.pulso.mobile.presentation.components.FeedbackText
import com.pulso.mobile.presentation.components.IconCircleButton
import com.pulso.mobile.presentation.components.PulsoButton
import com.pulso.mobile.presentation.components.PulsoButtonStyle
import com.pulso.mobile.presentation.components.PulsoCard
import com.pulso.mobile.presentation.components.PulsoChip
import com.pulso.mobile.presentation.components.ScreenHeader
import com.pulso.mobile.presentation.components.SegmentedControl
import com.pulso.mobile.presentation.i18n.LocalAppLanguage
import com.pulso.mobile.presentation.i18n.bpmUnit
import com.pulso.mobile.presentation.i18n.formatShortDate
import com.pulso.mobile.presentation.i18n.formatTime
import com.pulso.mobile.presentation.i18n.tr
import com.pulso.mobile.presentation.theme.Pulso
import com.pulso.mobile.presentation.theme.PulsoIcons
import com.pulso.mobile.presentation.theme.PulsoType
import java.util.Calendar
import kotlin.math.ceil
import kotlin.math.cos
import kotlin.math.PI

private data class PlanOption(val id: String, val labelEs: String, val labelEn: String, val descEs: String, val descEn: String)

private val PLAN_OPTIONS = listOf(
    PlanOption(
        "Relajante 4-7-8", "4-7-8", "4-7-8",
        "Inhala 4 · sostén 7 · exhala 8. Ideal para bajar revoluciones.",
        "Inhale 4 · hold 7 · exhale 8. Best for winding down."
    ),
    PlanOption(
        "Cuadrada 4-4-4-4", "Caja 4-4-4-4", "Box 4-4-4-4",
        "Cuatro lados iguales. Te centra antes de una reunión.",
        "Four equal sides. Steadies you before a meeting."
    ),
    PlanOption(
        "Calma 4-6", "Calma 4-6", "Calm 4-6",
        "Inhala 4 · exhala 6. La exhalación larga activa la calma.",
        "Inhale 4 · exhale 6. A longer exhale switches on calm."
    )
)

private enum class Phase { IN, HOLD, OUT, HOLD2 }

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun BreatheScreen(
    config: BreathingConfig,
    sessions: List<PhoneBreathingSessionEntity>,
    weeklySummary: BreathingWeeklySummary,
    isStartingOnWatch: Boolean,
    feedback: String?,
    onUpdatePlan: (String) -> Unit,
    onUpdateDuration: (Int) -> Unit,
    onUpdateIntensity: (String) -> Unit,
    onStartOnWatch: () -> Unit
) {
    val c = Pulso.colors
    val en = LocalAppLanguage.current == com.pulso.mobile.presentation.i18n.AppLanguage.EN
    var showHistory by rememberSaveable { mutableStateOf(false) }
    var sliderMinutes by remember(config.durationMinutes) { mutableFloatStateOf(config.durationMinutes.toFloat()) }
    var phoneSessionEndsAt by remember { mutableLongStateOf(0L) }

    val selected = PLAN_OPTIONS.firstOrNull { it.id == config.preferredPlanName } ?: PLAN_OPTIONS.last()
    val plan = remember(config.preferredPlanName) { config.toPlan() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        ScreenHeader(
            kicker = tr("Calma guiada", "Guided calm"),
            title = tr("Respirar", "Breathe"),
            trailing = {
                IconCircleButton(
                    PulsoIcons.Clock,
                    contentDescription = tr("Historial de sesiones", "Session history"),
                    onClick = { showHistory = !showHistory },
                    background = if (showHistory) c.primarySoft else c.surface,
                    tint = if (showHistory) c.primary else c.onSurface
                )
            }
        )

        Box(Modifier.reveal(1)) { WeekCard(sessions, weeklySummary) }

        androidx.compose.animation.AnimatedVisibility(
            visible = showHistory,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) { SessionHistory(sessions) }

        PulsoCard(modifier = Modifier.reveal(2), radius = 26.dp, horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(14.dp)) {
            BreathingOrb(plan = plan, phoneSessionEndsAt = phoneSessionEndsAt, onSessionEnd = { phoneSessionEndsAt = 0L })
            FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                PLAN_OPTIONS.forEach { p ->
                    PulsoChip(if (en) p.labelEn else p.labelEs, selected = p.id == selected.id, onClick = { onUpdatePlan(p.id) })
                }
            }
            Text(if (en) selected.descEn else selected.descEs, style = PulsoType.bodySmall, color = c.muted, textAlign = TextAlign.Center)
        }

        PulsoCard(modifier = Modifier.reveal(3), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(12.dp)) {
            Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(tr("Duración", "Duration"), style = PulsoType.label.copy(fontSize = 14.sp), color = c.onSurface)
                    Text("${sliderMinutes.toInt()} min", style = PulsoType.number.copy(fontSize = 14.sp), color = c.primary)
                }
                Slider(
                    value = sliderMinutes,
                    onValueChange = { sliderMinutes = it },
                    onValueChangeFinished = { onUpdateDuration(sliderMinutes.toInt().coerceIn(1, 10)) },
                    valueRange = 1f..10f,
                    steps = 8,
                    colors = SliderDefaults.colors(
                        thumbColor = c.primary, activeTrackColor = c.primary, inactiveTrackColor = c.surface2,
                        activeTickColor = c.onPrimary.copy(alpha = 0.4f), inactiveTickColor = c.muted.copy(alpha = 0.4f)
                    )
                )
            }
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Vibración en el reloj", "Vibration on the watch"), style = PulsoType.label.copy(fontSize = 14.sp), color = c.onSurface)
                val keys = listOf("LOW", "MEDIUM", "HIGH")
                SegmentedControl(
                    options = listOf(tr("Suave", "Soft"), tr("Media", "Medium"), tr("Fuerte", "Strong")),
                    selectedIndex = keys.indexOf(config.hapticIntensity).coerceAtLeast(0),
                    onSelect = { onUpdateIntensity(keys[it]) },
                    fill = true,
                    segmentHeight = 34.dp,
                    textStyle = PulsoType.chip,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Row(Modifier.reveal(4), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PulsoButton(
                tr("Empezar en el reloj", "Start on watch"),
                onStartOnWatch,
                Modifier.weight(1f),
                icon = PulsoIcons.Watch,
                height = 54.dp,
                loading = isStartingOnWatch
            )
            val running = phoneSessionEndsAt > 0L
            PulsoButton(
                if (running) tr("Detener", "Stop") else tr("Aquí", "Here"),
                {
                    phoneSessionEndsAt = if (running) 0L else System.currentTimeMillis() + config.durationMinutes * 60_000L
                },
                style = PulsoButtonStyle.Tonal,
                height = 54.dp
            )
        }
        FeedbackText(feedback, positive = feedback?.contains("iniciada") == true || feedback?.contains("started") == true)
    }
}

@Composable
private fun BreathingOrb(plan: BreathingPlan, phoneSessionEndsAt: Long, onSessionEnd: () -> Unit) {
    val c = Pulso.colors
    val segments = remember(plan) {
        listOf(
            Phase.IN to plan.inhaleMs, Phase.HOLD to plan.holdAfterInhaleMs,
            Phase.OUT to plan.exhaleMs, Phase.HOLD2 to plan.holdAfterExhaleMs
        ).filter { it.second > 0 }
    }
    val cycleMs = segments.sumOf { it.second }.coerceAtLeast(1L)
    var t0 by remember(plan, phoneSessionEndsAt) { mutableLongStateOf(System.currentTimeMillis()) }
    var now by remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(plan, phoneSessionEndsAt) {
        t0 = System.currentTimeMillis()
        while (true) {
            withFrameMillis { }
            now = System.currentTimeMillis()
            if (phoneSessionEndsAt in 1..now) onSessionEnd()
        }
    }
    val elapsed = (now - t0).mod(cycleMs)
    var acc = 0L
    var phase = segments.first().first
    var into = 0L
    var len = segments.first().second
    for ((p, d) in segments) {
        if (elapsed < acc + d) { phase = p; into = elapsed - acc; len = d; break }
        acc += d
    }
    val k = into / len.toFloat()
    val ease = (0.5 - 0.5 * cos(PI * k)).toFloat()
    val scale = when (phase) {
        Phase.IN -> 0.55f + 0.45f * ease
        Phase.OUT -> 1f - 0.45f * ease
        Phase.HOLD -> 1f
        Phase.HOLD2 -> 0.55f
    }
    val label = when (phase) {
        Phase.IN -> tr("Inhala", "Inhale")
        Phase.OUT -> tr("Exhala", "Exhale")
        else -> tr("Sostén", "Hold")
    }
    val count = ceil((len - into) / 1000.0).toInt().coerceAtLeast(1)
    val remaining = if (phoneSessionEndsAt > 0) ((phoneSessionEndsAt - now) / 1000).coerceAtLeast(0) else null

    Box(Modifier.width(220.dp).height(200.dp), contentAlignment = Alignment.Center) {
        androidx.compose.foundation.Canvas(Modifier.size(196.dp)) {
            drawCircle(c.outline, style = Stroke(1.5.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 8f))))
        }
        Box(Modifier.size(196.dp).scale(0.7f + 0.3f * scale).clip(CircleShape).background(c.primarySoft))
        Box(Modifier.size(150.dp).scale(scale).clip(CircleShape).background(c.primary.copy(alpha = 0.9f)))
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
        ) {
            Text(label, style = PulsoType.title.copy(fontSize = 18.sp), color = c.onPrimary)
            Text("$count", style = PulsoType.number.copy(fontSize = 34.sp, lineHeight = 38.sp), color = c.onPrimary)
            if (remaining != null) {
                Text(
                    "%d:%02d".format(remaining / 60, remaining % 60),
                    style = PulsoType.caption.copy(fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
                    color = c.onPrimary.copy(alpha = 0.8f)
                )
            }
        }
    }
}

@Composable
private fun WeekCard(sessions: List<PhoneBreathingSessionEntity>, summary: BreathingWeeklySummary) {
    val c = Pulso.colors
    val weekStart = remember {
        Calendar.getInstance().apply {
            firstDayOfWeek = Calendar.MONDAY
            set(Calendar.DAY_OF_WEEK, Calendar.MONDAY)
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            if (timeInMillis > System.currentTimeMillis()) add(Calendar.DAY_OF_YEAR, -7)
        }.timeInMillis
    }
    val day = 24 * 60 * 60 * 1000L
    val minutesPerDay = remember(sessions, weekStart) {
        (0 until 7).map { i ->
            sessions.filter { it.startedAt >= weekStart + i * day && it.startedAt < weekStart + (i + 1) * day }
                .sumOf { ((it.completedAt - it.startedAt) / 60_000.0) }
        }
    }
    val todayIndex = ((System.currentTimeMillis() - weekStart) / day).toInt().coerceIn(0, 6)
    val streak = remember(sessions) {
        val todayStart = weekStart + todayIndex * day
        var n = 0
        var d = todayStart
        while (sessions.any { it.startedAt >= d && it.startedAt < d + day }) { n++; d -= day }
        n
    }
    val maxM = minutesPerDay.maxOrNull()?.takeIf { it > 0 } ?: 1.0
    val grow = com.pulso.mobile.presentation.components.drawInProgress(minutesPerDay)
    val labels = listOf(tr("L", "M"), tr("M", "T"), tr("X", "W"), tr("J", "T"), tr("V", "F"), tr("S", "S"), tr("D", "S"))

    PulsoCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(tr("Esta semana", "This week"), style = PulsoType.caption.copy(fontWeight = FontWeight.Bold), color = c.muted)
                Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    com.pulso.mobile.presentation.components.AnimatedNumber(summary.totalMinutesBreathed, PulsoType.number.copy(fontSize = 26.sp), c.onSurface)
                    Text("min", style = PulsoType.label.copy(fontSize = 14.sp), color = c.muted, modifier = Modifier.padding(bottom = 3.dp))
                }
                Text(
                    when {
                        streak >= 2 -> tr("Racha de $streak días", "$streak-day streak")
                        summary.totalSessionsThisWeek > 0 -> tr("${summary.averageFeelingBetterPct} % se sintió mejor", "${summary.averageFeelingBetterPct}% felt better")
                        else -> tr("Empieza tu primera sesión", "Start your first session")
                    },
                    style = PulsoType.caption.copy(fontWeight = FontWeight.Bold),
                    color = c.primary
                )
            }
            Row(Modifier.height(60.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                minutesPerDay.forEachIndexed { i, m ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Box(
                            Modifier.width(14.dp).height((4 + (m / maxM) * 38 * grow).dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(if (m <= 0) c.surface2 else if (i == todayIndex) c.primary else c.primarySoft)
                        )
                        Text(labels[i], style = PulsoType.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = c.muted)
                    }
                }
            }
        }
    }
}

@Composable
private fun SessionHistory(sessions: List<PhoneBreathingSessionEntity>) {
    val c = Pulso.colors
    val lang = LocalAppLanguage.current
    PulsoCard(contentPadding = PaddingValues(horizontal = 16.dp, vertical = 6.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
        if (sessions.isEmpty()) {
            Text(
                tr("Aún no hay sesiones registradas.", "No sessions recorded yet."),
                style = PulsoType.bodySmall, color = c.muted, modifier = Modifier.padding(vertical = 12.dp)
            )
        }
        val shown = sessions.sortedByDescending { it.startedAt }.take(8)
        shown.forEachIndexed { i, s ->
            Row(
                Modifier.fillMaxWidth().padding(vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                    Text(s.planName, style = PulsoType.label.copy(fontSize = 14.sp), color = c.onSurface)
                    val hr = if (s.hrBefore != null && s.hrAfter != null) " · ${s.hrBefore.toInt()}→${s.hrAfter.toInt()} ${bpmUnit()}" else ""
                    Text(
                        formatShortDate(s.startedAt, lang) + " " + formatTime(s.startedAt, lang) + " · ${s.completedCycles}/${s.targetCycles}" + hr,
                        style = PulsoType.caption.copy(fontFeatureSettings = "tnum"), color = c.muted
                    )
                }
                val (label, col) = when (s.feeling) {
                    "BETTER" -> tr("Mejor", "Better") to c.primary
                    "SAME" -> tr("Igual", "Same") to c.hrv
                    "WORSE" -> tr("Peor", "Worse") to c.warn
                    else -> "—" to c.muted
                }
                Text(label, style = PulsoType.chip, color = col)
            }
            if (i < shown.lastIndex) Box(Modifier.fillMaxWidth().height(1.dp).background(c.outline))
        }
    }
}
