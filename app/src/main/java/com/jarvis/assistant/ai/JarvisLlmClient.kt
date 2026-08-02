package com.jarvis.assistant.ai

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.OutputStreamWriter
import java.net.HttpURLConnection
import java.net.URL

/**
 * Cloud LLM for Jarvis — Groq (OpenAI-compatible), free + fast.
 * Put key in local.properties: GROQ_API_KEY=gsk_...
 * and expose via BuildConfig, or pass at runtime from secure storage.
 */
class JarvisLlmClient(
    private val apiKeyProvider: () -> String?,
    private val baseUrl: String = "https://api.groq.com/openai/v1",
    private val model: String = "llama-3.1-8b-instant"
) {
    data class Result(
        val ok: Boolean,
        val text: String,
        val error: String? = null
    )

    companion object {
        val SYSTEM_PROMPT = """
You are JARVIS, a personal AI assistant inspired by Tony Stark's JARVIS.
Speak formally, briefly, and politely. Use British English tone.
Address the user as "sir" unless they ask otherwise.
No emojis. No slang. Keep answers short unless detail is requested.
You help with information, reasoning, and explanations.
You do NOT invent that you executed phone actions (calls, SMS, apps) —
those are handled by the device offline brain; only discuss them if asked.
If you are unsure, say so clearly.
""".trimIndent()
    }

    suspend fun chat(userMessage: String, history: List<Pair<String, String>> = emptyList()): Result =
        withContext(Dispatchers.IO) {
            val key = apiKeyProvider()?.trim().orEmpty()
            if (key.isEmpty()) {
                return@withContext Result(
                    ok = false,
                    text = "",
                    error = "API key missing. Add GROQ_API_KEY."
                )
            }
            if (userMessage.isBlank()) {
                return@withContext Result(false, "", "Empty message")
            }

            try {
                val messages = JSONArray()
                messages.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
                history.takeLast(8).forEach { (role, content) ->
                    val r = if (role == "assistant") "assistant" else "user"
                    messages.put(JSONObject().put("role", r).put("content", content))
                }
                messages.put(JSONObject().put("role", "user").put("content", userMessage))

                val body = JSONObject()
                    .put("model", model)
                    .put("messages", messages)
                    .put("temperature", 0.6)
                    .put("max_tokens", 512)

                val url = URL("$baseUrl/chat/completions")
                val conn = (url.openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    setRequestProperty("Authorization", "Bearer $key")
                    setRequestProperty("Content-Type", "application/json")
                    doOutput = true
                    connectTimeout = 20_000
                    readTimeout = 45_000
                }
                OutputStreamWriter(conn.outputStream, Charsets.UTF_8).use { it.write(body.toString()) }

                val code = conn.responseCode
                val stream = if (code in 200..299) conn.inputStream else conn.errorStream
                val raw = stream?.bufferedReader()?.use(BufferedReader::readText).orEmpty()
                conn.disconnect()

                if (code !in 200..299) {
                    val errMsg = try {
                        JSONObject(raw).optJSONObject("error")?.optString("message") ?: raw
                    } catch (_: Exception) {
                        raw.ifBlank { "HTTP $code" }
                    }
                    return@withContext Result(false, "", errMsg)
                }

                val json = JSONObject(raw)
                val text = json
                    .optJSONArray("choices")
                    ?.optJSONObject(0)
                    ?.optJSONObject("message")
                    ?.optString("content")
                    ?.trim()
                    .orEmpty()

                if (text.isEmpty()) {
                    Result(false, "", "Empty model response")
                } else {
                    Result(true, text)
                }
            } catch (e: Exception) {
                Result(false, "", e.message ?: "Network error")
            }
        }
}
