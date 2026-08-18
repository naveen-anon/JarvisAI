package com.jarvis.assistant.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit
import java.util.regex.Pattern

/** Top headlines via Google News RSS (India/English). On-device parse, no API key. */
class NewsClient {

    private val client = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(8, TimeUnit.SECONDS)
        .build()

    suspend fun topHeadlines(limit: Int = 3): List<String> = withContext(Dispatchers.IO) {
        val url = "https://news.google.com/rss?hl=en-IN&gl=IN&ceid=IN:en"
        try {
            val req = Request.Builder().url(url).header("User-Agent", "JarvisAI/1.0").build()
            client.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@withContext emptyList()
                val body = resp.body?.string() ?: return@withContext emptyList()
                val titles = mutableListOf<String>()
                val p = Pattern.compile("<item>\\s*<title>(?:<!\\[CDATA\\[)?(.*?)(?:\\]\\]>)?</title>", Pattern.DOTALL)
                val m = p.matcher(body)
                while (m.find() && titles.size < limit) {
                    var t = m.group(1)?.replace("&amp;", "&")?.replace("&#39;", "'")
                        ?.replace("&quot;", "\"")?.trim().orEmpty()
                    // Strip trailing " - Source"
                    if (t.contains(" - ")) t = t.substringBeforeLast(" - ").trim()
                    if (t.isNotBlank() && t.lowercase() != "google news") titles.add(t)
                }
                titles
            }
        } catch (_: Exception) {
            emptyList()
        }
    }
}
