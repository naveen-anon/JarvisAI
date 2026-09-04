package com.jarvis.assistant.ui

/**
 * Stark Layer 3 — presence states for HUD / arc reactor / status text.
 */
enum class HudState {
    IDLE,
    LISTENING,
    THINKING,
    EXECUTING,
    SPEAKING,
    DONE,
    ERROR;

    val label: String
        get() = when (this) {
            IDLE -> "Standing by"
            LISTENING -> "Listening…"
            THINKING -> "Thinking…"
            EXECUTING -> "Executing…"
            SPEAKING -> "Speaking…"
            DONE -> "Done"
            ERROR -> "Unable"
        }

    /** Soft cyan intensity hint 0f..1f for reactor glow. */
    val intensity: Float
        get() = when (this) {
            IDLE -> 0.35f
            LISTENING -> 0.85f
            THINKING -> 0.65f
            EXECUTING -> 1.0f
            SPEAKING -> 0.75f
            DONE -> 0.5f
            ERROR -> 0.4f
        }
}
