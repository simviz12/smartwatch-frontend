package com.pulso.wear.presentation.components

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.pulso.core.breathing.BreathingPlan
import com.pulso.core.breathing.BreathingSessionSnapshot
import com.pulso.core.breathing.BreathingSessionState
import com.pulso.core.breathing.TimelineSegmentType
import com.pulso.wear.data.breathing.BreathingDisplayMode

@Composable
fun BreathingSessionScreen(
    snapshot: BreathingSessionSnapshot?,
    displayMode: BreathingDisplayMode,
    liveHr: Double?,
    onToggleDisplayMode: () -> Unit,
    onStartSession: (plan: BreathingPlan, minutes: Int, mode: BreathingDisplayMode) -> Unit,
    onStopSession: () -> Unit,
    onSaveFeeling: (String) -> Unit,
    onFinishScreen: () -> Unit
) {
    var selectedPlanType by remember { mutableStateOf("Calma 4-6") }
    var selectedMinutes by remember { mutableStateOf(2) }
    var showFeelingPrompt by remember { mutableStateOf(false) }

    val state = snapshot?.state ?: BreathingSessionState.IDLE

    if (state == BreathingSessionState.COMPLETED || state == BreathingSessionState.STOPPED) {
        if (!showFeelingPrompt) {
            showFeelingPrompt = true
        }
    }

    Scaffold(
        timeText = { if (displayMode != BreathingDisplayMode.VIBRATION_ONLY || state == BreathingSessionState.IDLE) TimeText() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black)
                .pointerInput(state) {
                    detectTapGestures(
                        onTap = {
                            if (state == BreathingSessionState.RUNNING) {
                                onStopSession()
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            when {
                showFeelingPrompt -> {
                    FeelingEvaluationScreen(
                        completedCycles = snapshot?.currentCycle ?: 0,
                        totalCycles = snapshot?.totalCycles ?: 0,
                        liveHr = liveHr,
                        onSelectFeeling = { feeling ->
                            onSaveFeeling(feeling)
                            showFeelingPrompt = false
                            onFinishScreen()
                        },
                        onSkip = {
                            showFeelingPrompt = false
                            onFinishScreen()
                        }
                    )
                }

                state == BreathingSessionState.IDLE -> {
                    BreathingSetupScreen(
                        selectedPlan = selectedPlanType,
                        selectedMinutes = selectedMinutes,
                        displayMode = displayMode,
                        liveHr = liveHr,
                        onSelectPlan = { selectedPlanType = it },
                        onSelectMinutes = { selectedMinutes = it },
                        onToggleDisplayMode = onToggleDisplayMode,
                        onStart = {
                            val base = when (selectedPlanType) {
                                "Cuadrada 4-4-4-4" -> BreathingPlan.cuadrada4444()
                                "Relajante 4-7-8" -> BreathingPlan.relajante478()
                                else -> BreathingPlan.calma46()
                            }
                            val plan = BreathingPlan.withTargetDuration(base, selectedMinutes)
                            onStartSession(plan, selectedMinutes, displayMode)
                        },
                        onBack = onFinishScreen
                    )
                }

                state == BreathingSessionState.RUNNING || state == BreathingSessionState.PAUSED -> {
                    if (displayMode == BreathingDisplayMode.VIBRATION_ONLY) {
                        // Screen stays dark / minimal ambient hint to save eyes and battery
                        VibrationOnlyScreen(
                            snapshot = snapshot,
                            onStop = onStopSession
                        )
                    } else {
                        VisualGuideScreen(
                            snapshot = snapshot,
                            onStop = onStopSession
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VibrationOnlyScreen(
    snapshot: BreathingSessionSnapshot?,
    onStop: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF131C24))
                .padding(horizontal = 8.dp, vertical = 4.dp)
        ) {
            Text(
                text = "MODO HÁPTICO SILENCIOSO",
                fontSize = 8.sp,
                color = Color(0xFF64748B),
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
        Spacer(modifier = Modifier.height(10.dp))
        Text(
            text = "Cierra los ojos",
            fontSize = 15.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFFF1F5F9)
        )
        Spacer(modifier = Modifier.height(3.dp))
        Text(
            text = "Sigue la vibración en tu muñeca",
            fontSize = 10.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(Color(0xFF1E293B))
                .clickable { onStop() }
                .padding(horizontal = 12.dp, vertical = 6.dp)
        ) {
            Text(
                text = "Terminar",
                fontSize = 10.sp,
                fontWeight = FontWeight.SemiBold,
                color = Color(0xFFE2E8F0)
            )
        }
    }
}

@Composable
fun VisualGuideScreen(
    snapshot: BreathingSessionSnapshot?,
    onStop: () -> Unit
) {
    val segmentType = snapshot?.currentSegmentType ?: TimelineSegmentType.INHALE
    val progress = snapshot?.segmentProgress ?: 0f

    val targetScale = when (segmentType) {
        TimelineSegmentType.START_TAP -> 1.0f
        TimelineSegmentType.INHALE -> 0.45f + (progress * 0.55f) // Expands to 1.0
        TimelineSegmentType.HOLD_AFTER_INHALE -> 1.0f            // Stays expanded
        TimelineSegmentType.EXHALE -> 1.0f - (progress * 0.55f) // Contracts to 0.45
        TimelineSegmentType.HOLD_AFTER_EXHALE -> 0.45f          // Stays contracted
        TimelineSegmentType.END_DOUBLE_TAP -> 0.7f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 120, easing = FastOutSlowInEasing),
        label = "circleScale"
    )

    val labelText = when (segmentType) {
        TimelineSegmentType.START_TAP -> "PREPÁRATE"
        TimelineSegmentType.INHALE -> "INHALA"
        TimelineSegmentType.HOLD_AFTER_INHALE -> "SOSTÉN"
        TimelineSegmentType.EXHALE -> "EXHALA"
        TimelineSegmentType.HOLD_AFTER_EXHALE -> "PAUSA"
        TimelineSegmentType.END_DOUBLE_TAP -> "COMPLETO"
    }

    val glowColor = when (segmentType) {
        TimelineSegmentType.INHALE -> Color(0xFF38BDF8)
        TimelineSegmentType.HOLD_AFTER_INHALE, TimelineSegmentType.HOLD_AFTER_EXHALE -> Color(0xFFFBBF24)
        TimelineSegmentType.EXHALE -> Color(0xFF34D399)
        else -> Color(0xFF2DD4BF)
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(130.dp),
            contentAlignment = Alignment.Center
        ) {
            // Anillo exterior suave
            Box(
                modifier = Modifier
                    .size((130 * animatedScale).dp)
                    .clip(CircleShape)
                    .background(glowColor.copy(alpha = 0.12f))
            )
            // Anillo intermedio
            Box(
                modifier = Modifier
                    .size((105 * animatedScale).dp)
                    .clip(CircleShape)
                    .background(glowColor.copy(alpha = 0.25f))
            )
            // Núcleo central
            Box(
                modifier = Modifier
                    .size((80 * animatedScale).dp)
                    .clip(CircleShape)
                    .background(glowColor.copy(alpha = 0.40f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = labelText,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Black,
                    color = Color.White,
                    letterSpacing = 1.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Info de ciclos
        val currentCycle = snapshot?.currentCycle ?: 1
        val totalCycles = snapshot?.totalCycles ?: 1
        Text(
            text = "Ciclo $currentCycle de $totalCycles",
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            color = Color(0xFF64748B)
        )

        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Toca para detener",
            fontSize = 9.sp,
            color = Color(0xFF475569)
        )
    }
}

@Composable
fun BreathingSetupScreen(
    selectedPlan: String,
    selectedMinutes: Int,
    displayMode: BreathingDisplayMode,
    liveHr: Double?,
    onSelectPlan: (String) -> Unit,
    onSelectMinutes: (Int) -> Unit,
    onToggleDisplayMode: () -> Unit,
    onStart: () -> Unit,
    onBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F14))
            .padding(horizontal = 12.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "RESPIRACIÓN",
            fontSize = 11.sp,
            fontWeight = FontWeight.Black,
            color = Color(0xFF2DD4BF),
            letterSpacing = 2.sp
        )

        Spacer(modifier = Modifier.height(8.dp))

        // Selector de Protocolo Clínico
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            listOf("Calma 4-6" to "Calma 4-6", "Cuadrada" to "Cuadrada 4-4-4-4", "4-7-8" to "Relajante 4-7-8").forEach { (label, fullName) ->
                val isSelected = selectedPlan == fullName
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(10.dp))
                        .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF131C24))
                        .clickable { onSelectPlan(fullName) }
                        .padding(vertical = 6.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = label,
                        fontSize = 9.sp,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Selector de Duración (1m, 2m, 5m)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            listOf(1, 2, 5).forEach { mins ->
                val isSelected = selectedMinutes == mins
                Box(
                    modifier = Modifier
                        .weight(1f)
                        .clip(RoundedCornerShape(8.dp))
                        .background(if (isSelected) Color(0xFF0D9488) else Color(0xFF131C24))
                        .clickable { onSelectMinutes(mins) }
                        .padding(vertical = 4.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "${mins} min",
                        fontSize = 9.sp,
                        color = if (isSelected) Color.White else Color(0xFF94A3B8),
                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Alternador de Modo (Guía Visual / Háptico Silencioso)
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .background(Color(0xFF1E293B))
                .clickable { onToggleDisplayMode() }
                .padding(horizontal = 10.dp, vertical = 4.dp)
        ) {
            Text(
                text = if (displayMode == BreathingDisplayMode.VIBRATION_ONLY) "Modo: Solo vibración" else "Modo: Con guía visual",
                fontSize = 9.sp,
                fontWeight = FontWeight.Medium,
                color = Color(0xFFCBD5E1)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Botón Iniciar
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .clip(RoundedCornerShape(14.dp))
                .background(Color(0xFF0284C7))
                .clickable { onStart() }
                .padding(vertical = 8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "INICIAR SESIÓN",
                fontSize = 11.sp,
                fontWeight = FontWeight.Black,
                color = Color.White,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
fun FeelingEvaluationScreen(
    completedCycles: Int,
    totalCycles: Int,
    liveHr: Double?,
    onSelectFeeling: (String) -> Unit,
    onSkip: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF0A0F14))
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "¿CÓMO TE SIENTES?",
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF2DD4BF),
            letterSpacing = 2.sp
        )
        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Evaluación post-crisis",
            fontSize = 9.sp,
            color = Color(0xFF64748B)
        )

        if (liveHr != null) {
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "Pulso: ${liveHr.toInt()} BPM",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF34D399)
            )
        }

        Spacer(modifier = Modifier.height(10.dp))

        // 3 Opciones de Bienestar
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF059669))
                    .clickable { onSelectFeeling("BETTER") }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Mejor", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFF334155))
                    .clickable { onSelectFeeling("SAME") }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Igual", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }

            Box(
                modifier = Modifier
                    .weight(1f)
                    .clip(RoundedCornerShape(10.dp))
                    .background(Color(0xFFE11D48))
                    .clickable { onSelectFeeling("WORSE") }
                    .padding(vertical = 8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Peor", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        Text(
            text = "Omitir",
            fontSize = 10.sp,
            color = Color(0xFF64748B),
            modifier = Modifier.clickable { onSkip() }
        )
    }
}
