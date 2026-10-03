package com.jarvis.assistant.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.database.Cursor
import android.net.Uri
import android.provider.ContactsContract
import android.telephony.PhoneStateListener
import android.telephony.TelephonyManager
import androidx.core.content.ContextCompat
import com.jarvis.assistant.voice.TextToSpeechHelper

class IncomingCallAnnouncer(
    private val context: Context,
    private val ttsProvider: () -> TextToSpeechHelper?
) {
    private var tm: TelephonyManager? = null
    private var listening = false
    private var lastAnnounce = 0L

    @Suppress("DEPRECATION")
    private val listener = object : PhoneStateListener() {
        @Deprecated("Deprecated in Java")
        override fun onCallStateChanged(state: Int, phoneNumber: String?) {
            if (state != TelephonyManager.CALL_STATE_RINGING) return
            val now = System.currentTimeMillis()
            if (now - lastAnnounce < 4000L) return
            lastAnnounce = now
            val raw = phoneNumber?.trim().orEmpty()
            val label = when {
                raw.isNotBlank() -> resolveName(raw) ?: formatNumber(raw)
                else -> "an unknown number"
            }
            try {
                ttsProvider()?.speak("Incoming call from $label.")
            } catch (_: Exception) {}
        }
    }

    fun start() {
        if (listening) return
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_PHONE_STATE)
            != PackageManager.PERMISSION_GRANTED
        ) return
        tm = context.getSystemService(Context.TELEPHONY_SERVICE) as? TelephonyManager
        try {
            @Suppress("DEPRECATION")
            tm?.listen(listener, PhoneStateListener.LISTEN_CALL_STATE)
            listening = true
        } catch (_: Exception) {}
    }

    fun stop() {
        try {
            @Suppress("DEPRECATION")
            tm?.listen(listener, PhoneStateListener.LISTEN_NONE)
        } catch (_: Exception) {}
        listening = false
    }

    private fun formatNumber(n: String): String =
        n.filter { it.isDigit() || it == '+' }.ifBlank { "an unknown number" }

    private fun resolveName(number: String): String? {
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS)
            != PackageManager.PERMISSION_GRANTED
        ) return null
        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(number)
        )
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null, null, null
            )?.use { c: Cursor ->
                if (c.moveToFirst()) c.getString(0) else null
            }
        } catch (_: Exception) {
            null
        }
    }
}
