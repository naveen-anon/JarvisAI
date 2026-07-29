package com.jarvis.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import java.util.Locale

object TextToSpeechHelper {
    private var tts: TextToSpeech? = null
    private var isInitialized = false

    fun speak(context: Context, text: String) {
        if (tts == null) {
            tts = TextToSpeech(context.applicationContext) { status ->
                if (status == TextToSpeech.SUCCESS) {
                    tts?.language = Locale.getDefault()
                    isInitialized = true
                    tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JarvisTTS")
                }
            }
        } else {
            tts?.speak(text, TextToSpeech.QUEUE_FLUSH, null, "JarvisTTS")
        }
    }
}
