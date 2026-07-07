    """.trimIndent()

    private val conversationHistory = mutableListOf<Map<String, String>>()

    suspend fun getCommand(userSpeech: String): AssistantCommand = withContext(Dispatchers.IO) {
        conversationHistory.add(mapOf("role" to "user", "content" to userSpeech))

        val messagesArray = JSONArray()
        conversationHistory.forEach { msg ->
            messagesArray.put(JSONObject(msg))
        }

        val body = JSONObject().apply {
            put("model", "claude-sonnet-4-6")
            put("max_tokens", 300)
            put("system", systemPrompt)
            put("messages", messagesArray)
        }
            conversationHistory.add(mapOf("role" to "assistant", "content" to text))
            pruneHistory()


    private fun pruneHistory(maxTurns: Int = 5) {
        if (conversationHistory.size > maxTurns * 2) {
            conversationHistory.removeAt(0)
            conversationHistory.removeAt(0)
        }
    }
