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
    ANALYZE_SCREEN("analyze_screen"),
    SET_VOLUME("set_volume"),      // target = "up" | "down" | "mute" | "max" | a number string
    MEDIA_CONTROL("media_control"),// target = "play" | "pause" | "next" | "previous"
    SET_ALARM("set_alarm"),        // target = "HH:mm"
    SET_TIMER("set_timer"),        // target = total seconds as string
    OPEN_VISION("open_vision"),    // target = "ocr" | "objects" | "faces"
    WEB_SEARCH("web_search"),      // target = search query
    LOCK_APP("lock_app"),          // target = app name
    UNLOCK_APP("unlock_app"),      // target = app name
    SET_PIN("set_pin"),            // target = new PIN
    PC_CONNECT("pc_connect"),      // toggles the PC bridge server on/off; target = "on" | "off"
    WHATSAPP_MESSAGE("whatsapp_message"), // target = contact name, message = text to pre-fill
    TELEGRAM_MESSAGE("telegram_message"),  // target = username/contact name, message = text to pre-fill
    SET_REMINDER("set_reminder"),
    READ_CLIPBOARD("read_clipboard"),
    OPEN_CLIPBOARD_LINK("open_clipboard_link"),
    NOTIF_SUMMARY("notif_summary"),
    CALENDAR_NEXT("calendar_next"),
    FOCUS_MODE("focus_mode"),
    SHOW_ARMOR("show_armor"), // target = mark number or codename (e.g. "33", "silver centurion")
    REPLY("reply"),          // pure conversational reply, no system action
    UNKNOWN("unknown");

    companion object {
        fun fromKey(key: String): ActionType = entries.find { it.key == key } ?: UNKNOWN
    }
}
