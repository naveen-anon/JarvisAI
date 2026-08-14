package com.jarvis.assistant.chat

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Color
import android.graphics.Typeface
import android.net.Uri
import android.os.Bundle
import android.os.IBinder
import android.provider.OpenableColumns
import android.util.Base64
import android.view.Gravity
import android.view.View
import android.view.inputmethod.EditorInfo
import android.widget.EditText
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.jarvis.assistant.BuildConfig
import com.jarvis.assistant.R
import com.jarvis.assistant.ai.JarvisLlmClient
import com.jarvis.assistant.brain.BrainState
import com.jarvis.assistant.service.AssistantForegroundService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.ByteArrayOutputStream

class ChatActivity : AppCompatActivity(), AssistantForegroundService.AssistantListener {

    private var service: AssistantForegroundService? = null
    private var bound = false
    private val scope = CoroutineScope(Dispatchers.Main)

    private lateinit var chatMessages: LinearLayout
    private lateinit var chatScroll: ScrollView
    private lateinit var etInput: EditText
    private lateinit var attachmentPreview: LinearLayout
    private lateinit var attachmentName: TextView

    private var attachedUri: Uri? = null
    private var attachedMime: String? = null

    // Anything not clearly text/code is treated as unsupported for now.
    private val textLikeExtensions = setOf(
        "txt", "py", "kt", "java", "js", "ts", "json", "xml", "md", "csv",
        "html", "css", "yml", "yaml", "gradle", "properties", "sh", "log", "c", "cpp", "h"
    )

    private val pickFileLauncher = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        if (uri != null) onFilePicked(uri)
    }

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
        attachmentPreview = findViewById(R.id.attachmentPreview)
        attachmentName = findViewById(R.id.attachmentName)

        findViewById<TextView>(R.id.btnCloseChat).setOnClickListener { finish() }
        findViewById<TextView>(R.id.btnSendChat).setOnClickListener { sendCurrent() }
        findViewById<ImageView>(R.id.btnAttach).setOnClickListener { pickFileLauncher.launch("*/*") }
        findViewById<TextView>(R.id.btnRemoveAttachment).setOnClickListener { clearAttachment() }

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

    private fun onFilePicked(uri: Uri) {
        attachedUri = uri
        attachedMime = contentResolver.getType(uri)
        attachmentName.text = fileNameOf(uri)
        attachmentPreview.visibility = View.VISIBLE
    }

    private fun clearAttachment() {
        attachedUri = null
        attachedMime = null
        attachmentPreview.visibility = View.GONE
    }

    private fun fileNameOf(uri: Uri): String {
        var name = "attachment"
        contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx != -1 && cursor.moveToFirst()) {
                name = cursor.getString(idx) ?: name
            }
        }
        return name
    }

    private fun isImage(mime: String?, name: String) =
        mime?.startsWith("image/") == true || name.substringAfterLast('.', "").lowercase() in
            setOf("jpg", "jpeg", "png", "webp")

    private fun isTextLike(mime: String?, name: String): Boolean {
        if (mime != null && (mime.startsWith("text/") || mime == "application/json")) return true
        return name.substringAfterLast('.', "").lowercase() in textLikeExtensions
    }

    private fun sendCurrent() {
        val text = etInput.text?.toString()?.trim().orEmpty()
        val uri = attachedUri

        if (uri == null) {
            if (text.isEmpty()) return
            etInput.setText("")
            addBubble(text, isUser = true)
            if (bound) service?.submitText(text) else addBubble("Service not ready — try again in a second.", isUser = false)
            return
        }

        val name = fileNameOf(uri)
        val mime = attachedMime

        when {
            isImage(mime, name) -> {
                etInput.setText("")
                addBubble((if (text.isBlank()) "[Image] $name" else "[Image] $name\n$text"), isUser = true)
                clearAttachment()
                sendImage(uri, mime ?: "image/jpeg", text)
            }
            isTextLike(mime, name) -> {
                val content = readTextFile(uri)
                if (content == null) {
                    Toast.makeText(this, "Couldn't read that file.", Toast.LENGTH_SHORT).show()
                    return
                }
                etInput.setText("")
                addBubble((if (text.isBlank()) "[File] $name" else "[File] $name\n$text"), isUser = true)
                clearAttachment()
                val combined = buildString {
                    append("Attached file \"").append(name).append("\":\n\n")
                    append("```\n").append(content).append("\n```\n\n")
                    append(if (text.isNotBlank()) text else "Please review this file.")
                }
                sendFileText(combined)
            }
            else -> {
                Toast.makeText(this, "Only images and text/code files are supported right now.", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun sendImage(uri: Uri, mime: String, prompt: String) {
        addBubble("Analyzing image…", isUser = false)
        scope.launch {
            val base64 = withContext(Dispatchers.IO) { readImageAsBase64(uri) }
            if (base64 == null) {
                addBubble("Couldn't read that image.", isUser = false)
                return@launch
            }
            val result = withContext(Dispatchers.IO) {
                JarvisLlmClient(apiKeyProvider = { BuildConfig.GROQ_API_KEY }).chatWithImage(prompt, base64, mime)
            }
            if (result.ok) {
                addBubble(result.text, isUser = false)
            } else {
                addBubble("Image analysis failed: ${result.error}", isUser = false)
            }
        }
    }

    private fun sendFileText(combined: String) {
        addBubble("Reading file\u2026", isUser = false)
        scope.launch {
            val result = withContext(Dispatchers.IO) {
                JarvisLlmClient(apiKeyProvider = { BuildConfig.GROQ_API_KEY }).chat(combined)
            }
            if (result.ok) {
                addBubble(result.text, isUser = false)
            } else {
                addBubble("Couldn't process the file: ${result.error}", isUser = false)
            }
        }
    }

    private fun readTextFile(uri: Uri, maxChars: Int = 8000): String? = try {
        val raw = contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }
        raw?.let { if (it.length > maxChars) it.take(maxChars) + "\n… (truncated)" else it }
    } catch (e: Exception) {
        null
    }

    private fun readImageAsBase64(uri: Uri, maxDimension: Int = 1280): String? {
        return try {
            val input = contentResolver.openInputStream(uri) ?: return null
            val original = BitmapFactory.decodeStream(input)
            input.close()
            if (original == null) return null

            val scale = maxDimension.toFloat() / maxOf(original.width, original.height)
            val bitmap = if (scale < 1f) {
                Bitmap.createScaledBitmap(original, (original.width * scale).toInt(), (original.height * scale).toInt(), true)
            } else {
                original
            }

            val out = ByteArrayOutputStream()
            bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
            Base64.encodeToString(out.toByteArray(), Base64.NO_WRAP)
        } catch (e: Exception) {
            null
        }
    }

    private fun avatar(isUser: Boolean) = TextView(this).apply {
        text = if (isUser) "" else ""
        val size = 90
        layoutParams = LinearLayout.LayoutParams(size, size).apply {
            if (isUser) marginStart = 10 else marginEnd = 10
        }
        background = ContextCompat.getDrawable(
            this@ChatActivity,
            if (isUser) R.drawable.avatar_user_bg else R.drawable.avatar_reactor_bg
        )
        gravity = Gravity.CENTER
        textSize = 16f
    }

    private fun bubbleText(text: String, isUser: Boolean) = TextView(this).apply {
        this.text = text
        setTextColor(if (isUser) Color.parseColor("#F0FBFF") else Color.parseColor("#00E5FF"))
        textSize = 14f
        typeface = Typeface.MONOSPACE
        background = ContextCompat.getDrawable(
            this@ChatActivity,
            if (isUser) R.drawable.bubble_user_bg else R.drawable.bubble_jarvis_bg
        )
    }

    private fun addBubble(text: String, isUser: Boolean) {
        val row = LinearLayout(this).apply {
            orientation = LinearLayout.HORIZONTAL
            gravity = if (isUser) Gravity.END else Gravity.START
        }

        val bubble = bubbleText(text, isUser)
        val bubbleLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.WRAP_CONTENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            marginStart = if (isUser) 48 else 0
            marginEnd = if (isUser) 0 else 48
        }

        if (isUser) {
            row.addView(bubble, bubbleLp)
            row.addView(avatar(true))
        } else {
            row.addView(avatar(false))
            row.addView(bubble, bubbleLp)
        }

        val rowLp = LinearLayout.LayoutParams(
            LinearLayout.LayoutParams.MATCH_PARENT,
            LinearLayout.LayoutParams.WRAP_CONTENT
        ).apply {
            topMargin = 10
            bottomMargin = 4
        }
        chatMessages.addView(row, rowLp)
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
