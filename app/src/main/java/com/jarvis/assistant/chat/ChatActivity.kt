package com.jarvis.assistant.chat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.os.IBinder
import android.view.Gravity
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.R
import com.jarvis.assistant.brain.BrainState
import com.jarvis.assistant.service.AssistantForegroundService

class ChatActivity : AppCompatActivity(), AssistantForegroundService.AssistantListener {

    private var service: AssistantForegroundService? = null
    private var bound = false

    private lateinit var chatMessages: LinearLayout
    private lateinit var chatScroll: ScrollView
    private lateinit var etInput: EditText

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as AssistantForegroundService.LocalBinder).getService()
            service?.listener = this@ChatActivity
            bound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
            service = null
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_chat)

        chatMessages = findViewById(R.id.chatMessages)
        chatScroll = findViewById(R.id.chatScroll)
        etInput = findViewById(R.id.etChatInput)

        findViewById<TextView>(R.id.btnCloseChat).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnSendChat).setOnClickListener { sendCurrent() }

        etInput.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_SEND) {
                sendCurrent()
                true
            } else false
        }

        addBubble("Jarvis online. Type anything — offline commands work here too.", isUser = false)

        val intent = Intent(this, AssistantForegroundService::class.java)
        startForegroundService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    private fun sendCurrent() {
        val text = etInput.text?.toString()?.trim().orEmpty()
        if (text.isEmpty()) return
        etInput.setText("")
        addBubble(text, isUser = true)
        if (bound) {
            service?.submitText(text)
        } else {
            addBubble("Service not ready — try again in a second.", isUser = false)
        }
    }

    private fun addBubble(text: String, isUser: Boolean) {
        val bubble = TextView(this).apply {
            this.text = text
            setTextColor(if (isUser) Color.parseColor("#F0FBFF") else Color.parseColor("#00E5FF"))
            textSize = 14f
            typeface = Typeface.MONOSPACE
            setPadding(28, 20, 28, 20)
            background = GradientDrawable().apply {
                setColor(if (isUser) Color.parseColor("#122230") else Color.parseColor("#0A1520"))
                cornerRadius = 18f
                setStroke(2, Color.parseColor(if (isUser) "#1A3A4A" else "#0B7A94"))
            }
        }
        val lp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 10
            bottomMargin = 4
            gravity = if (isUser) Gravity.END else Gravity.START
            marginStart = if (isUser) 64 else 0
            marginEnd = if (isUser) 0 else 64
        }
        chatMessages.addView(bubble, lp)
        chatScroll.post { chatScroll.fullScroll(ScrollView.FOCUS_DOWN) }
    }

    override fun onStateChanged(state: BrainState) {}

    override fun onTranscript(text: String) {}

    override fun onResponse(text: String, fromCloud: Boolean) {
        val prefix = if (fromCloud) "" else "[OFFLINE] "
        addBubble(prefix + text, isUser = false)
    }

    override fun onDestroy() {
        if (bound) {
            if (service?.listener === this) service?.listener = null
            unbindService(connection)
            bound = false
        }
        super.onDestroy()
    }
}
