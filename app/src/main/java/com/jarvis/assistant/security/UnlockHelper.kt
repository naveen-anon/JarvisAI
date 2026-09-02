package com.jarvis.assistant.security

import android.app.KeyguardManager
import android.content.Context
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

/**
 * Voice-triggered unlock helper. Deliberately does NOT bypass the actual
 * lock screen credential -- Android blocks that for every app, by design.
 * Instead:
 *  1. If there's no secure lock set (swipe-only) -> dismiss it directly.
 *  2. If a secure lock IS set -> surface the system's own biometric
 *     prompt so the real owner completes authentication themselves.
 */
class UnlockHelper(private val activity: FragmentActivity) {

    private val keyguardManager =
        activity.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager

    fun requestUnlock(onResult: (String) -> Unit) {
        if (!keyguardManager.isKeyguardSecure) {
            activity.setShowWhenLocked(true)
            activity.setTurnScreenOn(true)
            keyguardManager.requestDismissKeyguard(activity, object : KeyguardManager.KeyguardDismissCallback() {
                override fun onDismissSucceeded() {
                    onResult("Phone unlock ho gaya hai, sir.")
                }
                override fun onDismissError() {
                    onResult("Automatically unlock nahi ho paya -- please manually unlock karein.")
                }
            })
            return
        }

        val biometricManager = BiometricManager.from(activity)
        val canAuth = biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)
        if (canAuth != BiometricManager.BIOMETRIC_SUCCESS) {
            onResult("Biometric setup nahi hai -- please manually unlock karein.")
            return
        }

        val executor = ContextCompat.getMainExecutor(activity)
        val prompt = BiometricPrompt(activity, executor, object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                onResult("Phone unlock ho gaya hai, sir.")
            }
            override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                onResult("Unlock cancel ho gaya.")
            }
            override fun onAuthenticationFailed() {
                // system prompt handles retry UI itself
            }
        })

        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle("Jarvis")
            .setSubtitle("Confirm it's you to unlock")
            .setNegativeButtonText("Cancel")
            .build()

        onResult("Please authenticate to unlock.")
        prompt.authenticate(promptInfo)
    }

    companion object {
        const val EXTRA_TRIGGER_UNLOCK = "trigger_unlock"
    }
}
