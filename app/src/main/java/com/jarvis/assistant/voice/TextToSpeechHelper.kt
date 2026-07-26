package com.jarvis.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechHelper(context: Context) {

    private var ready = false
    // Queue up anything spoken before the engine finished initializing instead of
    // silently dropping it — engine init is async and commonly takes 200-500ms,
    // easily longer than the time between service start and the first response.
    private var pending: String? = null

    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ready = true
            tts.language = Locale.US
            pending?.let { speak(it) }
            pending = null
        }
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready) {
            pending = text
            return
        }
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utterance")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
