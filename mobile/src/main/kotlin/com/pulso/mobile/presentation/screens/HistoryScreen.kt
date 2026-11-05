package com.pulso.mobile.presentation.screens

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
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
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulso.core.aggregation.StressMapReport
import com.pulso.core.stress.StressReasonCode
import com.pulso.mobile.data.local.PhoneStressEventEntity
import com.pulso.mobile.presentation.components.reveal
import com.pulso.mobile.presentation.components.ChipStyle
import com.pulso.mobile.presentation.components.FeedbackText
import com.pulso.mobile.presentation.components.IconBadge
import com.pulso.mobile.presentation.components.PulsoButton
import com.pulso.mobile.presentation.components.PulsoButtonStyle
import com.pulso.mobile.presentation.components.PulsoCard
import com.pulso.mobile.presentation.components.PulsoChip
import com.pulso.mobile.presentation.components.PulsoConfirmSheet
import com.pulso.mobile.presentation.components.PulsoProgressBar
import com.pulso.mobile.presentation.components.ScreenHeader
import com.pulso.mobile.presentation.components.SegmentedControl
import com.pulso.mobile.presentation.i18n.AppLanguage
import com.pulso.mobile.presentation.i18n.LocalAppLanguage
import com.pulso.mobile.presentation.i18n.formatShortDate
import com.pulso.mobile.presentation.i18n.formatTime
import com.pulso.mobile.presentation.i18n.tr
import com.pulso.mobile.presentation.theme.Pulso
import com.pulso.mobile.presentation.theme.PulsoIcons
import com.pulso.mobile.presentation.theme.PulsoType
import java.util.Calendar

/** Historial: Eventos and Mapa share one destination behind a segmented control. */
@Composable
fun HistoryScreen(
    initialTab: Int,
    events: List<PhoneStressEventEntity>,
    report: StressMapReport,
    sampleFeedback: String?,
    onTabChange: (Int) -> Unit,
    onUpdateFeedback: (eventId: String, feedback: String) -> Unit,
    onGenerateSampleData: () -> Unit,
    onClearAllData: () -> Unit
) {
    var tab by rememberSaveable(initialTab) { mutableIntStateOf(initialTab) }
    val lang = LocalAppLanguage.current
    val now = System.currentTimeMillis()

    val header: @Composable () -> Unit = {
        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            ScreenHeader(
                kicker = if (tab == 0) tr("Últimos 7 días", "Last 7 days")
                else formatShortDate(now - 6 * DAY_MS, lang) + " – " + formatShortDate(now, lang),
                title = tr("Historial", "History")
            )
            SegmentedControl(
                options = listOf(tr("Eventos", "Events"), tr("Mapa", "Map")),
                icons = listOf(PulsoIcons.Bolt, PulsoIcons.Grid),
                selectedIndex = tab,
                onSelect = { tab = it; onTabChange(it) },
                fill = true,
                segmentHeight = 40.dp,
                textStyle = PulsoType.chip.copy(fontSize = 14.sp),
                modifier = Modifier.fillMaxWidth()
            )
        }
    }

    if (tab == 0) {
        EventsList(header, events, onUpdateFeedback)
    } else {
        MapContent(header, report, sampleFeedback, onGenerateSampleData, onClearAllData)
    }
}

private const val DAY_MS = 24 * 60 * 60 * 1000L

private fun startOfDay(time: Long): Long = Calendar.getInstance().apply {
    timeInMillis = time
    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
}.timeInMillis

@Composable
private fun EventsList(
    header: @Composable () -> Unit,
    events: List<PhoneStressEventEntity>,
    onUpdateFeedback: (String, String) -> Unit
) {
    val c = Pulso.colors
    val lang = LocalAppLanguage.current
    var range by rememberSaveable { mutableIntStateOf(1) }
    val today = startOfDay(System.currentTimeMillis())
    val since = when (range) {
        0 -> today
        1 -> today - 6 * DAY_MS
        else -> today - 29 * DAY_MS
    }
    val filtered = remember(events, since) { events.filter { it.time >= since }.sortedByDescending { it.time } }
    val groups = remember(filtered) { filtered.groupBy { startOfDay(it.time) }.toList() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        item { header() }
        item {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                listOf(tr("Hoy", "Today"), tr("7 días", "7 days"), tr("30 días", "30 days")).forEachIndexed { i, l ->
                    PulsoChip(l, selected = i == range, onClick = { range = i }, style = ChipStyle.Inverse, height = 36.dp)
                }
                Spacer(Modifier.weight(1f))
                Text(
                    if (filtered.size == 1) tr("1 evento", "1 event") else tr("${filtered.size} eventos", "${filtered.size} events"),
                    style = PulsoType.chip, color = c.muted
                )
            }
        }
        if (filtered.isEmpty()) {
            item {
                PulsoCard(horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(24.dp)) {
                    IconBadge(PulsoIcons.Check, tint = c.primary, background = c.primarySoft, size = 48.dp, iconSize = 24.dp, radius = 16.dp)
                    Text(tr("Sin eventos de tensión", "No tension events"), style = PulsoType.title, color = c.onSurface)
                    Text(
                        tr("Cuando Pulso detecte tensión la verás aquí para que confirmes si lo fue.", "When Pulso detects tension it shows up here so you can confirm it."),
                        style = PulsoType.bodySmall, color = c.muted, textAlign = TextAlign.Center
                    )
                }
            }
        }
        groups.forEach { (day, dayEvents) ->
            item(key = "h$day") {
                val label = when (day) {
                    today -> tr("Hoy", "Today")
                    today - DAY_MS -> tr("Ayer", "Yesterday")
                    else -> formatShortDate(day, lang)
                }
                Text(label.uppercase(), style = PulsoType.kicker.copy(fontWeight = FontWeight.ExtraBold), color = c.muted, modifier = Modifier.padding(horizontal = 4.dp))
            }
            items(dayEvents, key = { it.id }) { e ->
                Box(Modifier.animateItem().reveal(1)) {
                    EventCard(e, lang, onFeedback = { fb -> onUpdateFeedback(e.id, fb) })
                }
            }
        }
    }
}

@Composable
private fun EventCard(entity: PhoneStressEventEntity, lang: AppLanguage, onFeedback: (String) -> Unit) {
    val c = Pulso.colors
    val event = remember(entity) { entity.toDomain() }
    val pct = (entity.confidence * 100).toInt()
    val high = entity.confidence >= 0.75
    val color = if (high) c.heart else c.warn
    val soft = if (high) c.heartSoft else c.warnSoft
    val rule = when {
        event.reasons.any { it.code == StressReasonCode.ELEVATED_HR_LOW_HRV } -> tr("Pulso alto + HRV baja", "High pulse + low HRV")
        event.reasons.any { it.code == StressReasonCode.ELEVATED_HR_ONLY } -> tr("Pulso sostenido", "Sustained pulse")
        else -> tr("Patrón de tensión", "Tension pattern")
    }
    val minutes = event.windowsUsed.size * 5
    val meta = buildString {
        append(formatTime(entity.time, lang))
        if (minutes > 0) append(" · $minutes min")
        append(" · ").append(rule)
    }
    val answered = entity.userFeedback in setOf("REVISED_STRESSED", "TALK", "BREATHE", "NOT_STRESSED")
    var showWhy by remember { mutableStateOf(false) }

    PulsoCard(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            IconBadge(PulsoIcons.Bolt, tint = color, background = soft)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(if (high) tr("Tensión alta", "High tension") else tr("Tensión en aumento", "Rising tension"), style = PulsoType.title, color = c.onSurface)
                Text(meta, style = PulsoType.bodySmall.copy(fontFeatureSettings = "tnum"), color = c.muted)
            }
            Text("$pct%", style = PulsoType.number.copy(fontSize = 22.sp), color = color)
        }
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            PulsoProgressBar(entity.confidence.toFloat(), color)
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                Text(tr("Certeza de la detección", "Detection confidence"), style = PulsoType.caption, color = c.muted)
                if (event.reasons.isNotEmpty()) {
                    Text(
                        if (showWhy) tr("Ocultar motivo", "Hide reason") else tr("¿Por qué?", "Why?"),
                        style = PulsoType.caption.copy(fontWeight = FontWeight.ExtraBold),
                        color = c.primary,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable { showWhy = !showWhy }
                    )
                }
            }
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = showWhy,
            enter = androidx.compose.animation.expandVertically() + androidx.compose.animation.fadeIn(),
            exit = androidx.compose.animation.shrinkVertically() + androidx.compose.animation.fadeOut()
        ) {
            Column(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface2).padding(12.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                event.reasons.forEach { r ->
                    Text("• ${r.description}", style = PulsoType.bodySmall, color = c.onSurface)
                }
            }
        }
        if (!answered) {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PulsoButton(tr("Sí, fue tensión", "Yes, it was"), { onFeedback("REVISED_STRESSED") }, Modifier.weight(1f), style = PulsoButtonStyle.Tonal, height = 44.dp, textStyle = PulsoType.button.copy(fontSize = 14.sp))
                PulsoButton(tr("No lo fue", "It wasn’t"), { onFeedback("NOT_STRESSED") }, Modifier.weight(1f), style = PulsoButtonStyle.Ghost, height = 44.dp, textStyle = PulsoType.button.copy(fontSize = 14.sp, fontWeight = FontWeight.Bold))
            }
        } else {
            val label = when (entity.userFeedback) {
                "NOT_STRESSED" -> tr("Marcado como no tensión", "Marked as not tension")
                "TALK" -> tr("Confirmado · quisiste hablar", "Confirmed · you chose to talk")
                "BREATHE" -> tr("Confirmado · respiraste", "Confirmed · you breathed")
                else -> tr("Confirmado como tensión", "Confirmed as tension")
            }
            Row(
                Modifier.fillMaxWidth().clip(RoundedCornerShape(14.dp)).background(c.surface2).padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Icon(PulsoIcons.Check, null, tint = c.primary, modifier = Modifier.size(18.dp))
                Text(label, style = PulsoType.chip, color = c.onSurface, modifier = Modifier.weight(1f))
                val flip = if (entity.userFeedback == "NOT_STRESSED") "REVISED_STRESSED" else "NOT_STRESSED"
                Text(
                    tr("Cambiar", "Change"),
                    style = PulsoType.chip.copy(fontWeight = FontWeight.ExtraBold),
                    color = c.primary,
                    modifier = Modifier.clip(RoundedCornerShape(16.dp)).clickable { onFeedback(flip) }.padding(horizontal = 6.dp, vertical = 6.dp)
                )
            }
        }
    }
}

@Composable
private fun MapContent(
    header: @Composable () -> Unit,
    report: StressMapReport,
    sampleFeedback: String?,
    onGenerate: () -> Unit,
    onClearAll: () -> Unit
) {
    val c = Pulso.colors
    var confirmClear by remember { mutableStateOf(false) }
    val days = listOf(tr("L", "M"), tr("M", "T"), tr("X", "W"), tr("J", "T"), tr("V", "F"), tr("S", "S"), tr("D", "S"))
    val maxLoad = report.heatmap.maxOfOrNull { it.stressLoadRatio }?.takeIf { it > 0 } ?: 1.0
    val cells = remember(report) { report.heatmap.associateBy { it.dayOfWeek to it.hourOfDay } }
    val grow = com.pulso.mobile.presentation.components.drawInProgress(report.totalWindowsAnalyzed, 1100)
    val heatColor: (Double) -> Color = { v ->
        val k = v / maxLoad
        if (k < 0.12) c.surface2 else c.heart.copy(alpha = when {
            k < 0.35 -> 0.25f; k < 0.6 -> 0.45f; k < 0.85 -> 0.7f; else -> 1f
        })
    }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 18.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        header()
        if (!report.hasSufficientData) {
            PulsoCard(modifier = Modifier.reveal(1), horizontalAlignment = Alignment.CenterHorizontally, contentPadding = PaddingValues(24.dp)) {
                IconBadge(PulsoIcons.Grid, tint = c.hrv, background = c.hrvSoft, size = 48.dp, iconSize = 24.dp, radius = 16.dp)
                Text(tr("Tu mapa se está formando", "Your map is taking shape"), style = PulsoType.title, color = c.onSurface)
                Text(
                    tr("Con unos días de mediciones verás a qué horas sube tu tensión.", "After a few days of readings you’ll see when your tension tends to rise."),
                    style = PulsoType.bodySmall, color = c.muted, textAlign = TextAlign.Center
                )
            }
        } else {
            PulsoCard(modifier = Modifier.reveal(2), radius = 24.dp, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(tr("Tensión por hora", "Tension by hour"), style = PulsoType.title, color = c.onSurface)
                    Text(tr("${report.totalWindowsAnalyzed} mediciones", "${report.totalWindowsAnalyzed} readings"), style = PulsoType.caption, color = c.muted)
                }
                Column(verticalArrangement = Arrangement.spacedBy(3.dp)) {
                    days.forEachIndexed { di, d ->
                        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            Text(d, style = PulsoType.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.Bold), color = c.muted, modifier = Modifier.width(16.dp))
                            Canvas(Modifier.weight(1f).height(24.dp)) {
                                val gap = 2.dp.toPx()
                                val cw = (size.width - gap * 23) / 24
                                for (h in 0 until 24) {
                                    val v = cells[(di + 1) to h]?.stressLoadRatio ?: 0.0
                                    // Rows fade in top to bottom, cells left to right.
                                    val a = ((grow * 1.6f) - di * 0.08f - h * 0.012f).coerceIn(0f, 1f)
                                    drawRoundRect(heatColor(v).copy(alpha = heatColor(v).alpha * a), Offset(h * (cw + gap), 0f), Size(cw, size.height), CornerRadius(4.dp.toPx()))
                                }
                            }
                        }
                    }
                    Row(Modifier.fillMaxWidth().padding(start = 22.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                        listOf("0h", "6h", "12h", "18h", "23h").forEach {
                            Text(it, style = PulsoType.caption.copy(fontSize = 11.sp, fontWeight = FontWeight.SemiBold), color = c.muted)
                        }
                    }
                }
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(tr("Menos", "Less"), style = PulsoType.caption, color = c.muted)
                    listOf(0.0, 0.3, 0.5, 0.75, 1.0).forEach { k ->
                        Box(Modifier.width(18.dp).height(10.dp).clip(RoundedCornerShape(3.dp)).background(heatColor(k * maxLoad)))
                    }
                    Text(tr("Más", "More"), style = PulsoType.caption, color = c.muted)
                }
            }

            val loads = report.hourlyLoads.sortedBy { it.hourOfDay }
            val maxAvg = loads.maxOfOrNull { it.averageLoad }?.takeIf { it > 0 } ?: 1.0
            PulsoCard(modifier = Modifier.reveal(3), radius = 24.dp, verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                    Text(tr("Carga por hora", "Load by hour"), style = PulsoType.title, color = c.onSurface)
                    Text(tr("Promedio del periodo", "Period average"), style = PulsoType.caption, color = c.muted)
                }
                Row(Modifier.fillMaxWidth().height(76.dp), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
                    (0 until 24).forEach { h ->
                        val v = (loads.firstOrNull { it.hourOfDay == h }?.averageLoad ?: 0.0) / maxAvg
                        val col = when {
                            v > 0.75 -> c.heart; v > 0.45 -> c.warn; else -> c.primary
                        }
                        Box(
                            Modifier.weight(1f).fillMaxHeight((v.toFloat() * grow).coerceIn(0.04f, 1f))
                                .clip(RoundedCornerShape(topStart = 3.dp, topEnd = 3.dp, bottomStart = 1.dp, bottomEnd = 1.dp))
                                .background(col)
                        )
                    }
                }
            }

            if (report.neutralInsight.isNotBlank()) {
                PulsoCard(radius = 20.dp, background = c.warnSoft, border = null, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp)) {
                    Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                        Icon(PulsoIcons.Clock, null, tint = c.warn, modifier = Modifier.size(22.dp).padding(top = 1.dp))
                        Text(report.neutralInsight, style = PulsoType.bodySmall.copy(fontSize = 14.sp, fontWeight = FontWeight.SemiBold, lineHeight = 20.sp), color = c.onSurface)
                    }
                }
            }
        }

        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            PulsoButton(tr("Datos de ejemplo", "Sample data"), onGenerate, Modifier.weight(1f), style = PulsoButtonStyle.Ghost, height = 44.dp, textStyle = PulsoType.chip)
            PulsoButton(tr("Quitar ejemplo", "Remove sample"), { confirmClear = true }, Modifier.weight(1f), style = PulsoButtonStyle.DangerGhost, height = 44.dp, textStyle = PulsoType.chip)
        }
        Text(
            tr("Los datos de ejemplo están marcados: quitarlos no borra tus mediciones reales.", "Sample data is tagged: removing it never deletes your real readings."),
            style = PulsoType.caption, color = c.muted, textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth()
        )
        FeedbackText(sampleFeedback)
    }

    if (confirmClear) {
        PulsoConfirmSheet(
            icon = PulsoIcons.Grid,
            title = tr("¿Quitar los datos de ejemplo?", "Remove sample data?"),
            body = tr(
                "Solo se borran las mediciones, eventos y sesiones de ejemplo. Tus datos reales del reloj y tu consentimiento se mantienen.",
                "Only sample readings, events and sessions are removed. Your real watch data and consent are kept."
            ),
            confirmLabel = tr("Quitar", "Remove"),
            onConfirm = { confirmClear = false; onClearAll() },
            onDismiss = { confirmClear = false }
        )
    }
}

