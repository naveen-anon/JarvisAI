package com.jarvis.assistant.brain

import com.jarvis.assistant.util.ConversationContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

object OfflineBrain {

    fun processQuery(query: String): String {
        val cleanQuery = query.lowercase().trim()
        val currentApp = ConversationContext.currentAppName

        return when {
            // 1. Live Context Queries (Real-time App Status)
            cleanQuery.contains("kya khola hai") || cleanQuery.contains("kaun sa app") || cleanQuery.contains("current app") -> {
                if (currentApp.isNotEmpty()) {
                    "Aap abhi $currentApp use kar rahe hain, Boss."
                } else {
                    "Abhi screen par koi specific app tracked nahi hai."
                }
            }

            // 2. Time & Date Queries
            cleanQuery.contains("time") || cleanQuery.contains("samay") || cleanQuery.contains("waqt") -> {
                val sdf = SimpleDateFormat("hh:mm a", Locale.getDefault())
                "Abhi time ${sdf.format(Date())} ho raha hai."
            }

            cleanQuery.contains("date") || cleanQuery.contains("tareekh") || cleanQuery.contains("tarikh") -> {
                val sdf = SimpleDateFormat("dd MMMM yyyy, EEEE", Locale.getDefault())
                "Aaj ${sdf.format(Date())} hai."
            }

            // 3. System & Jarvis Status
            cleanQuery.contains("kaise ho") || cleanQuery.contains("status") -> {
                "Main Offline Mode par ekdum smoothly run kar raha hoon! Context tracking active hai."
            }

            cleanQuery.contains("hello") || cleanQuery.contains("jarvis") || cleanQuery.contains("hey") -> {
                "Ji Boss, main sun raha hoon. Offline system aapke command ke liye ready hai."
            }

            // 4. Fallback Rule-Based Response
            else -> {
                if (currentApp.isNotEmpty()) {
                    "Command received: '$query'. (Aap abhi $currentApp screen par hain)."
                } else {
                    "Command received: '$query'. Offline brain active hai."
                }
            }
        }
    }
}
