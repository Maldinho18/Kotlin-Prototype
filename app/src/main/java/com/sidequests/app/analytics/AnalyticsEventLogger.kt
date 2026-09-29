package com.sidequests.app.analytics

import android.content.Context
import com.sidequests.app.BuildConfig
import com.sidequests.app.auth.SupabaseAuthService
import org.json.JSONObject
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.util.UUID
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext


interface AnalyticsEventLogger {
    suspend fun log(eventName: String, sessionId: String, properties: Map<String, String>)
}

class SupabaseAnalyticsEventLogger(private val context: Context) : AnalyticsEventLogger {
    private val authService = SupabaseAuthService(context)
    private val fallback = LocalAnalyticsEventLogger(context)

    override suspend fun log(eventName: String, sessionId: String, properties: Map<String, String>) {
        val url = BuildConfig.SUPABASE_URL
        val key = BuildConfig.SUPABASE_PUBLISHABLE_KEY
        if (url.isBlank() || key.isBlank()) {
            fallback.log(eventName, sessionId, properties)
            return
        }

        withContext(Dispatchers.IO) {
            try {
                val payload = JSONObject().apply {
                    put("event_name", eventName)
                    put("session_id", sessionId)
                    put("user_id", authService.currentSession()?.userId ?: JSONObject.NULL)
                    put("created_at", Instant.now().toString())
                    put("properties", JSONObject(properties))
                }
                val connection = (URL("${url.trimEnd('/')}/rest/v1/analytics_events").openConnection() as HttpURLConnection).apply {
                    requestMethod = "POST"
                    connectTimeout = 7_000
                    readTimeout = 7_000
                    doOutput = true
                    setRequestProperty("apikey", key)
                    setRequestProperty("Authorization", "Bearer ${authService.currentSession()?.accessToken ?: key}")
                    setRequestProperty("Content-Type", "application/json")
                    setRequestProperty("Prefer", "return=minimal")
                }
                connection.outputStream.use { it.write(payload.toString().toByteArray()) }
                if (connection.responseCode !in 200..299) {
                    fallback.log(eventName, sessionId, properties)
                }
                connection.disconnect()
            } catch (_: Exception) {
                fallback.log(eventName, sessionId, properties)
            }
        }
    }
}


class LocalAnalyticsEventLogger(private val context: Context) : AnalyticsEventLogger {
    override suspend fun log(eventName: String, sessionId: String, properties: Map<String, String>) {
        val prefs = context.getSharedPreferences("bq10_analytics", Context.MODE_PRIVATE)
        val line = JSONObject().apply {
            put("event_name", eventName)
            put("session_id", sessionId)
            put("created_at", Instant.now().toString())
            put("properties", JSONObject(properties))
        }.toString()
        val current = prefs.getString("events", "") ?: ""
        prefs.edit().putString("events", if (current.isBlank()) line else "$current\n$line").apply()
    }
}

fun createSessionId(context: Context): String = UUID.randomUUID().toString()
