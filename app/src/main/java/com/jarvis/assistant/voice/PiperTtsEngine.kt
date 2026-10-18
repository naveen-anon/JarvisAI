package com.jarvis.assistant.voice

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.util.Log
import java.io.File
import java.io.FileOutputStream
import java.util.concurrent.atomic.AtomicBoolean

/**
 * On-device Piper TTS — no Google, no ElevenLabs.
 *
 * Expected assets (copy into app/src/main/assets/piper/):
 *   - en_GB-northern_english_male-medium.onnx
 *   - en_GB-northern_english_male-medium.onnx.json
 *   - espeak-ng-data/   (folder, from Piper release)
 *
 * Native library (optional, recommended):
 *   jniLibs/arm64-v8a/libpiper_jni.so  (build from rhasspy/piper or use a prebuilt)
 *
 * Until the .so + voice pack are present, [isReady] is false and [speak] returns false.
 */
class PiperTtsEngine(private val context: Context) {

    private val app = context.applicationContext
    private val modelDir = File(app.filesDir, "piper")
    private var player: MediaPlayer? = null
    private val speaking = AtomicBoolean(false)

    @Volatile
    private var nativeLoaded = false

    @Volatile
    var isReady: Boolean = false
        private set

    init {
        try {
            ensureAssetsExtracted()
            try {
                System.loadLibrary("piper_jni")
                nativeLoaded = true
                Log.i(TAG, "libpiper_jni loaded")
            } catch (e: UnsatisfiedLinkError) {
                nativeLoaded = false
                Log.w(TAG, "libpiper_jni not found — add jniLibs + voice pack. ${e.message}")
            }
            val onnx = File(modelDir, MODEL_ONNX)
            val json = File(modelDir, MODEL_JSON)
            isReady = nativeLoaded && onnx.exists() && json.exists()
            if (isReady) {
                nativeInit(onnx.absolutePath, json.absolutePath, modelDir.absolutePath)
                Log.i(TAG, "Piper ready at ${modelDir.absolutePath}")
            } else {
                Log.w(
                    TAG,
                    "Piper not ready. onnx=${onnx.exists()} json=${json.exists()} native=$nativeLoaded"
                )
            }
        } catch (e: Exception) {
            isReady = false
            Log.e(TAG, "Piper init failed: ${e.message}", e)
        }
    }

    /**
     * Synthesize [text] and play. Returns true if playback started.
     * Safe to call off main thread; playback is local.
     */
    fun speak(text: String): Boolean {
        if (!isReady) return false
        val cleaned = text
            .replace("...", ", ")
            .replace(" — ", ", ")
            .replace(" – ", ", ")
            .trim()
        if (cleaned.isEmpty()) return false
        val clipped = if (cleaned.length > 2000) cleaned.take(2000) else cleaned

        return try {
            stop()
            val wav = File(app.cacheDir, "piper_out.wav")
            if (wav.exists()) wav.delete()
            val ok = nativeSynthesize(clipped, wav.absolutePath)
            if (!ok || !wav.exists() || wav.length() < 44L) {
                Log.e(TAG, "nativeSynthesize failed")
                return false
            }
            playWav(wav)
            true
        } catch (e: Exception) {
            Log.e(TAG, "speak failed: ${e.message}", e)
            false
        }
    }

    fun stop() {
        try {
            player?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
        } catch (_: Exception) {
        }
        player = null
        speaking.set(false)
        try {
            if (nativeLoaded) nativeStop()
        } catch (_: Exception) {
        }
    }

    fun release() {
        stop()
        try {
            if (nativeLoaded) nativeRelease()
        } catch (_: Exception) {
        }
        isReady = false
    }

    private fun playWav(file: File) {
        val mp = MediaPlayer()
        player = mp
        mp.setAudioAttributes(
            AudioAttributes.Builder()
                .setUsage(AudioAttributes.USAGE_ASSISTANT)
                .setContentType(AudioAttributes.CONTENT_TYPE_SPEECH)
                .build()
        )
        mp.setDataSource(file.absolutePath)
        mp.setOnCompletionListener {
            speaking.set(false)
            try {
                it.release()
            } catch (_: Exception) {
            }
            if (player === it) player = null
        }
        mp.setOnErrorListener { _, _, _ ->
            speaking.set(false)
            true
        }
        mp.prepare()
        speaking.set(true)
        mp.start()
    }

    private fun ensureAssetsExtracted() {
        if (!modelDir.exists()) modelDir.mkdirs()
        val am = app.assets
        val prefix = "piper"
        val listed = try {
            am.list(prefix) ?: emptyArray()
        } catch (_: Exception) {
            emptyArray()
        }
        for (name in listed) {
            if (name.endsWith(".onnx") || name.endsWith(".json") || name.endsWith(".txt")) {
                val out = File(modelDir, name)
                if (out.exists() && out.length() > 0L) continue
                try {
                    am.open("$prefix/$name").use { input ->
                        FileOutputStream(out).use { output -> input.copyTo(output) }
                    }
                    Log.i(TAG, "Extracted $name")
                } catch (e: Exception) {
                    Log.w(TAG, "Skip asset $name: ${e.message}")
                }
            }
        }
        // espeak-ng-data directory copy (shallow + one level)
        extractDirAsset("$prefix/espeak-ng-data", File(modelDir, "espeak-ng-data"))
    }

    private fun extractDirAsset(assetPath: String, outDir: File) {
        val am = app.assets
        val children = try {
            am.list(assetPath) ?: return
        } catch (_: Exception) {
            return
        }
        if (children.isEmpty()) return
        if (!outDir.exists()) outDir.mkdirs()
        for (child in children) {
            val subAsset = "$assetPath/$child"
            val subOut = File(outDir, child)
            val deeper = try {
                am.list(subAsset)
            } catch (_: Exception) {
                null
            }
            if (deeper != null && deeper.isNotEmpty()) {
                extractDirAsset(subAsset, subOut)
            } else {
                if (subOut.exists() && subOut.length() > 0L) continue
                try {
                    am.open(subAsset).use { input ->
                        FileOutputStream(subOut).use { output -> input.copyTo(output) }
                    }
                } catch (_: Exception) {
                }
            }
        }
    }

    /** JNI — implemented in libpiper_jni.so */
    private external fun nativeInit(modelPath: String, configPath: String, dataDir: String): Boolean
    private external fun nativeSynthesize(text: String, wavOutPath: String): Boolean
    private external fun nativeStop()
    private external fun nativeRelease()

    companion object {
        private const val TAG = "PiperTTS"
        const val MODEL_ONNX = "en_GB-northern_english_male-medium.onnx"
        const val MODEL_JSON = "en_GB-northern_english_male-medium.onnx.json"
    }
}
