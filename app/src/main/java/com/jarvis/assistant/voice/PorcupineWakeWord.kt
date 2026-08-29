package com.jarvis.assistant.voice

import android.content.Context
import android.util.Log
import ai.picovoice.porcupine.Porcupine
import ai.picovoice.porcupine.PorcupineException
import ai.picovoice.porcupine.PorcupineManager
import ai.picovoice.porcupine.PorcupineManagerCallback
import ai.picovoice.porcupine.PorcupineManagerErrorCallback
import com.jarvis.assistant.BuildConfig

/**
 * On-device wake-word engine using Picovoice Porcupine.
 *
 * Uses the built-in keyword **JARVIS** (English). No custom .ppn file required.
 * AccessKey: https://console.picovoice.ai/ → set PICOVOICE_ACCESS_KEY in local.properties.
 *
 * When the key is missing or init fails, [isAvailable] is false and the service
 * falls back to SpeechRecognizer continuous keyword matching.
 */
class PorcupineWakeWord(private val context: Context) {

    private var manager: PorcupineManager? = null
    private var started = false

    val isAvailable: Boolean
        get() = accessKey.isNotBlank()

    private val accessKey: String
        get() = BuildConfig.PICOVOICE_ACCESS_KEY.trim()

    /**
     * Start continuous mic listening for "Jarvis".
     * [onWake] may be invoked off the main thread — hop to main if updating UI/STT.
     */
    fun start(onWake: () -> Unit, onError: ((String) -> Unit)? = null): Boolean {
        if (!isAvailable) {
            onError?.invoke("PICOVOICE_ACCESS_KEY missing")
            return false
        }
        stop()
        return try {
            val wakeCallback = PorcupineManagerCallback {
                Log.d(TAG, "Wake word detected: JARVIS")
                onWake()
            }
            val errorCallback = PorcupineManagerErrorCallback { error ->
                Log.e(TAG, "Porcupine error: ${error.message}")
                onError?.invoke(error.message ?: "Porcupine error")
            }
            manager = PorcupineManager.Builder()
                .setAccessKey(accessKey)
                .setKeyword(Porcupine.BuiltInKeyword.JARVIS)
                .setSensitivity(0.65f)
                .setErrorCallback(errorCallback)
                .build(context, wakeCallback)
            manager?.start()
            started = true
            Log.i(TAG, "Porcupine listening for \"Jarvis\"")
            true
        } catch (e: PorcupineException) {
            Log.e(TAG, "Failed to start Porcupine", e)
            manager = null
            started = false
            onError?.invoke(e.message ?: e.toString())
            false
        } catch (e: Exception) {
            Log.e(TAG, "Unexpected Porcupine failure", e)
            manager = null
            started = false
            onError?.invoke(e.message ?: e.toString())
            false
        }
    }

    fun stop() {
        try {
            manager?.stop()
        } catch (e: Exception) {
            Log.w(TAG, "stop(): ${e.message}")
        }
        try {
            manager?.delete()
        } catch (e: Exception) {
            Log.w(TAG, "delete(): ${e.message}")
        }
        manager = null
        started = false
    }

    fun isRunning(): Boolean = started && manager != null

    companion object {
        private const val TAG = "PorcupineWakeWord"
    }
}
