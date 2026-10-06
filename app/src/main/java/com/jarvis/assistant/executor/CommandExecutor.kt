package com.jarvis.assistant.executor

import android.app.SearchManager
import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.media.AudioManager
import android.net.Uri
import android.provider.AlarmClock
import android.provider.MediaStore
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
        // Stark L6 — confirm gate
        try {
            val held = com.jarvis.assistant.executor.RiskConfirmGate.takeIfConfirm(
                listOfNotNull(cmd.message, cmd.target, cmd.action).joinToString(" ")
            )
            // actual confirm speech is handled in service; here gate risky new commands
        } catch (_: Exception) {}
        if (!riskBypass && com.jarvis.assistant.executor.RiskConfirmGate.isRisky(cmd)) {
            try { com.jarvis.assistant.ui.HudController.thinking() } catch (_: Exception) {}
            return com.jarvis.assistant.executor.RiskConfirmGate.hold(cmd)
        }

        return when (ActionType.fromKey(cmd.action)) {
            ActionType.OPEN_APP -> openApp(cmd.target)
            ActionType.APP_CALL -> appCall(cmd.target, cmd.message)
            ActionType.CALL -> callContact(cmd.target)
            ActionType.SEND_SMS -> sendSms(cmd.target, cmd.message)
            ActionType.TOGGLE_SETTING -> toggleSetting(cmd.target)
            ActionType.SET_VOLUME -> setVolume(cmd.target)
            ActionType.MEDIA_CONTROL -> mediaControl(cmd.target)
            ActionType.SET_ALARM -> setAlarm(cmd.target)
            ActionType.SET_TIMER -> setTimer(cmd.target)
            ActionType.OPEN_CLOCK -> openClock(cmd.target, cmd.message)
            ActionType.STOPWATCH -> openClock("stopwatch", cmd.target ?: "start")
            ActionType.OPEN_VISION -> openVision(cmd.target)
            ActionType.SHOW_ARMOR -> showArmor(cmd.target)
            ActionType.WEB_SEARCH -> webSearch(cmd.target)
            ActionType.PLAY_MEDIA -> playMedia(cmd.target, cmd.message)
            ActionType.LOCK_APP -> lockApp(cmd.target)
            ActionType.UNLOCK_APP -> unlockApp(cmd.target)
            ActionType.UNLOCK_PHONE -> unlockPhone()
            ActionType.SET_PIN -> setPin(cmd.target)
            ActionType.WHATSAPP_CALL -> whatsappCall(cmd.target, cmd.message?.contains("video") == true)
            ActionType.WHATSAPP_MESSAGE -> whatsappMessage(cmd.target, cmd.message)
            ActionType.TELEGRAM_MESSAGE -> telegramMessage(cmd.target, cmd.message)
            ActionType.SCREENSHOT -> runScreenshotAnalyze()
            ActionType.ANALYZE_SCREEN -> runScreenshotAnalyze()
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

                                    ActionType.EQUIP_SUIT -> runEquipSuit(cmd)
            ActionType.SCREEN_ACT -> runScreenAct(cmd)
            ActionType.OPEN_URL -> openUrl(cmd.target)
            ActionType.MULTI_STEP -> executeMultiStep(cmd)
                        ActionType.REMEMBER -> runRemember(cmd)
            ActionType.RECALL -> runRecall()
                        ActionType.DEBRIEF -> runDebrief()
            ActionType.BRIEFING -> runBriefing()
            ActionType.REPLY -> cmd.message ?: ""
            ActionType.PC_CONNECT -> "PC connect is handled by the assistant service, not here."
            ActionType.UNKNOWN -> "I didn't understand that command."
        }
    }


    private fun appCall(app: String?, kind: String?): String {
        if (app.isNullOrBlank()) return "Which app should I use for the call, sir?"
        val a = app.trim().lowercase()
        val video = (kind ?: "voice").lowercase().contains("video")
        val pm = context.packageManager
        val packages = when {
            "business" in a -> listOf("com.whatsapp.w4b", "com.whatsapp")
            "whatsapp" in a -> listOf("com.whatsapp", "com.whatsapp.w4b")
            "telegram" in a -> listOf("org.telegram.messenger", "org.telegram.messenger.web")
            "instagram" in a || "insta" in a -> listOf("com.instagram.android")
            "facebook" in a || "messenger" in a || a == "fb" ->
                listOf("com.facebook.orca", "com.facebook.mlite", "com.facebook.katana")
            "discord" in a -> listOf("com.discord")
            "signal" in a -> listOf("org.thoughtcrime.securesms")
            "snap" in a -> listOf("com.snapchat.android")
            else -> emptyList()
        }
        if (packages.isEmpty()) return openApp(app)
        val label = a.replaceFirstChar { it.uppercase() }
        for (pkg in packages) {
            val launch = pm.getLaunchIntentForPackage(pkg) ?: continue
            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            try {
                com.jarvis.assistant.security.AppLockManager(context).markSessionUnlocked(pkg)
            } catch (_: Exception) {}
            context.startActivity(launch)
            val mode = if (video) "video call" else "voice call"
            return "Opening $label for a $mode. Select the contact in the app, sir."
        }
        return "$label does not appear to be installed, sir."
    }


    private fun whatsappCall(name: String?, video: Boolean): String {
        if (name.isNullOrBlank()) return "Whom should I call on WhatsApp, sir?"
        val number = lookupContactNumber(name)
            ?: return "I couldn't find a number for $name, sir."
        val digits = number.filter { it.isDigit() }
        if (digits.length < 8) return "Number for $name looks invalid, sir."
        val phone = if (digits.length == 10) "91$digits" else digits
        return try {
            val uri = android.net.Uri.parse("https://wa.me/$phone")
            val i = Intent(Intent.ACTION_VIEW, uri).apply {
                setPackage("com.whatsapp")
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            if (i.resolveActivity(context.packageManager) == null) {
                i.setPackage("com.whatsapp.w4b")
            }
            if (i.resolveActivity(context.packageManager) == null) {
                i.setPackage(null)
            }
            try {
                com.jarvis.assistant.security.AppLockManager(context).markSessionUnlocked("com.whatsapp")
                com.jarvis.assistant.security.AppLockManager(context).markSessionUnlocked("com.whatsapp.w4b")
            } catch (_: Exception) {}
            context.startActivity(i)
            "Opening WhatsApp with $name."
        } catch (_: Exception) {
            "Couldn't open WhatsApp for $name."
        }
    }

    private fun openApp(appName: String?): String {
        if (appName.isNullOrBlank()) return "Which application should I open, sir?"
        val q = appName.trim().lowercase()
        val pm = context.packageManager

        val aliases = linkedMapOf(
            "whatsapp business" to listOf("com.whatsapp.w4b"),
            "wa business" to listOf("com.whatsapp.w4b"),
            "business whatsapp" to listOf("com.whatsapp.w4b"),
            "whatsapp" to listOf("com.whatsapp"),
            "telegram" to listOf("org.telegram.messenger", "org.telegram.messenger.web"),
            "chrome" to listOf("com.android.chrome", "com.chrome.beta", "com.chrome.dev", "com.sec.android.app.sbrowser", "com.opera.browser", "org.mozilla.firefox", "com.android.browser"),
            "google chrome" to listOf("com.android.chrome", "com.chrome.beta"),
            "browser" to listOf("com.android.chrome", "com.sec.android.app.sbrowser", "com.android.browser"),
            "youtube" to listOf("com.google.android.youtube"),
            "gmail" to listOf("com.google.android.gm"),
            "maps" to listOf("com.google.android.apps.maps"),
            "google maps" to listOf("com.google.android.apps.maps"),
            "settings" to listOf("com.android.settings"),
            "clock" to listOf(
                "com.google.android.deskclock",
                "com.android.deskclock",
                "com.sec.android.app.clockpackage",
                "com.oneplus.deskclock",
                "com.coloros.alarmclock",
                "com.miui.clock"
            ),
            "grok" to listOf("ai.x.grok", "com.x.grok", "com.x.ai", "ai.x.android", "com.twitter.android"),
            "x ai" to listOf("ai.x.grok", "com.x.grok", "com.x.ai"),
            "instagram" to listOf("com.instagram.android"),
            "spotify" to listOf("com.spotify.music"),
            "paytm" to listOf("net.one97.paytm"),
            "phonepe" to listOf("com.phonepe.app"),
            "gpay" to listOf("com.google.android.apps.nbu.paisa.user"),
            "google pay" to listOf("com.google.android.apps.nbu.paisa.user")
        )

        for ((key, packages) in aliases.entries.sortedByDescending { it.key.length }.map { it.key to it.value }) {
            if (
                q == key ||
                q == key.replace(" ", "") ||
                (key.contains(" ") && (q == key || q.contains(key))) ||
                (!key.contains(" ") && q == key)
            ) {
                for (pkg in packages) {
                    val launch = pm.getLaunchIntentForPackage(pkg)
                    if (launch != null) {
                        launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                        context.startActivity(launch)
                        try {
                            com.jarvis.assistant.security.AppLockManager(context).markSessionUnlocked(pkg)
                        } catch (_: Exception) {}
                        return "Opening $appName."
                    }
                }
            }
        }

        // Browser fallback if Chrome package missing / restricted
        if (q.contains("chrome") || q.contains("browser")) {
            try {
                val view = Intent(Intent.ACTION_VIEW, android.net.Uri.parse("https://www.google.com")).apply {
                    addCategory(Intent.CATEGORY_BROWSABLE)
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(view)
                return "Opening browser."
            } catch (_: Exception) {}
        }

        val apps = pm.getInstalledApplications(0)
        val labeled = apps.map { app -> pm.getApplicationLabel(app).toString() to app }
        val exact = labeled.firstOrNull { it.first.equals(appName.trim(), ignoreCase = true) }
        val wantBusiness = q.contains("business")
        val containsList = labeled.filter { it.first.contains(appName.trim(), ignoreCase = true) }
        val contains = if (wantBusiness) {
            containsList.filter {
                it.first.contains("business", ignoreCase = true) ||
                    it.second.packageName.contains("w4b")
            }.maxByOrNull { it.first.length } ?: containsList.maxByOrNull { it.first.length }
        } else {
            containsList
                .filter { !it.first.contains("business", ignoreCase = true) || q.contains("business") }
                .maxByOrNull { it.first.length }
        }
        val match = exact?.second ?: contains?.second
        if (match != null) {
            val launchIntent = pm.getLaunchIntentForPackage(match.packageName)
            if (launchIntent != null) {
                launchIntent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launchIntent)
                try {
                    com.jarvis.assistant.security.AppLockManager(context).markSessionUnlocked(match.packageName)
                } catch (_: Exception) {}
                return "Opening ${pm.getApplicationLabel(match)}."
            }
            return "Can't launch $appName."
        }
        return "I couldn't find $appName installed."
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
        val q = name.trim()
        if (q.isEmpty()) return null
        val resolver = context.contentResolver
        val uri = ContactsContract.CommonDataKinds.Phone.CONTENT_URI
        val projection = arrayOf(
            ContactsContract.CommonDataKinds.Phone.NUMBER,
            ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
        )
        return try {
            val likeHit = resolver.query(
                uri, projection,
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$q%"),
                null
            )?.use { c ->
                if (c.moveToFirst()) c.getString(0) else null
            }
            if (!likeHit.isNullOrBlank()) return likeHit

            resolver.query(uri, projection, null, null, null)?.use { c ->
                val numIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                val nameIdx = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                if (numIdx < 0 || nameIdx < 0) return@use null
                val ql = q.lowercase()
                var exact: String? = null
                var starts: String? = null
                var contains: String? = null
                var tokenHit: String? = null
                while (c.moveToNext()) {
                    val display = c.getString(nameIdx)?.trim() ?: continue
                    val number = c.getString(numIdx) ?: continue
                    val d = display.lowercase()
                    when {
                        d == ql -> { exact = number; break }
                        starts == null && (d.startsWith(ql) || ql.startsWith(d)) -> starts = number
                        contains == null && (d.contains(ql) || ql.contains(d)) -> contains = number
                    }
                    val tokens = ql.split(Regex("\\s+")).filter { it.length >= 2 }
                    if (tokenHit == null && tokens.isNotEmpty() && tokens.all { d.contains(it) }) {
                        tokenHit = number
                    }
                }
                exact ?: starts ?: tokenHit ?: contains
            }
        } catch (_: SecurityException) {
            null
        } catch (_: Exception) {
            null
        }
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
        // "2 minutes" style → treat as timer
        val asTimer = target?.trim()?.let { raw ->
            val m = Regex("""(?i)(\d+)\s*(min|minute|minutes|m)\b""").find(raw)
            m?.groupValues?.get(1)?.toIntOrNull()?.times(60)
        }
        if (asTimer != null && asTimer > 0) {
            return setTimer(asTimer.toString())
        }

        val parsed = parseHourMinute(target)
        if (parsed == null) {
            // No clock time — open Jarvis alarm UI
            return openClock("alarm", null)
        }
        val (hour, minute) = parsed
        val intent = Intent(AlarmClock.ACTION_SET_ALARM).apply {
            putExtra(AlarmClock.EXTRA_HOUR, hour)
            putExtra(AlarmClock.EXTRA_MINUTES, minute)
            putExtra(AlarmClock.EXTRA_SKIP_UI, false)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Jarvis")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        val h12 = if (hour % 12 == 0) 12 else hour % 12
        val ampm = if (hour < 12) "AM" else "PM"
        val display = "$h12:" + minute.toString().padStart(2, '0') + " $ampm"
        try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return "Alarm set for $display."
            }
        } catch (_: Exception) { }
        // No system clock app — Jarvis window
        return openClock("alarm", null)
    }


    private fun openClock(mode: String?, action: String?): String {
        val m = (mode ?: "alarm").lowercase()
        val i = Intent(context, com.jarvis.assistant.ui.clock.ClockActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(com.jarvis.assistant.ui.clock.ClockActivity.EXTRA_MODE, m)
            when {
                m == "timer" -> {
                    val sec = action?.toIntOrNull() ?: 0
                    if (sec > 0) putExtra(com.jarvis.assistant.ui.clock.ClockActivity.EXTRA_SECONDS, sec)
                    putExtra(com.jarvis.assistant.ui.clock.ClockActivity.EXTRA_AUTO_START, sec > 0)
                }
                m == "stopwatch" -> {
                    val auto = action.isNullOrBlank() || action == "start"
                    putExtra(com.jarvis.assistant.ui.clock.ClockActivity.EXTRA_AUTO_START, auto)
                }
            }
        }
        return try {
            context.startActivity(i)
            when (m) {
                "timer" -> "Timer ready, sir."
                "stopwatch" -> "Stopwatch ready, sir."
                else -> "Clock open, sir."
            }
        } catch (_: Exception) {
            "Couldn't open clock."
        }
    }

    private fun setTimer(target: String?): String {
        val seconds = target?.toIntOrNull() ?: return "How long should the timer be, sir?"
        // 1) System clock if available
        val intent = Intent(AlarmClock.ACTION_SET_TIMER).apply {
            putExtra(AlarmClock.EXTRA_LENGTH, seconds)
            putExtra(AlarmClock.EXTRA_SKIP_UI, true)
            putExtra(AlarmClock.EXTRA_MESSAGE, "Jarvis")
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        try {
            if (intent.resolveActivity(context.packageManager) != null) {
                context.startActivity(intent)
                return "Timer started for ${formatDuration(seconds)}."
            }
        } catch (_: Exception) {}
        // 2) In-app Jarvis clock window
        return openClock("timer", seconds.toString())
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
        if (query.isNullOrBlank()) return "What should I search for, sir?"
        return try {
            val intent = Intent(Intent.ACTION_VIEW, Uri.parse(
                "https://www.google.com/search?q=" + Uri.encode(query.trim())
            )).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
            context.startActivity(intent)
            "Searching for $query."
        } catch (_: Exception) {
            "Couldn't open web search, sir."
        }
    }



    /**
     * Personal design: play on the app the user asked for.
     * message = optional app hint: spotify | youtube music | youtube
     */
    private fun playMedia(query: String?, appHint: String?): String {
        if (query.isNullOrBlank()) return "What should I play, sir?"
        val q = query.trim()
        val hint = (appHint ?: "").lowercase()
        val pm = context.packageManager

        fun tryStart(i: Intent): Boolean {
            return try {
                i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (i.resolveActivity(pm) == null) return false
                context.startActivity(i)
                true
            } catch (_: Exception) {
                false
            }
        }

        when {
            "spotify" in hint -> {
                val deep = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:" + Uri.encode(q)))
                if (tryStart(deep)) return "Opening Spotify for $q."
                val web = Intent(Intent.ACTION_VIEW, Uri.parse("https://open.spotify.com/search/" + Uri.encode(q)))
                if (tryStart(web)) return "Opening Spotify search for $q."
                return "Spotify doesn't appear to be installed, sir."
            }
            "youtube music" in hint || "yt music" in hint -> {
                val i = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.apps.youtube.music")
                    putExtra("query", q)
                }
                if (tryStart(i)) return "Opening YouTube Music for $q."
                val v = Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://music.youtube.com/search?q=" + Uri.encode(q))
                ).apply { setPackage("com.google.android.apps.youtube.music") }
                if (tryStart(v)) return "Opening YouTube Music for $q."
                return "YouTube Music doesn't appear to be installed, sir."
            }
            "youtube" in hint && "music" !in hint -> {
                val play = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra(SearchManager.QUERY, q)
                    putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/audio")
                }
                if (tryStart(play)) return "Playing $q on YouTube."
                val search = Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.youtube")
                    putExtra("query", q)
                }
                if (tryStart(search)) return "Searching YouTube for $q — tap the first result to play."
            }
        }

        // No / unknown hint: system play-from-search (often Spotify / YT Music)
        val globalPlay = Intent(MediaStore.INTENT_ACTION_MEDIA_PLAY_FROM_SEARCH).apply {
            putExtra(SearchManager.QUERY, q)
            putExtra(MediaStore.EXTRA_MEDIA_FOCUS, "vnd.android.cursor.item/audio")
        }
        if (tryStart(globalPlay)) return "Playing $q."

        if (pm.getLaunchIntentForPackage("com.spotify.music") != null) {
            val i = Intent(Intent.ACTION_VIEW, Uri.parse("spotify:search:" + Uri.encode(q)))
            if (tryStart(i)) return "Opening Spotify for $q."
        }

        val ytMusic = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.apps.youtube.music")
            putExtra("query", q)
        }
        if (tryStart(ytMusic)) return "Opening YouTube Music for $q."

        val yt = Intent(Intent.ACTION_SEARCH).apply {
            setPackage("com.google.android.youtube")
            putExtra("query", q)
        }
        if (tryStart(yt)) return "Searching YouTube for $q — tap the first result to play."

        val web = Intent(
            Intent.ACTION_VIEW,
            Uri.parse("https://www.youtube.com/results?search_query=" + Uri.encode(q))
        )
        if (tryStart(web)) return "Opening YouTube for $q."
        return "Couldn't start playback, sir."
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


    /**
     * Best-effort unlock: wake screen + request keyguard dismiss.
     * Cannot bypass PIN/pattern/biometric — Android forbids that.
     * Opens MainActivity and asks system to dismiss keyguard (user may still need biometric/PIN).
     */
    private fun unlockPhone(): String {
        return try {
            val intent = Intent(context, com.jarvis.assistant.MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                putExtra(com.jarvis.assistant.security.UnlockHelper.EXTRA_TRIGGER_UNLOCK, true)
            }
            context.startActivity(intent)
            // Also try to turn screen on via activity flags handled in MainActivity/UnlockHelper
            "Bringing Jarvis forward. Confirm with your fingerprint or PIN if the system asks, sir."
        } catch (e: Exception) {
            "I couldn't start the unlock flow: ${e.message}"
        }
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

    /** Opens Hall of Armor (holo grid) or a specific mark detail. */
    private fun showArmor(target: String?): String {
        val q = target?.trim().orEmpty()
        // No mark specified → full interactive holographic archive
        if (q.isEmpty() || q.equals("archive", true) || q.equals("suits", true) ||
            q.equals("hall", true) || q.equals("list", true)
        ) {
            return try {
                val intent = Intent(context, com.jarvis.assistant.armor.ArmorHoloArchiveActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(intent)
                "Opening the Hall of Armor."
            } catch (e: Exception) {
                "Couldn't open the Hall of Armor: ${e.message}"
            }
        }
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


    private fun executeMultiStep(cmd: AssistantCommand): String {
        try { com.jarvis.assistant.ui.HudController.executing() } catch (_: Exception) {}
        val steps = cmd.steps
        if (steps.isNullOrEmpty()) {
            return cmd.message?.takeIf { it.isNotBlank() } ?: "No steps to execute."
        }
        val out = mutableListOf<String>()
        for (step in steps) {
            try {
                val r = execute(step)
                if (r.isNotBlank()) out.add(r)
            } catch (e: Exception) {
                out.add("Step failed: ${e.message}")
                break
            }
        }
        return out.joinToString(" ").ifBlank { "Done." }
    }


    private fun runBriefing(): String {
        return try {
            val text = kotlinx.coroutines.runBlocking {
                com.jarvis.assistant.util.BriefingHelper(context).build(includeWeather = true)
            }.text
            text
        } catch (e: Exception) {
            "Briefing unavailable right now, sir."
        }
    }


    private fun runScreenshotAnalyze(): String {
        return try {
            val result = kotlinx.coroutines.runBlocking {
                com.jarvis.assistant.util.ScreenshotHelper.captureAndAnalyze()
            }
            result.summary
        } catch (e: Exception) {
            "Could not analyze the screen, sir."
        }
    }


    private fun runRemember(cmd: AssistantCommand): String {
        val mem = com.jarvis.assistant.memory.JarvisMemory(context)
        val target = cmd.target?.lowercase().orEmpty()
        val msg = cmd.message?.trim().orEmpty()
        return when {
            target == "name" && msg.isNotBlank() -> {
                mem.setUserName(msg)
                "Understood. I will address you as $msg."
            }
            target == "note" && msg.isNotBlank() -> {
                mem.addNote(msg)
                "Noted."
            }
            msg.lowercase().startsWith("call me ") -> {
                val n = msg.substringAfter(" ").substringAfter(" ").trim()
                mem.setUserName(n)
                "Understood. I will address you as $n."
            }
            msg.isNotBlank() -> {
                mem.addNote(msg)
                "I will remember that."
            }
            else -> "What should I remember?"
        }
    }

    private fun runRecall(): String {
        val mem = com.jarvis.assistant.memory.JarvisMemory(context)
        val name = mem.getUserName()
        val notes = mem.listNotes(5)
        val parts = mutableListOf<String>()
        if (name.isNotBlank()) parts += "You asked me to call you $name."
        else parts += "You have not set a preferred name yet."
        parts += "Preferred chat app is ${mem.getPreferredChatApp()}."
        if (notes.isNotEmpty()) parts += "Recent notes: " + notes.joinToString("; ")
        else parts += "No additional notes stored."
        return parts.joinToString(" ")
    }


    private fun runScreenAct(cmd: AssistantCommand): String {
        try { com.jarvis.assistant.ui.HudController.executing() } catch (_: Exception) {}
        val text = com.jarvis.assistant.util.ScreenActHelper.captureText()
        val plan = com.jarvis.assistant.util.ScreenActHelper.plan(context, text, cmd.message)
        if (plan.action != null) {
            val follow = plan.action!!
            val type = try {
                com.jarvis.assistant.model.ActionType.fromKey(follow.action)
            } catch (_: Exception) {
                null
            }
            when (type) {
                com.jarvis.assistant.model.ActionType.OPEN_URL -> openUrl(follow.target)
                com.jarvis.assistant.model.ActionType.CALL -> dialNumber(follow.target)
                else -> { /* report only */ }
            }
        }
        try {
            if (plan.action != null) com.jarvis.assistant.ui.HudController.done()
            else com.jarvis.assistant.ui.HudController.done()
        } catch (_: Exception) {}
        return plan.spoken
    }

    private fun openUrl(url: String?): String {
        if (url.isNullOrBlank()) return "Which link should I open?"
        return if (com.jarvis.assistant.util.ScreenActHelper.openUrl(context, url)) {
            "Opening link."
        } else {
            "I could not open that link."
        }
    }

    private fun dialNumber(number: String?): String {
        if (number.isNullOrBlank()) return "Which number should I call?"
        return try {
            val intent = android.content.Intent(android.content.Intent.ACTION_DIAL).apply {
                data = android.net.Uri.parse("tel:$number")
                addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(intent)
            "Dialing $number."
        } catch (e: Exception) {
            "I could not start the dialer."
        }
    }


    private fun runEquipSuit(cmd: AssistantCommand): String {
        return try {
            val repo = com.jarvis.ai.data.repository.SuitRepository()
            val q = listOfNotNull(cmd.target, cmd.message).joinToString(" ").lowercase()
            val suit = repo.getAllSuits().firstOrNull { s ->
                q.contains(s.id.replace("_", " ")) ||
                    q.contains(s.mark.name.replace("_", " ").lowercase()) ||
                    q.contains(s.name.substringBefore("—").substringBefore("-").trim().lowercase())
            } ?: repo.getAllSuits().firstOrNull {
                it.id == "mark_3" || it.mark.name.contains("3")
            } ?: return "Suit not found in archive."
            try { com.jarvis.assistant.ui.HudController.executing() } catch (_: Exception) {}
            val msg = com.jarvis.ai.controller.ArmorController.equipWithContext(context, suit)
            try { com.jarvis.assistant.ui.HudController.done() } catch (_: Exception) {}
            msg
        } catch (e: Exception) {
            "Unable to equip suit: ${e.message}"
        }
    }


    @Volatile private var riskBypass: Boolean = false

    fun executeConfirmed(cmd: AssistantCommand): String {
        riskBypass = true
        try {
            return execute(cmd)
        } finally {
            riskBypass = false
        }
    }


    private fun runDebrief(): String {
        return try {
            com.jarvis.assistant.util.EveningDebrief(context).buildDebrief()
        } catch (e: Exception) {
            "Debrief unavailable."
        }
    }

}
