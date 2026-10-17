package com.pulso.mobile.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulso.core.breathing.BreathingConfig
import com.pulso.core.breathing.BreathingPlan
import com.pulso.mobile.data.local.PhoneBreathingSessionEntity
import com.pulso.mobile.domain.breathing.BreathingWeeklySummary
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun BreathingScreen(
    currentConfig: BreathingConfig,
    sessions: List<PhoneBreathingSessionEntity>,
    weeklySummary: BreathingWeeklySummary,
    isTestingOnWatch: Boolean,
    testingFeedback: String?,
    onUpdatePlan: (String) -> Unit,
    onUpdateDuration: (Int) -> Unit,
    onUpdateIntensity: (String) -> Unit,
    onTestOnWatch: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Respiración Guiada", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(12.dp))

        // Weekly Summary Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Resumen Semanal", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(6.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Column {
                        Text("${weeklySummary.totalSessionsThisWeek} sesiones", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
                        Text("Últimos 7 días", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column {
                        Text("${weeklySummary.totalMinutesBreathed} min", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                        Text("Tiempo total", fontSize = 11.sp, color = Color.Gray)
                    }
                    Column {
                        Text("${weeklySummary.averageFeelingBetterPct}%", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color(0xFFFBBF24))
                        Text("Se sintieron mejor", fontSize = 11.sp, color = Color.Gray)
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Plan Selection Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Patrón de Respiración", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(8.dp))

                val plans = listOf(
                    Triple("Calma 4-6", "Inhala 4s, exhala 6s. El ritmo más natural y efectivo para activar el sistema nervioso parasimpático y reducir la tensión.", "Calma 4-6"),
                    Triple("Cuadrada 4-4-4-4", "Inhala 4s, sostén 4s, exhala 4s, pausa 4s. Conocida como respiración de caja (box breathing), ideal para recuperar el enfoque y la estabilidad mental.", "Cuadrada 4-4-4-4"),
                    Triple("Relajante 4-7-8", "Inhala 4s, sostén 7s, exhala 8s. Técnica avanzada de relajación profunda para momentos de alta inquietud o antes de dormir.", "Relajante 4-7-8")
                )

                plans.forEach { (name, desc, planId) ->
                    val isSelected = currentConfig.preferredPlanName == planId
                    Card(
                        colors = CardDefaults.cardColors(
                            containerColor = if (isSelected) Color(0xFF0F2B48) else Color(0xFF0F172A)
                        ),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 4.dp)
                            .clickable { onUpdatePlan(planId) }
                    ) {
                        Column(modifier = Modifier.padding(12.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) Color(0xFF38BDF8) else Color.White
                                )
                                if (isSelected) {
                                    Text("Activo", fontSize = 11.sp, color = Color(0xFF38BDF8), fontWeight = FontWeight.SemiBold)
                                }
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(desc, fontSize = 11.sp, color = Color(0xFF94A3B8), lineHeight = 15.sp)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Duration & Intensity Card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Duración de la sesión", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf(1, 2, 5).forEach { mins ->
                        val isSelected = currentConfig.durationMinutes == mins
                        Button(
                            onClick = { onUpdateDuration(mins) },
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF0284C7) else Color(0xFF0F172A)
                            )
                        ) {
                            Text("$mins min", fontSize = 12.sp, color = if (isSelected) Color.White else Color.LightGray)
                        }
                    }
                }

                Spacer(modifier = Modifier.height(12.dp))
                HorizontalDivider(color = Color(0xFF334155))
                Spacer(modifier = Modifier.height(12.dp))

                Text("Intensidad de vibración en el reloj", fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    listOf("LOW" to "Baja", "MEDIUM" to "Media", "HIGH" to "Alta").forEach { (key, label) ->
                        val isSelected = currentConfig.hapticIntensity == key
                        Button(
                            onClick = { onUpdateIntensity(key) },
                            modifier = Modifier.weight(1f).height(38.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = if (isSelected) Color(0xFF0284C7) else Color(0xFF0F172A)
                            )
                        ) {
                            Text(label, fontSize = 12.sp, color = if (isSelected) Color.White else Color.LightGray)
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Phone Visual Rhythm Preview (No sound)
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(14.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text("Vista previa visual del ritmo", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 13.sp)
                Text("Sincronizada con el patrón seleccionado (sin sonido)", fontSize = 11.sp, color = Color.Gray)
                Spacer(modifier = Modifier.height(12.dp))

                BreathingCirclePreview(plan = currentConfig.toPlan())
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        // Probar en el reloj Button
        Button(
            onClick = onTestOnWatch,
            enabled = !isTestingOnWatch,
            modifier = Modifier.fillMaxWidth().height(46.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
        ) {
            if (isTestingOnWatch) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White, modifier = Modifier.size(20.dp))
            } else {
                Text("Iniciar en Smartwatch", fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }

        testingFeedback?.let {
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = it,
                fontSize = 12.sp,
                color = if (it.contains("iniciada")) Color(0xFF4ADE80) else Color(0xFFEF4444),
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        // History Section
        Text("Historial de Sesiones", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(8.dp))

        if (sessions.isEmpty()) {
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Box(modifier = Modifier.padding(20.dp).fillMaxWidth(), contentAlignment = Alignment.Center) {
                    Text("No hay sesiones de respiración registradas aún.", color = Color.Gray, fontSize = 13.sp)
                }
            }
        } else {
            val dateFmt = remember { SimpleDateFormat("dd MMM, HH:mm", Locale.getDefault()) }
            sessions.take(15).forEach { session ->
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp)
                ) {
                    Column(modifier = Modifier.padding(12.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(session.planName, fontWeight = FontWeight.Bold, color = Color.White, fontSize = 13.sp)
                            Text(dateFmt.format(Date(session.completedAt)), fontSize = 11.sp, color = Color.Gray)
                        }

                        Spacer(modifier = Modifier.height(4.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Ciclos: ${session.completedCycles}/${session.targetCycles} (${if (session.endedBy == "COMPLETED") "Completado" else "Detenido antes"})",
                                fontSize = 11.sp,
                                color = if (session.endedBy == "COMPLETED") Color(0xFF4ADE80) else Color(0xFFFBBF24)
                            )
                            val feelingText = when (session.feeling) {
                                "BETTER" -> "Mejor"
                                "SAME" -> "Igual"
                                "WORSE" -> "Peor"
                                else -> "Sin reporte"
                            }
                            Text(feelingText, fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = Color(0xFF38BDF8))
                        }

                        if (session.hrBefore != null || session.hrAfter != null) {
                            Spacer(modifier = Modifier.height(4.dp))
                            val hrB = session.hrBefore?.let { "${it.toInt()} BPM" } ?: "--"
                            val hrA = session.hrAfter?.let { "${it.toInt()} BPM" } ?: "--"
                            Text("Pulso: antes $hrB -> despues $hrA", fontSize = 11.sp, color = Color.LightGray)
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun BreathingCirclePreview(plan: BreathingPlan) {
    var phase by remember { mutableStateOf("Inhala") }
    var targetScale by remember { mutableStateOf(0.4f) }
    var animDuration by remember { mutableStateOf(4000) }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = animDuration, easing = FastOutSlowInEasing),
        label = "phoneCirclePreview"
    )

    LaunchedEffect(plan) {
        while (true) {
            // INHALE
            phase = "Inhala"
            animDuration = plan.inhaleMs.toInt()
            targetScale = 1.0f
            delay(plan.inhaleMs)

            // HOLD
            if (plan.holdAfterInhaleMs > 0) {
                phase = "Sostén"
                delay(plan.holdAfterInhaleMs)
            }

            // EXHALE
            phase = "Exhala"
            animDuration = plan.exhaleMs.toInt()
            targetScale = 0.4f
            delay(plan.exhaleMs)

            // PAUSE
            if (plan.holdAfterExhaleMs > 0) {
                phase = "Pausa"
                delay(plan.holdAfterExhaleMs)
            }
        }
    }

    Box(
        modifier = Modifier.size(130.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size((130 * animatedScale).dp)
                .clip(CircleShape)
                .background(Color(0xFF0284C7).copy(alpha = 0.25f))
        )
        Box(
            modifier = Modifier
                .size((100 * animatedScale).dp)
                .clip(CircleShape)
                .background(Color(0xFF38BDF8).copy(alpha = 0.45f))
        )
        Text(
            text = phase,
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}
