package com.jarvis.assistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.os.Handler
import android.os.Looper
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log

class JarvisAccessibilityService : AccessibilityService() {

    private val handler = Handler(Looper.getMainLooper())

    private val sendButtonHints = listOf(
        "send", "भेजें", "com.whatsapp:id/send", "org.telegram.messenger:id/chat_send_button"
    )

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {}
    override fun onInterrupt() {}

    fun getScreenText(): String {
        val root = rootInActiveWindow ?: return "No screen content available."
        val seen = LinkedHashSet<String>()
        collectText(root, seen, 0)
        if (seen.isEmpty()) return "Screen appears empty."
        return seen.joinToString(" ").trim().replace(Regex("\\s+"), " ")
    }

    private fun collectText(node: AccessibilityNodeInfo, seen: MutableSet<String>, depth: Int) {
        if (depth > 28) return
        fun add(s: CharSequence?) {
            val t = s?.toString()?.trim() ?: return
            if (t.length < 2) return
            if (t.length == 1 && !t[0].isLetterOrDigit()) return
            seen.add(t)
        }
        add(node.text)
        add(node.contentDescription)
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
