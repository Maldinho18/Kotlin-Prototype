package com.sidequests.app.context

import com.sidequests.app.model.Quest

class ContextAwareRecommendationEngine {
    fun apply(base: List<Quest>, context: UserContext): List<Quest> {
        val weather = context.weather?.condition ?: WeatherCondition.UNKNOWN
        return base
            .map { quest ->
                var score = 0.0
                val tags = quest.tags.map { it.lowercase() }

                when (weather) {
                    WeatherCondition.RAIN, WeatherCondition.STORM, WeatherCondition.SNOW -> {
                        if (tags.any { it in setOf("indoor", "home", "creative", "food", "learning", "calm") }) score += 3.0
                        if (tags.any { it in setOf("outdoor", "park", "hiking", "walking", "nature") }) score -= 2.0
                    }
                    WeatherCondition.CLEAR -> {
                        if (tags.any { it in setOf("outdoor", "park", "walking", "movement", "nature") }) score += 2.0
                    }
                    WeatherCondition.CLOUDY -> {
                        if (tags.any { it in setOf("walking", "art", "learning", "food") }) score += 0.5
                    }
                    else -> Unit
                }

                when (context.timeOfDay) {
                    TimeOfDay.MORNING -> if (tags.any { it in setOf("learning", "coffee", "movement") }) score += 1.0
                    TimeOfDay.AFTERNOON -> if (tags.any { it in setOf("art", "food", "walking", "social") }) score += 0.5
                    TimeOfDay.EVENING -> if (tags.any { it in setOf("food", "art", "social", "indoor") }) score += 1.0
                    TimeOfDay.NIGHT -> if (quest.budgetAmount == 0 || tags.any { it in setOf("home", "indoor", "calm", "solo") }) score += 1.5
                }

                // A location-independent quest is always viable when GPS is unavailable.
                if (context.location == null && quest.location.name == "Anywhere") score += 2.0

                quest to score
            }
            .sortedByDescending { it.second }
            .take(3)
            .map { it.first }
    }
}
