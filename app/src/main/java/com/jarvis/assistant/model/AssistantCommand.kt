package com.jarvis.assistant.model

/**
 * Structured command returned by the LLM after parsing user speech.
 * The LLM is prompted to ALWAYS respond in this JSON shape (see ClaudeClient's system prompt),
 * so parsing is deterministic instead of regex-matching free text.
 */
data class AssistantCommand(
    val action: String,          // e.g. "open_app", "send_sms", "call", "toggle_setting", "reply", "read_screen"
    val target: String? = null,  // app name / contact name / setting name
    val message: String? = null, // sms body / spoken reply text
    val extra: Map<String, String> = emptyMap()
)

enum class ActionType(val key: String) {
    OPEN_APP("open_app"),
    CALL("call"),
    SEND_SMS("send_sms"),
    TOGGLE_SETTING("toggle_setting"),
    READ_SCREEN("read_screen"),
    REPLY("reply"),          // pure conversational reply, no system action
    UNKNOWN("unknown");

    companion object {
        fun fromKey(key: String): ActionType = values().find { it.key == key } ?: UNKNOWN
    }
}
