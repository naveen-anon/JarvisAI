package com.jarvis.assistant.vision

import android.content.Context
import org.json.JSONArray
import org.json.JSONObject
import java.io.File
import kotlin.math.sqrt

/**
 * On-device face identity gallery. Stores name + float embedding only (no raw photos required).
 * Matching uses cosine similarity.
 */
class FaceGallery(context: Context) {

    data class Entry(val id: String, val name: String, val embedding: FloatArray)

    data class Match(val name: String, val score: Float)

    private val file = File(context.filesDir, "face_gallery.json")

    fun list(): List<Entry> {
        if (!file.exists()) return emptyList()
        return try {
            val arr = JSONArray(file.readText())
            buildList {
                for (i in 0 until arr.length()) {
                    val o = arr.getJSONObject(i)
                    val embArr = o.getJSONArray("embedding")
                    val emb = FloatArray(embArr.length()) { embArr.getDouble(it).toFloat() }
                    add(Entry(o.getString("id"), o.getString("name"), emb))
                }
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun enroll(name: String, embedding: FloatArray): Entry {
        val entries = list().toMutableList()
        val id = "f_${System.currentTimeMillis()}"
        // Replace same name
        entries.removeAll { it.name.equals(name, ignoreCase = true) }
        val e = Entry(id, name.trim(), l2Normalize(embedding))
        entries.add(e)
        save(entries)
        return e
    }

    fun delete(name: String): Boolean {
        val entries = list().toMutableList()
        val before = entries.size
        entries.removeAll { it.name.equals(name, ignoreCase = true) }
        if (entries.size != before) {
            save(entries)
            return true
        }
        return false
    }

    fun match(embedding: FloatArray, threshold: Float = 0.72f): Match? {
        val probe = l2Normalize(embedding)
        var best: Match? = null
        for (e in list()) {
            val s = cosine(probe, e.embedding)
            if (s >= threshold && (best == null || s > best.score)) {
                best = Match(e.name, s)
            }
        }
        return best
    }

    private fun save(entries: List<Entry>) {
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(JSONObject().apply {
                put("id", e.id)
                put("name", e.name)
                put("embedding", JSONArray().also { ja ->
                    e.embedding.forEach { ja.put(it.toDouble()) }
                })
            })
        }
        file.writeText(arr.toString())
    }

    companion object {
        fun l2Normalize(v: FloatArray): FloatArray {
            var s = 0f
            for (x in v) s += x * x
            val n = sqrt(s).coerceAtLeast(1e-6f)
            return FloatArray(v.size) { v[it] / n }
        }

        fun cosine(a: FloatArray, b: FloatArray): Float {
            val n = minOf(a.size, b.size)
            var dot = 0f
            for (i in 0 until n) dot += a[i] * b[i]
            return dot
        }
    }
}
