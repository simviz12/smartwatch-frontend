package com.pulso.mobile.presentation.screens

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.animateFloat
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulso.core.baseline.Baseline
import com.pulso.core.model.MeasurementWindow
import com.pulso.mobile.data.local.PhoneStressEventEntity
import androidx.compose.ui.graphics.drawscope.clipRect
import com.pulso.mobile.presentation.components.AnimatedNumber
import com.pulso.mobile.presentation.components.drawInProgress
import com.pulso.mobile.presentation.components.reveal
import com.pulso.mobile.presentation.components.IconBadge
import com.pulso.mobile.presentation.components.LegendSwatch
import com.pulso.mobile.presentation.components.PulsoButton
import com.pulso.mobile.presentation.components.PulsoButtonStyle
import com.pulso.mobile.presentation.components.PulsoCard
import com.pulso.mobile.presentation.components.ScreenHeader
import com.pulso.mobile.presentation.components.SegmentedControl
import com.pulso.mobile.presentation.components.Sparkline
import com.pulso.mobile.presentation.i18n.LocalAppLanguage
import com.pulso.mobile.presentation.i18n.bpmUnit
import com.pulso.mobile.presentation.i18n.formatLongDate
import com.pulso.mobile.presentation.i18n.formatTime
import com.pulso.mobile.presentation.i18n.minutesAgoLabel
import com.pulso.mobile.presentation.i18n.tr
import com.pulso.mobile.presentation.model.Range
import com.pulso.mobile.presentation.model.TensionLevel
import com.pulso.mobile.presentation.model.computeTensionLevel
import com.pulso.mobile.presentation.model.currentBucketStats
import com.pulso.mobile.presentation.model.hrRange
import com.pulso.mobile.presentation.model.hrvRange
import com.pulso.mobile.presentation.theme.Pulso
import com.pulso.mobile.presentation.theme.PulsoIcons
import com.pulso.mobile.presentation.theme.PulsoType
import java.util.Calendar

@Composable
fun TodayScreen(
    windows: List<MeasurementWindow>,
    latestWindow: MeasurementWindow?,
    baseline: Baseline?,
    todayEvents: List<PhoneStressEventEntity>,
    breathingSessionTimes: List<Long>,
    watchConnected: Boolean,
    breathingMinutes: Int,
    onOpenDiagnostics: () -> Unit,
    onBreathe: () -> Unit,
    onOpenEvents: () -> Unit
) {
    val c = Pulso.colors
    val lang = LocalAppLanguage.current
    val now = System.currentTimeMillis()
    val stats = baseline.currentBucketStats(now, latestWindow?.activityState)
    val hrBand = stats?.hrRange()
    val hrvBand = stats?.hrvRange()
    val level = computeTensionLevel(latestWindow, baseline, todayEvents.maxOfOrNull { it.time }, now)

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(Modifier.reveal(0)) {
            ScreenHeader(
                kicker = formatLongDate(now, lang),
                title = tr("Hoy", "Today"),
                trailing = { WatchPill(connected = watchConnected, onClick = onOpenDiagnostics) }
            )
        }

        Box(Modifier.reveal(1)) { StatusCard(
            level = level,
            latestWindow = latestWindow,
            readingsToday = windows.size,
            breathingMinutes = breathingMinutes,
            onBreathe = onBreathe
        ) }

        Row(Modifier.reveal(2), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            MetricTile(
                label = tr("Pulso", "Pulse"),
                icon = PulsoIcons.Heart,
                color = c.heart,
                value = latestWindow?.meanHr?.toInt(),
                unit = bpmUnit(),
                spark = windows.takeLast(24).map { it.meanHr },
                base = hrBand?.let { tr("Base ${it.low.toInt()}–${it.high.toInt()}", "Baseline ${it.low.toInt()}–${it.high.toInt()}") }
                    ?: tr("Base en aprendizaje", "Baseline learning"),
                modifier = Modifier.weight(1f)
            )
            MetricTile(
                label = "HRV",
                icon = PulsoIcons.Wave,
                color = c.hrv,
                value = latestWindow?.rmssd?.toInt(),
                unit = "ms",
                spark = windows.takeLast(24).mapNotNull { it.rmssd },
                base = hrvBand?.let { tr("Base ${it.low.toInt()}–${it.high.toInt()}", "Baseline ${it.low.toInt()}–${it.high.toInt()}") }
                    ?: if (latestWindow?.rmssd != null) tr("Base en aprendizaje", "Baseline learning") else tr("Sin muestras IBI", "No IBI samples"),
                modifier = Modifier.weight(1f)
            )
        }

        Box(Modifier.reveal(3)) { DayCard(
            windows = windows,
            hrBand = hrBand,
            hrvBand = hrvBand,
            eventTimes = todayEvents.map { it.time },
            breathingTimes = breathingSessionTimes
        ) }

        Box(Modifier.reveal(4)) { EventsLinkCard(events = todayEvents, onClick = onOpenEvents) }
    }
}

@Composable
private fun WatchPill(connected: Boolean, onClick: () -> Unit) {
    val c = Pulso.colors
    Row(
        modifier = Modifier
            .height(40.dp)
            .clip(CircleShape)
            .background(c.surface)
            .border(1.dp, c.outline, CircleShape)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(if (connected) c.primary else c.warn))
        Text(
            if (connected) tr("Reloj", "Watch") else tr("Sin reloj", "No watch"),
            style = PulsoType.chip, color = c.onSurface
        )
        Icon(PulsoIcons.ChevronRight, null, tint = c.onSurface, modifier = Modifier.size(16.dp))
    }
}

@Composable
private fun StatusCard(
    level: TensionLevel,
    latestWindow: MeasurementWindow?,
    readingsToday: Int,
    breathingMinutes: Int,
    onBreathe: () -> Unit
) {
    val c = Pulso.colors
    val lang = LocalAppLanguage.current
    val (color, soft, fraction) = when (level) {
        TensionLevel.CALM -> Triple(c.primary, c.primarySoft, 0.28f)
        TensionLevel.ALERT -> Triple(c.warn, c.warnSoft, 0.6f)
        TensionLevel.TENSION -> Triple(c.heart, c.heartSoft, 0.9f)
        TensionLevel.LEARNING -> Triple(c.hrv, c.hrvSoft, 0.15f)
        TensionLevel.NO_DATA -> Triple(c.muted, c.surface2, 0f)
    }
    val levelLabel = when (level) {
        TensionLevel.CALM -> tr("Calma", "Calm")
        TensionLevel.ALERT -> tr("Atento", "Alert")
        TensionLevel.TENSION -> tr("Tensión", "Tension")
        TensionLevel.LEARNING -> tr("Aprendiendo", "Learning")
        TensionLevel.NO_DATA -> tr("Sin datos", "No data")
    }
    val status = when (level) {
        TensionLevel.CALM -> tr("Estás dentro de tu línea base", "You’re within your baseline")
        TensionLevel.ALERT -> tr("Tu pulso está algo por encima de tu base", "Your pulse is a bit above your baseline")
        TensionLevel.TENSION -> tr("Tu pulso está muy por encima de tu base", "Your pulse is well above your baseline")
        TensionLevel.LEARNING -> tr("Estamos aprendiendo tus rangos normales", "We’re learning your normal ranges")
        TensionLevel.NO_DATA -> tr("Aún no llegan mediciones del reloj", "No readings from the watch yet")
    }
    val updated = latestWindow?.let {
        val mins = ((System.currentTimeMillis() - it.windowEnd) / 60_000).coerceAtLeast(0)
        minutesAgoLabel(mins, lang) + " · " + tr("$readingsToday mediciones hoy", "$readingsToday readings today")
    } ?: tr("Abre Pulso en tu reloj para empezar", "Open Pulso on your watch to begin")
    val animated by animateFloatAsState(fraction, tween(900, easing = androidx.compose.animation.core.FastOutSlowInEasing), label = "gauge")
    // Slow "breathing" halo (≈5 s cycle, the pace of a calm breath).
    val breath = androidx.compose.animation.core.rememberInfiniteTransition(label = "halo")
    val halo by breath.animateFloat(
        0f, 1f,
        androidx.compose.animation.core.infiniteRepeatable(tween(2600, easing = androidx.compose.animation.core.FastOutSlowInEasing), androidx.compose.animation.core.RepeatMode.Reverse),
        label = "haloP"
    )

    PulsoCard(radius = 24.dp, contentPadding = PaddingValues(18.dp), verticalArrangement = Arrangement.spacedBy(14.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            Canvas(
                Modifier
                    .size(92.dp)
                    .semantics { contentDescription = levelLabel }
            ) {
                val stroke = 9.dp.toPx()
                val inset = stroke / 2 + 4.dp.toPx()
                val arcSize = Size(size.width - inset * 2, size.height - inset * 2)
                if (level != TensionLevel.NO_DATA) {
                    drawCircle(color.copy(alpha = 0.05f + 0.10f * halo), radius = (size.width / 2 - stroke - 6.dp.toPx()) * (0.86f + 0.14f * halo))
                }
                drawArc(c.surface2, 0f, 360f, false, Offset(inset, inset), arcSize, style = Stroke(stroke))
                if (animated > 0f) {
                    drawArc(color, -90f, 360f * animated, false, Offset(inset, inset), arcSize, style = Stroke(stroke, cap = StrokeCap.Round))
                }
                val s = size.width / 92f
                val p = Path().apply {
                    moveTo(30 * s, 47 * s); lineTo(39 * s, 47 * s); lineTo(43 * s, 39 * s)
                    lineTo(49 * s, 54 * s); lineTo(53 * s, 45 * s); lineTo(62 * s, 45 * s)
                }
                drawPath(p, color, style = Stroke(3.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(
                    levelLabel.uppercase(),
                    style = PulsoType.caption.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.5.sp),
                    color = color,
                    modifier = Modifier.clip(CircleShape).background(soft).padding(horizontal = 10.dp, vertical = 4.dp)
                )
                Text(status, style = PulsoType.h2.copy(fontSize = 18.sp, fontWeight = FontWeight.Bold, lineHeight = 23.sp), color = c.onSurface)
                Text(updated, style = PulsoType.bodySmall, color = c.muted)
            }
        }
        PulsoButton(
            text = tr("Respirar $breathingMinutes min", "Breathe for $breathingMinutes min"),
            onClick = onBreathe,
            style = PulsoButtonStyle.Tonal,
            icon = PulsoIcons.Wind,
            height = 48.dp,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun MetricTile(
    label: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    value: Int?,
    unit: String,
    spark: List<Double>,
    base: String,
    modifier: Modifier = Modifier
) {
    val c = Pulso.colors
    PulsoCard(
        modifier = modifier,
        radius = 20.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            Icon(icon, null, tint = color, modifier = Modifier.size(16.dp))
            Text(label.uppercase(), style = PulsoType.caption.copy(fontWeight = FontWeight.ExtraBold, letterSpacing = 0.7.sp), color = color)
        }
        Row(verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            AnimatedNumber(value, PulsoType.metric, c.onSurface)
            Text(unit, style = PulsoType.label.copy(fontSize = 14.sp), color = c.muted, modifier = Modifier.padding(bottom = 6.dp))
        }
        if (spark.size >= 2) {
            Sparkline(spark, color, Modifier.fillMaxWidth().height(22.dp))
        } else {
            Spacer(Modifier.height(22.dp))
        }
        Text(base, style = PulsoType.caption, color = c.muted)
    }
}

@Composable
private fun DayCard(
    windows: List<MeasurementWindow>,
    hrBand: Range?,
    hrvBand: Range?,
    eventTimes: List<Long>,
    breathingTimes: List<Long>
) {
    val c = Pulso.colors
    var metric by rememberSaveable { mutableIntStateOf(0) }
    val isHrv = metric == 1
    val series = remember(windows, isHrv) {
        windows.mapNotNull { w -> (if (isHrv) w.rmssd else w.meanHr)?.let { w.windowStart to it } }
    }
    val color = if (isHrv) c.hrv else c.heart
    val bandColor = if (isHrv) c.hrvSoft else c.primarySoft

    PulsoCard(radius = 24.dp, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween, modifier = Modifier.fillMaxWidth()) {
            Text(tr("Tu día", "Your day"), style = PulsoType.title, color = c.onSurface)
            SegmentedControl(
                options = listOf(tr("Pulso", "Pulse"), "HRV"),
                selectedIndex = metric,
                onSelect = { metric = it },
                textStyle = PulsoType.chip
            )
        }
        if (series.size < 2) {
            Box(Modifier.fillMaxWidth().height(132.dp), contentAlignment = Alignment.Center) {
                Text(
                    if (isHrv) tr("HRV no disponible o sin muestras IBI hoy", "HRV not available or no IBI samples today")
                    else tr("Aún no hay suficientes datos para graficar hoy", "Not enough data to chart today yet"),
                    style = PulsoType.bodySmall, color = c.muted, textAlign = TextAlign.Center
                )
            }
        } else {
            DayChart(
                series = series,
                band = if (isHrv) hrvBand else hrBand,
                defaultRange = if (isHrv) 20.0 to 80.0 else 45.0 to 100.0,
                lineColor = color,
                bandColor = bandColor,
                eventTimes = eventTimes,
                breathingTimes = breathingTimes,
                contentDescription = tr("Gráfica de hoy", "Chart of today")
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
            LegendSwatch(tr("Tu línea base", "Your baseline"), bandColor, color)
            LegendSwatch(tr("Evento de tensión", "Tension event"), Color.Transparent, c.heart, round = true)
            if (breathingTimes.isNotEmpty()) LegendSwatch(tr("Respiración", "Breathing"), c.primary, c.primary, round = true)
        }
    }
}

@Composable
private fun DayChart(
    series: List<Pair<Long, Double>>,
    band: Range?,
    defaultRange: Pair<Double, Double>,
    lineColor: Color,
    bandColor: Color,
    eventTimes: List<Long>,
    breathingTimes: List<Long>,
    contentDescription: String
) {
    val c = Pulso.colors
    val dayStart = remember {
        Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
    val dayMs = 24 * 60 * 60 * 1000f
    val values = series.map { it.second }
    val drawIn = drawInProgress(lineColor)
    val minV = minOf(values.min(), band?.low ?: Double.MAX_VALUE, defaultRange.first) - 4
    val maxV = maxOf(values.max(), band?.high ?: Double.MIN_VALUE, defaultRange.second) + 4

    Column(Modifier.fillMaxWidth()) {
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(112.dp)
                .semantics { this.contentDescription = contentDescription }
        ) {
            val w = size.width
            val h = size.height
            fun x(t: Long) = ((t - dayStart) / dayMs).coerceIn(0f, 1f) * w
            fun y(v: Double) = (h - 6.dp.toPx()) - ((v - minV) / (maxV - minV)).toFloat() * (h - 12.dp.toPx()) + 0f

            band?.let {
                val top = y(it.high)
                val bottom = y(it.low)
                drawRect(bandColor, Offset(0f, top), Size(w, bottom - top))
            }
            drawLine(c.outline, Offset(0f, h - 1f), Offset(w, h - 1f), 1.dp.toPx())
            val nowX = x(System.currentTimeMillis())
            drawLine(c.outline, Offset(nowX, 0f), Offset(nowX, h), 1.dp.toPx(), pathEffect = PathEffect.dashPathEffect(floatArrayOf(6f, 6f)))

            val line = Path()
            val area = Path()
            series.forEachIndexed { i, (t, v) ->
                val px = x(t)
                val py = y(v)
                if (i == 0) {
                    line.moveTo(px, py); area.moveTo(px, h); area.lineTo(px, py)
                } else {
                    line.lineTo(px, py); area.lineTo(px, py)
                }
            }
            area.lineTo(x(series.last().first), h)
            area.close()
            clipRect(right = w * drawIn) {
                drawPath(area, lineColor.copy(alpha = 0.10f))
                drawPath(line, lineColor, style = Stroke(2.4.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
            }

            fun valueAt(t: Long): Double = series.minByOrNull { kotlin.math.abs(it.first - t) }?.second ?: values.last()
            breathingTimes.filter { it >= dayStart }.forEach { t ->
                drawCircle(c.primary, 4.dp.toPx(), Offset(x(t), h - 6.dp.toPx()))
            }
            eventTimes.filter { x(it) <= w * drawIn }.forEach { t ->
                val center = Offset(x(t), y(valueAt(t)))
                drawCircle(c.surface, 5.5.dp.toPx(), center)
                drawCircle(c.heart, 5.5.dp.toPx(), center, style = Stroke(2.5.dp.toPx()))
            }
        }
        Spacer(Modifier.height(6.dp))
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            listOf("00", "06", "12", "18", "24").forEach {
                Text(it, style = PulsoType.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = c.muted)
            }
        }
    }
}

@Composable
private fun EventsLinkCard(events: List<PhoneStressEventEntity>, onClick: () -> Unit) {
    val c = Pulso.colors
    val lang = LocalAppLanguage.current
    val latest = events.maxByOrNull { it.time }
    val title = when (events.size) {
        0 -> tr("Sin eventos de tensión hoy", "No tension events today")
        1 -> tr("1 evento de tensión hoy", "1 tension event today")
        else -> tr("${events.size} eventos de tensión hoy", "${events.size} tension events today")
    }
    val sub = latest?.let {
        formatTime(it.time, lang) + " · " + tr("${(it.confidence * 100).toInt()} % de certeza", "${(it.confidence * 100).toInt()}% confidence")
    } ?: tr("Ver tu historial de 7 días", "See your 7-day history")
    PulsoCard(
        radius = 20.dp,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp),
        onClick = onClick
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            IconBadge(PulsoIcons.Bolt, tint = if (events.isEmpty()) c.primary else c.heart, background = if (events.isEmpty()) c.primarySoft else c.heartSoft)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(title, style = PulsoType.label, color = c.onSurface)
                Text(sub, style = PulsoType.bodySmall, color = c.muted)
            }
            Icon(PulsoIcons.ChevronRight, null, tint = c.muted, modifier = Modifier.size(18.dp))
        }
    }
}
