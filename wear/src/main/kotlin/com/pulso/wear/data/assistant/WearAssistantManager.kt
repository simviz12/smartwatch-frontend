package com.pulso.wear.data.assistant

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer
import android.util.Log
import com.pulso.core.model.AssistantAsk
import com.pulso.core.model.AssistantReply
import com.pulso.wear.data.speech.WearSpeechManager
import com.pulso.wear.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID

enum class AssistantPhase { IDLE, LISTENING, THINKING, SPEAKING, ANSWERED, ERROR }

data class AssistantUiState(
    val phase: AssistantPhase = AssistantPhase.IDLE,
    val transcript: String = "",
    val reply: String = "",
    val suggestsBreathing: Boolean = false,
    /** Microphone level 0..1 for the listening bars. */
    val level: Float = 0f,
    val error: String? = null
)

/**
 * Voice assistant on the wrist: speech is transcribed on the watch, the phone answers
 * with its LLM over the Data Layer, and the answer is spoken by the watch's TTS.
 */
class WearAssistantManager private constructor(private val context: Context) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val mainHandler = Handler(Looper.getMainLooper())
    private val speech by lazy { WearSpeechManager.getInstance(context) }
    private var recognizer: SpeechRecognizer? = null
    private var pendingRequestId: String? = null
    private var timeoutJob: Job? = null
    private var speakingWatch: Job? = null

    private val _state = MutableStateFlow(AssistantUiState())
    val state: StateFlow<AssistantUiState> = _state.asStateFlow()

    /** True when the watch can transcribe in-app; otherwise the UI falls back to the system voice input. */
    fun isInAppRecognitionAvailable(): Boolean = SpeechRecognizer.isRecognitionAvailable(context)

    fun startListening() {
        speech.stop()
        mainHandler.post {
            try {
                recognizer?.destroy()
                val r = SpeechRecognizer.createSpeechRecognizer(context)
                recognizer = r
                r.setRecognitionListener(listener)
                _state.value = AssistantUiState(phase = AssistantPhase.LISTENING)
                r.startListening(recognizerIntent())
            } catch (e: Exception) {
                Log.e(TAG, "Speech recognizer failed to start", e)
                fail("No pude usar el micrófono")
            }
        }
    }

    fun stopListening() {
        mainHandler.post { recognizer?.stopListening() }
    }

    fun recognizerIntent(): Intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
        putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
        putExtra(RecognizerIntent.EXTRA_LANGUAGE, "es-CO")
        putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
        putExtra(RecognizerIntent.EXTRA_PROMPT, "Te escucho…")
    }

    /** Sends a transcribed sentence to the phone assistant and waits for its answer. */
    fun submit(text: String) {
        val clean = text.trim()
        if (clean.isEmpty()) {
            fail("No te escuché bien. Toca para intentar de nuevo")
            return
        }
        val requestId = UUID.randomUUID().toString()
        pendingRequestId = requestId
        _state.value = AssistantUiState(phase = AssistantPhase.THINKING, transcript = clean)
        scope.launch {
            val sent = SyncManager.getInstance(context).sendAssistantAsk(
                AssistantAsk(requestId = requestId, text = clean.take(AssistantAsk.MAX_TEXT), sentAt = System.currentTimeMillis())
            )
            if (!sent) {
                fail("Tu celular no está conectado")
                return@launch
            }
            timeoutJob?.cancel()
            timeoutJob = scope.launch {
                delay(REPLY_TIMEOUT_MS)
                if (pendingRequestId == requestId) fail("El celular tardó demasiado en responder")
            }
        }
    }

    fun onReply(reply: AssistantReply) {
        scope.launch {
            if (reply.requestId != pendingRequestId) return@launch
            pendingRequestId = null
            timeoutJob?.cancel()
            _state.value = _state.value.copy(
                phase = AssistantPhase.SPEAKING,
                reply = reply.text,
                suggestsBreathing = reply.suggestsBreathing,
                error = null
            )
            speech.speak(reply.text)
            speakingWatch?.cancel()
            speakingWatch = scope.launch {
                // Wait for TTS to start then finish; fall back to "answered" if TTS is unavailable.
                delay(1500)
                while (speech.isSpeaking.value) delay(200)
                if (_state.value.phase == AssistantPhase.SPEAKING) {
                    _state.value = _state.value.copy(phase = AssistantPhase.ANSWERED)
                }
            }
        }
    }

    fun stopSpeaking() {
        speech.stop()
        speakingWatch?.cancel()
        if (_state.value.phase == AssistantPhase.SPEAKING) _state.value = _state.value.copy(phase = AssistantPhase.ANSWERED)
    }

    fun reset() {
        stopSpeaking()
        mainHandler.post { recognizer?.cancel() }
        pendingRequestId = null
        timeoutJob?.cancel()
        _state.value = AssistantUiState()
    }

    private fun fail(message: String) {
        scope.launch { _state.value = _state.value.copy(phase = AssistantPhase.ERROR, error = message, level = 0f) }
    }

    private val listener = object : RecognitionListener {
        override fun onReadyForSpeech(params: Bundle?) {}
        override fun onBeginningOfSpeech() {}
        override fun onRmsChanged(rmsdB: Float) {
            // rmsdB is roughly -2..10 dB; normalise to 0..1 for the bars.
            val level = ((rmsdB + 2f) / 12f).coerceIn(0f, 1f)
            if (_state.value.phase == AssistantPhase.LISTENING) _state.value = _state.value.copy(level = level)
        }
        override fun onBufferReceived(buffer: ByteArray?) {}
        override fun onEndOfSpeech() {
            _state.value = _state.value.copy(level = 0f)
        }
        override fun onError(error: Int) {
            Log.w(TAG, "Speech recognition error $error")
            fail(
                when (error) {
                    SpeechRecognizer.ERROR_NO_MATCH, SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "No te escuché bien. Toca para intentar de nuevo"
                    SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "Activa el permiso de micrófono"
                    SpeechRecognizer.ERROR_NETWORK, SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "Sin conexión para reconocer tu voz"
                    else -> "No pude escucharte"
                }
            )
        }
        override fun onResults(results: Bundle?) {
            val text = results?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull().orEmpty()
            submit(text)
        }
        override fun onPartialResults(partialResults: Bundle?) {
            val partial = partialResults?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)?.firstOrNull()
            if (!partial.isNullOrBlank()) _state.value = _state.value.copy(transcript = partial)
        }
        override fun onEvent(eventType: Int, params: Bundle?) {}
    }

    companion object {
        private const val TAG = "WearAssistant"
        private const val REPLY_TIMEOUT_MS = 25_000L

        @Volatile
        private var INSTANCE: WearAssistantManager? = null

        fun getInstance(context: Context): WearAssistantManager =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: WearAssistantManager(context.applicationContext).also { INSTANCE = it }
            }
    }
}
