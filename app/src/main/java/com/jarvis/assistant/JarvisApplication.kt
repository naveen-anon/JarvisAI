package com.jarvis.assistant

import android.app.Application
import com.jarvis.assistant.diagnostics.CrashHandler

class JarvisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        CrashHandler.install(this)
    }
}
