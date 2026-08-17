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
 *
 * Model notes (checked against Groq's deprecations page):
 *  - llama-3.1-8b-instant was deprecated June 17, 2026 → replaced with openai/gpt-oss-20b.
 *  - Vision (image) requests need a multimodal model; qwen/qwen3.6-27b is Groq's current
 *    documented production vision model as of this writing. Groq's lineup changes often —
 *    if image replies start failing, check console.groq.com/docs/vision for the current ID.
 */
class JarvisLlmClient(
    private val apiKeyProvider: () -> String?,
    private val baseUrl: String = "https://api.groq.com/openai/v1",
    private val model: String = "openai/gpt-oss-20b",
    private val visionModel: String = "qwen/qwen3.6-27b"
) {
    data class Result(
        val ok: Boolean,
        val text: String,
        val error: String? = null
    )

    companion object {
        val SYSTEM_PROMPT = """
You are J.A.R.V.I.S. (Just A Rather Very Intelligent System) from the Iron Man films.

IDENTITY: You are JARVIS — loyal, calm, brilliant. Never say you are a generic AI model.

LANGUAGE (critical):
- Understand ANY language the user uses (English, Hindi, Hinglish, Spanish, Arabic, French, Chinese, etc.).
- ALWAYS reply in the same language the user just used, unless they explicitly ask for another language.
- Keep the JARVIS personality in every language: formal, precise, slightly dry wit, address as "sir" in English or the closest polite form in other languages (e.g. Hindi: polite register).

SPEECH STYLE:
- Concise, spoken-friendly. No emojis. No slang.
- English default tone is formal British when the user writes in English.

MEMORY: Use MEMORY CONTEXT when provided. Do not invent memories.

RULES: Do not claim you executed phone actions (calls/SMS/apps). On-device systems do that.
""".trimIndent()
    }

    suspend fun chat(userMessage: String, history: List<Pair<String, String>> = emptyList(), memoryContext: String = ""): Result =
        withContext(Dispatchers.IO) {
            if (userMessage.isBlank()) {
                return@withContext Result(false, "", "Empty message")
            }
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

            performRequest(body)
        }

    /**
     * Sends one image plus an optional instruction to a vision-capable Groq model.
     * [base64Image] must be raw base64 (no data: prefix) and [mimeType] like "image/jpeg".
     */
    suspend fun chatWithImage(text: String, base64Image: String, mimeType: String): Result =
        withContext(Dispatchers.IO) {
            if (base64Image.isBlank()) {
                return@withContext Result(false, "", "No image data")
            }
            val content = JSONArray()
            content.put(
                JSONObject().put("type", "text").put(
                    "text",
                    text.ifBlank { "Describe this image." }
                )
            )
            content.put(
                JSONObject().put("type", "image_url").put(
                    "image_url",
                    JSONObject().put("url", "data:$mimeType;base64,$base64Image")
                )
            )

            val messages = JSONArray()
            messages.put(JSONObject().put("role", "system").put("content", SYSTEM_PROMPT))
            messages.put(JSONObject().put("role", "user").put("content", content))

            val body = JSONObject()
                .put("model", visionModel)
                .put("messages", messages)
                .put("temperature", 0.4)
                .put("max_tokens", 512)

            performRequest(body)
        }

    private fun performRequest(body: JSONObject): Result {
        val key = apiKeyProvider()?.trim().orEmpty()
        if (key.isEmpty()) {
            return Result(ok = false, text = "", error = "API key missing. Add GROQ_API_KEY.")
        }
        return try {
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
                return Result(false, "", errMsg)
            }

            val json = JSONObject(raw)
            val text = json
                .optJSONArray("choices")
                ?.optJSONObject(0)
                ?.optJSONObject("message")
                ?.optString("content")
                ?.trim()
                .orEmpty()

            if (text.isEmpty()) Result(false, "", "Empty model response") else Result(true, text)
        } catch (e: Exception) {
            Result(false, "", e.message ?: "Network error")
        }
    }
}
