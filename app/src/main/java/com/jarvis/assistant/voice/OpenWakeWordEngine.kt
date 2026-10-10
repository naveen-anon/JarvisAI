package com.jarvis.assistant.voice

import android.content.Context
import android.util.Log
import com.rementia.openwakeword.lib.WakeWordEngine
import com.rementia.openwakeword.lib.model.DetectionMode
import com.rementia.openwakeword.lib.model.WakeWordModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch

class OpenWakeWordEngine(private val context: Context) {

    private var engine: WakeWordEngine? = null
    private var collectJob: Job? = null
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    fun start(onWake: () -> Unit): Boolean {
        return try {
            stop()
            val models = listOf(
                WakeWordModel(
                    name = "Hey Jarvis",
                    modelPath = "hey_jarvis.onnx",
                    threshold = 0.35f
                )
            )
            val eng = WakeWordEngine(
                context = context.applicationContext,
                models = models,
                detectionMode = DetectionMode.SINGLE_BEST,
                detectionCooldownMs = 2500L
            )
            engine = eng
            eng.start()
            collectJob = scope.launch {
                eng.detections
                    .catch { e -> Log.e(TAG, "detections error", e) }
                    .collect { det ->
                        Log.i(TAG, "wake detected: " + det.model.name + " score=" + det.score)
                        onWake()
                    }
            }
            Log.i(TAG, "started — say hey jarvis")
            true
        } catch (e: Exception) {
            Log.e(TAG, "start failed: " + e.message, e)
            false
        }
    }

    fun stop() {
        try { collectJob?.cancel() } catch (_: Exception) {}
        collectJob = null
        try { engine?.stop() } catch (_: Exception) {}
        try { engine?.release() } catch (_: Exception) {}
        engine = null
    }

    companion object {
        private const val TAG = "OpenWakeWord"
    }
}
