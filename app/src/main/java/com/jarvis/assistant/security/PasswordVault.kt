package com.jarvis.assistant.security

import android.content.Context
import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * Local password vault for personal Jarvis use.
 * Master PIN unlocks session; entries encrypted with key derived from PIN.
 * Not a substitute for Bitwarden on shared devices — disable if phone is not yours alone.
 */
class PasswordVault(context: Context) {

    data class Entry(
        val id: String,
        val title: String,
        val username: String,
        val password: String,
        val notes: String = ""
    )

    private val prefs = context.getSharedPreferences("jarvis_pwm", Context.MODE_PRIVATE)
    private var sessionKey: ByteArray? = null

    fun hasMasterPin(): Boolean = prefs.contains(KEY_PIN_HASH)

    fun setMasterPin(pin: String): Boolean {
        if (pin.length < 4) return false
        val salt = ByteArray(16).also { SecureRandom().nextBytes(it) }
        prefs.edit()
            .putString(KEY_PIN_HASH, hash(pin, salt))
            .putString(KEY_SALT, Base64.encodeToString(salt, Base64.NO_WRAP))
            .apply()
        sessionKey = deriveKey(pin, salt)
        return true
    }

    fun unlock(pin: String): Boolean {
        val saltB64 = prefs.getString(KEY_SALT, null) ?: return false
        val salt = Base64.decode(saltB64, Base64.NO_WRAP)
        val expected = prefs.getString(KEY_PIN_HASH, null) ?: return false
        if (hash(pin, salt) != expected) return false
        sessionKey = deriveKey(pin, salt)
        return true
    }

    fun lock() {
        sessionKey = null
    }

    fun isUnlocked(): Boolean = sessionKey != null

    fun list(): List<Entry> {
        val key = sessionKey ?: return emptyList()
        val raw = prefs.getString(KEY_BLOB, null) ?: return emptyList()
        return try {
            val json = String(decrypt(Base64.decode(raw, Base64.NO_WRAP), key))
            val arr = JSONArray(json)
            (0 until arr.length()).map { i ->
                val o = arr.getJSONObject(i)
                Entry(
                    o.getString("id"),
                    o.optString("title"),
                    o.optString("username"),
                    o.optString("password"),
                    o.optString("notes")
                )
            }
        } catch (_: Exception) {
            emptyList()
        }
    }

    fun save(entries: List<Entry>): Boolean {
        val key = sessionKey ?: return false
        val arr = JSONArray()
        entries.forEach { e ->
            arr.put(
                JSONObject()
                    .put("id", e.id)
                    .put("title", e.title)
                    .put("username", e.username)
                    .put("password", e.password)
                    .put("notes", e.notes)
            )
        }
        val enc = encrypt(arr.toString().toByteArray(Charsets.UTF_8), key)
        prefs.edit().putString(KEY_BLOB, Base64.encodeToString(enc, Base64.NO_WRAP)).apply()
        return true
    }

    fun add(title: String, username: String, password: String, notes: String = ""): Boolean {
        val list = list().toMutableList()
        list.add(
            Entry(
                id = System.currentTimeMillis().toString(),
                title = title.trim(),
                username = username.trim(),
                password = password,
                notes = notes.trim()
            )
        )
        return save(list)
    }

    fun delete(id: String): Boolean {
        val list = list().filter { it.id != id }
        return save(list)
    }

    private fun hash(pin: String, salt: ByteArray): String {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(pin.toByteArray(Charsets.UTF_8))
        return md.digest().joinToString("") { "%02x".format(it) }
    }

    private fun deriveKey(pin: String, salt: ByteArray): ByteArray {
        val md = MessageDigest.getInstance("SHA-256")
        md.update(salt)
        md.update(pin.toByteArray(Charsets.UTF_8))
        md.update("jarvis-pwm-v1".toByteArray())
        return md.digest()
    }

    private fun encrypt(data: ByteArray, key: ByteArray): ByteArray {
        val iv = ByteArray(12).also { SecureRandom().nextBytes(it) }
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        val ct = cipher.doFinal(data)
        return iv + ct
    }

    private fun decrypt(data: ByteArray, key: ByteArray): ByteArray {
        val iv = data.copyOfRange(0, 12)
        val ct = data.copyOfRange(12, data.size)
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(key, "AES"), GCMParameterSpec(128, iv))
        return cipher.doFinal(ct)
    }

    companion object {
        private const val KEY_PIN_HASH = "pin_hash"
        private const val KEY_SALT = "salt"
        private const val KEY_BLOB = "vault_blob"
    }
}
