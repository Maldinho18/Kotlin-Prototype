package com.sidequests.app.context

interface WeatherService {
    suspend fun getWeather(
        latitude: Double,
        longitude: Double
    ): WeatherData?
}
