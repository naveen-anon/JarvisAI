package com.jarvis.assistant.util

import android.app.ActivityManager
import android.content.Context
import java.io.RandomAccessFile

class PerformanceMonitor(private val context: Context) {

    private var lastCpuIdle = 0L
    private var lastCpuTotal = 0L

    fun getRamUsagePercent(): Int {
        val am = context.getSystemService(Context.ACTIVITY_SERVICE) as ActivityManager
        val memInfo = ActivityManager.MemoryInfo()
        am.getMemoryInfo(memInfo)
        val used = memInfo.totalMem - memInfo.availMem
        return ((used.toDouble() / memInfo.totalMem) * 100).toInt()
    }

    fun getCpuUsagePercent(): Int {
        return try {
            val reader = RandomAccessFile("/proc/stat", "r")
            val load = reader.readLine()
            reader.close()

            val toks = load.split(" ").filter { it.isNotBlank() }
            val user = toks[1].toLong()
            val nice = toks[2].toLong()
            val system = toks[3].toLong()
            val idle = toks[4].toLong()
            val iowait = toks[5].toLong()
            val irq = toks[6].toLong()
            val softirq = toks[7].toLong()

            val total = user + nice + system + idle + iowait + irq + softirq
            val diffIdle = idle - lastCpuIdle
            val diffTotal = total - lastCpuTotal

            lastCpuIdle = idle
            lastCpuTotal = total

            if (diffTotal <= 0) return -1
            (((diffTotal - diffIdle).toDouble() / diffTotal) * 100).toInt()
        } catch (e: Exception) {
            -1
        }
    }
}
