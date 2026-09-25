package com.sidequests.app.context

import java.util.Calendar

class ContextManager(
    private val locationProvider: LocationProvider,
    private val weatherService: WeatherService
) {

    suspend fun getCurrentContext(): UserContext {
        val location = locationProvider.getCurrentLocation()

        val weather = location?.let {
            weatherService.getWeather(
                latitude = it.latitude,
                longitude = it.longitude
            )
        }

        return UserContext(
            location = location,
            weather = weather,
            timeOfDay = currentTimeOfDay(),
            timestamp = System.currentTimeMillis()
        )
    }

    private fun currentTimeOfDay(): TimeOfDay {
        val hour = Calendar.getInstance().get(Calendar.HOUR_OF_DAY)

        return when (hour) {
            in 6..11 -> TimeOfDay.MORNING
            in 12..17 -> TimeOfDay.AFTERNOON
            in 18..21 -> TimeOfDay.EVENING
            else -> TimeOfDay.NIGHT
        }
    }
}
