package com.sidequests.app.data.analytics

import com.sidequests.app.context.LocationData
import com.sidequests.app.context.TimeOfDay
import com.sidequests.app.context.UserContext
import com.sidequests.app.context.WeatherCondition
import com.sidequests.app.context.WeatherData
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Test
import org.junit.Assert.assertEquals

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
        assertEquals("afternoon", metadata.getValue("time_of_day").jsonPrimitive.content)
        assertEquals("true", metadata["location_available"].toString())
        assertEquals("true", metadata["weather_available"].toString())
        assertEquals("false", metadata["context_fallback"].toString())
        assertEquals("open_meteo", metadata.getValue("weather_provider").jsonPrimitive.content)
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
        assertEquals("night", metadata.getValue("time_of_day").jsonPrimitive.content)
    }
}
