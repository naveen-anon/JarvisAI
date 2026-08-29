package com.jarvis.assistant

import android.app.Application
import com.jarvis.assistant.diagnostics.CrashHandler

class JarvisApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        // Installed first thing, before anything else can crash, so we catch
        // even a crash that happens during MainActivity's own onCreate().
        CrashHandler.install(this)
    }
}
