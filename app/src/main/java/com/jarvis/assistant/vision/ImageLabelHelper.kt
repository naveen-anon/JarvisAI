package com.jarvis.assistant.vision

import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.label.ImageLabeling
import com.google.mlkit.vision.label.defaults.ImageLabelerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

object ImageLabelHelper {
    suspend fun label(image: InputImage): String = suspendCancellableCoroutine { cont ->
        val labeler = ImageLabeling.getClient(
            ImageLabelerOptions.Builder().setConfidenceThreshold(0.7f).build()
        )
        labeler.process(image)
            .addOnSuccessListener { labels ->
                if (labels.isEmpty()) {
                    cont.resume("I don't recognize any clear scene labels.")
                    return@addOnSuccessListener
                }
                val top = labels.sortedByDescending { it.confidence }.take(6)
                val line = top.joinToString(", ") { lab ->
                    val pct = (lab.confidence * 100).toInt()
                    lab.text + " (" + pct + "%)"
                }
                cont.resume("Scene labels: " + line + ".")
            }
            .addOnFailureListener { e ->
                cont.resume("Image labeling failed: " + (e.message ?: "unknown"))
            }
    }
}
