package com.sidequests.app.analytics

import com.sidequests.app.context.QuestLocationMode
import com.sidequests.app.context.TimeOfDay

interface ContextAnalytics {
    suspend fun trackSessionStarted(
        sessionId: String,
        timeOfDay: TimeOfDay,
        weatherCondition: String?
    )

    suspend fun trackLocationModeSelected(
        sessionId: String,
        mode: QuestLocationMode,
        timeOfDay: TimeOfDay,
        weatherCondition: String?
    )
}
