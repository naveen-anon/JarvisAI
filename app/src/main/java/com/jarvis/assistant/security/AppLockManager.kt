package com.jarvis.assistant.security

import android.content.Context
import java.security.MessageDigest

/**
 * App lock by voice. PIN hashed in prefs.
 * Session unlock is also in prefs so AccessibilityService + LockScreenActivity share state.
 */
class AppLockManager(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("jarvis_app_lock", Context.MODE_PRIVATE)

    fun lockApp(packageName: String) {
        val locked = getLockedPackages().toMutableSet()
        locked.add(packageName)
        prefs.edit().putStringSet(KEY_LOCKED, locked).apply()
        clearSessionUnlock(packageName)
    }

    fun unlockApp(packageName: String) {
        val locked = getLockedPackages().toMutableSet()
        locked.remove(packageName)
        prefs.edit().putStringSet(KEY_LOCKED, locked).apply()
        clearSessionUnlock(packageName)
    }

    fun isLocked(packageName: String): Boolean = packageName in getLockedPackages()

    fun isSessionUnlocked(packageName: String): Boolean {
        val until = prefs.getLong(sessionKey(packageName), 0L)
        return until > System.currentTimeMillis()
    }

    fun markSessionUnlocked(packageName: String) {
        val until = System.currentTimeMillis() + 30L * 60L * 1000L
        prefs.edit().putLong(sessionKey(packageName), until).apply()
    }

    fun clearSessionUnlock(packageName: String) {
        prefs.edit().remove(sessionKey(packageName)).apply()
    }

    fun clearSessionUnlocks() {
        val ed = prefs.edit()
        prefs.all.keys.filter { it.startsWith(KEY_SESSION_PREFIX) }.forEach { ed.remove(it) }
        ed.apply()
    }

    fun getLockedPackages(): Set<String> =
        prefs.getStringSet(KEY_LOCKED, emptySet())?.toSet() ?: emptySet()

    fun hasPin(): Boolean = prefs.getString(KEY_PIN_HASH, null) != null

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN_HASH, hash(pin)).apply()
    }

    fun checkPin(pin: String): Boolean {
        val stored = prefs.getString(KEY_PIN_HASH, null) ?: return false
        return stored == hash(pin)
    }

    private fun sessionKey(packageName: String) = KEY_SESSION_PREFIX + packageName

    private fun hash(input: String): String {
        val digest = MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_LOCKED = "locked_packages"
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_SESSION_PREFIX = "session_until_"
    }
}
