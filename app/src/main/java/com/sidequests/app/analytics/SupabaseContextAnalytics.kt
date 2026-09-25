package com.sidequests.app.analytics

import com.sidequests.app.BuildConfig
import com.sidequests.app.context.QuestLocationMode
import com.sidequests.app.context.TimeOfDay
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL

/**
 * Sends the BQ10 events directly to a Supabase PostgREST table.
 *
 * The table expected by this class is `context_analytics_events`.
 * See docs/BQ10_SUPABASE.md for the SQL schema and configuration.
 */
class SupabaseContextAnalytics : ContextAnalytics {

    override suspend fun trackSessionStarted(
        sessionId: String,
        timeOfDay: TimeOfDay,
        weatherCondition: String?
    ) {
        postEvent(
            sessionId = sessionId,
            eventName = "session_started",
            mode = null,
            timeOfDay = timeOfDay,
            weatherCondition = weatherCondition
        )
    }

    override suspend fun trackLocationModeSelected(
        sessionId: String,
        mode: QuestLocationMode,
        timeOfDay: TimeOfDay,
        weatherCondition: String?
    ) {
        postEvent(
            sessionId = sessionId,
            eventName = "location_mode_selected",
            mode = mode,
            timeOfDay = timeOfDay,
            weatherCondition = weatherCondition
        )
    }

    private suspend fun postEvent(
        sessionId: String,
        eventName: String,
        mode: QuestLocationMode?,
        timeOfDay: TimeOfDay,
        weatherCondition: String?
    ) = withContext(Dispatchers.IO) {
        if (BuildConfig.SUPABASE_URL.isBlank() || BuildConfig.SUPABASE_ANON_KEY.isBlank()) {
            return@withContext
        }

        try {
            val endpoint = BuildConfig.SUPABASE_URL.trimEnd('/') +
                "/rest/v1/context_analytics_events"

            val payload = JSONObject().apply {
                put("session_id", sessionId)
                put("event_name", eventName)
                put("location_mode", mode?.name)
                put("time_of_day", timeOfDay.name)
                put("weather_condition", weatherCondition)
                put("created_at", java.time.Instant.now().toString())
            }

            val connection = URL(endpoint).openConnection() as HttpURLConnection
            connection.requestMethod = "POST"
            connection.connectTimeout = 5_000
            connection.readTimeout = 5_000
            connection.doOutput = true
            connection.setRequestProperty("Content-Type", "application/json")
            connection.setRequestProperty("apikey", BuildConfig.SUPABASE_ANON_KEY)
            connection.setRequestProperty(
                "Authorization",
                "Bearer ${BuildConfig.SUPABASE_ANON_KEY}"
            )
            connection.setRequestProperty("Prefer", "return=minimal")

            try {
                connection.outputStream.use { output ->
                    output.write(payload.toString().toByteArray(Charsets.UTF_8))
                }
                connection.responseCode
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            // Analytics must never break the user's quest flow.
        }
    }
}
