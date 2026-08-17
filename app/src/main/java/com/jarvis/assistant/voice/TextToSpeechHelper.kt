package com.jarvis.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.jarvis.assistant.util.SettingsManager
import java.util.Locale

/**
 * MCU-style JARVIS speech: formal, measured, prefers British English.
 * Also supports Hindi (hi-IN) when assistant language is hi or auto+Hindi content.
 */
class TextToSpeechHelper(context: Context) {

    private var ready = false
    private var pending: String? = null
    private val settings = SettingsManager(context)
    private var voicePicked = false
    private var currentLangTag = "en-GB"

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                pickBestVoice(detectLangFor(""))
                applyVoiceSettings()
                pending?.let { speak(it) }
                pending = null
            }
        }
    }

    private fun detectLangFor(text: String): String {
        val pref = settings.getAssistantLanguage()
        if (pref == "hi") return "hi-IN"
        if (pref == "en" || pref == "en-GB") return "en-GB"
        // auto: Devanagari → Hindi, else British English (JARVIS default)
        if (text.any { it in '\u0900'..'\u097F' }) return "hi-IN"
        return "en-GB"
    }

    private fun pickBestVoice(langTag: String) {
        currentLangTag = langTag
        try {
            val target = Locale.forLanguageTag(langTag)
            val voices = tts.voices ?: emptySet()
            if (voices.isEmpty()) {
                tts.language = target
                voicePicked = true
                return
            }

            fun score(v: Voice): Int {
                var s = 0
                val tag = v.locale.toLanguageTag().lowercase()
                val name = v.name.lowercase()
                val want = langTag.lowercase()
                when {
                    tag == want.lowercase() -> s += 120
                    tag.startsWith(want.take(2)) -> s += 80
                    want.startsWith("en") && tag.startsWith("en-gb") -> s += 110
                    want.startsWith("en") && tag.startsWith("en") -> s += 50
                    else -> s -= 30
                }
                s += when (v.quality) {
                    Voice.QUALITY_VERY_HIGH -> 30
                    Voice.QUALITY_HIGH -> 20
                    Voice.QUALITY_NORMAL -> 10
                    else -> 0
                }
                if (want.startsWith("en") && (
                    name.contains("british") || name.contains("gb") ||
                    name.contains("male") || name.contains("daniel")
                )) s += 20
                if (name.contains("robot") || name.contains("funny")) s -= 40
                if (v.isNetworkConnectionRequired) s -= 3
                return s
            }

            val best = voices.maxByOrNull { score(it) }
            if (best != null && score(best) > 0) {
                tts.voice = best
                val d = "$" + "{best.name}"
                val e = "$" + "{best.locale}"
                Log.d("JarvisTTS", "Selected voice: " + best.name + " (" + best.locale + ")")
            } else {
                tts.language = target
            }
        } catch (_: Exception) {
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
        val lang = detectLangFor(text)
        if (lang != currentLangTag || !voicePicked) {
            pickBestVoice(lang)
        }
        applyVoiceSettings()
        val spoken = text
            .replace("...", ", ")
            .replace(" — ", ", ")
            .replace(" – ", ", ")
        tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utterance")
    }

    private fun applyVoiceSettings() {
        val userSpeed = settings.getVoiceSpeed()
        val userPitch = settings.getVoicePitch()
        // Original JARVIS: slightly deep, unhurried
        val (basePitch, baseRate) = when (settings.getVoiceType()) {
            "male" -> 0.86f to 0.90f
            "female" -> 1.10f to 0.94f
            "robot" -> 0.55f to 0.85f
            else -> 0.88f to 0.90f
        }
        tts.setPitch((basePitch * userPitch).coerceIn(0.5f, 1.5f))
        tts.setSpeechRate((baseRate * userSpeed).coerceIn(0.5f, 1.4f))
    }

    fun shutdown() {
        try {
            tts.stop()
            tts.shutdown()
        } catch (_: Exception) {
        }
    }
}
