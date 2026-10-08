package com.jarvis.assistant.vision

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect

object FaceIdentityHelper {

    data class Identified(
        val box: Rect,
        val label: String,
        val detail: String,
        val score: Float?,
        val known: Boolean
    )

    suspend fun identify(context: Context, bitmap: Bitmap, threshold: Float = 0.72f): List<Identified> {
        val embedder = FaceEmbedder(context)
        val gallery = FaceGallery(context)
        val hits = embedder.analyze(bitmap)
        if (hits.isEmpty()) return emptyList()
        return hits.map { hit ->
            val match = gallery.match(hit.embedding, threshold)
            if (match != null) {
                Identified(
                    box = hit.box,
                    label = match.name.uppercase(),
                    detail = "MATCH ${(match.score * 100).toInt()}%",
                    score = match.score,
                    known = true
                )
            } else {
                val smile = hit.smiling?.let { if (it > 0.6f) "smiling" else null }
                Identified(
                    box = hit.box,
                    label = "UNKNOWN",
                    detail = listOfNotNull("no gallery match", smile).joinToString(" · "),
                    score = null,
                    known = false
                )
            }
        }
    }

    suspend fun enrollFromBitmap(context: Context, bitmap: Bitmap, name: String): String {
        val embedder = FaceEmbedder(context)
        val hits = embedder.analyze(bitmap)
        if (hits.isEmpty()) return "No face found to enroll, sir."
        // largest face
        val best = hits.maxBy { it.box.width() * it.box.height() }
        FaceGallery(context).enroll(name, best.embedding)
        return "Enrolled $name into the identity gallery, sir."
    }

    fun galleryNames(context: Context): List<String> =
        FaceGallery(context).list().map { it.name }
}
