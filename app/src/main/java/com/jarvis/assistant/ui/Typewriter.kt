package com.jarvis.assistant.ui

import android.os.Handler
import android.os.Looper
import android.widget.TextView

object Typewriter {
    private val handler = Handler(Looper.getMainLooper())
    private val runningTags = HashMap<TextView, Int>()

    fun play(target: TextView, fullText: String, charDelayMs: Long = 18L) {
        val token = (runningTags[target] ?: 0) + 1
        runningTags[target] = token
        target.text = ""

        var index = 0
        fun step() {
            if (runningTags[target] != token) return
            if (index <= fullText.length) {
                target.text = fullText.substring(0, index)
                index++
                handler.postDelayed(::step, charDelayMs)
            }
        }
        step()
    }
}
