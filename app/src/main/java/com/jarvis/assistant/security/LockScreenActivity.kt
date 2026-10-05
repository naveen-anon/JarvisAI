package com.jarvis.assistant.security

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.text.InputType
import android.util.TypedValue
import android.view.Gravity
import android.view.WindowManager
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

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

    private fun dp(v: Int): Int =
        TypedValue.applyDimension(
            TypedValue.COMPLEX_UNIT_DIP, v.toFloat(), resources.displayMetrics
        ).toInt()

    private fun buildUi(): LinearLayout {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            gravity = Gravity.CENTER_HORIZONTAL
            setBackgroundColor(0xFF03080E.toInt())
            setPadding(dp(28), dp(48), dp(28), dp(28))
        }

        val brand = TextView(this).apply {
            text = "J.A.R.V.I.S"
            setTextColor(0xFF00D9FF.toInt())
            textSize = 14f
            setTypeface(Typeface.MONOSPACE)
            gravity = Gravity.CENTER
            letterSpacing = 0.12f
        }

        val title = TextView(this).apply {
            text = "APP LOCK"
            setTextColor(0xFFF0FBFF.toInt())
            textSize = 22f
            gravity = Gravity.CENTER
            setPadding(0, dp(8), 0, 0)
            setTypeface(Typeface.MONOSPACE, Typeface.BOLD)
        }

        val lockType = try { lockManager.getLockType() } catch (_: Exception) {
            AppLockManager.LockType.PIN
        }
        val hint = when (lockType) {
            AppLockManager.LockType.PASSWORD -> "Enter password to continue"
            AppLockManager.LockType.PATTERN -> "Enter pattern path (e.g. 0-1-2-5-8)"
            else -> "Enter PIN to continue"
        }
        val subtitle = TextView(this).apply {
            text = hint
            setTextColor(0xFF5A8A99.toInt())
            textSize = 13f
            setTypeface(Typeface.MONOSPACE)
            gravity = Gravity.CENTER
            setPadding(0, dp(10), 0, dp(28))
        }

        val pinInput = EditText(this).apply {
            inputType = when (lockType) {
                AppLockManager.LockType.PASSWORD ->
                    InputType.TYPE_CLASS_TEXT or InputType.TYPE_TEXT_VARIATION_PASSWORD
                AppLockManager.LockType.PATTERN ->
                    InputType.TYPE_CLASS_TEXT
                else ->
                    InputType.TYPE_CLASS_NUMBER or InputType.TYPE_NUMBER_VARIATION_PASSWORD
            }
            setTextColor(0xFFE8FBFF.toInt())
            setHintTextColor(0xFF5A8A99.toInt())
            setHint(
                when (lockType) {
                    AppLockManager.LockType.PASSWORD -> "Password"
                    AppLockManager.LockType.PATTERN -> "0-1-2-5-8"
                    else -> "••••"
                }
            )
            gravity = Gravity.CENTER
            textSize = 20f
            setTypeface(Typeface.MONOSPACE)
            background = GradientDrawable().apply {
                cornerRadius = dp(16).toFloat()
                setColor(0xCC0A1825.toInt())
                setStroke(dp(1), 0x8800D9FF.toInt())
            }
            setPadding(dp(16), dp(14), dp(16), dp(14))
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            )
        }

        val error = TextView(this).apply {
            setTextColor(0xFFFF6B6B.toInt())
            textSize = 12f
            setTypeface(Typeface.MONOSPACE)
            gravity = Gravity.CENTER
            setPadding(0, dp(12), 0, dp(8))
        }

        val unlockBtn = Button(this).apply {
            text = "UNLOCK"
            background = glassButtonDrawable(filled = true)
            setTextColor(0xFF03080E.toInt())
            setTypeface(Typeface.MONOSPACE)
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply { topMargin = dp(12) }
            setOnClickListener {
                val entered = pinInput.text.toString()
                if (lockManager.checkCredential(entered)) {
                    targetPackage?.let { lockManager.markSessionUnlocked(it) }
                    pinInput.postDelayed({ finish() }, 150)
                } else {
                    error.text = when (lockType) {
                        AppLockManager.LockType.PASSWORD -> "Incorrect password"
                        AppLockManager.LockType.PATTERN -> "Incorrect pattern"
                        else -> "Incorrect PIN"
                    }
                    pinInput.setText("")
                }
            }
        }

        val cancelBtn = Button(this).apply {
            text = "GO HOME"
            background = glassButtonDrawable(filled = false)
            setTextColor(0xFF00D9FF.toInt())
            setTypeface(Typeface.MONOSPACE)
            isAllCaps = false
            layoutParams = LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                dp(48)
            ).apply { topMargin = dp(10) }
            setOnClickListener { goHome() }
        }

        root.addView(brand)
        root.addView(title)
        root.addView(subtitle)
        root.addView(pinInput)
        root.addView(error)
        root.addView(unlockBtn)
        root.addView(cancelBtn)
        return root
    }

    private fun glassButtonDrawable(filled: Boolean) = GradientDrawable().apply {
        cornerRadius = dp(24).toFloat()
        if (filled) {
            colors = intArrayOf(0xFF00D9FF.toInt(), 0xFF0099B8.toInt())
            orientation = GradientDrawable.Orientation.TOP_BOTTOM
        } else {
            setColor(0xCC0A1825.toInt())
            setStroke(dp(1), 0x8800D9FF.toInt())
        }
    }

    private fun goHome() {
        startActivity(
            Intent(Intent.ACTION_MAIN).apply {
                addCategory(Intent.CATEGORY_HOME)
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
        )
        finish()
    }

    @Deprecated("Deprecated in Java")
    override fun onBackPressed() {
        goHome()
    }

    companion object {
        const val EXTRA_PACKAGE = "target_package"
    }
}
