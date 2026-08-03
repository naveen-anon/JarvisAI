package com.jarvis.assistant.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class FeedbackClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val endpoint = "https://jarvis-site.naveen1khatri.workers.dev/api/feedback"

    suspend fun send(message: String, name: String? = null, contact: String? = null, rating: Int? = null): Boolean =
        withContext(Dispatchers.IO) {
            if (message.isBlank()) return@withContext false

            val json = JSONObject().apply {
                put("message", message.trim())
                if (!name.isNullOrBlank()) put("name", name.trim())
                if (!contact.isNullOrBlank()) put("contact", contact.trim())
                if (rating != null) put("rating", rating)
            }

            try {
                val body = json.toString().toRequestBody("application/json".toMediaType())
                val request = Request.Builder().url(endpoint).post(body).build()
                client.newCall(request).execute().use { response -> response.isSuccessful }
            } catch (e: Exception) {
                false
            }
        }
}
