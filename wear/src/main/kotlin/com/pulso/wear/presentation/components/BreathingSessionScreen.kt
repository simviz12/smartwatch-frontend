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
        // Very subtle, dim indicator so user knows it's active if they peek
        Text(
            text = "Solo vibración",
            fontSize = 11.sp,
            color = Color(0xFF475569),
            fontWeight = FontWeight.SemiBold
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Cierra los ojos y sigue el ritmo",
            fontSize = 10.sp,
            color = Color(0xFF334155),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(14.dp))
        Text(
            text = "Toca para detener",
            fontSize = 9.sp,
            color = Color(0xFF1E293B)
        )
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
        TimelineSegmentType.INHALE -> 0.4f + (progress * 0.6f) // Expands to 1.0
        TimelineSegmentType.HOLD_AFTER_INHALE -> 1.0f          // Stays expanded
        TimelineSegmentType.EXHALE -> 1.0f - (progress * 0.6f) // Contracts to 0.4
        TimelineSegmentType.HOLD_AFTER_EXHALE -> 0.4f          // Stays contracted
        TimelineSegmentType.END_DOUBLE_TAP -> 0.7f
    }

    val animatedScale by animateFloatAsState(
        targetValue = targetScale,
        animationSpec = tween(durationMillis = 100, easing = FastOutSlowInEasing),
        label = "circleScale"
    )

    val labelText = when (segmentType) {
        TimelineSegmentType.START_TAP -> "Comenzando..."
        TimelineSegmentType.INHALE -> "Inhala"
        TimelineSegmentType.HOLD_AFTER_INHALE -> "Sostén"
        TimelineSegmentType.EXHALE -> "Exhala"
        TimelineSegmentType.HOLD_AFTER_EXHALE -> "Pausa"
        TimelineSegmentType.END_DOUBLE_TAP -> "Completado"
    }

    val labelColor = when (segmentType) {
        TimelineSegmentType.INHALE -> Color(0xFF38BDF8)
        TimelineSegmentType.HOLD_AFTER_INHALE, TimelineSegmentType.HOLD_AFTER_EXHALE -> Color(0xFFFBBF24)
        TimelineSegmentType.EXHALE -> Color(0xFF34D399)
        else -> Color.White
    }

    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            modifier = Modifier.size(110.dp),
            contentAlignment = Alignment.Center
        ) {
            // Animated breathing circle
            Box(
                modifier = Modifier
                    .size((110 * animatedScale).dp)
                    .clip(CircleShape)
                    .background(labelColor.copy(alpha = 0.25f))
            )
            Box(
                modifier = Modifier
                    .size((90 * animatedScale).dp)
                    .clip(CircleShape)
                    .background(labelColor.copy(alpha = 0.45f))
            )
            Text(
                text = labelText,
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Progress info
        val currentCycle = snapshot?.currentCycle ?: 1
        val totalCycles = snapshot?.totalCycles ?: 1
        Text(
            text = "Ciclo $currentCycle de $totalCycles",
            fontSize = 11.sp,
            color = Color(0xFF94A3B8)
        )

        Spacer(modifier = Modifier.height(2.dp))
        Text(
            text = "Toca para detener",
            fontSize = 9.sp,
            color = Color(0xFF64748B)
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
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Respiración", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))

        Spacer(modifier = Modifier.height(6.dp))

        // Plan toggle row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            listOf("Calma 4-6", "Cuadrada", "4-7-8").forEach { name ->
                val fullPlanName = when (name) {
                    "Cuadrada" -> "Cuadrada 4-4-4-4"
                    "4-7-8" -> "Relajante 4-7-8"
                    else -> "Calma 4-6"
                }
                val isSelected = selectedPlan == fullPlanName
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFF0284C7) else Color(0xFF1E293B))
                        .clickable { onSelectPlan(fullPlanName) }
                        .padding(horizontal = 6.dp, vertical = 4.dp)
                ) {
                    Text(name, fontSize = 9.sp, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Duration row (1, 2, 5 min)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.Center
        ) {
            listOf(1, 2, 5).forEach { mins ->
                val isSelected = selectedMinutes == mins
                Box(
                    modifier = Modifier
                        .padding(horizontal = 4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(if (isSelected) Color(0xFF0D9488) else Color(0xFF1E293B))
                        .clickable { onSelectMinutes(mins) }
                        .padding(horizontal = 8.dp, vertical = 4.dp)
                ) {
                    Text("${mins}m", fontSize = 10.sp, color = Color.White, fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal)
                }
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        // Display mode pill
        Box(
            modifier = Modifier
                .clip(RoundedCornerShape(6.dp))
                .background(Color(0xFF334155))
                .clickable { onToggleDisplayMode() }
                .padding(horizontal = 8.dp, vertical = 3.dp)
        ) {
            Text(
                text = if (displayMode == BreathingDisplayMode.VIBRATION_ONLY) "📳 Solo vibración" else "👁️ Con guía visual",
                fontSize = 9.sp,
                color = Color(0xFFE2E8F0)
            )
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Start button
        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .height(34.dp),
            colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xFF0284C7))
        ) {
            Text("Empezar", fontSize = 12.sp, fontWeight = FontWeight.Bold)
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
            .padding(14.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("¿Cómo te sientes ahora?", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = Color.White, textAlign = TextAlign.Center)

        if (liveHr != null) {
            Spacer(modifier = Modifier.height(2.dp))
            Text("Pulso: ${liveHr.toInt()} BPM", fontSize = 10.sp, color = Color(0xFF38BDF8))
        }

        Spacer(modifier = Modifier.height(8.dp))

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            Button(
                onClick = { onSelectFeeling("BETTER") },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .padding(horizontal = 2.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF059669))
            ) {
                Text("Mejor", fontSize = 10.sp, color = Color.White)
            }
            Button(
                onClick = { onSelectFeeling("SAME") },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .padding(horizontal = 2.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFF475569))
            ) {
                Text("Igual", fontSize = 10.sp, color = Color.White)
            }
            Button(
                onClick = { onSelectFeeling("WORSE") },
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    .padding(horizontal = 2.dp),
                colors = ButtonDefaults.buttonColors(backgroundColor = Color(0xFFDC2626))
            ) {
                Text("Peor", fontSize = 10.sp, color = Color.White)
            }
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = "Omitir",
            fontSize = 10.sp,
            color = Color.Gray,
            modifier = Modifier.clickable { onSkip() }
        )
    }
}
