package com.jarvis.assistant.voice

import android.content.Context
import android.speech.tts.TextToSpeech
import android.speech.tts.Voice
import android.util.Log
import com.jarvis.assistant.util.NetworkStatusManager
import com.jarvis.assistant.util.SettingsManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import java.util.Locale

/**
 * Speaks replies with upgraded voice pipeline:
 * 1. **ElevenLabs** (human / cinematic) when API key is set + online + cloud voice enabled
 * 2. **Android system TTS** offline fallback — prefers British English male (MCU JARVIS tone)
 */
class TextToSpeechHelper(context: Context) {

    private val appContext = context.applicationContext
    private var ready = false
    private var pending: String? = null
    private val settings = SettingsManager(appContext)
    private val network = NetworkStatusManager(appContext)
    private val eleven = ElevenLabsTtsClient(appContext)
    private val scope = CoroutineScope(Dispatchers.Main)
    private var cloudJob: Job? = null
    private var voicePicked = false
    private var currentLangTag = "en-GB"

    private lateinit var tts: TextToSpeech

    init {
        tts = TextToSpeech(appContext) { status ->
            if (status == TextToSpeech.SUCCESS) {
                ready = true
                pickBestVoice("en-GB")
                applyVoiceSettings()
                pending?.let { speak(it) }
                pending = null
            }
        }
    }

    /** Detect language tag from script / preference. */
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

        for (ch in text) {
            val c = ch.code
            when {
                c in 0x0900..0x097F -> return "hi-IN"
                c in 0x0980..0x09FF -> return "bn-IN"
                c in 0x0A00..0x0A7F -> return "pa-IN"
                c in 0x0A80..0x0AFF -> return "gu-IN"
                c in 0x0B00..0x0B7F -> return "or-IN"
                c in 0x0B80..0x0BFF -> return "ta-IN"
                c in 0x0C00..0x0C7F -> return "te-IN"
                c in 0x0C80..0x0CFF -> return "kn-IN"
                c in 0x0D00..0x0D7F -> return "ml-IN"
                c in 0x0600..0x06FF -> return "ar"
                c in 0x0400..0x04FF -> return "ru-RU"
                c in 0x4E00..0x9FFF -> return "zh-CN"
                c in 0x3040..0x30FF -> return "ja-JP"
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
                if (want.startsWith("en")) {
                    if (name.contains("british") || name.contains("gb") || name.contains("uk")) s += 20
                    if (name.contains("male") || name.contains("daniel") || name.contains("george")
                        || name.contains("arthur") || name.contains("ryan")
                    ) s += 18
                    if (name.contains("network") || name.contains("enhanced") || name.contains("premium")) s += 10
                }
                if (name.contains("robot") || name.contains("funny") || name.contains("whisper")) s -= 40
                return s
            }

            val best = voices.maxByOrNull { score(it) }
            if (best != null && score(best) > 0) {
                tts.voice = best
                Log.d(TAG, "Offline voice: ${best.name} (${best.locale})")
            } else {
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

        val preferCloud = settings.getPreferCloudVoice()
        if (preferCloud && eleven.isConfigured && network.isOnline()) {
            cloudJob?.cancel()
            eleven.stop()
            try {
                tts.stop()
            } catch (_: Exception) {
            }
            cloudJob = scope.launch {
                val ok = eleven.speak(text)
                if (!ok) {
                    Log.w(TAG, "Cloud TTS failed — offline fallback")
                    speakOffline(text)
                }
            }
            return
        }

        speakOffline(text)
    }

    private fun speakOffline(text: String) {
        eleven.stop()
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
            "male" -> 0.82f to 0.88f
            "female" -> 1.08f to 0.94f
            "robot" -> 0.50f to 0.82f
            else -> 0.84f to 0.88f
        }
        tts.setPitch((basePitch * userPitch).coerceIn(0.5f, 1.5f))
        tts.setSpeechRate((baseRate * userSpeed).coerceIn(0.5f, 1.4f))
    }

    fun stop() {
        cloudJob?.cancel()
        eleven.stop()
        try {
            tts.stop()
        } catch (_: Exception) {
        }
    }

    fun shutdown() {
        stop()
        try {
            tts.shutdown()
        } catch (_: Exception) {
        }
    }

    companion object {
        private const val TAG = "JarvisTTS"
    }
}
