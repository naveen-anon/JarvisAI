package com.jarvis.assistant.executor

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.SmsManager
import android.view.KeyEvent
import com.jarvis.assistant.model.ActionType
import com.jarvis.assistant.model.AssistantCommand
import com.jarvis.assistant.security.AppLockManager
import com.jarvis.assistant.vision.VisionActivity

class CommandExecutor(private val context: Context) {

    private val lockManager = AppLockManager(context)

    fun execute(cmd: AssistantCommand): String {
        return when (ActionType.fromKey(cmd.action)) {
            ActionType.OPEN_APP -> openApp(cmd.target)
            ActionType.CALL -> callContact(cmd.target)
            ActionType.SEND_SMS -> sendSms(cmd.target, cmd.message)
            ActionType.TOGGLE_SETTING -> toggleSetting(cmd.target)
            ActionType.SET_VOLUME -> setVolume(cmd.target)
            ActionType.MEDIA_CONTROL -> mediaControl(cmd.target)
            ActionType.SET_ALARM -> setAlarm(cmd.target)
            ActionType.SET_TIMER -> setTimer(cmd.target)
            ActionType.OPEN_VISION -> openVision(cmd.target)
            ActionType.WEB_SEARCH -> webSearch(cmd.target)
            ActionType.LOCK_APP -> lockApp(cmd.target)
            ActionType.UNLOCK_APP -> unlockApp(cmd.target)
            ActionType.SET_PIN -> setPin(cmd.target)
            ActionType.READ_SCREEN -> "Reading screen requires the Accessibility Service overlay."
            ActionType.REPLY -> cmd.message ?: ""
            ActionType.PC_CONNECT -> "PC connect is handled by the assistant service, not here."
            ActionType.UNKNOWN -> "I didn't understand that command."
        }
    }

    private fun openApp(appName: String?): String {
        if (appName.isNullOrBlank()) return "Which app?"
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0)

        val match = apps.firstOrNull {
            pm.getApplicationLabel(it).toString().equals(appName, ignoreCase = true)
        } ?: apps.firstOrNull {
            pm.getApplicationLabel(it).toString().contains(appName, ignoreCase = true)
        }

        return if (match != null) {
            val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                "Opening $appName"
            } else "Can't launch $appName"
        } else "I couldn't find $appName installed."
    }

    // Requires CALL_PHONE permission granted at runtime.
    private fun callContact(name: String?): String {
        if (name.isNullOrBlank()) return "Call whom?"
        val number = lookupContactNumber(name) ?: return "No number found for $name"
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Calling $name"
    }

    // Requires SEND_SMS permission granted at runtime.
    private fun sendSms(name: String?, message: String?): String {
        if (name.isNullOrBlank() || message.isNullOrBlank()) return "Missing recipient or message."
        val number = lookupContactNumber(name) ?: return "No number found for $name"
        SmsManager.getDefault().sendTextMessage(number, null, message, null, null)
        return "Message sent to $name"
    }

    private fun lookupContactNumber(name: String): String? {
        val resolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(ContactsContract.CommonDataKinds.Phone.NUMBER)
        val selection = "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?"
        resolver.query(uri, projection, selection, arrayOf("%$name%"), null)?.use { cursor ->
            if (cursor.moveToFirst()) {
                return cursor.getString(0)
            }
        }
        return null
    }

    /**
     * Android 10+ blocks apps from directly flipping Wi-Fi/Bluetooth for the user
     * (Settings.Global write access was locked down). The compliant approach is to
     * deep-link into the relevant Settings panel, or use the Quick Settings Tile API
     * for a one-tap toggle from the notification shade. Flashlight is the one thing
     * we CAN toggle directly via CameraManager.
     */
    private fun toggleSetting(setting: String?): String {
        return when (setting?.lowercase()) {
            "wifi" -> {
                context.startActivity(Intent(Settings.Panel.ACTION_WIFI).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Opening Wi-Fi panel"
            }
            "bluetooth" -> {
                context.startActivity(Intent(Settings.ACTION_BLUETOOTH_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Opening Bluetooth settings"
            }
            "flashlight" -> toggleFlashlight()
            "airplane_mode" -> {
                context.startActivity(Intent(Settings.ACTION_AIRPLANE_MODE_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                "Opening airplane mode settings"
            }
            else -> "Unknown setting: $setting"
        }
    }

    private var torchOn = false
    private fun toggleFlashlight(): String {
        val cameraManager = context.getSystemService(Context.CAMERA_SERVICE) as CameraManager
        val camId = cameraManager.cameraIdList.firstOrNull() ?: return "No camera found"
        torchOn = !torchOn
        cameraManager.setTorchMode(camId, torchOn)
        return if (torchOn) "Flashlight on" else "Flashlight off"
    }

    /** Volume control on the music stream (what "volume up/down/mute" means for most users). */
    private fun setVolume(target: String?): String {
        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        val stream = AudioManager.STREAM_MUSIC
        val max = am.getStreamMaxVolume(stream)

        return when (val t = target?.lowercase()?.trim()) {
            null, "" -> "To what level?"
            "up", "increase", "raise", "badhao", "badhaiye" ->
                { am.adjustStreamVolume(stream, AudioManager.ADJUST_RAISE, AudioManager.FLAG_SHOW_UI); "Volume up" }
            "down", "decrease", "lower", "kam karo", "kam karo do" ->
                { am.adjustStreamVolume(stream, AudioManager.ADJUST_LOWER, AudioManager.FLAG_SHOW_UI); "Volume down" }
            "mute", "silent", "band karo" ->
                { am.adjustStreamVolume(stream, AudioManager.ADJUST_MUTE, AudioManager.FLAG_SHOW_UI); "Muted" }
            "max", "maximum", "full" ->
                { am.setStreamVolume(stream, max, AudioManager.FLAG_SHOW_UI); "Volume at maximum" }
            else -> {
                val pct = t.toIntOrNull()
                if (pct != null) {
                    val level = (pct.coerceIn(0, 100) * max) / 100
                    am.setStreamVolume(stream, level, AudioManager.FLAG_SHOW_UI)
                    "Volume set to $pct%"
                } else "I didn't catch what volume level you want."
            }
        }
    }

    /**
     * Sends a media key event system-wide so whatever app is currently playing audio
     * (Spotify, YouTube Music, etc.) responds — no need to know which app is active.
     */
    private fun mediaControl(target: String?): String {
        val keyCode = when (target?.lowercase()?.trim()) {
            "play", "resume", "chalao" -> KeyEvent.KEYCODE_MEDIA_PLAY
            "pause", "ruko", "roko" -> KeyEvent.KEYCODE_MEDIA_PAUSE
            "next", "skip", "agla" -> KeyEvent.KEYCODE_MEDIA_NEXT
            "previous", "back", "pichla" -> KeyEvent.KEYCODE_MEDIA_PREVIOUS
            "stop" -> KeyEvent.KEYCODE_MEDIA_STOP
            else -> null
        } ?: return "I didn't understand that music command."

        val am = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_DOWN, keyCode))
        am.dispatchMediaKeyEvent(KeyEvent(KeyEvent.ACTION_UP, keyCode))

        return when (keyCode) {
            KeyEvent.KEYCODE_MEDIA_PLAY -> "Playing"
            KeyEvent.KEYCODE_MEDIA_PAUSE -> "Paused"
            KeyEvent.KEYCODE_MEDIA_NEXT -> "Next track"
            KeyEvent.KEYCODE_MEDIA_PREVIOUS -> "Previous track"
            else -> "Stopped"
        }
    }

    /** Deep-links into the default Clock app to create an alarm — no special permission needed. */
    private fun setAlarm(target: String?): String {
        val (hour, minute) = parseHourMinute(target) ?: return "What time should the alarm be?"
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            val display = String.format("%02d:%02d", hour, minute)
            "Alarm set for $display"
        } catch (e: Exception) {
            "I couldn't find a clock app to set the alarm."
        }
    }

    /** Deep-links into the default Clock app to start a timer. */
    private fun setTimer(target: String?): String {
        val seconds = target?.toIntOrNull() ?: return "How long should the timer be?"
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        return try {
            context.startActivity(intent)
            "Timer started for ${formatDuration(seconds)}"
        } catch (e: Exception) {
            "I couldn't find a clock app to start the timer."
        }
    }

    private fun parseHourMinute(target: String?): Pair<Int, Int>? {
        if (target.isNullOrBlank()) return null
        val parts = target.trim().split(":")
        if (parts.size != 2) return null
        val h = parts[0].toIntOrNull() ?: return null
        val m = parts[1].toIntOrNull() ?: return null
        if (h !in 0..23 || m !in 0..59) return null
        return h to m
    }

    private fun formatDuration(totalSeconds: Int): String {
        val m = totalSeconds / 60
        val s = totalSeconds % 60
        return when {
            m > 0 && s > 0 -> "$m minute${if (m != 1) "s" else ""} $s seconds"
            m > 0 -> "$m minute${if (m != 1) "s" else ""}"
            else -> "$s seconds"
        }
    }

    /** Phase 4 — launches the camera vision screen in a given mode: "ocr" | "objects" | "faces". */
    private fun openVision(mode: String?): String {
        val intent = Intent(context, VisionActivity::class.java).apply {
            putExtra(VisionActivity.EXTRA_MODE, mode ?: "ocr")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return when (mode) {
            "objects" -> "Opening object detection."
            "faces" -> "Opening face detection."
            else -> "Opening the text scanner."
        }
    }

    /** Phase 5 — "web search". Opens a browser search rather than trying to scrape results
     *  itself, since Jarvis has no in-app browsing/rendering surface. */
    private fun webSearch(query: String?): String {
        if (query.isNullOrBlank()) return "What should I search for?"
        val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
            "https://www.google.com/search?q=" + Uri.encode(query)
        )).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
        return "Searching for $query"
    }

    /** Phase 5 — "App lock by voice". Requires a PIN to already be set (see setPin). */
    private fun lockApp(appName: String?): String {
        if (appName.isNullOrBlank()) return "Which app should I lock?"
        if (!lockManager.hasPin()) {
            return "Set a PIN first — say \"set my pin to\" followed by 4 digits."
        }
        val packageName = resolvePackageName(appName) ?: return "I couldn't find $appName installed."
        lockManager.lockApp(packageName)
        return "$appName is now locked. You'll need your PIN to open it."
    }

    private fun unlockApp(appName: String?): String {
        if (appName.isNullOrBlank()) return "Which app should I unlock?"
        val packageName = resolvePackageName(appName) ?: return "I couldn't find $appName installed."
        lockManager.unlockApp(packageName)
        return "$appName is unlocked."
    }

    private fun setPin(pin: String?): String {
        if (pin.isNullOrBlank() || !pin.all { it.isDigit() } || pin.length < 4) {
            return "PINs need to be at least 4 digits."
        }
        lockManager.setPin(pin)
        return "PIN set. You can now lock apps by voice."
    }

    private fun resolvePackageName(appName: String): String? {
        val pm = context.packageManager
        val apps = pm.getInstalledApplications(0)
        val match = apps.firstOrNull {
            pm.getApplicationLabel(it).toString().equals(appName, ignoreCase = true)
        } ?: apps.firstOrNull {
            pm.getApplicationLabel(it).toString().contains(appName, ignoreCase = true)
        }
        return match?.packageName
    }
}
