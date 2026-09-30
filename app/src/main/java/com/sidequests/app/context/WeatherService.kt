package com.sidequests.app.context

import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


interface WeatherProvider {
    suspend fun getWeather(location: LocationData): WeatherData?
}

class OpenMeteoWeatherProvider : WeatherProvider {
    override suspend fun getWeather(location: LocationData): WeatherData? = withContext(Dispatchers.IO) {
        val url = URL(
            "https://api.open-meteo.com/v1/forecast" +
                "?latitude=${location.latitude}" +
                "&longitude=${location.longitude}" +
                "&current=temperature_2m,weather_code" +
                "&temperature_unit=celsius"
        )
        val connection = (url.openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 7_000
            readTimeout = 7_000
        }
        try {
            if (connection.responseCode !in 200..299) return@withContext null
            val body = connection.inputStream.bufferedReader().use { it.readText() }
            val current = JSONObject(body).getJSONObject("current")
            WeatherData(
                temperatureCelsius = current.optDouble("temperature_2m", Double.NaN),
                condition = weatherCodeToCondition(current.optInt("weather_code", -1)),
            )
        } catch (_: Exception) {
            null
        } finally {
            connection.disconnect()
        }
    }

    private fun weatherCodeToCondition(code: Int): WeatherCondition = when (code) {
        0, 1 -> WeatherCondition.CLEAR
        2, 3, 45, 48 -> WeatherCondition.CLOUDY
        in 51..67, in 80..82 -> WeatherCondition.RAIN
        in 71..77, 85, 86 -> WeatherCondition.SNOW
        in 95..99 -> WeatherCondition.STORM
        else -> WeatherCondition.UNKNOWN
    }
}

class MockWeatherProvider : WeatherProvider {
    override suspend fun getWeather(location: LocationData): WeatherData =
        WeatherData(20.0, WeatherCondition.CLEAR)
}
