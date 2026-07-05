package com.jarvis.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

class TextToSpeechHelper(context: Context) {

    private var ready = false
    private val tts: TextToSpeech = TextToSpeech(context) { status ->
        if (status == TextToSpeech.SUCCESS) {
            ready = true
        }
    }

    init {
        tts.language = Locale.US
    }

    fun speak(text: String) {
        if (!ready || text.isBlank()) return
        tts.speak(text, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utterance")
    }

    fun shutdown() {
        tts.stop()
        tts.shutdown()
    }
}
