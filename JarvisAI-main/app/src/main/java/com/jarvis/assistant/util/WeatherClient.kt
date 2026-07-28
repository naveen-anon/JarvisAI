package com.jarvis.assistant.util

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class WeatherClient(private val apiKey: String) {

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    data class WeatherResult(val tempCelsius: Int, val condition: String)

    suspend fun getWeather(lat: Double, lon: Double): WeatherResult? = withContext(Dispatchers.IO) {
        if (apiKey.isBlank()) return@withContext null

        val url = "https://api.openweathermap.org/data/2.5/weather" +
                "?lat=$lat&lon=$lon&units=metric&appid=$apiKey"

        try {
            val request = Request.Builder().url(url).build()
            client.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@withContext null
                val body = response.body?.string() ?: return@withContext null
                val json = JSONObject(body)
                val temp = json.getJSONObject("main").getDouble("temp").toInt()
                val condition = json.getJSONArray("weather")
                    .getJSONObject(0)
                    .getString("main")
                WeatherResult(temp, condition)
            }
        } catch (e: Exception) {
            null
        }
    }
}
