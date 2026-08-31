package com.jarvis.assistant.util

/**
 * Heuristic split for multi-step speech without calling the LLM.
 * Examples:
 *  - "open whatsapp then set alarm for 7"
 *  - "wifi on phir flashlight on"
 *  - "open camera and then take a note" (second may stay as reply via LLM later)
 */
object CommandChainSplitter {

    private val separators = listOf(
        Regex("""\s+then\s+""", RegexOption.IGNORE_CASE),
        Regex("""\s+and\s+then\s+""", RegexOption.IGNORE_CASE),
        Regex("""\s+phir\s+""", RegexOption.IGNORE_CASE),
        Regex("""\s+baad\s+mein\s+""", RegexOption.IGNORE_CASE),
        Regex("""\s+after\s+that\s+""", RegexOption.IGNORE_CASE),
        Regex("""\s*,\s*then\s+""", RegexOption.IGNORE_CASE)
    )

    fun looksLikeChain(speech: String): Boolean {
        val s = speech.trim()
        if (s.length < 8) return false
        return separators.any { it.containsMatchIn(s) }
    }

    /** Returns 2+ segments, or null if not a chain. */
    fun split(speech: String): List<String>? {
        var parts = listOf(speech.trim())
        for (sep in separators) {
            if (sep.containsMatchIn(speech)) {
                parts = speech.split(sep).map { it.trim() }.filter { it.isNotEmpty() }
                break
            }
        }
        return if (parts.size >= 2) parts else null
    }
}
