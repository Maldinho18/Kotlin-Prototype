package com.sidequests.app.context

data class LocationData(
    val latitude: Double,
    val longitude: Double
)

data class WeatherData(
    val temperatureCelsius: Double?,
    val condition: String?
)

enum class QuestLocationMode {
    LOCATION_BASED,
    LOCATION_INDEPENDENT,
    ALL
}

enum class TimeOfDay {
    MORNING,
    AFTERNOON,
    EVENING,
    NIGHT
}

data class UserContext(
    val location: LocationData?,
    val weather: WeatherData?,
    val timeOfDay: TimeOfDay,
    val timestamp: Long
)
