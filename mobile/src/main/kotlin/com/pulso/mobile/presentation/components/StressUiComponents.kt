package com.pulso.mobile.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulso.core.model.QuietHours
import com.pulso.core.stress.StressConfig
import com.pulso.core.stress.StressEvent
import com.pulso.mobile.domain.stress.ReplaySimulationResult
import com.pulso.mobile.domain.stress.ReplayTimelinePoint
import com.pulso.mobile.domain.stress.SensitivityPreset
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.roundToInt

@Composable
fun SensitivitySettingsSection(
    currentConfig: StressConfig,
    currentPreset: SensitivityPreset,
    quietHours: QuietHours,
    isSaving: Boolean,
    saveFeedback: String?,
    qualityMetrics: com.pulso.mobile.domain.intervention.QualityMetrics? = null,
    adaptiveSuggestion: com.pulso.core.intervention.AdaptiveSuggestion? = null,
    onPresetSelected: (SensitivityPreset) -> Unit,
    onUpdateThresholds: (hrZ: Double, hrvZ: Double, hrOnlyZ: Double, consecA: Int, consecB: Int, cooldown: Int, dailyCap: Int) -> Unit,
    onUpdateQuietHours: (startH: Int, startM: Int, endH: Int, endM: Int) -> Unit,
    onResetDefaults: () -> Unit,
    onSaveConfig: () -> Unit,
    onOpenLab: () -> Unit,
    onCancelSnooze: () -> Unit = {},
    onApplyAdaptiveSuggestion: () -> Unit = {},
    onDismissAdaptiveSuggestion: () -> Unit = {}
) {
    var showAdvanced by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Text(
                text = "Sensibilidad de Detección",
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
                fontSize = 15.sp
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Define qué tan marcado debe ser el cambio en tu pulso y variabilidad antes de identificar una posible tensión.",
                fontSize = 12.sp,
                color = Color.LightGray
            )

            // Adaptive Sensitivity Suggestion Banner from watch
            adaptiveSuggestion?.let { suggestion ->
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E1B4B)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "💡 Sugerencia de sensibilidad adaptativa",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFA5B4FC)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = "Has marcado ${suggestion.consecutiveNotStressedCount} avisos consecutivos como 'No estoy estresado' en el horario ${suggestion.bucket.lowercase()}. ¿Deseas reducir la sensibilidad para evitar avisos innecesarios?",
                            fontSize = 12.sp,
                            color = Color(0xFFE0E7FF)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(8.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Button(
                                onClick = onApplyAdaptiveSuggestion,
                                modifier = Modifier.weight(1f).height(34.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4F46E5))
                            ) {
                                Text("Aplicar", fontSize = 11.sp, fontWeight = FontWeight.Bold)
                            }
                            OutlinedButton(
                                onClick = onDismissAdaptiveSuggestion,
                                modifier = Modifier.weight(1f).height(34.dp)
                            ) {
                                Text("Descartar", fontSize = 11.sp, color = Color.LightGray)
                            }
                        }
                    }
                }
            }

            // Quality Panel (Últimos 30 días)
            qualityMetrics?.let { metrics ->
                Spacer(modifier = Modifier.height(10.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Text(
                            text = "Precisión de Avisos (Últimos 30 días)",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        if (metrics.totalInterventions == 0) {
                            Text(
                                text = "Aún no hay avisos con respuestas registradas en este período.",
                                fontSize = 11.sp,
                                color = Color.Gray
                            )
                        } else {
                            Text(
                                text = "De los últimos 30 días: ${metrics.totalInterventions} avisos generados.",
                                fontSize = 12.sp,
                                color = Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "• Acertados: ${String.format(Locale.getDefault(), "%.0f", metrics.accuratePct)}% (${metrics.accurateCount})",
                                fontSize = 11.sp,
                                color = Color(0xFF4ADE80)
                            )
                            Text(
                                text = "• Falsas alarmas: ${String.format(Locale.getDefault(), "%.0f", metrics.falseAlarmPct)}% (${metrics.falseAlarmCount})",
                                fontSize = 11.sp,
                                color = Color(0xFFF87171)
                            )
                            Text(
                                text = "• Ignorados: ${String.format(Locale.getDefault(), "%.0f", metrics.ignoredPct)}% (${metrics.ignoredCount})",
                                fontSize = 11.sp,
                                color = Color.LightGray
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Presets Selector (Baja / Media / Alta)
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(SensitivityPreset.BAJA, SensitivityPreset.MEDIA, SensitivityPreset.ALTA).forEach { preset ->
                    val isSelected = currentPreset == preset
                    Button(
                        onClick = { onPresetSelected(preset) },
                        modifier = Modifier.weight(1f).height(38.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isSelected) Color(0xFF0284C7) else Color(0xFF334155)
                        )
                    ) {
                        Text(
                            text = preset.displayName,
                            fontSize = 12.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                            color = Color.White
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = currentPreset.description,
                fontSize = 11.sp,
                color = Color(0xFF94A3B8)
            )

            Spacer(modifier = Modifier.height(14.dp))

            // Horas silenciosas y límite diario
            Text("Horas de Silencio (Sin Avisos)", fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 13.sp)
            Spacer(modifier = Modifier.height(4.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = "Desde ${String.format(Locale.getDefault(), "%02d:%02d", quietHours.startHour, quietHours.startMinute)} hasta ${String.format(Locale.getDefault(), "%02d:%02d", quietHours.endHour, quietHours.endMinute)}",
                    fontSize = 12.sp,
                    color = Color(0xFFFBBF24)
                )
                OutlinedButton(
                    onClick = {
                        // Toggle between standard sleep 22:00-07:00 and 23:00-08:00
                        if (quietHours.startHour == 22) onUpdateQuietHours(23, 0, 8, 0)
                        else onUpdateQuietHours(22, 0, 7, 0)
                    },
                    modifier = Modifier.height(32.dp)
                ) {
                    Text("Cambiar", fontSize = 11.sp, color = Color(0xFF38BDF8))
                }
            }

            // Current Snooze / Pausa de avisos
            val currentSnooze = currentConfig.snoozeUntil
            if (currentSnooze != null && currentSnooze > System.currentTimeMillis()) {
                val remainingMin = ((currentSnooze - System.currentTimeMillis()) / 60000).coerceAtLeast(1)
                Spacer(modifier = Modifier.height(8.dp))
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF451A03)),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Row(
                        modifier = Modifier.padding(10.dp).fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = "⏸️ Avisos pausados ($remainingMin min restantes)",
                            fontSize = 12.sp,
                            color = Color(0xFFFDBA74),
                            fontWeight = FontWeight.SemiBold
                        )
                        OutlinedButton(
                            onClick = onResetDefaults, // or cancel snooze
                            modifier = Modifier.height(28.dp)
                        ) {
                            Text("Reanudar", fontSize = 10.sp, color = Color.White)
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Límite diario de avisos:", fontSize = 12.sp, color = Color.LightGray)
                Text("${currentConfig.dailyCap} por día", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Advanced Toggle
            Button(
                onClick = { showAdvanced = !showAdvanced },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth().height(36.dp)
            ) {
                Text(
                    text = if (showAdvanced) "▲ Ocultar sección avanzada" else "▼ Configuración avanzada (Umbrales)",
                    fontSize = 12.sp,
                    color = Color(0xFF38BDF8)
                )
            }

            if (showAdvanced) {
                Spacer(modifier = Modifier.height(12.dp))

                // Slider HR Z
                Text(
                    text = "Umbral elevación pulso: +${String.format(Locale.getDefault(), "%.1f", currentConfig.hrZThreshold)} desviaciones",
                    fontSize = 12.sp,
                    color = Color.White
                )
                Slider(
                    value = currentConfig.hrZThreshold.toFloat(),
                    onValueChange = { onUpdateThresholds(it.toDouble(), currentConfig.hrvZThreshold, currentConfig.hrOnlyZThreshold, currentConfig.consecutiveWindowsRuleA, currentConfig.consecutiveWindowsRuleB, currentConfig.cooldownMinutes, currentConfig.dailyCap) },
                    valueRange = 0.5f..2.5f,
                    steps = 19,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF0284C7))
                )

                // Slider HRV Z
                Text(
                    text = "Umbral caída de HRV: ${String.format(Locale.getDefault(), "%.1f", currentConfig.hrvZThreshold)} desviaciones",
                    fontSize = 12.sp,
                    color = Color.White
                )
                Slider(
                    value = currentConfig.hrvZThreshold.toFloat(),
                    onValueChange = { onUpdateThresholds(currentConfig.hrZThreshold, it.toDouble(), currentConfig.hrOnlyZThreshold, currentConfig.consecutiveWindowsRuleA, currentConfig.consecutiveWindowsRuleB, currentConfig.cooldownMinutes, currentConfig.dailyCap) },
                    valueRange = -2.5f..-0.5f,
                    steps = 19,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF0284C7))
                )

                // Consecutive windows Rule A
                Text(
                    text = "Ventanas consecutivas requeridas: ${currentConfig.consecutiveWindowsRuleA} (~${currentConfig.consecutiveWindowsRuleA * 5} min)",
                    fontSize = 12.sp,
                    color = Color.White
                )
                Slider(
                    value = currentConfig.consecutiveWindowsRuleA.toFloat(),
                    onValueChange = { onUpdateThresholds(currentConfig.hrZThreshold, currentConfig.hrvZThreshold, currentConfig.hrOnlyZThreshold, it.roundToInt(), currentConfig.consecutiveWindowsRuleB, currentConfig.cooldownMinutes, currentConfig.dailyCap) },
                    valueRange = 2f..6f,
                    steps = 3,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF0284C7))
                )

                // Cooldown
                Text(
                    text = "Tiempo de espera entre avisos: ${currentConfig.cooldownMinutes} min",
                    fontSize = 12.sp,
                    color = Color.White
                )
                Slider(
                    value = currentConfig.cooldownMinutes.toFloat(),
                    onValueChange = { onUpdateThresholds(currentConfig.hrZThreshold, currentConfig.hrvZThreshold, currentConfig.hrOnlyZThreshold, currentConfig.consecutiveWindowsRuleA, currentConfig.consecutiveWindowsRuleB, it.roundToInt(), currentConfig.dailyCap) },
                    valueRange = 20f..180f,
                    steps = 15,
                    colors = SliderDefaults.colors(thumbColor = Color(0xFF38BDF8), activeTrackColor = Color(0xFF0284C7))
                )

                Spacer(modifier = Modifier.height(6.dp))
                OutlinedButton(
                    onClick = onResetDefaults,
                    modifier = Modifier.fillMaxWidth().height(36.dp)
                ) {
                    Text("Restablecer valores predeterminados", fontSize = 11.sp, color = Color(0xFFF87171))
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Save Button
            Button(
                onClick = onSaveConfig,
                enabled = !isSaving,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
            ) {
                if (isSaving) {
                    CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                } else {
                    Text("Guardar y sincronizar con reloj", fontWeight = FontWeight.Bold, fontSize = 13.sp)
                }
            }

            saveFeedback?.let {
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = it,
                    fontSize = 11.sp,
                    color = if (it.contains("exitos")) Color(0xFF4ADE80) else Color(0xFFFBBF24)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Replay Lab Entry Button
            Button(
                onClick = onOpenLab,
                modifier = Modifier.fillMaxWidth().height(40.dp),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA))
            ) {
                Text("🔬 Abrir Laboratorio de Simulación (Replay)", fontSize = 12.sp, fontWeight = FontWeight.Bold)
            }
        }
    }
}

@Composable
fun StressEventListScreen(
    entities: List<com.pulso.mobile.data.local.PhoneStressEventEntity>,
    onUpdateFeedback: (eventId: String, feedback: String) -> Unit,
    onBack: () -> Unit
) {
    val timeFormat = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Eventos de Tensión", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.height(34.dp)
            ) {
                Text("‹ Volver", fontSize = 12.sp, color = Color(0xFF38BDF8))
            }
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (entities.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    "No se han registrado eventos de tensión aún.",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                items(entities) { entity ->
                    StressEventCard(
                        entity = entity,
                        timeFormat = timeFormat,
                        onUpdateFeedback = { fb -> onUpdateFeedback(entity.id, fb) }
                    )
                }
            }
        }
    }
}

@Composable
fun StressEventCard(
    entity: com.pulso.mobile.data.local.PhoneStressEventEntity,
    timeFormat: SimpleDateFormat,
    onUpdateFeedback: (String) -> Unit
) {
    val event = entity.toDomain()
    var expanded by remember { mutableStateOf(false) }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = timeFormat.format(Date(event.time)),
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                    val ruleDescription = when {
                        event.reasons.any { it.code == com.pulso.core.stress.StressReasonCode.ELEVATED_HR_LOW_HRV } -> "Pulso elevado + HRV bajo"
                        event.reasons.any { it.code == com.pulso.core.stress.StressReasonCode.ELEVATED_HR_ONLY } -> "Pulso sostenido (sin HRV)"
                        else -> "Patrón de tensión"
                    }
                    Text(
                        text = "Detección: $ruleDescription",
                        fontSize = 11.sp,
                        color = Color(0xFF94A3B8)
                    )
                }

                // Confidence badge
                val pct = (event.confidence * 100).toInt()
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (pct > 75) Color(0xFFDC2626) else Color(0xFFD97706))
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("$pct% certeza", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // User Feedback Status Row
            val feedbackLabel = when (entity.userFeedback) {
                "NOT_STRESSED" -> "❌ Marcado como: No estaba estresado"
                "TALK" -> "💬 Marcado como: Quería hablar"
                "BREATHE" -> "🫁 Marcado como: Respirar"
                "DISMISS" -> "⏳ Marcado como: Ahora no"
                "IGNORED" -> "⚪ Descartado automáticamente (sin respuesta)"
                "REVISED_STRESSED" -> "✅ Rectificado: En realidad sí estaba estresado"
                else -> "Sin respuesta registrada aún"
            }
            Text(
                text = "Tu respuesta: $feedbackLabel",
                fontSize = 12.sp,
                color = when (entity.userFeedback) {
                    "NOT_STRESSED" -> Color(0xFFF87171)
                    "TALK", "BREATHE", "REVISED_STRESSED" -> Color(0xFF4ADE80)
                    else -> Color(0xFFFBBF24)
                },
                fontWeight = FontWeight.SemiBold
            )

            // Let user change feedback if marked as not stressed or ignored
            if (entity.userFeedback == "NOT_STRESSED" || entity.userFeedback == "IGNORED" || entity.userFeedback == "DISMISS") {
                Spacer(modifier = Modifier.height(4.dp))
                Button(
                    onClick = { onUpdateFeedback("REVISED_STRESSED") },
                    modifier = Modifier.height(30.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                ) {
                    Text("✏️ En realidad sí estaba estresado", fontSize = 10.sp, color = Color(0xFF38BDF8))
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Plain-language summary
            Text(
                text = "¿Por qué se activó?",
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFF38BDF8)
            )
            Spacer(modifier = Modifier.height(2.dp))

            for (reason in event.reasons) {
                Text(
                    text = "• ${reason.description}",
                    fontSize = 12.sp,
                    color = Color(0xFFE2E8F0)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            OutlinedButton(
                onClick = { expanded = !expanded },
                modifier = Modifier.fillMaxWidth().height(32.dp)
            ) {
                Text(
                    text = if (expanded) "Ocultar detalles técnicos" else "Ver detalles técnicos",
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8)
                )
            }

            if (expanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Divider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(8.dp))
                Text("• Versión de configuración: v${event.configVersion}", fontSize = 11.sp, color = Color.LightGray)
                Text("• Ventanas usadas: ${event.windowsUsed.size} intervalos de 5 min", fontSize = 11.sp, color = Color.LightGray)
                for (reason in event.reasons) {
                    if (reason.hrZ != null || reason.hrvZ != null) {
                        val hrZStr = reason.hrZ?.let { "Z(pulso): ${String.format(Locale.getDefault(), "%.2f", it)}" } ?: ""
                        val hrvZStr = reason.hrvZ?.let { "Z(HRV): ${String.format(Locale.getDefault(), "%.2f", it)}" } ?: ""
                        val joined = listOf(hrZStr, hrvZStr).filter { it.isNotEmpty() }.joinToString(" | ")
                        Text("  - $joined", fontSize = 11.sp, color = Color(0xFF38BDF8))
                    }
                }
            }
        }
    }
}

@Composable
fun ReplayLabScreen(
    currentConfig: StressConfig,
    daysRange: Int,
    isRunning: Boolean,
    result: ReplaySimulationResult?,
    feedback: String?,
    onDaysRangeChange: (Int) -> Unit,
    onRunReplay: () -> Unit,
    onApplyConfig: (StressConfig) -> Unit,
    onBack: () -> Unit
) {
    var selectedPoint by remember { mutableStateOf<ReplayTimelinePoint?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Laboratorio (Replay)", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Button(
                onClick = onBack,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.height(34.dp)
            ) {
                Text("‹ Cerrar", fontSize = 12.sp, color = Color(0xFF38BDF8))
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Text("Simula la detección con tus mediciones reales pasadas.", fontSize = 12.sp, color = Color.LightGray)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    listOf(1, 3, 7, 14).forEach { days ->
                        val isSel = daysRange == days
                        Button(
                            onClick = { onDaysRangeChange(days) },
                            modifier = Modifier.weight(1f).height(34.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSel) Color(0xFF0284C7) else Color(0xFF334155)
                            )
                        ) {
                            Text("${days}d", fontSize = 11.sp, color = Color.White)
                        }
                    }
                }
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onRunReplay,
                    enabled = !isRunning,
                    modifier = Modifier.fillMaxWidth().height(40.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF4338CA))
                ) {
                    if (isRunning) {
                        CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
                    } else {
                        Text("Ejecutar simulación en historial", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        feedback?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = Color(0xFFFBBF24), fontSize = 12.sp)
        }

        Spacer(modifier = Modifier.height(12.dp))

        if (result != null) {
            // Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Resultados de la simulación", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text("• Ventanas analizadas: ${result.totalWindowsAnalyzed}", fontSize = 12.sp, color = Color.White)
                    Text("• Con esta configuración, Pulso te habría avisado ${result.totalTriggers} veces.", fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF4ADE80))
                    Text("• Avisos bloqueados por filtros: ${result.totalSuppressedRuleMatches}", fontSize = 12.sp, color = Color.LightGray)

                    if (result.topSuppressionReasons.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(6.dp))
                        Text("Filtros más frecuentes:", fontSize = 11.sp, color = Color(0xFFFBBF24), fontWeight = FontWeight.SemiBold)
                        result.topSuppressionReasons.entries.take(3).forEach { (reason, count) ->
                            Text("  - $reason: $count veces", fontSize = 11.sp, color = Color.LightGray)
                        }
                    }

                    Spacer(modifier = Modifier.height(10.dp))
                    Button(
                        onClick = { onApplyConfig(currentConfig) },
                        modifier = Modifier.fillMaxWidth().height(36.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
                    ) {
                        Text("Usar esta configuración", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            // Timeline points list
            Text("Línea de tiempo de eventos evaluados", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = Color.White)
            Spacer(modifier = Modifier.height(6.dp))

            val timeFmt = remember { SimpleDateFormat("HH:mm", Locale.getDefault()) }
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                verticalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                items(result.timeline.filter { it.decision.triggered || it.decision.suppressedBy.isNotEmpty() }) { point ->
                    val isTriggered = point.decision.triggered
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isTriggered) Color(0xFF450A0A) else Color(0xFF1E293B)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = timeFmt.format(Date(point.window.windowStart)) + " · ${point.window.meanHr.toInt()} BPM",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color.White
                                )
                                Text(
                                    text = if (isTriggered) "AVISO" else "BLOQUEADO",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isTriggered) Color(0xFFEF4444) else Color(0xFFFBBF24)
                                )
                            }
                            if (point.decision.suppressedBy.isNotEmpty()) {
                                Spacer(modifier = Modifier.height(2.dp))
                                Text(
                                    text = "Motivo: " + point.decision.suppressedBy.joinToString(", ") { it.detail },
                                    fontSize = 11.sp,
                                    color = Color.LightGray
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
