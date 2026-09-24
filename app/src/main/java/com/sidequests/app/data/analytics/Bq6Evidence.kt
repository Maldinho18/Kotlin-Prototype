package com.sidequests.app.data.analytics

import com.sidequests.app.model.AbandonmentReason
import com.sidequests.app.model.LocationMode
import com.sidequests.app.model.Quest
import com.sidequests.app.model.QuestProgress
import kotlin.math.roundToInt

data class Bq6AbandonmentEvidence(
    val reasonCode: String,
    val reasonLabel: String,
    val questDurationMinutes: Int,
    val estimatedCost: Int,
    val questDistanceLabel: String,
    val questDistanceMeters: Int?,
    val questDistanceBand: String,
    val currentStepIndex: Int,
    val completedStepCount: Int,
    val totalStepCount: Int,
    val progressPercent: Int,
    val hadPhotoProof: Boolean,
    val uploadedPhotoProofCount: Int,
)

fun buildBq6AbandonmentEvidence(
    reason: AbandonmentReason,
    quest: Quest,
    progress: QuestProgress,
): Bq6AbandonmentEvidence {
    val distanceMeters = quest.estimatedDistanceMeters()
    val totalSteps = quest.steps.size

    return Bq6AbandonmentEvidence(
        reasonCode = reason.code,
        reasonLabel = reason.label,
        questDurationMinutes = quest.durationMinutes,
        estimatedCost = quest.budgetAmount,
        questDistanceLabel = quest.distance,
        questDistanceMeters = distanceMeters,
        questDistanceBand = quest.distanceBand(distanceMeters),
        currentStepIndex = progress.currentStep,
        completedStepCount = progress.completedSteps.size,
        totalStepCount = totalSteps,
        progressPercent = if (totalSteps == 0) {
            0
        } else {
            (progress.completedSteps.size * 100.0 / totalSteps).roundToInt()
        },
        hadPhotoProof = progress.localPhotoProofByStep.isNotEmpty(),
        uploadedPhotoProofCount = progress.uploadedPhotoProofByStep.size,
    )
}

internal fun Quest.estimatedDistanceMeters(): Int? {
    val match = DISTANCE_PATTERN.find(distance.lowercase()) ?: return null
    val amount = match.groupValues[1].replace(',', '.').toDoubleOrNull() ?: return null
    val multiplier = when (match.groupValues[2]) {
        "mi", "mile", "miles" -> METERS_PER_MILE
        "km", "kilometer", "kilometers" -> 1_000.0
        "m", "meter", "meters" -> 1.0
        else -> return null
    }
    return (amount * multiplier).roundToInt()
}

private fun Quest.distanceBand(distanceMeters: Int?): String = when {
    location == LocationMode.Anywhere -> "location_independent"
    distance.equals("on campus", ignoreCase = true) -> "on_campus"
    distanceMeters == null -> "unknown"
    distanceMeters < 500 -> "under_500_m"
    distanceMeters < 1_500 -> "500_m_to_1_5_km"
    else -> "over_1_5_km"
}

private val DISTANCE_PATTERN = Regex(
    """([0-9]+(?:[.,][0-9]+)?)\s*(mi|mile|miles|km|kilometer|kilometers|m|meter|meters)\b"""
)
private const val METERS_PER_MILE = 1_609.344
