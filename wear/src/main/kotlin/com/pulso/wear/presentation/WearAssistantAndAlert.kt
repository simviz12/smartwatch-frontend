package com.pulso.wear.presentation

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.speech.RecognizerIntent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.LiveRegionMode
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.liveRegion
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Icon
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.pulso.wear.data.assistant.AssistantPhase
import com.pulso.wear.data.assistant.WearAssistantManager
import com.pulso.wear.presentation.theme.WearColors
import com.pulso.wear.presentation.theme.WearIcons
import com.pulso.wear.presentation.theme.WearType
import kotlin.math.abs
import kotlin.math.sin

// ───────────────────────── Voice assistant ─────────────────────────

@Composable
fun VoiceAssistantScreen(manager: WearAssistantManager, onBreathe: () -> Unit) {
    val context = LocalContext.current
    val state by manager.state.collectAsState()
    val listState = rememberScalingLazyListState()

    // Fallback when the watch has no in-app recognizer: system voice input screen.
    val systemVoice = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        val text = if (result.resultCode == Activity.RESULT_OK) {
            result.data?.getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS)?.firstOrNull()
        } else null
        if (text != null) manager.submit(text) else manager.reset()
    }
    val listen: () -> Unit = {
        if (manager.isInAppRecognitionAvailable()) manager.startListening()
        else runCatching { systemVoice.launch(manager.recognizerIntent()) }.onFailure { manager.submit("") }
    }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        if (granted) listen() else manager.reset()
    }
    val start: () -> Unit = {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) listen()
        else micPermission.launch(Manifest.permission.RECORD_AUDIO)
    }

    LaunchedEffect(Unit) { if (state.phase == AssistantPhase.IDLE) start() }

    val t = rememberInfiniteTransition(label = "voice")
    val n by t.animateFloat(0f, 100f, infiniteRepeatable(tween(14_000, easing = LinearEasing), RepeatMode.Restart), label = "n")
    val level by animateFloatAsState(state.level, tween(120), label = "level")
    val active = state.phase == AssistantPhase.LISTENING || state.phase == AssistantPhase.SPEAKING || state.phase == AssistantPhase.THINKING

    Scaffold(timeText = { TimeText() }, positionIndicator = { PositionIndicator(scalingLazyListState = listState) }) {
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize().background(WearColors.Bg),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            item {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp), modifier = Modifier.padding(top = 10.dp)) {
                    Icon(WearIcons.Spark, null, tint = WearColors.Hrv, modifier = Modifier.size(12.dp))
                    Text("ASISTENTE IA", style = WearType.kicker, color = WearColors.Hrv)
                }
            }
            item {
                Box(Modifier.size(62.dp), contentAlignment = Alignment.Center) {
                    val halo = 1f + if (state.phase == AssistantPhase.LISTENING) 0.12f * level else 0.04f * abs(sin(n * 0.5f))
                    Box(Modifier.size(62.dp).scale(halo).clip(CircleShape).background(WearColors.Hrv.copy(alpha = 0.10f)))
                    Box(
                        Modifier.size(50.dp).clip(CircleShape).background(WearColors.Primary.copy(alpha = 0.14f))
                            .border(1.dp, WearColors.Primary.copy(alpha = 0.5f), CircleShape)
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.CenterVertically, modifier = Modifier.height(34.dp)) {
                        repeat(7) { i ->
                            val shape = 1f - abs(i - 3) * 0.18f
                            val motion = when (state.phase) {
                                AssistantPhase.LISTENING -> 0.25f + level * 0.75f * (0.6f + 0.4f * abs(sin(n * 2.1f + i)))
                                AssistantPhase.SPEAKING -> 0.3f + 0.7f * abs(sin(n * 1.4f + i * 0.9f))
                                AssistantPhase.THINKING -> 0.2f + 0.25f * abs(sin(n * 0.8f + i * 0.6f))
                                else -> 0.18f
                            }
                            Box(
                                Modifier.width(4.dp).height((6 + 28 * motion * shape).dp).clip(CircleShape)
                                    .background(if (i % 2 == 0) WearColors.Primary else WearColors.Hrv)
                            )
                        }
                    }
                }
            }
            item {
                Text(
                    when (state.phase) {
                        AssistantPhase.IDLE -> "Toca para hablar"
                        AssistantPhase.LISTENING -> "Te escucho…"
                        AssistantPhase.THINKING -> "Pensando…"
                        AssistantPhase.SPEAKING -> "Respondiendo"
                        AssistantPhase.ANSWERED -> "Respuesta"
                        AssistantPhase.ERROR -> "Ups"
                    },
                    style = WearType.title,
                    modifier = Modifier.semantics { liveRegion = LiveRegionMode.Polite }
                )
            }
            if (state.transcript.isNotBlank() && state.phase != AssistantPhase.ERROR) {
                item {
                    Text(
                        "“${state.transcript}”",
                        style = WearType.body.copy(fontSize = 13.sp, lineHeight = 17.sp),
                        color = WearColors.Muted,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp)
                    )
                }
            }
            if (state.reply.isNotBlank() && (state.phase == AssistantPhase.SPEAKING || state.phase == AssistantPhase.ANSWERED)) {
                item {
                    Text(
                        state.reply,
                        style = WearType.body.copy(fontSize = 14.sp, lineHeight = 19.sp, fontWeight = FontWeight.SemiBold),
                        color = WearColors.Text,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.clip(androidx.compose.foundation.shape.RoundedCornerShape(18.dp))
                            .background(WearColors.Surface).padding(horizontal = 12.dp, vertical = 10.dp)
                    )
                }
            }
            state.error?.takeIf { state.phase == AssistantPhase.ERROR }?.let { msg ->
                item { Text(msg, style = WearType.body, color = WearColors.Warn, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 10.dp)) }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    when (state.phase) {
                        AssistantPhase.LISTENING -> WearRoundButton(WearIcons.Stop, "Detener", { manager.stopListening() }, size = 48.dp, background = WearColors.Heart, tint = WearColors.Bg)
                        AssistantPhase.SPEAKING -> WearRoundButton(WearIcons.Stop, "Silenciar", { manager.stopSpeaking() }, size = 48.dp, background = WearColors.Heart, tint = WearColors.Bg)
                        AssistantPhase.THINKING -> Spacer(Modifier.height(48.dp))
                        else -> WearRoundButton(WearIcons.Mic, "Hablar", start, size = 48.dp, background = WearColors.Primary, tint = WearColors.OnPrimary)
                    }
                    if (state.suggestsBreathing && !active) {
                        WearRoundButton(WearIcons.Wind, "Respirar", onBreathe, size = 48.dp, background = WearColors.Surface, tint = WearColors.Primary)
                    }
                }
            }
            item { Spacer(Modifier.height(14.dp)) }
        }
    }
}

// ───────────────────────── Stress alert ─────────────────────────

private const val ALERT_AUTO_DISMISS_MS = 20_000

@Composable
fun StressAlertScreen(
    subtitle: String,
    onBreathe: () -> Unit,
    onNotNow: () -> Unit,
    onNotStressed: () -> Unit,
    onTimeout: () -> Unit
) {
    val remaining = remember { Animatable(1f) }
    LaunchedEffect(Unit) {
        remaining.animateTo(0f, tween(ALERT_AUTO_DISMISS_MS, easing = LinearEasing))
        onTimeout()
    }
    val listState = rememberScalingLazyListState()

    Box(Modifier.fillMaxSize().background(WearColors.Bg)) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            val arc = Size(size.width - stroke, size.height - stroke)
            drawArc(WearColors.Heart.copy(alpha = 0.22f), 0f, 360f, false, Offset(stroke / 2, stroke / 2), arc, style = Stroke(stroke))
            drawArc(WearColors.Heart, -90f, 360f * remaining.value, false, Offset(stroke / 2, stroke / 2), arc, style = Stroke(stroke, cap = StrokeCap.Round))
        }
        ScalingLazyColumn(
            modifier = Modifier.fillMaxSize(),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(5.dp)
        ) {
            item {
                Box(Modifier.padding(top = 8.dp).size(30.dp).clip(CircleShape).background(WearColors.Heart.copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                    Icon(WearIcons.Bolt, null, tint = WearColors.Heart, modifier = Modifier.size(17.dp))
                }
            }
            item { Text("Tensión detectada", style = WearType.title, textAlign = TextAlign.Center) }
            item { Text(subtitle, style = WearType.body.copy(lineHeight = 16.sp), color = WearColors.Muted, textAlign = TextAlign.Center, modifier = Modifier.padding(horizontal = 14.dp)) }
            item {
                Row(
                    Modifier.fillMaxWidth().height(48.dp).clip(CircleShape).background(WearColors.Primary)
                        .clickable(role = Role.Button, onClick = onBreathe),
                    horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(WearIcons.Wind, null, tint = WearColors.OnPrimary, modifier = Modifier.size(18.dp))
                    Text("Respirar 1 min", style = WearType.label, color = WearColors.OnPrimary)
                }
            }
            item {
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    AlertSecondary("Ahora no", onNotNow, Modifier.weight(1f))
                    AlertSecondary("No lo era", onNotStressed, Modifier.weight(1f))
                }
            }
            item { Spacer(Modifier.height(12.dp)) }
        }
    }
}

@Composable
private fun AlertSecondary(label: String, onClick: () -> Unit, modifier: Modifier) {
    Box(
        modifier.height(44.dp).clip(CircleShape).background(WearColors.Surface).clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Text(label, style = WearType.label.copy(fontSize = 13.sp))
    }
}
