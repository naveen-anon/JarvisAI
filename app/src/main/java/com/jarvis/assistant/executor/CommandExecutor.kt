package com.jarvis.assistant.executor

import android.content.Context
import android.content.Intent
import android.hardware.camera2.CameraManager
import android.net.Uri
import android.provider.ContactsContract
import android.provider.Settings
import android.telephony.SmsManager
import com.jarvis.assistant.model.ActionType
import com.jarvis.assistant.model.AssistantCommand

class CommandExecutor(private val context: Context) {

    fun execute(cmd: AssistantCommand): String {
        return when (ActionType.fromKey(cmd.action)) {
            ActionType.OPEN_APP -> openApp(cmd.target)
            ActionType.CALL -> callContact(cmd.target)
            ActionType.SEND_SMS -> sendSms(cmd.target, cmd.message)
            ActionType.TOGGLE_SETTING -> toggleSetting(cmd.target)
            ActionType.READ_SCREEN -> "Reading screen requires the Accessibility Service overlay."
            ActionType.REPLY -> cmd.message ?: ""
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
}
