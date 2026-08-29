package com.jarvis.assistant.ui

import android.os.Handler
import android.os.Looper
import android.widget.TextView
import kotlin.random.Random

object Typewriter {

    private val handler = Handler(Looper.getMainLooper())
    private val glitchChars = "!<>-_\\/[]{}—=+*^?#_$%".toCharArray()

    fun animate(target: TextView, fullText: String, charDelayMs: Long = 22L) {
        handler.removeCallbacksAndMessages(null)
        target.text = ""
        val builder = StringBuilder()
        var index = 0

        fun revealNext() {
            if (index >= fullText.length) return
            val char = fullText[index]

            if (char.isLetterOrDigit() && Random.nextInt(100) < 40) {
                var glitchStep = 0
                val glitchRunnable = object : Runnable {
                    override fun run() {
                        if (glitchStep < 2) {
                            target.text = builder.toString() + glitchChars.random()
                            glitchStep++
                            handler.postDelayed(this, 12L)
                        } else {
                            builder.append(char)
                            target.text = builder.toString()
                            index++
                            handler.postDelayed(::revealNext, charDelayMs)
                        }
                    }
                }
                handler.post(glitchRunnable)
            } else {
                builder.append(char)
                target.text = builder.toString()
                index++
                handler.postDelayed(::revealNext, charDelayMs)
            }
        }
        revealNext()
    }
}
