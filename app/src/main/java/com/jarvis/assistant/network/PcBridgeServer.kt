package com.jarvis.assistant.network

import android.content.Context
import android.net.wifi.WifiManager
import android.text.format.Formatter
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.io.PrintWriter
import java.net.ServerSocket
import java.net.Socket

/**
 * Phase 5 — "PC connect (optional)". A minimal same-Wi-Fi bridge: a PC on the same
 * network can open a plain TCP socket to the phone and send single-line JSON commands,
 * getting the same offline-first response Jarvis would speak on-device.
 *
 * This intentionally has NO authentication beyond "must be on the same LAN" — it's meant
 * for a trusted home/office network, not the open internet. Don't port-forward this.
 *
 * Wire protocol (newline-delimited JSON, one request per line):
 *   PC -> phone:  {"speech": "what time is it"}
 *   phone -> PC:  {"reply": "It's 10:42 AM.", "offline": true}
 *
 * A PC can talk to it with nothing more than netcat or a 5-line Python script:
 *   import socket, json
 *   s = socket.create_connection(("<phone-ip>", 8765))
 *   s.sendall((json.dumps({"speech": "what time is it"}) + "\n").encode())
 *   print(s.recv(4096).decode())
 */
class PcBridgeServer(
    private val context: Context,
    private val onSpeech: suspend (String) -> Pair<String, Boolean>
) {
    private var serverSocket: ServerSocket? = null
    private var job: Job? = null
    private val scope = CoroutineScope(Dispatchers.IO)

    val isRunning: Boolean get() = serverSocket != null

    fun start(port: Int = DEFAULT_PORT): String {
        if (isRunning) return "PC bridge already running on ${localIp()}:$port"

        return try {
            val socket = ServerSocket(port)
            serverSocket = socket
            job = scope.launch { acceptLoop(socket) }
            "PC bridge running on ${localIp()}:$port"
        } catch (e: Exception) {
            "Couldn't start the PC bridge: ${e.message}"
        }
    }

    fun stop(): String {
        job?.cancel()
        try { serverSocket?.close() } catch (e: Exception) { /* already closed */ }
        serverSocket = null
        return "PC bridge stopped."
    }

    private suspend fun acceptLoop(socket: ServerSocket) {
        while (!socket.isClosed) {
            val client = try {
                socket.accept()
            } catch (e: Exception) {
                break // socket closed, stop() was called
            }
            scope.launch { handleClient(client) }
        }
    }

    private suspend fun handleClient(client: Socket) {
        client.use { c ->
            try {
                val reader = BufferedReader(InputStreamReader(c.getInputStream()))
                val writer = PrintWriter(c.getOutputStream(), true)
                val line = reader.readLine() ?: return
                val speech = JSONObject(line).optString("speech", "")
                if (speech.isBlank()) {
                    writer.println(JSONObject().put("error", "missing 'speech' field").toString())
                    return
                }
                val (reply, offline) = onSpeech(speech)
                writer.println(JSONObject().put("reply", reply).put("offline", offline).toString())
            } catch (e: Exception) {
                try {
                    PrintWriter(c.getOutputStream(), true)
                        .println(JSONObject().put("error", e.message ?: "unknown error").toString())
                } catch (ignored: Exception) { }
            }
        }
    }

    private fun localIp(): String {
        val wifiManager = context.applicationContext
            .getSystemService(Context.WIFI_SERVICE) as WifiManager
        return try {
            Formatter.formatIpAddress(wifiManager.connectionInfo.ipAddress)
        } catch (e: Exception) {
            "this device's IP"
        }
    }

    companion object {
        const val DEFAULT_PORT = 8765
    }
}
