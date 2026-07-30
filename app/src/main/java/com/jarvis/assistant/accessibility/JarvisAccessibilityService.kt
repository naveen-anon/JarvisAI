package com.jarvis.assistant.accessibility

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.jarvis.assistant.security.AppLockManager
import com.jarvis.assistant.security.LockScreenActivity

/**
 * Optional deep-control layer. Grants ability to read screen contents and
 * simulate clicks/gestures on behalf of the user (e.g. "tap the send button").
 * This permission is heavily scrutinized by Play Store review — expect to
 * justify it with an in-app disclosure + limited scope if you plan to publish.
 *
 * Also enforces Phase 5's "App lock by voice": on every window change, if the
 * newly-foregrounded package is in AppLockManager's locked set and hasn't been
 * unlocked this session, it launches LockScreenActivity on top of it.
 */
class JarvisAccessibilityService : AccessibilityService() {

    private lateinit var lockManager: AppLockManager
    private var lastCheckedPackage: String? = null

    override fun onCreate() {
        super.onCreate()
        lockManager = AppLockManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        if (event?.eventType != AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED) return
        val pkg = event.packageName?.toString() ?: return
        if (pkg == packageName || pkg == lastCheckedPackage) return
        lastCheckedPackage = pkg

        if (lockManager.isLocked(pkg) && !lockManager.isSessionUnlocked(pkg)) {
            val intent = Intent(this, LockScreenActivity::class.java).apply {
                putExtra(LockScreenActivity.EXTRA_PACKAGE, pkg)
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            }
            startActivity(intent)
        }
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
