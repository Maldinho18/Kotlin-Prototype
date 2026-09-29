package com.sidequests.app.context

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


class ContextManager(
    private val locationProvider: LocationProvider,
    private val weatherProvider: WeatherProvider,
    private val timeProvider: TimeProvider = SystemTimeProvider(),
) {
    suspend fun readContext(): UserContext = withContext(Dispatchers.IO) {
        val location = locationProvider.getCurrentLocation()
        val weather = location?.let { weatherProvider.getWeather(it) }
        val now = timeProvider.now()
        UserContext(
            location = location,
            weather = weather,
            timeOfDay = timeOfDayFromHour(now.hour),
            timestamp = now.toInstant(),
        )
    }
}
