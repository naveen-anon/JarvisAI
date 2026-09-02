package com.jarvis.assistant.util

import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import com.jarvis.assistant.accessibility.JarvisAccessibilityService
import com.jarvis.assistant.vision.TextRecognitionHelper
import com.google.mlkit.vision.common.InputImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import kotlin.coroutines.resume

/**
 * Screenshot → text analysis.
 * Prefer AccessibilityService.takeScreenshot (API 30+), else fall back to node text dump.
 */
object ScreenshotHelper {
    private const val TAG = "ScreenshotHelper"

    data class Result(
        val summary: String,
        val usedOcr: Boolean,
        val rawText: String
    )

    suspend fun captureAndAnalyze(): Result = withContext(Dispatchers.Main) {
        val svc = JarvisAccessibilityService.instance
            ?: return@withContext Result(
                summary = "Enable Accessibility for Jarvis first — Settings → Accessibility → Jarvis.",
                usedOcr = false,
                rawText = ""
            )

        // Try real screenshot + OCR on API 30+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            val bitmap = takeBitmap(svc)
            if (bitmap != null) {
                val ocr = withContext(Dispatchers.Default) {
                    try {
                        val image = InputImage.fromBitmap(bitmap, 0)
                        TextRecognitionHelper.recognize(image)
                    } catch (e: Exception) {
                        Log.e(TAG, "OCR failed", e)
                        null
                    } finally {
                        if (!bitmap.isRecycled) bitmap.recycle()
                    }
                }
                if (!ocr.isNullOrBlank() &&
                    !ocr.startsWith("I couldn't find") &&
                    !ocr.startsWith("Text recognition failed")
                ) {
                    val short = ocr.replace(Regex("\\s+"), " ").trim()
                    val clipped = if (short.length > 500) short.take(500).trimEnd() + "…" else short
                    return@withContext Result(
                        summary = "On screen I can read: $clipped",
                        usedOcr = true,
                        rawText = short
                    )
                }
            }
        }

        // Fallback: accessibility node text
        val text = try {
            svc.getScreenText()
        } catch (_: Exception) {
            "No screen content available."
        }
        if (text.isBlank() || text.startsWith("No screen") || text.startsWith("Screen appears")) {
            return@withContext Result(
                summary = text.ifBlank { "Screen appears empty." },
                usedOcr = false,
                rawText = text
            )
        }
        val short = text.replace(Regex("\\s+"), " ").trim()
        val clipped = if (short.length > 500) short.take(500).trimEnd() + "…" else short
        Result(
            summary = "On screen I can see: $clipped",
            usedOcr = false,
            rawText = short
        )
    }

    private suspend fun takeBitmap(svc: JarvisAccessibilityService): Bitmap? {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return null
        return suspendCancellableCoroutine { cont ->
            try {
                svc.takeScreenshot(
                    android.view.Display.DEFAULT_DISPLAY,
                    svc.mainExecutor,
                    object : android.accessibilityservice.AccessibilityService.TakeScreenshotCallback {
                        override fun onSuccess(screenshot: android.accessibilityservice.AccessibilityService.ScreenshotResult) {
                            try {
                                val hw = screenshot.hardwareBuffer
                                val colorSpace = screenshot.colorSpace
                                val bmp = Bitmap.wrapHardwareBuffer(hw, colorSpace)
                                hw.close()
                                cont.resume(bmp?.copy(Bitmap.Config.ARGB_8888, false))
                            } catch (e: Exception) {
                                Log.e(TAG, "bitmap wrap failed", e)
                                cont.resume(null)
                            }
                        }

                        override fun onFailure(errorCode: Int) {
                            Log.w(TAG, "takeScreenshot failed code=$errorCode")
                            cont.resume(null)
                        }
                    }
                )
            } catch (e: Exception) {
                Log.e(TAG, "takeScreenshot", e)
                cont.resume(null)
            }
        }
    }
}
