package com.jarvis.assistant.vision

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.objects.ObjectDetection
import com.google.mlkit.vision.objects.defaults.ObjectDetectorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** Phase 4 — "Camera se objects pehchane". Fully on-device, no network call. */
object ObjectDetectionHelper {

    private val options = ObjectDetectorOptions.Builder()
        .setDetectorMode(ObjectDetectorOptions.SINGLE_IMAGE_MODE)
        .enableMultipleObjects()
        .enableClassification()
        .build()

    suspend fun detect(image: InputImage): String = suspendCancellableCoroutine { cont ->
        val detector = ObjectDetection.getClient(options)
        detector.process(image)
            .addOnSuccessListener { detected ->
                if (detected.isEmpty()) {
                    cont.resume("I don't recognize anything specific in view.")
                    return@addOnSuccessListener
                }
                val labels = detected.mapNotNull { obj ->
                    obj.labels.maxByOrNull { it.confidence }?.text
                }.distinct()
                val description = if (labels.isEmpty()) {
                    "I see ${detected.size} object${if (detected.size != 1) "s" else ""}, but couldn't classify them."
                } else {
                    "I see: ${labels.joinToString(", ")}."
                }
                cont.resume(description)
            }
            .addOnFailureListener { e ->
                cont.resume("Object detection failed: ${e.message}")
            }
    }
}
