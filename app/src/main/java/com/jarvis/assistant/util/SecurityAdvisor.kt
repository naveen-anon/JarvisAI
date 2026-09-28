package com.jarvis.assistant.util

import android.accessibilityservice.AccessibilityServiceInfo
import android.app.KeyguardManager
import android.content.Context
import android.content.pm.PackageManager
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.provider.Settings
import android.view.accessibility.AccessibilityManager
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.jarvis.assistant.voice.VoiceAuthManager

/**
 * Advanced defensive security posture for JARVIS.
 * No offensive tooling — assessment + hardening advice only.
 */
class SecurityAdvisor(private val context: Context) {

    data class Finding(
        val id: String,
        val severity: Severity,
        val title: String,
        val detail: String,
        val action: String? = null
    )

    enum class Severity { CRITICAL, HIGH, MEDIUM, LOW, OK }

    data class Report(
        val score: Int,
        val grade: String,
        val summary: String,
        val spoken: String,
        val findings: List<Finding>
    )

    fun assess(): Report {
        val findings = mutableListOf<Finding>()
        findings += deviceLock()
        findings += adbDebug()
        findings += networkPosture()
        findings += vpnStatus()
        findings += voiceAuth()
        findings += backgroundListenPrivacy()
        findings += notificationAccess()
        findings += accessibilityJarvis()
        findings += thirdPartyAccessibility()
        findings += dangerousPermsSelf()
        findings += screenTimeout()

        val score = computeScore(findings)
        val grade = when {
            score >= 90 -> "A"
            score >= 80 -> "B"
            score >= 65 -> "C"
            score >= 50 -> "D"
            else -> "F"
        }

        val critical = findings.count { it.severity == Severity.CRITICAL }
        val high = findings.count { it.severity == Severity.HIGH }

        val summary = when {
            critical > 0 -> {
                val plural = if (critical > 1) "s" else ""
                "Security posture degraded, sir. $critical critical issue$plural require attention."
            }
            high > 0 -> {
                val plural = if (high > 1) "s" else ""
                "Security is acceptable but not optimal. $high high-priority item$plural to harden."
            }
            score >= 85 -> "Security posture is strong, sir. Score $score, grade $grade."
            else -> "Security score $score, grade $grade. A few improvements would tighten the perimeter."
        }

        val spoken = buildSpoken(summary, score, grade, findings)
        return Report(score, grade, summary, spoken, findings)
    }

    private fun buildSpoken(summary: String, score: Int, grade: String, findings: List<Finding>): String {
        val sb = StringBuilder()
        sb.append(summary).append(" ")
        sb.append("Overall score: $score out of 100, grade $grade. ")

        val problems = findings.filter {
            it.severity == Severity.CRITICAL ||
                it.severity == Severity.HIGH ||
                it.severity == Severity.MEDIUM
        }.sortedBy {
            when (it.severity) {
                Severity.CRITICAL -> 0
                Severity.HIGH -> 1
                Severity.MEDIUM -> 2
                else -> 3
            }
        }

        if (problems.isEmpty()) {
            sb.append("No material risks detected on the checks I can run locally. ")
        } else {
            sb.append("Priority findings: ")
            problems.take(4).forEachIndexed { i, f ->
                sb.append("${i + 1}. ${f.title}. ${f.detail}")
                f.action?.let { sb.append(" Recommendation: $it") }
                sb.append(" ")
            }
        }

        val oks = findings.count { it.severity == Severity.OK }
        sb.append("$oks controls look healthy. ")
        sb.append("This is a defensive assessment only.")
        return sb.toString().trim()
    }

    private fun computeScore(findings: List<Finding>): Int {
        var score = 100
        findings.forEach {
            score -= when (it.severity) {
                Severity.CRITICAL -> 25
                Severity.HIGH -> 12
                Severity.MEDIUM -> 6
                Severity.LOW -> 2
                Severity.OK -> 0
            }
        }
        return score.coerceIn(0, 100)
    }

    private fun deviceLock(): Finding {
        return try {
            val km = context.getSystemService(Context.KEYGUARD_SERVICE) as KeyguardManager
            if (km.isDeviceSecure) {
                Finding("lock", Severity.OK, "Device lock", "Screen lock with PIN, pattern, or biometric is configured.")
            } else {
                Finding(
                    "lock", Severity.CRITICAL, "No secure device lock",
                    "This device has no secure screen lock.",
                    "Set a PIN or biometric lock under system Security settings."
                )
            }
        } catch (_: Exception) {
            Finding("lock", Severity.MEDIUM, "Device lock", "Could not verify lock state.", "Check system Security settings.")
        }
    }

    private fun adbDebug(): Finding {
        return try {
            val adb = Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0)
            if (adb == 1) {
                Finding(
                    "adb", Severity.HIGH, "USB debugging enabled",
                    "ADB debugging is on — risky if the device is lost or connected to untrusted hosts.",
                    "Disable Developer options, USB debugging when not developing."
                )
            } else {
                Finding("adb", Severity.OK, "USB debugging", "ADB debugging is off.")
            }
        } catch (_: Exception) {
            Finding("adb", Severity.LOW, "USB debugging", "Could not read ADB state.")
        }
    }

    private fun networkPosture(): Finding {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork
            if (net == null) {
                return Finding("net", Severity.OK, "Network", "No active network — reduced remote attack surface.")
            }
            val caps = cm.getNetworkCapabilities(net)
                ?: return Finding("net", Severity.MEDIUM, "Network", "Active network but capabilities unknown.")

            val wifi = caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
            val cell = caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
            val validated = caps.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)

            when {
                wifi && validated -> Finding("net", Severity.OK, "Network", "On Wi-Fi with validated internet.")
                cell && validated -> Finding("net", Severity.OK, "Network", "On mobile data with validated connectivity.")
                !validated -> Finding(
                    "net", Severity.MEDIUM, "Captive or unvalidated network",
                    "Network may be captive portal or untrusted.",
                    "Avoid sensitive logins until the connection is validated."
                )
                else -> Finding("net", Severity.LOW, "Network", "Connected via alternate transport.")
            }
        } catch (_: Exception) {
            Finding("net", Severity.LOW, "Network", "Network posture unavailable.")
        }
    }

    private fun vpnStatus(): Finding {
        return try {
            val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val net = cm.activeNetwork
                ?: return Finding("vpn", Severity.LOW, "VPN", "No active network — VPN not applicable.")
            val caps = cm.getNetworkCapabilities(net)
            val vpn = caps?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true
            if (vpn) {
                Finding("vpn", Severity.OK, "VPN active", "Traffic appears to be tunneled through a VPN.")
            } else {
                Finding(
                    "vpn", Severity.LOW, "No VPN",
                    "No system VPN transport detected.",
                    "On public Wi-Fi, consider a trusted VPN before sensitive work."
                )
            }
        } catch (_: Exception) {
            Finding("vpn", Severity.LOW, "VPN", "VPN status unavailable.")
        }
    }

    private fun voiceAuth(): Finding {
        return try {
            val va = VoiceAuthManager(context)
            val enrolled = try {
                val m = va.javaClass.methods.find {
                    it.name in listOf("hasEnrollment", "isEnrolled", "hasVoiceprint", "isRegistered") &&
                        it.parameterCount == 0
                }
                (m?.invoke(va) as? Boolean) == true
            } catch (_: Exception) {
                false
            }
            if (enrolled) {
                Finding("voice_auth", Severity.OK, "Voice authentication", "Voiceprint enrollment is present.")
            } else {
                Finding(
                    "voice_auth", Severity.MEDIUM, "Voice authentication off",
                    "No voiceprint enrollment detected — anyone who triggers wake can issue commands.",
                    "Enroll voice authentication in Settings for command gating."
                )
            }
        } catch (_: Exception) {
            Finding("voice_auth", Severity.LOW, "Voice authentication", "Could not verify voice auth state.")
        }
    }

    private fun backgroundListenPrivacy(): Finding {
        return try {
            val on = SettingsManager(context).getBackgroundListen()
            if (on) {
                Finding(
                    "bg_listen", Severity.MEDIUM, "Background listening enabled",
                    "Continuous wake listening increases mic exposure and battery use.",
                    "Disable background listen in sensitive environments; use Talk or clap wake instead."
                )
            } else {
                Finding("bg_listen", Severity.OK, "Background listening", "Continuous mic wake is off.")
            }
        } catch (_: Exception) {
            Finding("bg_listen", Severity.LOW, "Background listening", "Preference unreadable.")
        }
    }

    private fun notificationAccess(): Finding {
        return try {
            val enabled = NotificationManagerCompat.getEnabledListenerPackages(context)
                .contains(context.packageName)
            if (enabled) {
                Finding(
                    "notif", Severity.OK, "Notification access",
                    "JARVIS notification access is enabled for briefing and alerts."
                )
            } else {
                Finding(
                    "notif", Severity.LOW, "Notification access off",
                    "Without notification access I cannot summarize what you missed.",
                    "Enable notification access for JARVIS in system settings if you want that capability."
                )
            }
        } catch (_: Exception) {
            Finding("notif", Severity.LOW, "Notification access", "Status unknown.")
        }
    }

    private fun accessibilityJarvis(): Finding {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
                .any { it.resolveInfo?.serviceInfo?.packageName == context.packageName }
            if (enabled) {
                Finding(
                    "a11y", Severity.OK, "Accessibility service",
                    "JARVIS accessibility is on — screen assist features available. Treat this as high privilege."
                )
            } else {
                Finding(
                    "a11y", Severity.LOW, "Accessibility off",
                    "Screen-reading and advanced automation need Accessibility enabled.",
                    "Enable JARVIS under Settings, Accessibility only if you use those features."
                )
            }
        } catch (_: Exception) {
            Finding("a11y", Severity.LOW, "Accessibility", "Status unknown.")
        }
    }

    private fun thirdPartyAccessibility(): Finding {
        return try {
            val am = context.getSystemService(Context.ACCESSIBILITY_SERVICE) as AccessibilityManager
            val enabled = am.getEnabledAccessibilityServiceList(AccessibilityServiceInfo.FEEDBACK_ALL_MASK)
            val others = enabled.mapNotNull { info ->
                val pkg = info.resolveInfo?.serviceInfo?.packageName ?: return@mapNotNull null
                if (pkg == context.packageName) return@mapNotNull null
                if (pkg.startsWith("com.android.") || pkg.startsWith("com.google.android.")) {
                    return@mapNotNull null
                }
                val label = try {
                    info.resolveInfo.loadLabel(context.packageManager).toString()
                } catch (_: Exception) {
                    pkg
                }
                label to pkg
            }
            when {
                others.isEmpty() -> Finding(
                    "a11y_third", Severity.OK, "Third-party accessibility",
                    "No third-party accessibility services enabled."
                )
                others.size == 1 -> Finding(
                    "a11y_third", Severity.HIGH, "Third-party accessibility active",
                    "Enabled: " + others[0].first + " [" + others[0].second + "]. Accessibility can read the screen and drive UI.",
                    "Disable any service you do not fully trust under Settings, Accessibility."
                )
                else -> {
                    val names = others.joinToString { it.first }
                    Finding(
                        "a11y_third", Severity.HIGH, "Multiple third-party accessibility services",
                        "Enabled: $names. Each is high privilege.",
                        "Review and remove unused accessibility services immediately."
                    )
                }
            }
        } catch (_: Exception) {
            Finding(
                "a11y_third", Severity.LOW, "Third-party accessibility",
                "Could not enumerate accessibility services."
            )
        }
    }

    private fun dangerousPermsSelf(): Finding {
        val dangerous = listOf(
            android.Manifest.permission.RECORD_AUDIO to "microphone",
            android.Manifest.permission.CAMERA to "camera",
            android.Manifest.permission.ACCESS_FINE_LOCATION to "precise location",
            android.Manifest.permission.READ_CONTACTS to "contacts",
            android.Manifest.permission.CALL_PHONE to "phone",
            android.Manifest.permission.SEND_SMS to "SMS"
        )
        val granted = dangerous.filter { (perm, _) ->
            ContextCompat.checkSelfPermission(context, perm) == PackageManager.PERMISSION_GRANTED
        }.map { it.second }

        return if (granted.isEmpty()) {
            Finding(
                "perms", Severity.MEDIUM, "Core permissions missing",
                "JARVIS lacks several operational permissions — functionality limited, attack surface smaller."
            )
        } else {
            val list = granted.joinToString(", ")
            Finding(
                "perms", Severity.OK, "App permissions",
                "Granted sensitive capabilities: $list. Review periodically in system App permissions."
            )
        }
    }

    private fun screenTimeout(): Finding {
        return try {
            val timeout = Settings.System.getInt(
                context.contentResolver,
                Settings.System.SCREEN_OFF_TIMEOUT,
                60_000
            )
            val sec = timeout / 1000
            when {
                sec <= 0 || timeout >= 10 * 60_000 -> Finding(
                    "timeout", Severity.MEDIUM, "Long screen timeout",
                    "Screen timeout is very long (" + sec + "s) — unattended unlock risk.",
                    "Set screen timeout to 30 to 60 seconds in Display settings."
                )
                sec > 120 -> Finding(
                    "timeout", Severity.LOW, "Screen timeout",
                    "Screen turns off after " + sec + " seconds.",
                    "Consider 60 seconds or less for tighter physical security."
                )
                else -> Finding(
                    "timeout", Severity.OK, "Screen timeout",
                    "Screen timeout is " + sec + " seconds — reasonable."
                )
            }
        } catch (_: Exception) {
            Finding("timeout", Severity.LOW, "Screen timeout", "Could not read timeout setting.")
        }
    }
}
