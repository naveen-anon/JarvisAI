package com.jarvis.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.jarvis.assistant.util.SettingsManager
import java.util.Locale

class TextToSpeechHelper(context: Context) {

    private var ready = false
    private var pending: String? = null
    private val settings = SettingsManager(context)
    private var voicePicked = false

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                pickBestVoice()
                applyVoiceSettings()
                pending?.let { speak(it) }
                pending = null
            }
        }
    }

    private fun pickBestVoice() {
        if (voicePicked) return
        try {
            val voices = tts.voices ?: emptySet()
            if (voices.isEmpty()) {
                tts.language = Locale.UK
                voicePicked = true
                return
            }

            fun score(v: Voice): Int {
                var s = 0
                val tag = v.locale.toLanguageTag().lowercase()
                val name = v.name.lowercase()
                when {
                    tag.startsWith("en-gb") -> s += 100
                    tag.startsWith("en-in") -> s += 70
                    tag.startsWith("en") -> s += 40
                    else -> s -= 50
                }
                s += when (v.quality) {
                    Voice.QUALITY_VERY_HIGH -> 30
                    Voice.QUALITY_HIGH -> 20
                    Voice.QUALITY_NORMAL -> 10
                    else -> 0
                }
                if (name.contains("male") || name.contains("british") ||
                    name.contains("gb") || name.contains("daniel") ||
                    name.contains("en-gb")
                ) s += 15
                if (name.contains("robot") || name.contains("funny")) s -= 40
                if (v.isNetworkConnectionRequired) s -= 5
                return s
            }

            val best = voices.maxByOrNull { score(it) }
            if (best != null && score(best) > 0) {
                tts.voice = best
                Log.d("JarvisTTS", "Selected voice: ${best.name} (${best.locale})")
            } else {
                tts.language = Locale.UK
            }
        } catch (e: Exception) {
            tts.language = Locale.UK
        }
        voicePicked = true
    }

    fun speak(text: String) {
        if (text.isBlank()) return
        if (!ready) {
            pending = text
            return
        }
        applyVoiceSettings()
        val spoken = text
            .replace("...", ", ")
            .replace(" — ", ", ")
            .replace(" – ", ", ")
        tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utterance")
    }

    private fun applyVoiceSettings() {
        if (!voicePicked) pickBestVoice()

        val userSpeed = settings.getVoiceSpeed()
        val userPitch = settings.getVoicePitch()

        val (basePitch, baseRate) = when (settings.getVoiceType()) {
            "male" -> 0.88f to 0.92f
            "female" -> 1.12f to 0.95f
            "robot" -> 0.55f to 0.85f
            else -> 0.90f to 0.92f
        }

        val pitch = (basePitch * userPitch).coerceIn(0.5f, 1.6f)
        val rate = (baseRate * userSpeed).coerceIn(0.5f, 1.5f)
        tts.setPitch(pitch)
        tts.setSpeechRate(rate)
    }

    fun shutdown() {
        try {
            tts.stop()
            tts.shutdown()
        } catch (_: Exception) {
        }
    }
}
