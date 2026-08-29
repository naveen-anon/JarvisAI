package com.jarvis.assistant.security

import android.content.Intent
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

/**
 * Full-screen PIN prompt shown over a locked app. Launched by JarvisAccessibilityService
 * as soon as it sees a locked package come to the foreground. On success, the target
 * package is marked session-unlocked and this activity finishes, revealing the app
 * underneath. On cancel, the user is sent back to the home screen instead of the locked app.
 */
class LockScreenActivity : AppCompatActivity() {

    private lateinit var lockManager: AppLockManager
    private var targetPackage: String? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.addFlags(
            WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
        )

        lockManager = AppLockManager(this)
        targetPackage = intent.getStringExtra(EXTRA_PACKAGE)

        setContentView(buildUi())
    }

    // Built in code rather than XML so this single-purpose screen has zero layout/resource
    // dependencies — it has to work reliably even if something else in the app is broken.
    private fun buildUi(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER
            setBackgroundColor(0xFF03080E.toInt())
            setPadding(48, 48, 48, 48)
        }

        val title = TextView(this).apply {
            text = "🔒 Jarvis App Lock"
            setTextColor(0xFF00D4FF.toInt())
            textSize = 20f
            gravity = Gravity.CENTER
        }

        val subtitle = TextView(this).apply {
            text = "Enter PIN to continue"
            setTextColor(0xFF5C8A94.toInt())
            textSize = 14f
            gravity = Gravity.CENTER
            setPadding(0, 16, 0, 32)
        }

        val pinInput = EditText(this).apply {
            inputType = InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            setTextColor(0xFFE8F9FF.toInt())
            gravity = Gravity.CENTER
            textSize = 22f
        }

        val error = TextView(this).apply {
            setTextColor(0xFFFF3B30.toInt())
            textSize = 12f
            gravity = Gravity.CENTER
            setPadding(0, 12, 0, 0)
        }

        val unlockBtn = Button(this).apply {
            text = "UNLOCK"
            background = glassButtonDrawable(filled = true)
            setTextColor(0xFF03080E.toInt())
            setOnClickListener {
                val entered = pinInput.text.toString()
                if (lockManager.checkPin(entered)) {
                    targetPackage?.let { lockManager.markSessionUnlocked(it) }
                    finish()
                } else {
                    error.text = "Incorrect PIN"
                    pinInput.text.clear()
                }
            }
        }

        val cancelBtn = Button(this).apply {
            text = "GO HOME"
            background = glassButtonDrawable(filled = false)
            setTextColor(0xFF00D4FF.toInt())
            setOnClickListener { goHome() }
        }

        root.addView(title)
        root.addView(subtitle)
        root.addView(pinInput)
        root.addView(error)
        root.addView(unlockBtn)
        root.addView(cancelBtn)
        return root
    }

    /** Built in code, not as an XML resource — keeps this screen's "must always render even
     *  if something else broke" guarantee intact while still matching the app's glass look. */
    private fun glassButtonDrawable(filled: Boolean) = GradientDrawable().apply {
        cornerRadius = 24f
        if (filled) {
            colors = intArrayOf(0xFF00E5FF.toInt(), 0xFF00A8CC.toInt())
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
        } else {
            setColor(0x26122230)
            setStroke(3, 0x8000E5FF.toInt())
        }
    }

    private fun goHome() {
        val homeIntent = Intent(Intent.ACTION_MAIN).apply {
            addCategory(Intent.CATEGORY_HOME)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
        }
        startActivity(homeIntent)
        finish()
    }

    override fun onBackPressed() {
        // Block back-button dismissal — leaving this screen without the PIN should
        // send the user home, not straight into the locked app underneath.
        goHome()
    }

    companion object {
        const val EXTRA_PACKAGE = "target_package"
    }
}
