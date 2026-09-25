package com.sidequests.app.context

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

class OpenMeteoWeatherService : WeatherService {

    override suspend fun getWeather(
        latitude: Double,
        longitude: Double
    ): WeatherData? = withContext(Dispatchers.IO) {
        try {
            val url = URL(
                "https://api.open-meteo.com/v1/forecast" +
                    "?latitude=$latitude" +
                    "&longitude=$longitude" +
                    "&current=temperature_2m,weather_code"
            )

            val connection = url.openConnection() as HttpURLConnection
            connection.requestMethod = "GET"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000

            try {
                if (connection.responseCode !in 200..299) return@withContext null

                val json = connection.inputStream.bufferedReader().use { it.readText() }
                val current = JSONObject(json).getJSONObject("current")

                WeatherData(
                    temperatureCelsius = current.optDouble("temperature_2m").takeIf { !it.isNaN() },
                    condition = weatherCodeToCondition(current.optInt("weather_code"))
                )
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    private fun weatherCodeToCondition(code: Int): String = when (code) {
        0 -> "CLEAR"
        1, 2 -> "PARTLY_CLOUDY"
        3 -> "CLOUDY"
        45, 48 -> "FOG"
        51, 53, 55, 56, 57 -> "DRIZZLE"
        61, 63, 65, 66, 67, 80, 81, 82 -> "RAIN"
        71, 73, 75, 77, 85, 86 -> "SNOW"
        95, 96, 99 -> "STORM"
        else -> "UNKNOWN"
    }
}
