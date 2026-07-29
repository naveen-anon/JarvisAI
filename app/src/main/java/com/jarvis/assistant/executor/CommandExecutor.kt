package com.jarvis.assistant.executor

import com.jarvis.assistant.brain.OfflineBrain
import com.jarvis.assistant.voice.TextToSpeechHelper
import android.content.Context

object CommandExecutor {

    fun execute(context: Context, userQuery: String): String {
        // Direct local brain processing
        val response = OfflineBrain.processQuery(userQuery)

        // Voice output
        TextToSpeechHelper.speak(context, response)

        return response
    }
}
