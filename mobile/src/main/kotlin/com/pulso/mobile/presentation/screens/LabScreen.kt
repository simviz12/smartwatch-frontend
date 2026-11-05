package com.pulso.mobile.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulso.mobile.domain.stress.ReplaySimulationResult
import com.pulso.mobile.domain.stress.SensitivityPreset
import com.pulso.mobile.presentation.components.reveal
import com.pulso.mobile.presentation.components.BackHeader
import com.pulso.mobile.presentation.components.FeedbackText
import com.pulso.mobile.presentation.components.PulsoButton
import com.pulso.mobile.presentation.components.PulsoCard
import com.pulso.mobile.presentation.components.PulsoProgressBar
import com.pulso.mobile.presentation.components.SegmentedControl
import com.pulso.mobile.presentation.i18n.LocalAppLanguage
import com.pulso.mobile.presentation.i18n.formatShortDate
import com.pulso.mobile.presentation.i18n.tr
import com.pulso.mobile.presentation.theme.Pulso
import com.pulso.mobile.presentation.theme.PulsoIcons
import com.pulso.mobile.presentation.theme.PulsoType

private val LAB_PERIODS = listOf(3, 7, 14)
private val LAB_PRESETS = listOf(SensitivityPreset.BAJA, SensitivityPreset.MEDIA, SensitivityPreset.ALTA)

@Composable
fun LabScreen(
    daysRange: Int,
    preset: SensitivityPreset,
    isRunning: Boolean,
    result: ReplaySimulationResult?,
    comparison: Map<SensitivityPreset, Int>,
    feedback: String?,
    onDaysRange: (Int) -> Unit,
    onPreset: (SensitivityPreset) -> Unit,
    onRun: () -> Unit,
    onApply: (SensitivityPreset) -> Unit,
    onBack: () -> Unit
) {
    val c = Pulso.colors
    val lang = LocalAppLanguage.current
    val presetLabels = listOf(tr("Baja", "Low"), tr("Media", "Medium"), tr("Alta", "High"))
    val selectedPreset = if (preset == SensitivityPreset.PERSONALIZADO) SensitivityPreset.MEDIA else preset

    LaunchedEffect(daysRange, selectedPreset) { onRun() }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackHeader(tr("Laboratorio", "Lab"), onBack) {
            Row(
                Modifier.height(28.dp).clip(CircleShape).background(c.hrvSoft).padding(horizontal = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                Icon(PulsoIcons.Flask, null, tint = c.hrv, modifier = Modifier.size(14.dp))
                Text("Replay", style = PulsoType.caption.copy(fontWeight = FontWeight.ExtraBold), color = c.hrv)
            }
        }
        Text(
            tr(
                "Repite la detección sobre tu historial real para ver cuántos avisos tendrías con cada ajuste, antes de cambiar nada.",
                "Replay detection over your real history to see how many alerts you’d get with each setting — before changing anything."
            ),
            style = PulsoType.bodySmall.copy(fontSize = 14.sp, lineHeight = 21.sp), color = c.muted,
            modifier = Modifier.padding(horizontal = 4.dp)
        )

        PulsoCard(modifier = Modifier.reveal(1), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
            LabeledSegment(tr("Periodo", "Period")) {
                SegmentedControl(
                    options = LAB_PERIODS.map { tr("$it días", "$it days") },
                    selectedIndex = LAB_PERIODS.indexOf(daysRange).coerceAtLeast(0),
                    onSelect = { onDaysRange(LAB_PERIODS[it]) },
                    fill = true, modifier = Modifier.fillMaxWidth()
                )
            }
            LabeledSegment(tr("Sensibilidad", "Sensitivity")) {
                SegmentedControl(
                    options = presetLabels,
                    selectedIndex = LAB_PRESETS.indexOf(selectedPreset),
                    onSelect = { onPreset(LAB_PRESETS[it]) },
                    fill = true, modifier = Modifier.fillMaxWidth()
                )
            }
        }

        PulsoCard(modifier = Modifier.reveal(2), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                Text(tr("Pulso · últimos $daysRange días", "Pulse · last $daysRange days"), style = PulsoType.label.copy(fontWeight = FontWeight.ExtraBold), color = c.onSurface)
                if (isRunning) CircularProgressIndicator(color = c.primary, strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                else result?.let { Text(tr("${it.totalTriggers} avisos", "${it.totalTriggers} alerts"), style = PulsoType.caption, color = c.warn) }
            }
            if (result == null || result.timeline.isEmpty()) {
                Box(Modifier.fillMaxWidth().height(106.dp), contentAlignment = Alignment.Center) {
                    Text(
                        com.pulso.mobile.presentation.i18n.localizeStatus(feedback) ?: if (isRunning) tr("Simulando…", "Simulating…") else tr("Sin mediciones en este periodo", "No readings in this period"),
                        style = PulsoType.bodySmall, color = c.muted, textAlign = TextAlign.Center
                    )
                }
            } else {
                ReplayChart(result, c.heart, c.warn)
                val first = result.timeline.first().window.windowStart
                val last = result.timeline.last().window.windowStart
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                    Text(formatShortDate(first, lang), style = PulsoType.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = c.muted)
                    Text(formatShortDate(last, lang), style = PulsoType.caption.copy(fontSize = 10.sp, fontWeight = FontWeight.Bold), color = c.muted)
                }
            }
        }

        if (result != null) {
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatTile("${result.totalTriggers}", tr("avisos simulados", "simulated alerts"), c.warn, Modifier.weight(1f))
                StatTile("${result.totalSuppressedRuleMatches}", tr("frenados por filtros", "blocked by filters"), c.primary, Modifier.weight(1f))
                StatTile("%.1f".format(result.totalTriggers / daysRange.toFloat()), tr("avisos por día", "alerts per day"), c.onSurface, Modifier.weight(1f))
            }
        }

        if (comparison.isNotEmpty()) {
            val maxN = comparison.values.maxOrNull()?.coerceAtLeast(1) ?: 1
            PulsoCard(radius = 20.dp, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(tr("Avisos por sensibilidad", "Alerts by sensitivity"), style = PulsoType.chip, color = c.muted)
                LAB_PRESETS.forEachIndexed { i, p ->
                    val n = comparison[p] ?: 0
                    val active = p == selectedPreset
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        Text(presetLabels[i], style = PulsoType.chip.copy(fontWeight = if (active) FontWeight.ExtraBold else FontWeight.SemiBold), color = c.onSurface, modifier = Modifier.width(60.dp))
                        PulsoProgressBar(n / maxN.toFloat(), if (active) c.warn else c.outline, Modifier.weight(1f), height = 8.dp)
                        Text("$n", style = PulsoType.number.copy(fontSize = 13.sp), color = c.onSurface, textAlign = TextAlign.End, modifier = Modifier.width(28.dp))
                    }
                }
            }
        }

        result?.topSuppressionReasons?.takeIf { it.isNotEmpty() }?.let { reasons ->
            PulsoCard(radius = 20.dp, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(tr("Filtros que más frenaron avisos", "Filters that blocked the most alerts"), style = PulsoType.chip, color = c.muted)
                reasons.entries.sortedByDescending { it.value }.take(3).forEach { (reason, count) ->
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(reason, style = PulsoType.bodySmall, color = c.onSurface, modifier = Modifier.weight(1f))
                        Text("$count", style = PulsoType.number.copy(fontSize = 13.sp), color = c.muted)
                    }
                }
            }
        }

        PulsoButton(
            tr("Aplicar sensibilidad ${presetLabels[LAB_PRESETS.indexOf(selectedPreset)].lowercase()}", "Apply ${presetLabels[LAB_PRESETS.indexOf(selectedPreset)].lowercase()} sensitivity"),
            { onApply(selectedPreset) },
            Modifier.fillMaxWidth(),
            enabled = result != null && !isRunning
        )
        FeedbackText(if (result != null) null else feedback, positive = false)
    }
}

@Composable
private fun LabeledSegment(label: String, content: @Composable () -> Unit) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(label, style = PulsoType.label.copy(fontSize = 14.sp), color = Pulso.colors.onSurface, modifier = Modifier.width(96.dp))
        Box(Modifier.weight(1f)) { content() }
    }
}

@Composable
private fun StatTile(value: String, label: String, color: Color, modifier: Modifier) {
    PulsoCard(modifier = modifier, radius = 18.dp, contentPadding = PaddingValues(12.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(value, style = PulsoType.number.copy(fontSize = 24.sp), color = color)
        Text(label, style = PulsoType.caption.copy(lineHeight = 16.sp), color = Pulso.colors.muted)
    }
}

@Composable
private fun ReplayChart(result: ReplaySimulationResult, line: Color, hit: Color) {
    val c = Pulso.colors
    val points = result.timeline
    val values = points.map { it.window.meanHr }
    val mn = (values.minOrNull() ?: 50.0) - 4
    val mx = (values.maxOrNull() ?: 100.0) + 4
    Canvas(Modifier.fillMaxWidth().height(106.dp)) {
        val w = size.width
        val h = size.height
        val n = (points.size - 1).coerceAtLeast(1)
        fun x(i: Int) = i / n.toFloat() * w
        fun y(v: Double) = (h - 2.dp.toPx()) - ((v - mn) / (mx - mn)).toFloat() * (h - 6.dp.toPx())
        drawLine(c.outline, Offset(0f, h - 1f), Offset(w, h - 1f), 1.dp.toPx())
        val path = Path()
        values.forEachIndexed { i, v -> if (i == 0) path.moveTo(x(i), y(v)) else path.lineTo(x(i), y(v)) }
        drawPath(path, line.copy(alpha = 0.85f), style = Stroke(1.6.dp.toPx(), join = StrokeJoin.Round))
        points.forEachIndexed { i, p ->
            if (p.decision.triggered) {
                drawLine(hit.copy(alpha = 0.35f), Offset(x(i), 0f), Offset(x(i), h), 1.dp.toPx())
                drawCircle(c.surface, 6.dp.toPx(), Offset(x(i), y(p.window.meanHr)))
                drawCircle(hit, 4.5.dp.toPx(), Offset(x(i), y(p.window.meanHr)))
            }
        }
    }
}
