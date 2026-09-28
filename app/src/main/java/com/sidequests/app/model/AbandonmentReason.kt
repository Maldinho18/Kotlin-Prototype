package com.sidequests.app.model

enum class AbandonmentReason(
    val code: String,
    val label: String,
    val emoji: String,
) {
    RanOutOfTime(
        code = "ran_out_of_time",
        label = "Ran out of time",
        emoji = "⏰",
    ),
    TooFar(
        code = "too_far",
        label = "Too far away",
        emoji = "📍",
    ),
    TooExpensive(
        code = "too_expensive",
        label = "More expensive than expected",
        emoji = "💸",
    ),
    PlaceUnavailable(
        code = "place_unavailable",
        label = "Place is closed or unavailable",
        emoji = "🔒",
    ),
    TooDifficult(
        code = "too_difficult",
        label = "Too difficult for today",
        emoji = "😅",
    ),
    WeatherOrMoodChanged(
        code = "weather_or_mood_changed",
        label = "Weather or mood changed",
        emoji = "🌧",
    ),
    Other(
        code = "other",
        label = "Something else",
        emoji = "💬",
    ),
}
