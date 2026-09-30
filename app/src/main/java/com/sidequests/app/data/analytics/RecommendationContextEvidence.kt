package com.sidequests.app.data.analytics

import com.sidequests.app.context.UserContext
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/**
 * Analytics evidence for Sebastián's BQ5/context integration.
 *
 * The context provider is owned by the context feature; this helper records
 * which context was actually available when Smart Picks were shown so the
 * recommendation flow degrades explicitly when GPS/Open-Meteo are unavailable.
 */
fun buildRecommendationContextMetadata(
    context: UserContext,
    rank: Int,
    source: String = "bq5_rpc",
): JsonObject = buildJsonObject {
    put("rank", rank)
    put("source", source)
    put("time_of_day", context.timeOfDay.name.lowercase())
    put("location_available", context.location != null)
    put("weather_available", context.weather != null)
    put("weather_provider", "open_meteo")
    put("context_fallback", context.location == null || context.weather == null)

    context.weather?.let { weather ->
        put("weather_condition", weather.condition.name.lowercase())
        put("temperature_celsius", weather.temperatureCelsius)
    }
}
