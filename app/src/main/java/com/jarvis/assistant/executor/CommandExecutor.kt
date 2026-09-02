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
import com.jarvis.assistant.armor.ArmorCatalog
import com.jarvis.assistant.armor.ArmorDetailActivity
import com.jarvis.assistant.model.AssistantCommand
import com.jarvis.assistant.security.AppLockManager
import com.jarvis.assistant.vision.VisionActivity
import com.jarvis.assistant.util.MessagingHelper
import com.jarvis.assistant.accessibility.JarvisAccessibilityService

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
            ActionType.SHOW_ARMOR -> showArmor(cmd.target)
            ActionType.WEB_SEARCH -> webSearch(cmd.target)
            ActionType.LOCK_APP -> lockApp(cmd.target)
            ActionType.UNLOCK_APP -> unlockApp(cmd.target)
            ActionType.SET_PIN -> setPin(cmd.target)
            ActionType.WHATSAPP_MESSAGE -> whatsappMessage(cmd.target, cmd.message)
            ActionType.TELEGRAM_MESSAGE -> telegramMessage(cmd.target, cmd.message)
            ActionType.ANALYZE_SCREEN -> summarizeScreen()
            ActionType.READ_SCREEN -> readScreenRaw()
            
            ActionType.SET_REMINDER -> {
                val mins = cmd.target?.toIntOrNull() ?: 10
                val msg = cmd.message ?: "Reminder"
                com.jarvis.assistant.util.ReminderScheduler(context).scheduleInMinutes(mins, msg)
            }
            ActionType.READ_CLIPBOARD -> com.jarvis.assistant.util.ClipboardHelper(context).readAloudFriendly()
            ActionType.OPEN_CLIPBOARD_LINK -> com.jarvis.assistant.util.ClipboardHelper(context).openIfUrl()
            ActionType.NOTIF_SUMMARY -> com.jarvis.assistant.notifications.JarvisNotificationListener.summaryOrHelp()
            ActionType.CALENDAR_NEXT -> com.jarvis.assistant.util.CalendarHelper(context).formatBrief(2)

            
            ActionType.FOCUS_MODE -> {
                val h = com.jarvis.assistant.util.FocusModeHelper(context)
                when (cmd.target?.lowercase()) {
                    "on", "enable", "start" -> h.enableDnd()
                    "off", "disable", "stop" -> h.disableDnd()
                    else -> h.status()
                }
            }

            ActionType.REPLY -> cmd.message ?: ""
            ActionType.PC_CONNECT -> "PC connect is handled by the assistant service, not here."
            ActionType.UNKNOWN -> "I didn't understand that command."
        }
    }

    private fun openApp(appName: String?): String {
        if (appName.isNullOrBlank()) return "Which application should I open, sir?"
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
        if (name.isNullOrBlank()) return "Whom shall I call, sir?"
        val number = lookupContactNumber(name) ?: return "I couldn't locate a number for $name, sir."
        val intent = Intent(Intent.ACTION_CALL, Uri.parse("tel:$number")).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        return "Calling $name, sir."
    }

    // Requires SEND_SMS permission granted at runtime.
    private fun sendSms(name: String?, message: String?): String {
        if (name.isNullOrBlank() || message.isNullOrBlank()) return "I need both a recipient and a message, sir."
        val number = lookupContactNumber(name) ?: return "I couldn't locate a number for $name, sir."
        SmsManager.getDefault().sendTextMessage(number, null, message, null, null)
        return "Message sent to $name, sir."
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
        val camId = cameraManager.cameraIdList.firstOrNull() ?: return "No suitable camera module detected, sir."
        torchOn = !torchOn
        cameraManager.setTorchMode(camId, torchOn)
        return if (torchOn) "Flashlight activated, sir." else "Flashlight deactivated, sir."
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
    /**
     * WhatsApp gives no official API for a regular app to silently send a message —
     * only the user tapping Send inside WhatsApp itself can do that (by design, to
     * prevent spam). This opens WhatsApp's chat with the contact and the message already
     * typed in, then the Accessibility Service (if enabled) auto-taps Send for a
     * hands-free flow.
     */
    private fun whatsappMessage(target: String?, message: String?): String {
        if (target.isNullOrBlank()) return "Send a WhatsApp message to whom?"
        if (message.isNullOrBlank()) return "What should the message say?"
        val rawNumber = lookupContactNumber(target) ?: return "No number found for $target"

        val digits = rawNumber.filter { it.isDigit() }
        val intlNumber = if (digits.length == 10) "91$digits" else digits

        val uri = Uri.parse("https://wa.me/$intlNumber?text=${Uri.encode(message)}")
        return try {
            context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            })
            JarvisAccessibilityService.instance?.autoTapSend()
            if (JarvisAccessibilityService.instance != null) "Message prepared and sent to $target on WhatsApp, sir."
            else "Opening WhatsApp for $target, sir. Tap Send to confirm, or enable the Accessibility Service for automatic delivery."
        } catch (e: Exception) {
            try {
                context.startActivity(Intent(Intent.ACTION_VIEW, uri).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                })
                JarvisAccessibilityService.instance?.autoTapSend()
                if (JarvisAccessibilityService.instance != null) "Message prepared and sent to $target on WhatsApp, sir."
                else "Opening WhatsApp for $target, sir. Tap Send to confirm, or enable the Accessibility Service for automatic delivery."
            } catch (e2: Exception) {
                "I was unable to open WhatsApp, sir. Please confirm it is installed."
            }
        }
    }

    /**
     * Telegram gives no official API for silently sending either — same
     * restriction as WhatsApp. Opens Telegram's chat with the message
     * pre-filled, then Accessibility Service auto-taps Send if enabled.
     */
    private fun telegramMessage(target: String?, message: String?): String {
        if (target.isNullOrBlank()) return "Send a Telegram message to whom?"
        if (message.isNullOrBlank()) return "What should the message say?"

        val messagingHelper = MessagingHelper(context)
        val sent = messagingHelper.sendTelegram(target, message)

        return if (sent) {
            JarvisAccessibilityService.instance?.autoTapSend()
            if (JarvisAccessibilityService.instance != null) "Message prepared and sent to $target on Telegram, sir."
            else "Opening Telegram for $target, sir. Tap Send to confirm, or enable the Accessibility Service for automatic delivery."
        } else {
            "I was unable to open Telegram, sir. Please confirm it is installed."
        }
    }


    /** Launches MainActivity with a flag telling it to trigger the real
     *  unlock flow there (biometric prompt needs a live Activity, which a
     *  background service/executor doesn't have). */
    private fun triggerUnlock(): String {
        val intent = Intent(context, com.jarvis.assistant.MainActivity::class.java).apply {
            putExtra(com.jarvis.assistant.security.UnlockHelper.EXTRA_TRIGGER_UNLOCK, true)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_REORDER_TO_FRONT)
        }
        context.startActivity(intent)
        return "One moment, sir."
    }

    private fun lockApp(appName: String?): String {
        if (appName.isNullOrBlank()) return "Which application should I lock, sir?"
        if (!lockManager.hasPin()) {
            return "A security PIN has not been configured yet, sir. Please say \"set my pin to\" followed by four digits."
        }
        val packageName = resolvePackageName(appName) ?: return "I couldn't find $appName installed."
        lockManager.lockApp(packageName)
        return "$appName is now locked, sir. Your PIN will be required to open it."
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

    /** Opens liquid-glass armor detail for Mark I–XLII. */
    private fun showArmor(target: String?): String {
        val q = target?.trim().orEmpty()
        if (q.isEmpty()) return "Which armor mark should I display? Say mark 1 through 42."
        val mark = ArmorCatalog.search(q)
            ?: return "I couldn't find armor matching \"$q\". Try mark 33 or silver centurion."
        return try {
            val intent = Intent(context, ArmorDetailActivity::class.java).apply {
                putExtra(ArmorDetailActivity.EXTRA_MARK, mark.number)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Displaying Mark ${mark.roman} — ${mark.codename}."
        } catch (e: Exception) {
            "Couldn't open the armor archive: ${e.message}"
        }
    }


    private fun summarizeScreen(): String {
        val svc = com.jarvis.assistant.accessibility.JarvisAccessibilityService.instance
            ?: return "Enable Accessibility for Jarvis first, sir — Settings → Accessibility → Jarvis."
        val raw = svc.getScreenText()
        if (raw.isBlank() || raw.startsWith("No screen") || raw.startsWith("Screen appears")) return raw
        val cleaned = raw.replace(Regex("\\s+"), " ").trim()
        val short = if (cleaned.length > 600) cleaned.take(600).trimEnd() + "…" else cleaned
        return "On your screen I can see: $short"
    }

    private fun readScreenRaw(): String {
        val svc = com.jarvis.assistant.accessibility.JarvisAccessibilityService.instance
            ?: return "Accessibility is off, sir. Enable Jarvis under Settings → Accessibility."
        val text = svc.getScreenText()
        if (text.isBlank() || text.startsWith("No screen") || text.startsWith("Screen appears")) return text
        return if (text.length > 900) text.take(900) + "… (truncated)" else text
    }

}
