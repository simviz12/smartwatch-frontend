package com.pulso.wear.data.speech

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.util.Log
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

class WearSpeechManager private constructor(private val context: Context) : TextToSpeech.OnInitListener {

    private var tts: TextToSpeech? = null
    private var isReady = false

    private val _isSpeaking = MutableStateFlow(false)
    val isSpeaking: StateFlow<Boolean> = _isSpeaking.asStateFlow()

    init {
        try {
            tts = TextToSpeech(context.applicationContext, this)
        } catch (e: Exception) {
            Log.e("WearSpeechManager", "Failed to init TextToSpeech on Wear OS", e)
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            val result = tts?.setLanguage(Locale("es", "ES"))
            if (result == TextToSpeech.LANG_MISSING_DATA || result == TextToSpeech.LANG_NOT_SUPPORTED) {
                tts?.setLanguage(Locale.US)
            }
            tts?.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
                override fun onStart(utteranceId: String?) {
                    _isSpeaking.value = true
                }

                override fun onDone(utteranceId: String?) {
                    _isSpeaking.value = false
                }

                @Deprecated("Deprecated in Java")
                override fun onError(utteranceId: String?) {
                    _isSpeaking.value = false
                }
            })
            isReady = true
            Log.i("WearSpeechManager", "Wear OS TextToSpeech ready")
        } else {
            Log.w("WearSpeechManager", "Wear OS TextToSpeech init failed code: $status")
        }
    }

    fun speak(text: String) {
        Log.i("WearSpeechManager", "speak called with: $text, isReady=$isReady")
        if (!isReady || tts == null) {
            Log.w("WearSpeechManager", "Wear OS TTS not ready to speak")
            return
        }
        _isSpeaking.value = true
        tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "PULSO_WEAR_TTS")
    }

    fun stop() {
        tts?.stop()
        _isSpeaking.value = false
    }

    fun shutdown() {
        tts?.stop()
        tts?.shutdown()
        _isSpeaking.value = false
    }

    companion object {
        @Volatile
        private var instance: WearSpeechManager? = null

        fun getInstance(context: Context): WearSpeechManager {
            return instance ?: synchronized(this) {
                instance ?: WearSpeechManager(context.applicationContext).also { instance = it }
            }
        }
    }
}
