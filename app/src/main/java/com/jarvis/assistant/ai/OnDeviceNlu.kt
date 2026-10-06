package com.jarvis.assistant.ai

import android.content.Context
import com.jarvis.assistant.model.AssistantCommand

/**
 * Personal design — on-device intent layer.
 * Now: light phrase heuristics. Later: TFLite / small SLM with same API.
 * Never touches the OS; only returns [AssistantCommand] for CommandExecutor.
 */
class OnDeviceNlu(@Suppress("unused") private val context: Context) {

    fun classify(raw: String): AssistantCommand? {
        val t = raw.trim()
        if (t.length < 3) return null
        val c = t.lowercase()

        if (c.contains("put on") && (c.contains("song") || c.contains("music") || c.contains("track"))) {
            var q = t.replace(Regex("(?i).*put on\\s+"), "")
            val app = when {
                "spotify" in c -> {
                    q = q.replace(Regex("(?i)\\s+on\\s+spotify.*"), "")
                    "spotify"
                }
                "youtube music" in c -> {
                    q = q.replace(Regex("(?i)\\s+on\\s+youtube music.*"), "")
                    "youtube music"
                }
                "youtube" in c -> {
                    q = q.replace(Regex("(?i)\\s+on\\s+youtube.*"), "")
                    "youtube"
                }
                else -> null
            }
            q = q.trim()
            if (q.isNotBlank()) return AssistantCommand("play_media", q, app)
        }
        if (c.startsWith("can you open") || c.startsWith("could you open") || c.startsWith("please open")) {
            val name = t.replace(Regex("(?i)^(can you|could you|please)\\s+open\\s+"), "").trim()
            if (name.isNotBlank()) return AssistantCommand("open_app", name)
        }
        if (c.startsWith("can you search") || c.startsWith("look up")) {
            val q = t.replace(Regex("(?i)^(can you search(\\s+for)?|look up)\\s*"), "").trim()
            if (q.isNotBlank()) return AssistantCommand("web_search", q)
        }
        return null
    }
}
