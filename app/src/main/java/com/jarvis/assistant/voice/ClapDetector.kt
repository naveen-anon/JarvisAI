package com.jarvis.assistant.voice

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlin.math.abs
import kotlin.math.sqrt

/**
 * Double-clap detector using mic energy peaks.
 * No cloud / no API key. Runs on a background thread with [AudioRecord].
 *
 * Note: Only one capture client can own the mic. If Porcupine is already
 * recording, start this only when Porcupine is stopped (or clap-only mode).
 */
class ClapDetector {

    @Volatile private var running = false
    private var thread: Thread? = null
    private var recorder: AudioRecord? = null

    /**
     * @param sensitivity 1.0 default; lower = more sensitive (0.5–2.5)
     * @param onDoubleClap invoked on detector thread — hop to main if needed
     */
    @SuppressLint("MissingPermission")
    fun start(sensitivity: Float = 1.0f, onDoubleClap: () -> Unit) {
        stop()
        running = true
        val sens = sensitivity.coerceIn(0.5f, 2.5f)
        thread = Thread({
            android.os.Process.setThreadPriority(android.os.Process.THREAD_PRIORITY_AUDIO)
            loop(sens, onDoubleClap)
        }, "JarvisClapDetector").also { it.start() }
    }

    fun stop() {
        running = false
        try {
            thread?.join(500)
        } catch (_: InterruptedException) { }
        thread = null
        releaseRecorder()
    }

    fun isRunning(): Boolean = running

    private fun releaseRecorder() {
        try {
            recorder?.stop()
        } catch (_: Exception) { }
        try {
            recorder?.release()
        } catch (_: Exception) { }
        recorder = null
    }

    @SuppressLint("MissingPermission")
    private fun loop(sensitivity: Float, onDoubleClap: () -> Unit) {
        val sampleRate = 44100
        val channel = AudioFormat.CHANNEL_IN_MONO
        val encoding = AudioFormat.ENCODING_PCM_16BIT
        val minBuf = AudioRecord.getMinBufferSize(sampleRate, channel, encoding)
        if (minBuf == AudioRecord.ERROR || minBuf == AudioRecord.ERROR_BAD_VALUE) {
            Log.e(TAG, "Invalid AudioRecord buffer")
            running = false
            return
        }
        val bufSize = minBuf * 2
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                channel,
                encoding,
                bufSize
            )
        } catch (e: Exception) {
            Log.e(TAG, "AudioRecord create failed", e)
            running = false
            return
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord not initialized")
            record.release()
            running = false
            return
        }
        recorder = record
        try {
            record.startRecording()
        } catch (e: Exception) {
            Log.e(TAG, "startRecording failed", e)
            releaseRecorder()
            running = false
            return
        }

        val buffer = ShortArray(bufSize / 2)
        // Adaptive noise floor
        var noiseFloor = 400.0
        val peakThresholdFactor = 4.5 * sensitivity
        val minPeakAmplitude = 1800.0 * sensitivity
        var lastClapMs = 0L
        var clapCount = 0
        var cooldownUntil = 0L
        // Ignore samples right after a peak (ringing)
        var refractoryUntil = 0L

        Log.i(TAG, "Clap detector started (sensitivity=$sensitivity)")

        while (running) {
            val n = try {
                record.read(buffer, 0, buffer.size)
            } catch (e: Exception) {
                Log.e(TAG, "read failed", e)
                break
            }
            if (n <= 0) continue

            var sumSq = 0.0
            var maxAbs = 0
            for (i in 0 until n) {
                val v = buffer[i].toInt()
                val a = abs(v)
                if (a > maxAbs) maxAbs = a
                sumSq += v.toDouble() * v
            }
            val rms = sqrt(sumSq / n)

            val now = System.currentTimeMillis()
            // Slow noise floor tracking
            if (rms < noiseFloor * 1.5) {
                noiseFloor = noiseFloor * 0.95 + rms * 0.05
            }

            if (now < refractoryUntil || now < cooldownUntil) continue

            val threshold = maxOf(minPeakAmplitude, noiseFloor * peakThresholdFactor)
            if (rms >= threshold && maxAbs >= threshold) {
                // Clap-like transient
                if (now - lastClapMs <= CLAP_WINDOW_MS) {
                    clapCount++
                } else {
                    clapCount = 1
                }
                lastClapMs = now
                refractoryUntil = now + REFRACTORY_MS
                Log.d(TAG, "Clap peak #$clapCount rms=${rms.toInt()} thr=${threshold.toInt()}")

                if (clapCount >= 2) {
                    clapCount = 0
                    cooldownUntil = now + COOLDOWN_MS
                    Log.i(TAG, "Double clap detected")
                    try {
                        onDoubleClap()
                    } catch (e: Exception) {
                        Log.e(TAG, "onDoubleClap error", e)
                    }
                }
            } else if (clapCount > 0 && now - lastClapMs > CLAP_WINDOW_MS) {
                clapCount = 0
            }
        }

        releaseRecorder()
        running = false
        Log.i(TAG, "Clap detector stopped")
    }

    companion object {
        private const val TAG = "ClapDetector"
        private const val CLAP_WINDOW_MS = 1500L
        private const val REFRACTORY_MS = 200L
        private const val COOLDOWN_MS = 2500L
    }
}
