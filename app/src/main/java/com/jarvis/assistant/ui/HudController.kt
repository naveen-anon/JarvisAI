package com.jarvis.assistant.ui

import android.os.Handler
import android.os.Looper
import java.util.concurrent.CopyOnWriteArrayList

/** Stark L3 — presence bus. Uses ArcReactorView.HudState (single enum). */
object HudController {
    interface Listener {
        fun onHudState(state: HudState)
    }

    @Volatile
    private var state: HudState = HudState.IDLE

    private val listeners = CopyOnWriteArrayList<Listener>()
    private val main = Handler(Looper.getMainLooper())
    private var resetRunnable: Runnable? = null

    fun current(): HudState = state

    fun addListener(l: Listener) {
        listeners.add(l)
        main.post { l.onHudState(state) }
    }

    fun removeListener(l: Listener) {
        listeners.remove(l)
    }

    fun set(state: HudState, autoIdleMs: Long = 0L) {
        this.state = state
        resetRunnable?.let { main.removeCallbacks(it) }
        resetRunnable = null
        main.post { listeners.forEach { it.onHudState(state) } }
        if (autoIdleMs > 0L && state != HudState.IDLE && state != HudState.LISTENING) {
            val r = Runnable { set(HudState.IDLE) }
            resetRunnable = r
            main.postDelayed(r, autoIdleMs)
        }
    }

    fun idle() = set(HudState.IDLE)
    fun listening() = set(HudState.LISTENING)
    fun thinking() = set(HudState.THINKING)
    fun executing() = set(HudState.EXECUTING)
    fun speaking() = set(HudState.SPEAKING)
    fun done(autoIdleMs: Long = 1800L) = set(HudState.DONE, autoIdleMs)
    fun error(autoIdleMs: Long = 2200L) = set(HudState.ERROR, autoIdleMs)

    fun labelOf(state: HudState): String = when (state) {
        HudState.IDLE -> "Standing by"
        HudState.LISTENING -> "Listening…"
        HudState.THINKING -> "Thinking…"
        HudState.EXECUTING -> "Executing…"
        HudState.SPEAKING -> "Speaking…"
        HudState.DONE -> "Done"
        HudState.ERROR -> "Unable"
    }
}
