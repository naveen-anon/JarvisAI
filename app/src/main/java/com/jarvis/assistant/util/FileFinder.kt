package com.jarvis.assistant.util

import android.os.Build
import android.os.Environment
import java.io.File

/**
 * Lets Jarvis find and read a file by name from shared device storage when the user
 * mentions it by voice/text (e.g. "upi_osint.py padho"). Requires "All files access"
 * (MANAGE_EXTERNAL_STORAGE) granted manually in system Settings — Jarvis can't request
 * this via a normal runtime permission popup, Android requires the special settings screen.
 */
object FileFinder {

    private val extensionRegex = Regex(
        """\b[\w\-]+\.(py|kt|kts|java|js|ts|json|xml|html|css|md|txt|c|cpp|h|sh|yml|yaml|gradle|properties|csv|sql)\b""",
        RegexOption.IGNORE_CASE
    )

    /** Pulls a filename-with-extension out of free-form speech/text, or null if none mentioned. */
    fun extractFileName(speech: String): String? = extensionRegex.find(speech)?.value

    fun hasStorageAccess(): Boolean =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) Environment.isExternalStorageManager() else true

    /**
     * Breadth-first search of shared storage for a file matching [fileName] (case-insensitive).
     * Skips the /Android folder (restricted, would just throw) and bails out after
     * [timeBudgetMs] so a huge SD card can't hang the assistant indefinitely.
     */
    fun findFile(fileName: String, timeBudgetMs: Long = 8000): File? {
        val root = Environment.getExternalStorageDirectory()
        val start = System.currentTimeMillis()
        val target = fileName.lowercase()
        val stack = ArrayDeque<File>()
        stack.addLast(root)

        while (stack.isNotEmpty()) {
            if (System.currentTimeMillis() - start > timeBudgetMs) break
            val dir = stack.removeLast()
            if (dir.name.equals("Android", ignoreCase = true)) continue
            val children = dir.listFiles() ?: continue
            for (child in children) {
                if (child.isDirectory) {
                    stack.addLast(child)
                } else if (child.name.lowercase() == target) {
                    return child
                }
            }
        }
        return null
    }

    fun readTextCapped(file: File, maxChars: Int = 8000): String? = try {
        val text = file.readText(Charsets.UTF_8)
        if (text.length > maxChars) text.take(maxChars) + "\n… (truncated)" else text
    } catch (e: Exception) {
        null
    }
}
