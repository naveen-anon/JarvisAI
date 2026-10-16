package com.jarvis.assistant.util

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.ContactsContract
import android.provider.MediaStore
import android.provider.Telephony
import androidx.core.content.ContextCompat

/**
 * Siri-like personal context: SMS inbox search, photo library search, Gmail search intent.
 */
class PersonalContextHelper(private val context: Context) {

    fun searchSms(query: String?): String {
        val q = query?.trim().orEmpty()
        if (q.isBlank()) return "What should I search in your messages, sir?"
        if (!hasSmsPermission()) {
            return "SMS permission is off, sir. Enable it in Settings so I can search messages."
        }
        return try {
            val uri = Telephony.Sms.Inbox.CONTENT_URI
            val projection = arrayOf(
                Telephony.Sms.ADDRESS,
                Telephony.Sms.BODY,
                Telephony.Sms.DATE
            )
            val results = mutableListOf<String>()
            val contactDigits = resolveContactDigits(q)
            val ql = q.lowercase()
            context.contentResolver.query(
                uri, projection, null, null,
                Telephony.Sms.DEFAULT_SORT_ORDER
            )?.use { c ->
                val iAddr = c.getColumnIndex(Telephony.Sms.ADDRESS)
                val iBody = c.getColumnIndex(Telephony.Sms.BODY)
                var scanned = 0
                while (c.moveToNext() && scanned < 500 && results.size < 6) {
                    scanned++
                    val addr = if (iAddr >= 0) c.getString(iAddr) ?: "" else ""
                    val body = if (iBody >= 0) c.getString(iBody) ?: "" else ""
                    val addrDigits = addr.filter { it.isDigit() }
                    val hit = body.lowercase().contains(ql) ||
                        addr.lowercase().contains(ql) ||
                        contactDigits.any { d -> d.length >= 8 && addrDigits.endsWith(d.takeLast(10)) }
                    if (hit) {
                        val snippet = body.replace(Regex("\\s+"), " ").trim()
                        val short = if (snippet.length > 120) snippet.take(120) + "…" else snippet
                        val who = contactLabelFor(addr) ?: addr
                        results += "From $who: $short"
                    }
                }
            }
            when {
                results.isEmpty() -> "No SMS matched \"$q\", sir."
                else -> "SMS matches for $q: " + results.joinToString(" | ")
            }
        } catch (e: SecurityException) {
            "SMS permission denied, sir."
        } catch (e: Exception) {
            "Couldn't search SMS: ${e.message}"
        }
    }

    fun searchPhotos(query: String?): String {
        val q = query?.trim().orEmpty()
        if (q.isBlank()) return "What photos should I look for, sir?"
        if (!hasMediaPermission()) {
            return "Photos permission is off, sir. Enable Photos / media access in Settings."
        }
        return try {
            val collection = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                MediaStore.Images.Media.getContentUri(MediaStore.VOLUME_EXTERNAL)
            } else {
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI
            }
            val projection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.DATE_TAKEN
            )
            val matches = mutableListOf<String>()
            context.contentResolver.query(
                collection, projection, null, null,
                MediaStore.Images.Media.DATE_TAKEN + " DESC"
            )?.use { c ->
                val iName = c.getColumnIndex(MediaStore.Images.Media.DISPLAY_NAME)
                val ql = q.lowercase()
                var scanned = 0
                while (c.moveToNext() && scanned < 800 && matches.size < 8) {
                    scanned++
                    val name = if (iName >= 0) c.getString(iName) ?: continue else continue
                    if (name.lowercase().contains(ql)) {
                        matches += name
                    }
                }
            }
            when {
                matches.isEmpty() -> {
                    openPhotosApp(q)
                    "No file-name match for \"$q\". Opening gallery, sir."
                }
                else -> "Found ${matches.size} photo(s): " + matches.take(5).joinToString(", ")
            }
        } catch (e: SecurityException) {
            "Media permission denied, sir."
        } catch (e: Exception) {
            "Couldn't search photos: ${e.message}"
        }
    }

    fun searchMail(query: String?): String {
        val q = query?.trim().orEmpty()
        if (q.isBlank()) return "What should I search in mail, sir?"
        return try {
            val intents = listOf(
                Intent(Intent.ACTION_VIEW, Uri.parse("googlegmail://co?q=" + Uri.encode(q))),
                Intent(Intent.ACTION_SEARCH).apply {
                    setPackage("com.google.android.gm")
                    putExtra("query", q)
                    putExtra(android.app.SearchManager.QUERY, q)
                },
                Intent(Intent.ACTION_VIEW, Uri.parse(
                    "https://mail.google.com/mail/mu/mp/#search/" + Uri.encode(q)
                ))
            )
            for (intent in intents) {
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                try {
                    if (intent.resolveActivity(context.packageManager) != null) {
                        context.startActivity(intent)
                        return "Opening mail search for \"$q\", sir."
                    }
                } catch (_: Exception) {}
            }
            val launch = context.packageManager.getLaunchIntentForPackage("com.google.android.gm")
            if (launch != null) {
                launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                context.startActivity(launch)
                return "Opened Gmail. Search for \"$q\" there, sir."
            }
            "Gmail isn't installed, sir."
        } catch (e: Exception) {
            "Couldn't open mail: ${e.message}"
        }
    }

    fun runAgentTask(task: String, runStep: (String) -> String): String {
        val t = task.trim()
        if (t.isBlank()) return "What should I handle, sir?"
        val parts = CommandChainSplitter.split(t) ?: listOf(t)
        if (parts.size == 1) {
            val one = runStep(parts[0])
            return if (one.isNotBlank()) one else "Done, sir."
        }
        val out = mutableListOf<String>()
        for ((i, step) in parts.withIndex()) {
            try {
                val r = runStep(step).trim()
                if (r.isNotBlank()) out += "Step ${i + 1}: $r"
            } catch (e: Exception) {
                out += "Step ${i + 1} failed: ${e.message}"
                break
            }
        }
        return buildString {
            append("Agent ran ${parts.size} steps. ")
            append(out.joinToString(" "))
        }.trim()
    }

    private fun resolveContactDigits(nameQuery: String): List<String> {
        val q = nameQuery.trim()
        if (q.length < 2) return emptyList()
        if (!hasContactsPermission()) return emptyList()
        val out = mutableListOf<String>()
        try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.NUMBER,
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME
                ),
                "${ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME} LIKE ?",
                arrayOf("%$q%"),
                null
            )?.use { c ->
                val iNum = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (c.moveToNext() && out.size < 5) {
                    val n = if (iNum >= 0) c.getString(iNum)?.filter { it.isDigit() } else null
                    if (!n.isNullOrBlank()) out += n
                }
            }
        } catch (_: Exception) {}
        return out
    }

    private fun contactLabelFor(address: String): String? {
        if (!hasContactsPermission()) return null
        val digits = address.filter { it.isDigit() }
        if (digits.length < 8) return null
        val tail = digits.takeLast(10)
        return try {
            context.contentResolver.query(
                ContactsContract.CommonDataKinds.Phone.CONTENT_URI,
                arrayOf(
                    ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME,
                    ContactsContract.CommonDataKinds.Phone.NUMBER
                ),
                null, null, null
            )?.use { c ->
                val iName = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.DISPLAY_NAME)
                val iNum = c.getColumnIndex(ContactsContract.CommonDataKinds.Phone.NUMBER)
                while (c.moveToNext()) {
                    val n = if (iNum >= 0) c.getString(iNum)?.filter { it.isDigit() } ?: "" else ""
                    if (n.endsWith(tail) || tail.endsWith(n.takeLast(10))) {
                        return@use if (iName >= 0) c.getString(iName) else null
                    }
                }
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun openPhotosApp(query: String) {
        try {
            val i = Intent(Intent.ACTION_VIEW).apply {
                type = "image/*"
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            }
            context.startActivity(i)
        } catch (_: Exception) {
            try {
                val launch = context.packageManager.getLaunchIntentForPackage("com.google.android.apps.photos")
                launch?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                if (launch != null) context.startActivity(launch)
            } catch (_: Exception) {}
        }
    }

    private fun hasSmsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_SMS) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasContactsPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) ==
            PackageManager.PERMISSION_GRANTED

    private fun hasMediaPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= 33) {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_MEDIA_IMAGES) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            ContextCompat.checkSelfPermission(context, Manifest.permission.READ_EXTERNAL_STORAGE) ==
                PackageManager.PERMISSION_GRANTED
        }
    }
}
