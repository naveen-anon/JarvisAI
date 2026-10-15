package com.jarvis.assistant.accessibility

import android.content.Intent
import com.jarvis.assistant.security.AppLockManager
import com.jarvis.assistant.security.LockScreenActivity

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class JarvisAccessibilityService : AccessibilityService() {

    private val appLockManager by lazy { AppLockManager(this) }


    private val handler = Handler(Looper.getMainLooper())

    private val sendButtonHints = listOf(
        "send", "भेजें", "com.whatsapp:id/send", "org.telegram.messenger:id/chat_send_button"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val ev = event ?: return

        // Jarvis app-lock: PIN gate over locked packages
        try {
            if (ev.eventType == AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED
            ) {
                val pkg = ev.packageName?.toString()
                if (!pkg.isNullOrBlank()
                    && pkg != packageName
                    && appLockManager.isLocked(pkg)
                    && !appLockManager.isSessionUnlocked(pkg)
                ) {
                    val i = Intent(this, LockScreenActivity::class.java).apply {
                        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                        putExtra(LockScreenActivity.EXTRA_PACKAGE, pkg)
                    }
                    startActivity(i)
                }
            }
        } catch (_: Exception) {}
    }

    override fun onInterrupt() {}


    /** Open the system Recents / Overview screen. */
    fun openRecents(): Boolean {
        return try {
            performGlobalAction(GLOBAL_ACTION_RECENTS)
        } catch (_: Exception) {
            false
        }
    }

    /**
     * Open Recents, then try to tap a "Clear all" style control (OEM-dependent labels).
     * Returns a short status string for TTS.
     */
    fun clearRecentsApps(): String {
        if (!openRecents()) {
            return "Couldn't open recent apps. Enable Accessibility for Jarvis, sir."
        }
        // Wait briefly for overview UI, then search for clear control
        handler.postDelayed({
            tryClickClearAll()
        }, 700L)
        return "Recent apps screen open — Clear all try kar raha hoon, sir."
    }

    private fun tryClickClearAll() {
        val root = rootInActiveWindow ?: return
        val labels = listOf(
            "Clear all", "Clear", "Close all", "CLEAR ALL", "CLEAR",
            "Remove all", "End all", "Clear everything",
            "सभी साफ़ करें", "सभी हटाएं", " sab saaf", "Clear all apps"
        )
        for (label in labels) {
            try {
                val nodes = root.findAccessibilityNodeInfosByText(label) ?: continue
                for (n in nodes) {
                    var cur: AccessibilityNodeInfo? = n
                    var depth = 0
                    while (cur != null && depth < 6) {
                        if (cur.isClickable) {
                            cur.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                            return
                        }
                        cur = cur.parent
                        depth++
                    }
                }
            } catch (_: Exception) {}
        }
        // Content-description search
        tryClickByDesc(root, 0)
    }

    private fun tryClickByDesc(node: AccessibilityNodeInfo, depth: Int) {
        if (depth > 18) return
        val d = (node.contentDescription?.toString() ?: "").lowercase()
        val t = (node.text?.toString() ?: "").lowercase()
        val hit = listOf("clear all", "close all", "remove all", "clear").any {
            d.contains(it) || t.contains(it)
        }
        if (hit && node.isClickable) {
            try { node.performAction(AccessibilityNodeInfo.ACTION_CLICK) } catch (_: Exception) {}
            return
        }
        for (i in 0 until node.childCount) {
            try {
                node.getChild(i)?.let { tryClickByDesc(it, depth + 1) }
            } catch (_: Exception) {}
        }
    }

    fun getScreenText(): String {
        val root = rootInActiveWindow ?: return "No screen content available."
        val seen = LinkedHashSet<String>()
        collectText(root, seen, 0)
        if (seen.isEmpty()) return "Screen appears empty."
        return seen.joinToString(" ").trim().replace(Regex("\\s+"), " ")
    }

    /**
     * Collect visible text + content descriptions, de-duplicated, depth-limited.
     */
    private fun collectText(node: AccessibilityNodeInfo, seen: MutableSet<String>, depth: Int) {
        if (depth > 28) return
        fun add(s: CharSequence?) {
            val t = s?.toString()?.trim() ?: return
            if (t.length < 2) return
            // Skip pure numeric noise / single symbols
            if (t.length == 1 && !t[0].isLetterOrDigit()) return
            seen.add(t)
        }
        add(node.text)
        add(node.contentDescription)
        // Some apps put primary label in hint
        try { add(node.hintText) } catch (_: Throwable) {}
        for (i in 0 until node.childCount) {
            try {
                node.getChild(i)?.let { collectText(it, seen, depth + 1) }
            } catch (_: Exception) { }
        }
    }

    fun autoTapSend(attemptsLeft: Int = 6) {
        if (attemptsLeft <= 0) {
            Log.w("JarvisAccessibility", "autoTapSend: gave up, no Send button found")
            return
        }
        handler.postDelayed({
            val root = rootInActiveWindow
            if (root == null) {
                autoTapSend(attemptsLeft - 1)
                return@postDelayed
            }
            val target = findSendButton(root)
            if (target != null) {
                target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
                Log.d("JarvisAccessibility", "autoTapSend: tapped send button")
            } else {
                autoTapSend(attemptsLeft - 1)
            }
        }, 700)
    }

    private fun findSendButton(node: AccessibilityNodeInfo): AccessibilityNodeInfo? {
        val desc = node.contentDescription?.toString()?.lowercase() ?: ""
        val id = node.viewIdResourceName ?: ""

        if (sendButtonHints.any { desc.contains(it, ignoreCase = true) || id.contains(it, ignoreCase = true) }) {
            if (node.isClickable) return node
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findSendButton(child)?.let { return it }
            }
        }
        return null
    }

    companion object {
        var instance: JarvisAccessibilityService? = null
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
    }

    override fun onDestroy() {
        instance = null
        super.onDestroy()
    }
}
