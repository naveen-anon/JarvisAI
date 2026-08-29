package com.jarvis.assistant.util

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.util.Log

class MessagingHelper(private val context: Context) {

    fun sendWhatsApp(phoneNumber: String, message: String): Boolean {
        return try {
            val cleanNumber = phoneNumber.filter { it.isDigit() }
            val encodedMsg = Uri.encode(message)
            val uri = Uri.parse("https://wa.me/$cleanNumber?text=$encodedMsg")

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("MessagingHelper", "WhatsApp send failed", e)
            false
        }
    }

    fun sendTelegram(username: String, message: String): Boolean {
        return try {
            val cleanUsername = username.removePrefix("@")
            val encodedMsg = Uri.encode(message)
            val uri = Uri.parse("https://t.me/$cleanUsername?text=$encodedMsg")

            val intent = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("org.telegram.messenger")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            true
        } catch (e: Exception) {
            Log.e("MessagingHelper", "Telegram send failed", e)
            false
        }
    }
}
