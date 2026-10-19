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
    CLOSE_APP("close_app"),
    CLEAR_RECENTS("clear_recents"),
    UNINSTALL_APP("uninstall_app"),
    INSTALL_APP("install_app"),
    CALL("call"),
    SEND_SMS("send_sms"),
    TOGGLE_SETTING("toggle_setting"),
    READ_SCREEN("read_screen"),
    SCREENSHOT("screenshot"),
    ANALYZE_SCREEN("analyze_screen"),
    SET_VOLUME("set_volume"),
    MEDIA_CONTROL("media_control"),
    SET_ALARM("set_alarm"),
    SET_TIMER("set_timer"),
    OPEN_CLOCK("open_clock"),
    STOPWATCH("stopwatch"),
    OPEN_VISION("open_vision"),
    WEB_SEARCH("web_search"),
    PLAY_MEDIA("play_media"),
    LOCK_APP("lock_app"),
    UNLOCK_APP("unlock_app"),
    UNLOCK_PHONE("unlock_phone"),
    SET_PIN("set_pin"),
    PC_CONNECT("pc_connect"),
    APP_CALL("app_call"),
    WHATSAPP_CALL("whatsapp_call"),
    WHATSAPP_MESSAGE("whatsapp_message"),
    TELEGRAM_MESSAGE("telegram_message"),
    SET_REMINDER("set_reminder"),
    READ_CLIPBOARD("read_clipboard"),
    OPEN_CLIPBOARD_LINK("open_clipboard_link"),
    NOTIF_SUMMARY("notif_summary"),
    SEARCH_SMS("search_sms"),
    SEARCH_PHOTOS("search_photos"),
    SEARCH_MAIL("search_mail"),
    AGENT_TASK("agent_task"),
    CREATE_EVENT("create_event"),
    REWRITE_TEXT("rewrite_text"),
    NOW_PLAYING("now_playing"),
    SHARE_SCREEN("share_screen"),
    SCREEN_SAVE_PHONE("screen_save_phone"),
    SCREEN_EMAIL("screen_email"),
    SCREEN_OPEN_LINK("screen_open_link"),
    PROACTIVE_TIP("proactive_tip"),
    CALENDAR_NEXT("calendar_next"),
    FOCUS_MODE("focus_mode"),
    SHOW_ARMOR("show_armor"),
    REMEMBER("remember"),
    RECALL("recall"),
    DEBRIEF("debrief"),
    BRIEFING("briefing"),
    EQUIP_SUIT("equip_suit"),
    SCREEN_ACT("screen_act"),
    OPEN_URL("open_url"),
    MULTI_STEP("multi_step"),
    REPLY("reply"),
    UNKNOWN("unknown");

    companion object {
        fun fromKey(key: String): ActionType = entries.find { it.key == key } ?: UNKNOWN
    }
}
