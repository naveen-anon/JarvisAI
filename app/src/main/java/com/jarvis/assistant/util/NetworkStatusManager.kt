package com.jarvis.assistant.util

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiManager

class NetworkStatusManager(private val context: Context) {

    fun getSignalLabel(): String {
        val cm = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val network = cm.activeNetwork ?: return "OFFLINE"
        val caps = cm.getNetworkCapabilities(network) ?: return "OFFLINE"

        return when {
            caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> {
                val wifiManager = context.applicationContext
                    .getSystemService(Context.WIFI_SERVICE) as WifiManager
                val rssi = wifiManager.connectionInfo.rssi
                val bars = when {
                    rssi >= -50 -> 4
                    rssi >= -60 -> 3
                    rssi >= -70 -> 2
                    else -> 1
                }
                "WIFI ${"▮".repeat(bars)}${"▯".repeat(4 - bars)}"
            }
            caps.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> "CELLULAR LINK"
            else -> "CONNECTED"
        }
    }
}
