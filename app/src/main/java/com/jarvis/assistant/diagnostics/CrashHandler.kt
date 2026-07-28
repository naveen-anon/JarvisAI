package com.jarvis.assistant.diagnostics

import android.content.Context
import java.io.File
import java.io.PrintWriter
import java.io.StringWriter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class CrashHandler(private val context: Context) : Thread.UncaughtExceptionHandler {

    private val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()

    override fun uncaughtException(thread: Thread, throwable: Throwable) {
        try {
            val sw = StringWriter()
            throwable.printStackTrace(PrintWriter(sw))
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US).format(Date())
            val report = "Crash at $timestamp\n\n$sw"
            crashFile(context).writeText(report)
        } catch (e: Exception) {
        }
        defaultHandler?.uncaughtException(thread, throwable)
    }

    companion object {
        private const val FILE_NAME = "last_crash.txt"

        fun install(context: Context) {
            Thread.setDefaultUncaughtExceptionHandler(CrashHandler(context.applicationContext))
        }

        fun crashFile(context: Context): File = File(context.filesDir, FILE_NAME)

        fun consumeLastCrash(context: Context): String? {
            val file = crashFile(context)
            if (!file.exists()) return null
            return try {
                val text = file.readText()
                file.delete()
                text
            } catch (e: Exception) {
                null
            }
        }
    }
}
