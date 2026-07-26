package com.jarvis.assistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo

/**
 * Optional deep-control layer. Grants ability to read screen contents and
 * simulate clicks/gestures on behalf of the user (e.g. "tap the send button").
 * This permission is heavily scrutinized by Play Store review — expect to
 * justify it with an in-app disclosure + limited scope if you plan to publish.
 */
class JarvisAccessibilityService : AccessibilityService() {

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Hook here to capture screen state changes if you want
        // context-aware commands like "reply to this message".
    }

    override fun onInterrupt() {}

    /** Returns visible text nodes on screen — used for "read screen" commands. */
    fun getScreenText(): String {
        val root: AccessibilityNodeInfo = rootInActiveWindow ?: return "No active window"
        val builder = StringBuilder()
        collectText(root, builder)
        return builder.toString().trim()
    }

    private fun collectText(node: AccessibilityNodeInfo, builder: StringBuilder) {
        node.text?.let { builder.append(it).append(". ") }
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { collectText(it, builder) }
        }
    }

    /** Finds a clickable node by its visible label and taps it. */
    fun tapByLabel(label: String): Boolean {
        val root = rootInActiveWindow ?: return false
        val target = findNodeByText(root, label) ?: return false
        return target.performAction(AccessibilityNodeInfo.ACTION_CLICK)
    }

    private fun findNodeByText(node: AccessibilityNodeInfo, text: String): AccessibilityNodeInfo? {
        if (node.text?.toString()?.contains(text, ignoreCase = true) == true) return node
        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                findNodeByText(child, text)?.let { return it }
            }
        }
        return null
    }
}
