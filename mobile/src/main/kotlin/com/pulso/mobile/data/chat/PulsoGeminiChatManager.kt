package com.pulso.mobile.data.chat

import android.content.Context
import android.os.Bundle
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.speech.tts.TextToSpeech
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

data class ChatMessageItem(
    val id: String = java.util.UUID.randomUUID().toString(),
    val sender: String, // "user" or "assistant"
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val isStreaming: Boolean = false,
    val isSpoken: Boolean = false
)

class PulsoGeminiChatManager(private val context: Context) : TextToSpeech.OnInitListener {

    private val _messages = MutableStateFlow<List<ChatMessageItem>>(emptyList())
    val messages: StateFlow<List<ChatMessageItem>> = _messages.asStateFlow()

    private val _isListening = MutableStateFlow(false)
    val isListening: StateFlow<Boolean> = _isListening.asStateFlow()

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    private val _isGenerating = MutableStateFlow(false)
    val isGenerating: StateFlow<Boolean> = _isGenerating.asStateFlow()

    private var speechRecognizer: SpeechRecognizer? = null
    private var textToSpeech: TextToSpeech? = null
    private var ttsReady = false

    // Dynamic Gemini API Key resolution (reads from System property or environment variable)
    private val apiKey: String
        get() = System.getProperty("GEMINI_API_KEY")
            ?: System.getenv("GEMINI_API_KEY")
            ?: ""
    private val modelEndpoint = "https://generativelanguage.googleapis.com/v1beta/models/gemini-2.5-flash:generateContent"

    private val scope = CoroutineScope(Dispatchers.Default)

    init {
        scope.launch(Dispatchers.IO) {
            try {
                textToSpeech = TextToSpeech(context.applicationContext, this@PulsoGeminiChatManager)
            } catch (e: Exception) {
                Log.e("GeminiChat", "Error initializing TTS", e)
            }
        }
        initInitialGreeting("ES")
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = textToSpeech?.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                textToSpeech?.setLanguage(Locale.US)
            }
            ttsReady = true
            Log.i("GeminiChat", "Android TextToSpeech initialized successfully")
        } else {
            Log.e("GeminiChat", "Failed to initialize Android TextToSpeech")
        }
    }

    fun initInitialGreeting(language: String = "ES") {
        if (_messages.value.isEmpty()) {
            val greeting = if (language == "ES") {
                "Hola, soy tu compañero de Pulso. Noto que podrías estar experimentando tensión. Respira profundo, estoy aquí contigo para escucharte y ayudarte. ¿Cómo te sientes ahora?"
            } else {
                "Hello, I am your Pulso companion. I noticed some physical tension. Take a deep breath, I'm here with you to listen and help. How are you feeling right now?"
            }
            val welcomeMsg = ChatMessageItem(sender = "assistant", text = greeting)
            _messages.value = listOf(welcomeMsg)
            speak(greeting, language)
        }
    }

    fun speak(text: String, language: String = "ES") {
        if (!ttsReady || textToSpeech == null) {
            Log.w("GeminiChat", "TTS not ready yet")
            return
        }
        val locale = if (language == "ES") Locale("es", "ES") else Locale.US
        textToSpeech?.language = locale
        _isSpeaking.value = true
        textToSpeech?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PULSO_GEMINI_TTS")
    }

    fun stopSpeaking() {
        textToSpeech?.stop()
        _isSpeaking.value = false
    }

    fun startVoiceInput(onResult: (String) -> Unit) {
        if (!SpeechRecognizer.isRecognitionAvailable(context)) {
            Log.w("GeminiChat", "Speech recognition not available on this device")
            return
        }

        stopSpeaking()
        speechRecognizer?.destroy()
        speechRecognizer = SpeechRecognizer.createSpeechRecognizer(context)

        val intent = android.content.Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
            putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
            putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault())
            putExtra(RecognizerIntent.EXTRA_PROMPT, "Pulso te escucha...")
        }

        speechRecognizer?.setRecognitionListener(object : RecognitionListener {
            override fun onReadyForSpeech(params: Bundle?) { _isListening.value = true }
            override fun onBeginningOfSpeech() {}
            override fun onRmsChanged(rmsdB: Float) {}
            override fun onBufferReceived(buffer: ByteArray?) {}
            override fun onEndOfSpeech() { _isListening.value = false }
            override fun onError(error: Int) {
                _isListening.value = false
                Log.w("GeminiChat", "Speech recognition error code: $error")
            }
            override fun onResults(results: Bundle?) {
                _isListening.value = false
                val matches = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                val voiceText = matches?.firstOrNull()
                if (!voiceText.isNullOrBlank()) {
                    onResult(voiceText)
                }
            }
            override fun onPartialResults(partialResults: Bundle?) {}
            override fun onEvent(eventType: Int, params: Bundle?) {}
        })

        speechRecognizer?.startListening(intent)
    }

    fun sendMessage(userText: String, language: String = "ES") {
        if (userText.isBlank()) return

        stopSpeaking()
        val userMsg = ChatMessageItem(sender = "user", text = userText)
        _messages.value = _messages.value + userMsg
        _isGenerating.value = true

        scope.launch {
            val responseText = callGeminiApi(userText, language)
            val assistantMsg = ChatMessageItem(sender = "assistant", text = responseText)
            _messages.value = _messages.value + assistantMsg
            _isGenerating.value = false

            // Make the AI talk immediately!
            speak(responseText, language)
        }
    }

    private suspend fun callGeminiApi(userPrompt: String, language: String): String = withContext(Dispatchers.IO) {
        try {
            val url = URL("$modelEndpoint?key=$apiKey")
            val conn = url.openConnection() as HttpURLConnection
            conn.requestMethod = "POST"
            conn.setRequestProperty("Content-Type", "application/json; utf-8")
            conn.doOutput = true
            conn.connectTimeout = 10000
            conn.readTimeout = 15000

            val systemInstruction = if (language == "ES") {
                "Eres el asistente emocional y de bienestar de Pulso. El usuario está usando un smartwatch Pixel 8 que detecta tensión o estrés. Habla siempre de manera empática, cálida, concisa y comprensiva (máximo 2 a 3 oraciones). Ofrece calma y técnicas de respiración si lo consideras adecuado. NUNCA des diagnósticos médicos."
            } else {
                "You are Pulso's empathetic wellness assistant. The user wears a smartwatch detecting tension. Always speak warmly, concisely, and empathetically (2 to 3 sentences maximum). Offer calm and breathing exercises. Never give medical diagnoses."
            }

            val payload = JSONObject().apply {
                put("contents", JSONArray().apply {
                    put(JSONObject().apply {
                        put("parts", JSONArray().apply {
                            put(JSONObject().put("text", "$systemInstruction\nUsuario: $userPrompt"))
                        })
                    })
                })
            }

            OutputStreamWriter(conn.outputStream).use { writer ->
                writer.write(payload.toString())
                writer.flush()
            }

            val responseCode = conn.responseCode
            if (responseCode == HttpURLConnection.HTTP_OK) {
                val reader = BufferedReader(InputStreamReader(conn.inputStream))
                val sb = java.lang.StringBuilder()
                var line: String?
                while (reader.readLine().also { line = it } != null) {
                    sb.append(line)
                }
                reader.close()

                val jsonResponse = JSONObject(sb.toString())
                val candidates = jsonResponse.optJSONArray("candidates")
                if (candidates != null && candidates.length() > 0) {
                    val candidate = candidates.getJSONObject(0)
                    val content = candidate.optJSONObject("content")
                    val parts = content?.optJSONArray("parts")
                    if (parts != null && parts.length() > 0) {
                        return@withContext parts.getJSONObject(0).optString("text").trim()
                    }
                }
            } else {
                Log.e("GeminiChat", "Gemini HTTP error: $responseCode")
            }
        } catch (e: Exception) {
            Log.e("GeminiChat", "Failed to contact Gemini API", e)
        }

        // Calming empathetic fallback
        return@withContext if (language == "ES") {
            "Estoy aquí contigo. Noto la tensión en tu cuerpo; respiremos despacio juntos inhalando en 4 segundos y exhalando en 6."
        } else {
            "I am right here with you. Take a slow, gentle breath with me, inhaling for 4 seconds and exhaling for 6."
        }
    }

    fun onDestroy() {
        speechRecognizer?.destroy()
        textToSpeech?.stop()
        textToSpeech?.shutdown()
    }

    companion object {
        @Volatile
        private var instance: PulsoGeminiChatManager? = null

        fun getInstance(context: Context): PulsoGeminiChatManager {
            return instance ?: synchronized(this) {
                instance ?: PulsoGeminiChatManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
