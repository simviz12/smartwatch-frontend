package com.pulso.mobile.presentation.screens

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import com.pulso.mobile.data.chat.ChatMessageItem
import com.pulso.mobile.data.chat.PulsoGeminiChatManager
import com.pulso.mobile.presentation.components.IconBadge
import com.pulso.mobile.presentation.components.IconCircleButton
import com.pulso.mobile.presentation.i18n.LocalAppLanguage
import com.pulso.mobile.presentation.i18n.formatTime
import com.pulso.mobile.presentation.i18n.tr
import com.pulso.mobile.presentation.theme.Pulso
import com.pulso.mobile.presentation.theme.PulsoIcons
import com.pulso.mobile.presentation.theme.PulsoType
import kotlin.math.abs
import kotlin.math.sin

@Composable
fun AssistantScreen(
    chatManager: PulsoGeminiChatManager,
    onStartBreathing: () -> Unit
) {
    val c = Pulso.colors
    val lang = LocalAppLanguage.current
    val context = LocalContext.current
    val messages by chatManager.messages.collectAsState()
    val isListening by chatManager.isListening.collectAsState()
    val isSpeaking by chatManager.isSpeaking.collectAsState()
    val isGenerating by chatManager.isGenerating.collectAsState()
    var input by rememberSaveable { mutableStateOf("") }
    var micDenied by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()

    val send: (String) -> Unit = { text ->
        if (text.isNotBlank()) {
            chatManager.sendMessage(text.trim(), lang.name)
            input = ""
        }
    }
    val startListening = { chatManager.startVoiceInput { spoken -> send(spoken) } }
    val micPermission = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { granted ->
        micDenied = !granted
        if (granted) startListening()
    }
    val onMic = {
        if (isListening) {
            Unit // speech recognizer stops by itself at end of speech
        } else if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
            startListening()
        } else {
            micPermission.launch(Manifest.permission.RECORD_AUDIO)
        }
    }

    LaunchedEffect(messages.size, isGenerating) {
        val extra = if (isGenerating) 1 else 0
        if (messages.isNotEmpty()) listState.animateScrollToItem(messages.size - 1 + extra)
    }

    Column(Modifier.fillMaxSize()) {
        // Header
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 12.dp, top = 16.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            IconBadge(PulsoIcons.Spark, tint = c.hrv, background = c.hrvSoft, size = 44.dp, iconSize = 22.dp, radius = 22.dp)
            Column(Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(tr("Asistente", "Assistant"), style = PulsoType.h2.copy(fontSize = 18.sp), color = c.onSurface)
                Text(
                    when {
                        isSpeaking -> tr("Hablando…", "Speaking…")
                        !chatManager.isAiConfigured -> tr("IA sin configurar · respuestas básicas", "AI not configured · basic replies")
                        else -> tr("Responde con IA · no reemplaza a un profesional", "AI replies · not a substitute for a professional")
                    },
                    style = PulsoType.caption, color = c.muted
                )
            }
            if (isSpeaking) {
                IconCircleButton(PulsoIcons.Stop, tr("Pausar audio", "Pause audio"), onClick = { chatManager.stopSpeaking() }, border = null, background = c.surface2)
            }
        }
        Box(Modifier.fillMaxWidth().height(1.dp).background(c.outline))

        // Crisis banner
        Row(
            Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp).fillMaxWidth()
                .clip(RoundedCornerShape(16.dp)).background(c.heartSoft).padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Icon(PulsoIcons.Shield, null, tint = c.heart, modifier = Modifier.size(20.dp))
            Text(
                tr("¿Necesitas ayuda ya? Llama a la línea 106 o al 123.", "Need help right now? Reach a crisis line."),
                style = PulsoType.bodySmall.copy(fontWeight = FontWeight.SemiBold, lineHeight = 18.sp),
                color = c.onSurface, modifier = Modifier.weight(1f)
            )
            Box(
                Modifier.height(34.dp).clip(CircleShape).background(c.heart)
                    .clickable {
                        runCatching { context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:106"))) }
                    }
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(tr("Ayuda", "Get help"), style = PulsoType.chip.copy(fontWeight = FontWeight.ExtraBold), color = c.bg)
            }
        }

        // Conversation
        LazyColumn(
            state = listState,
            modifier = Modifier.weight(1f).fillMaxWidth(),
            contentPadding = PaddingValues(start = 20.dp, end = 20.dp, top = 14.dp, bottom = 6.dp),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            messages.firstOrNull()?.let { first ->
                item(key = "day") {
                    Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Text(tr("Hoy", "Today") + " · " + formatTime(first.timestamp, lang), style = PulsoType.caption.copy(fontWeight = FontWeight.Bold), color = c.muted)
                    }
                }
            }
            items(messages, key = { it.id }) { m ->
                MessageBubble(m, onStartBreathing, onRepeat = { chatManager.speak(m.text, lang.name) })
            }
            if (isGenerating) item(key = "typing") { TypingBubble() }
        }

        // Suggestions
        Row(
            Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(start = 20.dp, end = 20.dp, top = 6.dp, bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(
                tr("Resume mi día", "Summarize my day"),
                tr("¿Por qué subió mi pulso?", "Why did my pulse rise?"),
                tr("Dame un ejercicio", "Give me an exercise"),
                tr("Me siento abrumado", "I feel overwhelmed")
            ).forEach { s ->
                Box(
                    Modifier.height(38.dp).clip(CircleShape).border(1.dp, c.outline, CircleShape)
                        .clickable(enabled = !isGenerating) { send(s) }.padding(horizontal = 14.dp),
                    contentAlignment = Alignment.Center
                ) { Text(s, style = PulsoType.chip, color = c.onSurface, maxLines = 1) }
            }
        }

        AnimatedVisibility(isListening) { ListeningBanner() }
        if (micDenied) {
            Text(
                tr("Activa el permiso de micrófono para hablar con el asistente.", "Allow microphone access to talk to the assistant."),
                style = PulsoType.caption, color = c.warn, modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }

        // Input
        Row(
            Modifier.fillMaxWidth().padding(start = 20.dp, end = 20.dp, bottom = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                Modifier.weight(1f).height(52.dp).clip(CircleShape).background(c.surface).border(1.dp, c.outline, CircleShape).padding(horizontal = 18.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (input.isEmpty()) Text(tr("Escribe o habla…", "Write or speak…"), style = PulsoType.body, color = c.muted)
                BasicTextField(
                    value = input,
                    onValueChange = { input = it },
                    singleLine = true,
                    textStyle = PulsoType.body.copy(color = c.onSurface),
                    cursorBrush = SolidColor(c.primary),
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Send),
                    keyboardActions = KeyboardActions(onSend = { send(input) }),
                    modifier = Modifier.fillMaxWidth().semantics { contentDescription = "message" }
                )
            }
            val hasText = input.isNotBlank()
            val bg = when {
                isListening -> c.heart
                else -> c.primary
            }
            Box(
                Modifier.size(52.dp).clip(CircleShape).background(bg)
                    .clickable(enabled = !(hasText && isGenerating)) { if (hasText) send(input) else onMic() }
                    .semantics { contentDescription = if (hasText) "send" else "mic" },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    when {
                        hasText -> PulsoIcons.ArrowUp
                        else -> PulsoIcons.Mic
                    },
                    null,
                    tint = if (isListening) c.bg else c.onPrimary,
                    modifier = Modifier.size(24.dp)
                )
            }
        }
    }
}

@Composable
private fun MessageBubble(m: ChatMessageItem, onStartBreathing: () -> Unit, onRepeat: () -> Unit) {
    val c = Pulso.colors
    val mine = m.sender == "user"
    val suggestsBreathing = !mine && Regex("respir|breath", RegexOption.IGNORE_CASE).containsMatchIn(m.text)
    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = if (mine) Alignment.End else Alignment.Start,
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        val shape = if (mine) RoundedCornerShape(20.dp, 20.dp, 6.dp, 20.dp) else RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)
        Text(
            m.text,
            style = PulsoType.body.copy(lineHeight = 22.sp),
            color = if (mine) c.onPrimary else c.onSurface,
            modifier = Modifier
                .widthIn(max = 300.dp)
                .clip(shape)
                .background(if (mine) c.primary else c.surface)
                .border(1.dp, if (mine) c.primary else c.outline, shape)
                .then(if (!mine) Modifier.clickable(onClick = onRepeat) else Modifier)
                .padding(horizontal = 14.dp, vertical = 12.dp)
        )
        if (suggestsBreathing) {
            Row(
                Modifier.height(40.dp).clip(CircleShape).background(c.primarySoft).clickable(onClick = onStartBreathing).padding(horizontal = 14.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(PulsoIcons.Wind, null, tint = c.primary, modifier = Modifier.size(18.dp))
                Text(tr("Empezar a respirar", "Start breathing"), style = PulsoType.chip.copy(fontWeight = FontWeight.ExtraBold), color = c.primary)
            }
        }
    }
}

@Composable
private fun TypingBubble() {
    val c = Pulso.colors
    val t = rememberInfiniteTransition(label = "typing")
    val phase by t.animateFloat(0f, 3f, infiniteRepeatable(tween(900), RepeatMode.Restart), label = "phase")
    Row(
        Modifier.clip(RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp)).background(c.surface).border(1.dp, c.outline, RoundedCornerShape(20.dp, 20.dp, 20.dp, 6.dp))
            .padding(horizontal = 16.dp, vertical = 16.dp),
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        repeat(3) { i ->
            val on = phase.toInt() == i
            Box(Modifier.size(7.dp).clip(CircleShape).background(if (on) c.primary else c.muted.copy(alpha = 0.5f)))
        }
    }
}

@Composable
private fun ListeningBanner() {
    val c = Pulso.colors
    val t = rememberInfiniteTransition(label = "listen")
    val n by t.animateFloat(0f, 100f, infiniteRepeatable(tween(15_000), RepeatMode.Restart), label = "n")
    Row(
        Modifier.padding(start = 20.dp, end = 20.dp, bottom = 10.dp).fillMaxWidth()
            .clip(RoundedCornerShape(18.dp)).background(c.surface).border(1.dp, c.primary, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Row(Modifier.height(28.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
            repeat(14) { i ->
                val h = 6 + abs(sin(n * 0.9 + i * 0.8)) * 22
                Box(Modifier.size(width = 4.dp, height = h.dp).clip(CircleShape).background(c.primary))
            }
        }
        Text(tr("Escuchando… habla con calma", "Listening… take your time"), style = PulsoType.label.copy(fontSize = 14.sp), color = c.onSurface, modifier = Modifier.weight(1f))
        Spacer(Modifier.size(1.dp))
    }
}
