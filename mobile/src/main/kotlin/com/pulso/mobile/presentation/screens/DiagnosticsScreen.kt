package com.pulso.mobile.presentation.screens

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.CornerRadius
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
import com.pulso.mobile.data.sync.MobileDiagnosticsState
import com.pulso.mobile.presentation.components.reveal
import com.pulso.mobile.presentation.components.BackHeader
import com.pulso.mobile.presentation.components.FeedbackText
import com.pulso.mobile.presentation.components.ListRow
import com.pulso.mobile.presentation.components.PulsoButton
import com.pulso.mobile.presentation.components.PulsoCard
import com.pulso.mobile.presentation.components.StatusPill
import com.pulso.mobile.presentation.i18n.LocalAppLanguage
import com.pulso.mobile.presentation.i18n.formatShortDate
import com.pulso.mobile.presentation.i18n.minutesAgoLabel
import com.pulso.mobile.presentation.i18n.tr
import com.pulso.mobile.presentation.theme.Pulso
import com.pulso.mobile.presentation.theme.PulsoIcons
import com.pulso.mobile.presentation.theme.PulsoType

@Composable
fun DiagnosticsScreen(
    diagnostics: MobileDiagnosticsState,
    baselineMismatch: String?,
    isTesting: Boolean,
    pingFeedback: String?,
    onTestConnection: () -> Unit,
    onBack: () -> Unit
) {
    val c = Pulso.colors
    val lang = LocalAppLanguage.current
    val status = diagnostics.lastWatchStatus
    val lastSample = diagnostics.lastSampleReceived?.windowEnd

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 24.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        BackHeader(tr("Diagnóstico del reloj", "Watch diagnostics"), onBack)

        PulsoCard(modifier = Modifier.reveal(1), radius = 26.dp, contentPadding = PaddingValues(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
            WatchIllustration(connected = diagnostics.watchConnected)
            if (diagnostics.watchConnected) {
                StatusPill(tr("Conectado", "Connected"), c.primary, c.primarySoft)
            } else {
                StatusPill(tr("Sin conexión", "Not connected"), c.warn, c.warnSoft)
            }
            Text(
                lastSample?.let {
                    tr("Última medición: ", "Last reading: ") + minutesAgoLabel(((System.currentTimeMillis() - it) / 60_000).coerceAtLeast(0), lang).replaceFirstChar { ch -> ch.lowercase() }
                } ?: tr("Aún no llegan mediciones", "No readings received yet"),
                style = PulsoType.bodySmall, color = c.muted
            )
        }

        baselineMismatch?.let {
            PulsoCard(background = c.heartSoft, border = null, contentPadding = PaddingValues(horizontal = 16.dp, vertical = 14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Row(horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(PulsoIcons.Info, null, tint = c.heart, modifier = Modifier.size(20.dp))
                    Text(tr("Línea base distinta en reloj y celular", "Baseline differs between watch and phone"), style = PulsoType.label.copy(fontSize = 14.sp), color = c.heart)
                }
                Text(it, style = PulsoType.bodySmall, color = c.onSurface)
            }
        }

        PulsoCard(modifier = Modifier.reveal(2), contentPadding = PaddingValues(horizontal = 16.dp, vertical = 4.dp), verticalArrangement = Arrangement.spacedBy(0.dp)) {
            val rows = buildList {
                add(Row4(PulsoIcons.Link, tr("Conexión", "Connection"),
                    diagnostics.connectedWatchNodeName?.takeIf { diagnostics.watchConnected } ?: tr("No conectado", "Not connected"),
                    if (diagnostics.watchConnected) c.primary else c.warn))
                add(Row4(PulsoIcons.Battery, tr("Batería del reloj", "Watch battery"), status?.let { "${it.batteryPct} %" } ?: "—", c.onSurface))
                add(Row4(PulsoIcons.Heart, tr("Servicio de medición", "Measuring service"),
                    when (status?.serviceRunning) { true -> tr("Activo", "Active"); false -> tr("Detenido", "Stopped"); null -> "—" },
                    if (status?.serviceRunning == true) c.primary else c.muted))
                add(Row4(PulsoIcons.Sync, tr("Mediciones guardadas", "Stored readings"), "${diagnostics.totalWindowsStored}", c.onSurface))
                add(Row4(PulsoIcons.Vibration, tr("Vibración", "Haptics"), diagnostics.watchHapticCapability ?: "—", c.onSurface))
                add(Row4(PulsoIcons.Shield, tr("Consentimiento", "Consent"),
                    if (diagnostics.consentGranted) tr("Aceptado", "Granted") + (diagnostics.consentTimestamp?.let { " · " + formatShortDate(it, lang) } ?: "") else tr("No otorgado", "Not granted"),
                    if (diagnostics.consentGranted) c.primary else c.heart))
                add(Row4(PulsoIcons.Info, tr("Modelo del reloj", "Watch model"), status?.watchModel ?: "—", c.muted))
            }
            rows.forEachIndexed { i, r ->
                ListRow(label = r.label, icon = r.icon, iconTint = c.muted, showDivider = i < rows.lastIndex, minHeight = 52.dp) {
                    Text(
                        r.value,
                        style = PulsoType.label.copy(fontSize = 14.sp, fontWeight = FontWeight.ExtraBold, fontFeatureSettings = "tnum"),
                        color = r.color, maxLines = 2,
                        modifier = Modifier.padding(start = 8.dp)
                    )
                }
            }
        }

        val t = rememberInfiniteTransition(label = "spin")
        val angle by t.animateFloat(0f, 360f, infiniteRepeatable(tween(1200, easing = LinearEasing), RepeatMode.Restart), label = "angle")
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            PulsoButton(
                if (isTesting) tr("Probando…", "Testing…") else tr("Probar conexión", "Test connection"),
                onTestConnection,
                Modifier.weight(1f),
                icon = null,
                enabled = !isTesting
            )
        }
        if (isTesting) {
            Row(Modifier.padding(horizontal = 4.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Icon(PulsoIcons.Sync, null, tint = c.primary, modifier = Modifier.size(16.dp).rotate(angle))
                Text(tr("Enviando ping al reloj…", "Pinging the watch…"), style = PulsoType.caption, color = c.muted)
            }
        }
        FeedbackText(pingFeedback, positive = pingFeedback?.contains("exitoso") == true)
        com.pulso.mobile.presentation.i18n.localizeStatus(diagnostics.lastError)?.takeIf { pingFeedback == null || !pingFeedback.contains("exitoso") }?.let {
            Text(
                tr("Último problema: ", "Last issue: ") + it,
                style = PulsoType.caption, color = c.muted,
                textAlign = androidx.compose.ui.text.style.TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

private data class Row4(val icon: androidx.compose.ui.graphics.vector.ImageVector, val label: String, val value: String, val color: Color)

@Composable
private fun WatchIllustration(connected: Boolean) {
    val c = Pulso.colors
    Canvas(Modifier.size(150.dp)) {
        val s = size.width / 150f
        drawCircle(c.primary.copy(alpha = 0.18f), 72 * s, style = Stroke(2 * s))
        drawRoundRect(c.surface2, Offset(55 * s, 6 * s), Size(40 * s, 26 * s), CornerRadius(8 * s))
        drawRoundRect(c.surface2, Offset(55 * s, 118 * s), Size(40 * s, 26 * s), CornerRadius(8 * s))
        drawCircle(Color.Black, 50 * s)
        drawCircle(c.outline, 50 * s, style = Stroke(5 * s))
        drawRoundRect(c.outline, Offset(126 * s, 66 * s), Size(7 * s, 18 * s), CornerRadius(3 * s))
        val ring = if (connected) Color(0xFF3DD6B5) else Color(0xFF93A4B1)
        drawArc(ring, -90f, 225f, false, Offset(37 * s, 37 * s), Size(76 * s, 76 * s), style = Stroke(4 * s, cap = StrokeCap.Round))
        val p = Path().apply {
            moveTo(53 * s, 77 * s); lineTo(63 * s, 77 * s); lineTo(67 * s, 68 * s)
            lineTo(73 * s, 85 * s); lineTo(77 * s, 75 * s); lineTo(97 * s, 75 * s)
        }
        drawPath(p, Color(0xFFFF7A66), style = Stroke(3.5f * s, cap = StrokeCap.Round, join = StrokeJoin.Round))
    }
}
