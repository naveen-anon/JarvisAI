package com.jarvis.assistant.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri

class ClipboardHelper(private val context: Context) {

    fun getText(): String? {
        val cm = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
        val clip = cm.primaryClip ?: return null
        if (clip.itemCount == 0) return null
        return clip.getItemAt(0).coerceToText(context)?.toString()?.trim()?.takeIf { it.isNotBlank() }
    }

    fun openIfUrl(): String {
        val text = getText() ?: return "Clipboard is empty, sir."
        val url = text.trim()
        return if (url.startsWith("http://") || url.startsWith("https://")) {
            try {
                context.startActivity(
                    Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
                "Opening the link from the clipboard, sir."
            } catch (_: Exception) {
                "I couldn't open that link, sir."
            }
        } else {
            "Clipboard does not contain a web link, sir. It says: ${text.take(120)}"
        }
    }

    fun readAloudFriendly(): String {
        val text = getText() ?: return "Clipboard is empty, sir."
        val trimmed = if (text.length > 500) text.take(500) + "…" else text
        return "Clipboard contains: $trimmed"
    }
}
