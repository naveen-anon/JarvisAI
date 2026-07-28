package com.jarvis.assistant.vision

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Phase 4 — "Image ka text padhe (OCR)". Fully on-device, no network call. */
object TextRecognitionHelper {

    suspend fun recognize(image: InputImage): String = suspendCancellableCoroutine { cont ->
        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        recognizer.process(image)
            .addOnSuccessListener { visionText ->
                val text = visionText.text.trim()
                cont.resume(if (text.isBlank()) "I couldn't find any text in view." else text)
            }
            .addOnFailureListener { e ->
                cont.resume("Text recognition failed: ${e.message}")
            }
    }
}
