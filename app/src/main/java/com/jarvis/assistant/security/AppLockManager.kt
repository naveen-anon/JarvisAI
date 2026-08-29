package com.jarvis.assistant.security

import android.content.Context
import java.security.MessageDigest

/**
 * Phase 5 — "App lock by voice". Stores which packages are locked and a PIN
 * (hashed, never stored in plaintext) in SharedPreferences. The actual enforcement
 * happens in JarvisAccessibilityService, which watches for window-state-changed
 * events and launches LockScreenActivity over any locked app until the correct
 * PIN is entered.
 *
 * This is a lightweight, in-app lock — it does not survive a device factory reset
 * bypass or ADB-level tampering, and like any accessibility-based lock it can be
 * disabled by someone who can reach Settings > Accessibility. It's meant to stop
 * casual snooping, not a determined attacker.
 */
class AppLockManager(context: Context) {

    private val prefs = context.getSharedPreferences("jarvis_app_lock", Context.MODE_PRIVATE)

    // Packages unlocked in the current "session" (until screen off / app switch away),
    // so the user isn't asked for a PIN every single time they glance at the app.
    private val sessionUnlocked = mutableSetOf<String>()

    fun lockApp(packageName: String) {
        val locked = getLockedPackages().toMutableSet()
        locked.add(packageName)
        prefs.edit().putStringSet(KEY_LOCKED, locked).apply()
        sessionUnlocked.remove(packageName)
    }

    fun unlockApp(packageName: String) {
        val locked = getLockedPackages().toMutableSet()
        locked.remove(packageName)
        prefs.edit().putStringSet(KEY_LOCKED, locked).apply()
        sessionUnlocked.remove(packageName)
    }

    fun isLocked(packageName: String): Boolean = packageName in getLockedPackages()

    fun isSessionUnlocked(packageName: String): Boolean = packageName in sessionUnlocked

    fun markSessionUnlocked(packageName: String) {
        sessionUnlocked.add(packageName)
    }

    fun clearSessionUnlocks() {
        sessionUnlocked.clear()
    }

    fun getLockedPackages(): Set<String> =
        prefs.getStringSet(KEY_LOCKED, emptySet()) ?: emptySet()

    fun hasPin(): Boolean = prefs.getString(KEY_PIN_HASH, null) != null

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, hash(pin)).apply()
    }

    fun checkPin(pin: String): Boolean = prefs.getString(KEY_PIN_HASH, null) == hash(pin)

    private fun hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(input.toByteArray())
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_LOCKED = "locked_packages"
        private const val KEY_PIN_HASH = "pin_hash"
    }
}
