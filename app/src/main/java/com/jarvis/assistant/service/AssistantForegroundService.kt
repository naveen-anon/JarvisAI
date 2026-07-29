package com.jarvis.assistant.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.jarvis.assistant.executor.CommandExecutor
import com.jarvis.assistant.model.AssistantCommand
import com.jarvis.assistant.util.SmartSuggestionEngine

class AssistantForegroundService : Service() {

    override fun onCreate() {
        super.onCreate()
        startForegroundServiceNotification()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        val rawQuery = intent?.getStringExtra("query")

        if (!rawQuery.isNullOrEmpty()) {
            val command = AssistantCommand(action = action, rawCommand = rawQuery)
            handleCommand(command)
        }

        return START_STICKY
    }

    private fun handleCommand(command: AssistantCommand) {
        val response = CommandExecutor.execute(this, command)
        SmartSuggestionEngine.recordInteraction(command.rawCommand ?: "", response)
    }

    private fun startForegroundServiceNotification() {
        val channelId = "jarvis_service_channel"
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                channelId,
                "Jarvis Background Service",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        val notification: Notification = NotificationCompat.Builder(this, channelId)
            .setContentTitle("Jarvis AI")
            .setContentText("Jarvis is running in background...")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .build()

        startForeground(101, notification)
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
