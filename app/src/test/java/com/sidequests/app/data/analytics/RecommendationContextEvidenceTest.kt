package com.sidequests.app.data.analytics

import com.sidequests.app.context.LocationData
import com.sidequests.app.context.TimeOfDay
import com.sidequests.app.context.UserContext
import com.sidequests.app.context.WeatherCondition
import com.sidequests.app.context.WeatherData
import kotlin.test.Test
import kotlin.test.assertEquals

class RecommendationContextEvidenceTest {

    @Test
    fun recordsOpenMeteoContextWhenAvailable() {
        val metadata = buildRecommendationContextMetadata(
            context = UserContext(
                location = LocationData(4.65, -74.05),
                weather = WeatherData(18.5, WeatherCondition.CLOUDY),
                timeOfDay = TimeOfDay.AFTERNOON,
            ),
            rank = 2,
        )

        assertEquals("2", metadata["rank"].toString())
        assertEquals(""afternoon"", metadata["time_of_day"].toString())
        assertEquals("true", metadata["location_available"].toString())
        assertEquals("true", metadata["weather_available"].toString())
        assertEquals("false", metadata["context_fallback"].toString())
        assertEquals(""open_meteo"", metadata["weather_provider"].toString())
    }

    @Test
    fun marksFallbackWhenExternalContextIsUnavailable() {
        val metadata = buildRecommendationContextMetadata(
            context = UserContext(
                location = null,
                weather = null,
                timeOfDay = TimeOfDay.NIGHT,
            ),
            rank = 1,
        )

        assertEquals("false", metadata["location_available"].toString())
        assertEquals("false", metadata["weather_available"].toString())
        assertEquals("true", metadata["context_fallback"].toString())
        assertEquals(""night"", metadata["time_of_day"].toString())
    }
}
