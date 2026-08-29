package com.jarvis.assistant.ai

/**
 * Routes user text:
 * 1) OfflineBrain handles device commands (call, SMS, WhatsApp, apps, …)
 * 2) If not a command / needs chat → Groq JARVIS LLM
 * 3) On LLM failure → polite offline fallback
 *
 * Wire OfflineBrain.handle(...) to return null/empty when it is NOT a command.
 */
class JarvisReplyRouter(
    private val llm: JarvisLlmClient,
    private val offlineHandle: suspend (String) -> OfflineOutcome,
) {
    sealed class OfflineOutcome {
        data class Handled(val spokenReply: String) : OfflineOutcome()
        object NotACommand : OfflineOutcome()
    }

    data class Reply(
        val text: String,
        val source: Source
    ) {
        enum class Source { OFFLINE_COMMAND, LLM, FALLBACK }
    }

    private val history = ArrayDeque<Pair<String, String>>()

    suspend fun reply(userText: String): Reply {
        val trimmed = userText.trim()
        if (trimmed.isEmpty()) {
            return Reply("I didn’t catch that, sir.", Reply.Source.FALLBACK)
        }

        when (val off = offlineHandle(trimmed)) {
            is OfflineOutcome.Handled -> {
                pushHistory("user", trimmed)
                pushHistory("assistant", off.spokenReply)
                return Reply(off.spokenReply, Reply.Source.OFFLINE_COMMAND)
            }
            OfflineOutcome.NotACommand -> { /* fall through */ }
        }

        val llmResult = llm.chat(trimmed, history.toList())
        if (llmResult.ok) {
            pushHistory("user", trimmed)
            pushHistory("assistant", llmResult.text)
            return Reply(llmResult.text, Reply.Source.LLM)
        }

        val fallback = buildFallback(llmResult.error)
        pushHistory("user", trimmed)
        pushHistory("assistant", fallback)
        return Reply(fallback, Reply.Source.FALLBACK)
    }

    fun clearHistory() = history.clear()

    private fun pushHistory(role: String, content: String) {
        history.addLast(role to content)
        while (history.size > 16) history.removeFirst()
    }

    private fun buildFallback(error: String?): String {
        val e = error?.lowercase().orEmpty()
        return when {
            e.contains("api key") ->
                "Online intelligence isn’t configured yet, sir. Device commands still work offline."
            e.contains("429") || e.contains("rate") ->
                "I’m under heavy load at the moment, sir. Please try again shortly."
            e.contains("network") || e.contains("unable") || e.contains("failed to connect") ->
                "I can’t reach the network right now, sir. Offline commands remain available."
            else ->
                "I’m afraid I couldn’t complete that request, sir."
        }
    }
}
