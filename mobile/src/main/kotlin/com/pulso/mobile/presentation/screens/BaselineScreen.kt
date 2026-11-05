package com.pulso.mobile.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulso.core.baseline.Baseline
import com.pulso.core.baseline.BaselineEngine
import com.pulso.core.baseline.BaselineState
import com.pulso.core.baseline.TimeBucket
import com.pulso.core.model.MeasurementWindow
import com.pulso.mobile.presentation.components.reveal
import com.pulso.mobile.presentation.components.BackHeader
import com.pulso.mobile.presentation.components.FeedbackText
import com.pulso.mobile.presentation.components.LegendSwatch
import com.pulso.mobile.presentation.components.PulsoButton
import com.pulso.mobile.presentation.components.PulsoButtonStyle
import com.pulso.mobile.presentation.components.PulsoCard
import com.pulso.mobile.presentation.components.PulsoConfirmSheet
import com.pulso.mobile.presentation.components.PulsoProgressBar
import com.pulso.mobile.presentation.i18n.tr
import com.pulso.mobile.presentation.model.Range
import com.pulso.mobile.presentation.model.hrRange
import com.pulso.mobile.presentation.model.hrvRange
import com.pulso.mobile.presentation.model.learningProgress
import com.pulso.mobile.presentation.model.slotStats
import com.pulso.mobile.presentation.model.toDaySlot
import com.pulso.mobile.presentation.theme.Pulso
import com.pulso.mobile.presentation.theme.PulsoIcons
import com.pulso.mobile.presentation.theme.PulsoType

@Composable
fun BaselineScreen(
    baseline: Baseline?,
    latestWindow: MeasurementWindow?,
    isResetting: Boolean,
    resetFeedback: String?,
    onReset: () -> Unit,
    onBack: () -> Unit
) {
    val c = Pulso.colors
    var confirm by remember { mutableStateOf(false) }
    val progress = baseline.learningProgress()
    val ready = baseline != null && baseline.buckets.isNotEmpty() &&
        listOf(TimeBucket.NIGHT, TimeBucket.MORNING, TimeBucket.AFTERNOON, TimeBucket.EVENING)
            .all { baseline.slotStats(it)?.state == BaselineState.READY }
    val totalWindows = baseline?.buckets?.values?.sumOf { it.windowCount } ?: 0
    val days = baseline?.buckets?.values?.maxOfOrNull { it.daysCollected } ?: 0
    val nowBucket = BaselineEngine.resolveBucket(System.currentTimeMillis(), latestWindow?.activityState ?: "PASSIVE").toDaySlot()

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackHeader(tr("Línea base", "Baseline"), onBack) {
            PulsoButton(
                tr("Reiniciar", "Reset"), { confirm = true },
                style = PulsoButtonStyle.DangerGhost, height = 40.dp,
                textStyle = PulsoType.chip.copy(fontWeight = FontWeight.ExtraBold), loading = isResetting
            )
        }

        PulsoCard(modifier = Modifier.reveal(1), background = c.primarySoft, border = null) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                Canvas(Modifier.size(52.dp)) {
                    val stroke = 6.dp.toPx()
                    val inset = stroke / 2 + 2.dp.toPx()
                    val arc = Size(size.width - inset * 2, size.height - inset * 2)
                    drawArc(c.outline, 0f, 360f, false, Offset(inset, inset), arc, style = Stroke(stroke))
                    drawArc(c.primary, -90f, 360f * (if (ready) 1f else progress), false, Offset(inset, inset), arc, style = Stroke(stroke, cap = StrokeCap.Round))
                    if (ready) {
                        val s = size.width / 52f
                        val p = Path().apply { moveTo(18 * s, 26.5f * s); lineTo(23.5f * s, 32 * s); lineTo(34 * s, 21 * s) }
                        drawPath(p, c.primary, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
                    }
                }
                Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text(
                        if (ready) tr("Tu línea base está lista", "Your baseline is ready")
                        else tr("Aprendiendo tu línea base · ${(progress * 100).toInt()} %", "Learning your baseline · ${(progress * 100).toInt()}%"),
                        style = PulsoType.title, color = c.onSurface
                    )
                    Text(
                        tr(
                            "Aprendida de $days días y $totalWindows mediciones. Se sigue ajustando cada noche.",
                            "Learned from $days days and $totalWindows readings. It keeps adjusting every night."
                        ),
                        style = PulsoType.bodySmall, color = c.muted
                    )
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(16.dp), modifier = Modifier.padding(horizontal = 4.dp)) {
            LegendSwatch(tr("Pulso", "Pulse"), c.heartSoft, c.heart)
            LegendSwatch("HRV", c.hrvSoft, c.hrv)
            LegendSwatch(tr("Ahora", "Now"), c.onSurface, c.onSurface, round = true)
        }

        listOf(
            Triple(TimeBucket.NIGHT, tr("Madrugada", "Overnight"), "00–06"),
            Triple(TimeBucket.MORNING, tr("Mañana", "Morning"), "06–12"),
            Triple(TimeBucket.AFTERNOON, tr("Tarde", "Afternoon"), "12–18"),
            Triple(TimeBucket.EVENING, tr("Noche", "Evening"), "18–24")
        ).forEachIndexed { slotIndex, (bucket, name, hours) ->
            val stats = baseline?.slotStats(bucket)
            val current = bucket == nowBucket
            PulsoCard(
                modifier = Modifier.reveal(2 + slotIndex),
                radius = 20.dp,
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
                border = if (current) c.primary else c.outline,
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(name, style = PulsoType.label.copy(fontWeight = FontWeight.ExtraBold), color = c.onSurface, modifier = Modifier.weight(1f))
                    Text(hours, style = PulsoType.caption.copy(fontWeight = FontWeight.Bold, fontFeatureSettings = "tnum"), color = c.muted)
                    if (current) {
                        Text(
                            tr("AHORA", "NOW"),
                            style = PulsoType.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.ExtraBold),
                            color = c.primary,
                            modifier = Modifier.clip(CircleShape).background(c.primarySoft).padding(horizontal = 8.dp, vertical = 3.dp)
                        )
                    }
                }
                if (stats == null || stats.state != BaselineState.READY) {
                    val pct = ((stats?.progressPct ?: 0.0) / 100.0).toFloat()
                    Text(
                        tr("Aprendiendo · ${(pct * 100).toInt()} % · ${stats?.windowCount ?: 0} mediciones", "Learning · ${(pct * 100).toInt()}% · ${stats?.windowCount ?: 0} readings"),
                        style = PulsoType.bodySmall, color = c.muted
                    )
                    PulsoProgressBar(pct, c.primary, height = 8.dp)
                } else {
                    RangeRow(stats.hrRange(), if (current) latestWindow?.meanHr else null, 40.0, 110.0, c.heart, c.heartSoft, "")
                    RangeRow(stats.hrvRange(), if (current) latestWindow?.rmssd else null, 10.0, 90.0, c.hrv, c.hrvSoft, " ms")
                }
            }
        }
        FeedbackText(resetFeedback, positive = resetFeedback?.contains("exitos") == true)
    }

    if (confirm) {
        PulsoConfirmSheet(
            icon = PulsoIcons.Sync,
            title = tr("¿Reiniciar tu línea base?", "Reset your baseline?"),
            body = tr(
                "Pulso volverá a aprender tus rangos normales durante los próximos días. Los avisos quedan en pausa hasta entonces.",
                "Pulso will relearn your normal ranges over the next few days. Alerts stay paused until then."
            ),
            confirmLabel = tr("Reiniciar", "Reset"),
            onConfirm = { confirm = false; onReset() },
            onDismiss = { confirm = false }
        )
    }
}

@Composable
private fun RangeRow(range: Range?, now: Double?, scaleMin: Double, scaleMax: Double, color: Color, soft: Color, unit: String) {
    val c = Pulso.colors
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            range?.let { "${it.low.toInt()}–${it.high.toInt()}$unit" } ?: "—",
            style = PulsoType.number.copy(fontSize = 13.sp),
            color = color,
            modifier = Modifier.width(66.dp)
        )
        BoxWithConstraints(Modifier.weight(1f).height(16.dp), contentAlignment = Alignment.CenterStart) {
            val w = maxWidth
            fun frac(v: Double) = ((v - scaleMin) / (scaleMax - scaleMin)).toFloat().coerceIn(0f, 1f)
            Box(Modifier.fillMaxWidth().height(10.dp).clip(CircleShape).background(c.surface2))
            if (range != null) {
                val l = frac(range.low)
                val r = frac(range.high)
                Box(
                    Modifier.offset(x = w * l).width(w * (r - l).coerceAtLeast(0.02f)).height(10.dp)
                        .clip(CircleShape).background(soft).border(1.dp, color, CircleShape)
                )
            }
            if (now != null) {
                Box(
                    Modifier.offset(x = w * frac(now) - 8.dp).size(16.dp).clip(CircleShape)
                        .background(c.onSurface).border(3.dp, c.surface, CircleShape)
                )
            }
        }
    }
}
