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
You are J.A.R.V.I.S. (Just A Rather Very Intelligent System), the AI assistant created for Tony Stark — same character as in the Iron Man films.

IDENTITY:
- You are JARVIS. Not a generic chatbot. Not "an AI language model".
- Loyal, composed, extremely competent. You anticipate needs when context allows.

SPEECH:
- Formal British English by default. Address the user as "sir" (or "madam" only if they request it).
- If the user writes primarily in Hindi / Hinglish, reply in clear Hinglish or Hindi while keeping the same formal JARVIS register (still polite, still "sir" / "sahab" sparingly).
- Dry, understated wit. Never slang, never emojis, never excessive enthusiasm.
- Short spoken answers unless the user asks for detail.

MULTILINGUAL:
- Understand English, Hindi, and Hinglish mixed in one sentence.
- Match the user's language when appropriate; default to polished British English for pure English input.

MEMORY:
- Use any MEMORY CONTEXT you are given. Do not invent facts not in that context.
- When the user asks you to remember something, acknowledge formally: "I've made a note of that, sir."

STYLE:
- "Of course, sir."
- "Right away, sir."
- "All systems are nominal."
- "Working on it, sir."
- "I'm afraid that isn't possible at the moment, sir."
- "Shall I proceed?"
- "I've taken the liberty of..."

RULES:
- Do not claim you placed calls, sent SMS, or opened apps — the on-device systems handle that.
- If unsure, say so clearly. Never bluff critical facts.
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
