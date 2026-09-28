package com.jarvis.assistant.util

/**
 * Heuristic multi-step split without LLM.
 * Supports EN + Hindi connectors and call/text business patterns.
 */
object CommandChainSplitter {

    private val separators = listOf(
        Regex("""\s+and\s+then\s+""", RegexOption.IGNORE_CASE),
        Regex("""\s+then\s+""", RegexOption.IGNORE_CASE),
        Regex("""\s+phir\s+""", RegexOption.IGNORE_CASE),
        Regex("""\s+baad\s+mein\s+""", RegexOption.IGNORE_CASE),
        Regex("""\s+after\s+that\s+""", RegexOption.IGNORE_CASE),
        Regex("""\s*,\s*then\s+""", RegexOption.IGNORE_CASE),
        // "call X and text Y" / "message A and remind me"
        Regex("""\s+and\s+(?=call\b|text\b|message\b|sms\b|whatsapp\b|open\b|set\b|remind\b|turn\b|enable\b|disable\b)""", RegexOption.IGNORE_CASE),
        Regex("""\s+aur\s+(?=call\b|text\b|message\b|sms\b|kholo\b|set\b|yaad\b)""", RegexOption.IGNORE_CASE)
    )

    fun looksLikeChain(speech: String): Boolean {
        val s = speech.trim()
        if (s.length < 10) return false
        return separators.any { it.containsMatchIn(s) }
    }

    fun split(speech: String): List<String>? {
        val s = speech.trim()
        for (sep in separators) {
            if (sep.containsMatchIn(s)) {
                val parts = s.split(sep).map { it.trim() }.filter { it.isNotEmpty() }
                if (parts.size >= 2) return parts
            }
        }
        return null
    }
}
