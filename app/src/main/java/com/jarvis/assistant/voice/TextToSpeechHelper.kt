package com.jarvis.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.jarvis.assistant.util.SettingsManager
import java.util.Locale

/**
 * Speaks in the language of the text when a matching TTS voice is installed.
 * Defaults to British English (MCU JARVIS). Supports many locales via Android TTS packs.
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
                pickBestVoice("en-GB")
                applyVoiceSettings()
                pending?.let { speak(it) }
                pending = null
            }
        }
    }

    /** Detect language tag from script / preference for "all languages" speaking. */
    private fun detectLangFor(text: String): String {
        val pref = settings.getAssistantLanguage()
        if (pref != "auto" && pref.isNotBlank()) {
            return when (pref) {
                "hi" -> "hi-IN"
                "en" -> "en-GB"
                else -> pref
            }
        }
        if (text.isBlank()) return "en-GB"

        // Unicode script ranges → BCP-47 tags (TTS pack must be installed on device)
        for (ch in text) {
            val c = ch.code
            when {
                c in 0x0900..0x097F -> return "hi-IN"   // Devanagari Hindi
                c in 0x0980..0x09FF -> return "bn-IN"   // Bengali
                c in 0x0A00..0x0A7F -> return "pa-IN"   // Gurmukhi Punjabi
                c in 0x0A80..0x0AFF -> return "gu-IN"   // Gujarati
                c in 0x0B00..0x0B7F -> return "or-IN"   // Odia
                c in 0x0B80..0x0BFF -> return "ta-IN"   // Tamil
                c in 0x0C00..0x0C7F -> return "te-IN"   // Telugu
                c in 0x0C80..0x0CFF -> return "kn-IN"   // Kannada
                c in 0x0D00..0x0D7F -> return "ml-IN"   // Malayalam
                c in 0x0600..0x06FF -> return "ar"      // Arabic
                c in 0x0400..0x04FF -> return "ru-RU"   // Cyrillic
                c in 0x4E00..0x9FFF -> return "zh-CN"   // CJK
                c in 0x3040..0x30FF -> return "ja-JP"   // Hiragana/Katakana
                c in 0xAC00..0xD7AF -> return "ko-KR"   // Hangul
                c in 0x0E00..0x0E7F -> return "th-TH"   // Thai
            }
        }
        return "en-GB"
    }

    private fun pickBestVoice(langTag: String) {
        currentLangTag = langTag
        try {
            val target = Locale.forLanguageTag(langTag.replace('_', '-'))
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
                val want = langTag.lowercase().replace('_', '-')
                val want2 = want.take(2)
                when {
                    tag.equals(want, true) -> s += 120
                    tag.startsWith(want2) -> s += 70
                    want.startsWith("en") && tag.startsWith("en-gb") -> s += 100
                    want.startsWith("en") && tag.startsWith("en") -> s += 40
                    else -> s -= 20
                }
                s += when (v.quality) {
                    Voice.QUALITY_VERY_HIGH -> 30
                    Voice.QUALITY_HIGH -> 20
                    Voice.QUALITY_NORMAL -> 10
                    else -> 0
                }
                if (want.startsWith("en") && (name.contains("british") || name.contains("gb") || name.contains("male"))) s += 15
                if (name.contains("robot") || name.contains("funny")) s -= 40
                return s
            }

            val best = voices.maxByOrNull { score(it) }
            if (best != null && score(best) > 0) {
                tts.voice = best
                Log.d("JarvisTTS", "Selected voice: " + best.name + " (" + best.locale + ")")
            } else {
                // Fallback chain
                if (tts.isLanguageAvailable(target) >= TextToSpeech.LANG_AVAILABLE) {
                    tts.language = target
                } else {
                    tts.language = Locale.UK
                }
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
        if (lang != currentLangTag || !voicePicked) pickBestVoice(lang)
        applyVoiceSettings()
        val spoken = text.replace("...", ", ").replace(" — ", ", ").replace(" – ", ", ")
        tts.speak(spoken, TextToSpeech.QUEUE_FLUSH, null, "jarvis_utterance")
    }

    private fun applyVoiceSettings() {
        val userSpeed = settings.getVoiceSpeed()
        val userPitch = settings.getVoicePitch()
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
        try { tts.stop(); tts.shutdown() } catch (_: Exception) {}
    }
}
