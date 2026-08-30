package com.jarvis.assistant.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import com.jarvis.assistant.BuildConfig
import com.jarvis.assistant.util.SettingsManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.io.File
import java.util.concurrent.TimeUnit

/**
 * Cloud TTS via ElevenLabs — natural / cinematic Jarvis-style voice.
 *
 * Free tier: https://elevenlabs.io → API key in local.properties as ELEVENLABS_API_KEY
 * Optional: ELEVENLABS_VOICE_ID (dashboard → Voices → copy ID)
 *
 * Default voice is a deep male stock voice suitable for JARVIS tone.
 */
class ElevenLabsTtsClient(private val context: Context) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(45, TimeUnit.SECONDS)
        .build()

    private var player: MediaPlayer? = null

    val isConfigured: Boolean
        get() = BuildConfig.ELEVENLABS_API_KEY.trim().isNotEmpty()

    private val apiKey: String
        get() = BuildConfig.ELEVENLABS_API_KEY.trim()

    private val settings = SettingsManager(context)

    /** Female preference always uses a distinct known-good voice; male keeps
     *  any custom BuildConfig override (from local.properties / CI secret),
     *  falling back to the stock male voice if none was set. */
    private val voiceId: String
        get() {
            if (settings.getVoiceType() == "female") return FEMALE_VOICE_ID
            val override = BuildConfig.ELEVENLABS_VOICE_ID.trim()
            return if (override.isNotEmpty()) override else DEFAULT_VOICE_ID
        }

    /**
     * Synthesize [text] and play. Returns true if audio started successfully.
     * Call from a background coroutine; playback is started on completion.
     */
    suspend fun speak(text: String): Boolean = withContext(Dispatchers.IO) {
        if (!isConfigured || text.isBlank()) return@withContext false
        val cleaned = text
            .replace("...", ", ")
            .replace(" — ", ", ")
            .replace(" – ", ", ")
            .trim()
        if (cleaned.isEmpty()) return@withContext false
        // Free tier: keep requests short
        val clipped = if (cleaned.length > 2500) cleaned.take(2500) else cleaned

        try {
            val bodyJson = JSONObject().apply {
                put("text", clipped)
                put("model_id", "eleven_multilingual_v2")
                put("voice_settings", JSONObject().apply {
                    put("stability", 0.45)
                    put("similarity_boost", 0.80)
                    put("style", 0.15)
                    put("use_speaker_boost", true)
                })
            }
            val request = Request.Builder()
                .url("$BASE_URL/text-to-speech/$voiceId")
                .addHeader("xi-api-key", apiKey)
                .addHeader("Accept", "audio/mpeg")
                .addHeader("Content-Type", "application/json")
                .post(bodyJson.toString().toRequestBody(JSON_MEDIA))
                .build()

            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) {
                    val err = response.body?.string()?.take(200) ?: ""
                    Log.e(TAG, "ElevenLabs HTTP ${response.code}: $err")
                    return@withContext false
                }
                val bytes = response.body?.bytes() ?: return@withContext false
                if (bytes.isEmpty()) return@withContext false

                val cache = File(context.cacheDir, "jarvis_tts_el.mp3")
                cache.writeBytes(bytes)

                withContext(Dispatchers.Main) {
                    playFile(cache)
                }
                true
            }
        } catch (e: Exception) {
            Log.e(TAG, "ElevenLabs speak failed: ${e.message}", e)
            false
        }
    }

    private fun playFile(file: File) {
        stop()
        try {
            player = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ASSISTANT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                        .build()
                )
                setDataSource(file.absolutePath)
                setOnCompletionListener {
                    try {
                        it.release()
                    } catch (_: Exception) {
                    }
                    if (player === it) player = null
                }
                setOnErrorListener { mp, _, _ ->
                    try {
                        mp.release()
                    } catch (_: Exception) {
                    }
                    if (player === mp) player = null
                    true
                }
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "MediaPlayer failed: ${e.message}")
            player = null
        }
    }

    fun stop() {
        try {
            player?.stop()
        } catch (_: Exception) {
        }
        try {
            player?.release()
        } catch (_: Exception) {
        }
        player = null
    }

    companion object {
        private const val TAG = "ElevenLabsTTS"
        private const val BASE_URL = "https://api.elevenlabs.io/v1"
        private val JSON_MEDIA = "application/json; charset=utf-8".toMediaType()

        /** Deep male stock voice (Adam) — override with ELEVENLABS_VOICE_ID */
        const val DEFAULT_VOICE_ID = "pNInz6obpgDQGcFmaJgB"
        /** Well-known ElevenLabs premade female voice (Rachel) */
        const val FEMALE_VOICE_ID = "21m00Tcm4TlvDq8ikWAM"
    }
}
