package com.jarvis.assistant

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.os.Build
import android.os.Bundle
import android.os.IBinder
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import com.jarvis.assistant.service.AssistantForegroundService
import com.jarvis.assistant.ui.ArcReactorView

class MainActivity : AppCompatActivity() {

    private var service: AssistantForegroundService? = null
    private var bound = false

    private val connection = object : ServiceConnection {
        override fun onServiceConnected(name: ComponentName?, binder: IBinder?) {
            service = (binder as AssistantForegroundService.LocalBinder).getService()
            bound = true
        }
        override fun onServiceDisconnected(name: ComponentName?) {
            bound = false
        }
    }

    private val requiredPermissions = mutableListOf(
        android.Manifest.permission.RECORD_AUDIO,
        android.Manifest.permission.CALL_PHONE,
        android.Manifest.permission.SEND_SMS,
        android.Manifest.permission.READ_CONTACTS
    ).apply {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            add(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }.toTypedArray()

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        if (results.values.all { it }) {
            startAssistantService()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        val arcReactor = findViewById<ArcReactorView>(R.id.arcReactor)
        arcReactor.setOnClickListener {
            service?.startListeningCycle()
        }

        permissionLauncher.launch(requiredPermissions)
    }

    private fun startAssistantService() {
        val intent = Intent(this, AssistantForegroundService::class.java)
        startForegroundService(intent)
        bindService(intent, connection, Context.BIND_AUTO_CREATE)
    }

    override fun onDestroy() {
        if (bound) unbindService(connection)
        super.onDestroy()
    }
}
