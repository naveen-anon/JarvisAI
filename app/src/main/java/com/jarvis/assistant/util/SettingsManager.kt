package com.jarvis.assistant.util

import android.content.Context
import androidx.appcompat.app.AppCompatDelegate

/**
 * Manages user preferences — voice speed, colors, theme, etc.
 * Backed by SharedPreferences so settings persist across restarts.
 */
class SettingsManager(context: Context) {

    private val prefs = context.getSharedPreferences("jarvis_settings", Context.MODE_PRIVATE)

    // Voice settings
    fun getVoiceSpeed(): Float = prefs.getFloat("voice_speed", 1.0f)
    fun setVoiceSpeed(speed: Float) = prefs.edit().putFloat("voice_speed", speed).apply()

    fun getVoicePitch(): Float = prefs.getFloat("voice_pitch", 1.0f)
    fun setVoicePitch(pitch: Float) = prefs.edit().putFloat("voice_pitch", pitch).apply()

    fun getVoiceType(): String = prefs.getString("voice_type", "male") ?: "male"
    fun setVoiceType(type: String) = prefs.edit().putString("voice_type", type).apply()

    // UI settings
    fun getArcReactorColor(): String = prefs.getString("reactor_color", "#00D4FF") ?: "#00D4FF"
    fun setArcReactorColor(color: String) = prefs.edit().putString("reactor_color", color).apply()

    fun getGlowIntensity(): Float = prefs.getFloat("glow_intensity", 1.0f)
    fun setGlowIntensity(intensity: Float) = prefs.edit().putFloat("glow_intensity", intensity).apply()

    fun getTheme(): String = prefs.getString("theme", "dark") ?: "dark"
    fun setTheme(theme: String) = prefs.edit().putString("theme", theme).apply()

    // Behavior settings
    fun getSmartSuggestionsEnabled(): Boolean = prefs.getBoolean("smart_suggestions", true)
    fun setSmartSuggestionsEnabled(enabled: Boolean) = prefs.edit().putBoolean("smart_suggestions", enabled).apply()

    fun getMemoryRetentionDays(): Int = prefs.getInt("memory_days", 30)
    fun setMemoryRetentionDays(days: Int) = prefs.edit().putInt("memory_days", days).apply()

    fun getLastLocationUpdate(): Long = prefs.getLong("last_location_update", 0)
    fun setLastLocationUpdate(time: Long) = prefs.edit().putLong("last_location_update", time).apply()

    fun getLastWeatherUpdate(): Long = prefs.getLong("last_weather_update", 0)
    fun setLastWeatherUpdate(time: Long) = prefs.edit().putLong("last_weather_update", time).apply()

    fun getUserName(): String = prefs.getString("user_name", "User") ?: "User"
    fun setUserName(name: String) = prefs.edit().putString("user_name", name).apply()

    fun getUseShortAnswers(): Boolean = prefs.getBoolean("short_answers", false)
    fun setUseShortAnswers(use: Boolean) = prefs.edit().putBoolean("short_answers", use).apply()

    fun clear() = prefs.edit().clear().apply()
}
