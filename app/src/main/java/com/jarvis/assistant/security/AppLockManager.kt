package com.jarvis.assistant.security

import android.content.Context
import java.security.MessageDigest

class AppLockManager(context: Context) {

    private val prefs =
        context.applicationContext.getSharedPreferences("jarvis_app_lock", Context.MODE_PRIVATE)

    enum class LockType { PIN, PASSWORD, PATTERN }

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

    fun getLockedPackages(): Set<String> =
        prefs.getStringSet(KEY_LOCKED, emptySet())?.toSet() ?: emptySet()

    fun getLockType(): LockType {
        val raw = prefs.getString(KEY_LOCK_TYPE, "PIN") ?: "PIN"
        return runCatching { LockType.valueOf(raw) }.getOrDefault(LockType.PIN)
    }

    fun hasCredential(): Boolean = prefs.getString(KEY_CRED_HASH, null) != null
    fun hasPin(): Boolean = hasCredential()

    fun setCredential(type: LockType, secret: String) {
        prefs.edit()
            .putString(KEY_LOCK_TYPE, type.name)
            .putString(KEY_CRED_HASH, hash(secret.trim()))
            .apply()
    }

    fun setPin(pin: String) = setCredential(LockType.PIN, pin)

    fun checkCredential(secret: String): Boolean {
        val stored = prefs.getString(KEY_CRED_HASH, null) ?: return false
        return stored == hash(secret.trim())
    }

    fun checkPin(pin: String): Boolean = checkCredential(pin)

    fun clearCredential() {
        prefs.edit().remove(KEY_CRED_HASH).remove(KEY_LOCK_TYPE).apply()
    }

    private fun sessionKey(packageName: String) = KEY_SESSION_PREFIX + packageName

    private fun hash(input: String): String {
        val d = MessageDigest.getInstance("SHA-256").digest(input.toByteArray(Charsets.UTF_8))
        return d.joinToString("") { "%02x".format(it) }
    }

    companion object {
        private const val KEY_LOCKED = "locked_packages"
        private const val KEY_CRED_HASH = "pin_hash"
        private const val KEY_LOCK_TYPE = "lock_type"
        private const val KEY_SESSION_PREFIX = "session_until_"
    }
}
