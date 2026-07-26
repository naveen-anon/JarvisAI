package com.jarvis.assistant.ai

import com.jarvis.assistant.model.AssistantCommand
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class GeminiClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build()

    private val systemPrompt = """
        You are the reasoning core of an Android voice assistant called Jarvis.
        The user speaks a command; you must respond with ONLY a single JSON object,
        no prose, no markdown fences, matching this exact schema:

        {
          "action": "open_app | call | send_sms | toggle_setting | read_screen | reply",
          "target": "string or null - app name / contact name / setting name",
          "message": "string or null - sms body, or the spoken reply text for the user",
          "extra": {"key": "value"}
        }

        Rules:
        - If the user just wants conversation (no device action), use action "reply" and put
          your spoken response in "message".
        - For "open_app", target must be the common app name as installed (e.g. "WhatsApp", "Camera").
        - For "send_sms", target = contact name, message = body text.
        - For "call", target = contact name.
        - For "toggle_setting", target = one of "wifi","bluetooth","flashlight","airplane_mode".
        - Never include commentary outside the JSON object.
    """.trimIndent()

    private val conversationHistory = mutableListOf<Pair<String, String>>()

    suspend fun getCommand(userSpeech: String): AssistantCommand = withContext(Dispatchers.IO) {
        conversationHistory.add("user" to userSpeech)

        val contentsArray = JSONArray()
        conversationHistory.forEach { (role, text) ->
            val geminiRole = if (role == "assistant") "model" else "user"
            val partsArray = JSONArray().put(JSONObject().put("text", text))
            contentsArray.put(JSONObject().apply {
                put("role", geminiRole)
                put("parts", partsArray)
            })
        }

        val body = JSONObject().apply {
            put("system_instruction", JSONObject().apply {
                put("parts", JSONArray().put(JSONObject().put("text", systemPrompt)))
            })
            put("contents", contentsArray)
            put("generationConfig", JSONObject().apply {
                put("maxOutputTokens", 300)
            })
        }

        val url = "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent?key=$apiKey"

        val request = Request.Builder()
            .url(url)
            .addHeader("content-type", "application/json")
            .post(body.toString().toRequestBody("application/json".toMediaType()))
            .build()

        client.newCall(request).execute().use { response ->
            if (!response.isSuccessful) {
                return@withContext AssistantCommand(
                    action = "reply",
                    message = "API error: ${response.code}"
                )
            }
            val raw = response.body?.string() ?: return@withContext fallback()

            val text = JSONObject(raw)
                .getJSONArray("candidates")
                .getJSONObject(0)
                .getJSONObject("content")
                .getJSONArray("parts")
                .getJSONObject(0)
                .getString("text")
                .trim()
                .removePrefix("```json").removePrefix("```").removeSuffix("```").trim()

            conversationHistory.add("assistant" to text)
            pruneHistory()

            parseCommandJson(text)
        }
    }

    private fun parseCommandJson(text: String): AssistantCommand {
        return try {
            val json = JSONObject(text)
            val extraObj = json.optJSONObject("extra")
            val extraMap = mutableMapOf<String, String>()
            extraObj?.keys()?.forEach { k -> extraMap[k] = extraObj.getString(k) }

            AssistantCommand(
                action = json.getString("action"),
                target = json.optString("target", null),
                message = json.optString("message", null),
                extra = extraMap
            )
        } catch (e: Exception) {
            AssistantCommand(action = "reply", message = "Sorry, I couldn't parse that.")
        }
    }

    private fun fallback() = AssistantCommand(action = "reply", message = "No response from server.")

    private fun pruneHistory(maxTurns: Int = 5) {
        if (conversationHistory.size > maxTurns * 2) {
            conversationHistory.removeAt(0)
            conversationHistory.removeAt(0)
        }
    }
}
