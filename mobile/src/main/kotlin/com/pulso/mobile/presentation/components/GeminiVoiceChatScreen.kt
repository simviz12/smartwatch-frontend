package com.pulso.mobile.presentation.components

import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
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
import com.pulso.mobile.data.chat.ChatMessageItem
import com.pulso.mobile.data.chat.PulsoGeminiChatManager
import com.pulso.mobile.presentation.i18n.AppLanguage
import com.pulso.mobile.presentation.i18n.PulsoStrings
import com.pulso.mobile.presentation.theme.PulsoThemeColors

@Composable
fun GeminiVoiceChatScreen(
    chatManager: PulsoGeminiChatManager,
    language: AppLanguage,
    theme: PulsoThemeColors,
    onBack: () -> Unit
) {
    val messages by chatManager.messages.collectAsState()
    val isListening by chatManager.isListening.collectAsState()
    val isSpeaking by chatManager.isSpeaking.collectAsState()
    val isGenerating by chatManager.isGenerating.collectAsState()

    var inputText by remember { mutableStateOf("") }
    val listState = rememberLazyListState()

    LaunchedEffect(messages.size) {
        if (messages.isNotEmpty()) {
            listState.animateScrollToItem(messages.size - 1)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(16.dp)
    ) {
        // Chat Header with status indicator
        // Header status bar
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(if (isSpeaking) theme.success else theme.primary)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = PulsoStrings.chatHeader(language),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = theme.textPrimary
                    )
                }
                Text(
                    text = if (isSpeaking) "Reproduciendo audio..." else PulsoStrings.chatSubHeader(language),
                    fontSize = 12.sp,
                    color = theme.textSecondary
                )
            }

            if (isSpeaking) {
                Button(
                    onClick = { chatManager.stopSpeaking() },
                    colors = ButtonDefaults.buttonColors(containerColor = theme.cardBackground),
                    border = androidx.compose.foundation.BorderStroke(1.dp, theme.border),
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.height(30.dp)
                ) {
                    Text("Pausar audio", fontSize = 11.sp, color = theme.textPrimary, fontWeight = FontWeight.Medium)
                }
            }
        }

        // Quick prompts suggestions
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            QuickChip(
                text = PulsoStrings.quickPrompt1(language),
                theme = theme,
                onClick = { chatManager.sendMessage(PulsoStrings.quickPrompt1(language), language.name) }
            )
            QuickChip(
                text = PulsoStrings.quickPrompt2(language),
                theme = theme,
                onClick = { chatManager.sendMessage(PulsoStrings.quickPrompt2(language), language.name) }
            )
        }

        // Messages history list
        LazyColumn(
            state = listState,
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth(),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            items(messages, key = { it.id }) { message ->
                MessageBubble(
                    item = message,
                    theme = theme,
                    onRepeatSpeak = { chatManager.speak(message.text, language.name) }
                )
            }

            if (isGenerating) {
                item {
                    Row(
                        modifier = Modifier.padding(start = 8.dp, top = 4.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            strokeWidth = 2.dp,
                            color = theme.primary
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = if (language == AppLanguage.ES) "Pensando respuesta..." else "Thinking response...",
                            fontSize = 12.sp,
                            color = theme.textSecondary
                        )
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Input and voice controls
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Voice Speech-To-Text mic button
            Button(
                onClick = {
                    chatManager.startVoiceInput { spokenText ->
                        inputText = spokenText
                        chatManager.sendMessage(spokenText, language.name)
                        inputText = ""
                    }
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = if (isListening) Color(0xFFEF4444) else theme.cardBackground
                ),
                shape = CircleShape,
                modifier = Modifier
                    .size(48.dp)
                    .border(
                        1.dp,
                        if (isListening) Color(0xFFEF4444) else theme.border,
                        CircleShape
                    )
            ) {
                Text(
                    text = if (isListening) "STOP" else "VOZ",
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isListening) Color.White else theme.primary
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            OutlinedTextField(
                value = inputText,
                onValueChange = { inputText = it },
                modifier = Modifier.weight(1f),
                placeholder = {
                    Text(
                        text = if (isListening) PulsoStrings.micListening(language) else PulsoStrings.typeMessagePlaceholder(language),
                        fontSize = 13.sp,
                        color = theme.textSecondary
                    )
                },
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = theme.primary,
                    unfocusedBorderColor = theme.border,
                    focusedTextColor = theme.textPrimary,
                    unfocusedTextColor = theme.textPrimary,
                    focusedContainerColor = theme.cardBackground,
                    unfocusedContainerColor = theme.cardBackground
                ),
                shape = RoundedCornerShape(24.dp),
                maxLines = 2
            )

            Spacer(modifier = Modifier.width(8.dp))

            Button(
                onClick = {
                    if (inputText.isNotBlank()) {
                        val text = inputText.trim()
                        inputText = ""
                        chatManager.sendMessage(text, language.name)
                    }
                },
                colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                shape = RoundedCornerShape(16.dp),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 12.dp, vertical = 8.dp),
                modifier = Modifier.height(48.dp),
                enabled = inputText.isNotBlank() && !isGenerating
            ) {
                Text("ENVIAR", fontSize = 11.sp, fontWeight = FontWeight.Bold, color = theme.onPrimary)
            }
        }
    }
}

@Composable
fun MessageBubble(
    item: ChatMessageItem,
    theme: PulsoThemeColors,
    onRepeatSpeak: () -> Unit
) {
    val isUser = item.sender == "user"
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = if (isUser) Arrangement.End else Arrangement.Start
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth(0.85f)
                .clip(
                    RoundedCornerShape(
                        topStart = 16.dp,
                        topEnd = 16.dp,
                        bottomStart = if (isUser) 16.dp else 2.dp,
                        bottomEnd = if (isUser) 2.dp else 16.dp
                    )
                ),
            colors = CardDefaults.cardColors(
                containerColor = if (isUser) {
                    if (theme.isLightMode) Color(0xFF0284C7) else Color(0xFF0369A1)
                } else {
                    theme.cardBackground
                }
            )
        ) {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isUser) "Tú" else "Pulso IA (Gemini)",
                        fontWeight = FontWeight.Bold,
                        fontSize = 11.sp,
                        color = if (isUser) Color.White else theme.primary
                    )
                    if (!isUser) {
                        Box(
                            modifier = Modifier
                                .padding(start = 4.dp)
                                .clip(RoundedCornerShape(6.dp))
                                .background(theme.primary.copy(alpha = 0.15f))
                                .clickable { onRepeatSpeak() }
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                "AUDIO",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.primary
                            )
                        }
                    }
                }
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = item.text,
                    fontSize = 14.sp,
                    color = Color.White,
                    lineHeight = 20.sp
                )
            }
        }
    }
}

@Composable
fun QuickChip(
    text: String,
    theme: PulsoThemeColors,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(12.dp))
            .background(theme.cardBackground)
            .border(1.dp, theme.border, RoundedCornerShape(12.dp))
            .clickable { onClick() }
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            fontSize = 11.sp,
            color = theme.textPrimary,
            maxLines = 1
        )
    }
}
