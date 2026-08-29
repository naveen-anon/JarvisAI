package com.jarvis.assistant.vision

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.face.FaceDetection
import com.google.mlkit.vision.face.FaceDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/**
 * Phase 4 — "Face recognition (optional)". Important distinction: ML Kit's on-device
 * Face Detection API finds and describes faces (position, smiling probability, eyes
 * open/closed) but does NOT identify *who* a face belongs to — true face *recognition*
 * (matching against a named person) needs a separate enrolled-embeddings pipeline, which
 * is a much bigger privacy/ML undertaking. This gives honest, useful detection-level
 * output ("I see 2 faces, one smiling") without overclaiming identification it can't do.
 */
object FaceDetectionHelper {

    private val options = FaceDetectorOptions.Builder()
        .setPerformanceMode(FaceDetectorOptions.PERFORMANCE_MODE_ACCURATE)
        .setClassificationMode(FaceDetectorOptions.CLASSIFICATION_MODE_ALL)
        .build()

    suspend fun detect(image: InputImage): String = suspendCancellableCoroutine { cont ->
        val detector = FaceDetection.getClient(options)
        detector.process(image)
            .addOnSuccessListener { faces ->
                if (faces.isEmpty()) {
                    cont.resume("I don't see any faces right now.")
                    return@addOnSuccessListener
                }
                val smiling = faces.count { (it.smilingProbability ?: 0f) > 0.6f }
                val summary = buildString {
                    append("I see ${faces.size} face${if (faces.size != 1) "s" else ""}")
                    if (smiling > 0) append(", $smiling smiling")
                    append(".")
                }
                cont.resume(summary)
            }
            .addOnFailureListener { e ->
                cont.resume("Face detection failed: ${e.message}")
            }
    }
}
