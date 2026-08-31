package com.jarvis.assistant.model

/**
 * Structured command returned by the LLM after parsing user speech.
 * Single action OR multi_step with nested [steps].
 */
data class AssistantCommand(
    val action: String,
    val target: String? = null,
    val message: String? = null,
    val extra: Map<String, String> = emptyMap(),
    /** Populated when action == "multi_step" */
    val steps: List<AssistantCommand>? = null
)

enum class ActionType(val key: String) {
    OPEN_APP("open_app"),
    CALL("call"),
    SEND_SMS("send_sms"),
    TOGGLE_SETTING("toggle_setting"),
    READ_SCREEN("read_screen"),
    ANALYZE_SCREEN("analyze_screen"),
    SET_VOLUME("set_volume"),
    MEDIA_CONTROL("media_control"),
    SET_ALARM("set_alarm"),
    SET_TIMER("set_timer"),
    OPEN_VISION("open_vision"),
    WEB_SEARCH("web_search"),
    LOCK_APP("lock_app"),
    UNLOCK_APP("unlock_app"),
    SET_PIN("set_pin"),
    PC_CONNECT("pc_connect"),
    WHATSAPP_MESSAGE("whatsapp_message"),
    TELEGRAM_MESSAGE("telegram_message"),
    SET_REMINDER("set_reminder"),
    READ_CLIPBOARD("read_clipboard"),
    OPEN_CLIPBOARD_LINK("open_clipboard_link"),
    NOTIF_SUMMARY("notif_summary"),
    CALENDAR_NEXT("calendar_next"),
    FOCUS_MODE("focus_mode"),
    SHOW_ARMOR("show_armor"),
    MULTI_STEP("multi_step"),
    REPLY("reply"),
    UNKNOWN("unknown");

    companion object {
        fun fromKey(key: String): ActionType = entries.find { it.key == key } ?: UNKNOWN
    }
}
