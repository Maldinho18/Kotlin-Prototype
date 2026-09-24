package com.sidequests.app.data.analytics

import com.sidequests.app.model.AbandonmentReason
import com.sidequests.app.model.LocationMode
import com.sidequests.app.model.Quest
import com.sidequests.app.model.QuestDifficulty
import com.sidequests.app.model.QuestProgress
import com.sidequests.app.model.QuestStep
import com.sidequests.app.model.SocialLevel
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class Bq6EvidenceTest {
    @Test
    fun `builds structured BQ6 evidence from the quest and lifecycle state`() {
        val quest = quest(distance = "0.4 mi")
        val progress = QuestProgress(
            currentStep = 2,
            completedSteps = setOf(0, 1),
            localPhotoProofByStep = mapOf(1 to "local.jpg"),
            uploadedPhotoProofByStep = mapOf(1 to "user/attempt/proof.jpg"),
        )

        val evidence = buildBq6AbandonmentEvidence(
            reason = AbandonmentReason.TooFar,
            quest = quest,
            progress = progress,
        )

        assertEquals("too_far", evidence.reasonCode)
        assertEquals(45, evidence.questDurationMinutes)
        assertEquals(12, evidence.estimatedCost)
        assertEquals(644, evidence.questDistanceMeters)
        assertEquals("500_m_to_1_5_km", evidence.questDistanceBand)
        assertEquals(50, evidence.progressPercent)
        assertTrue(evidence.hadPhotoProof)
        assertEquals(1, evidence.uploadedPhotoProofCount)
    }

    @Test
    fun `classifies location-independent quests without inventing a distance`() {
        val quest = quest(distance = "Anywhere", location = LocationMode.Anywhere)

        val evidence = buildBq6AbandonmentEvidence(
            reason = AbandonmentReason.RanOutOfTime,
            quest = quest,
            progress = QuestProgress(),
        )

        assertNull(evidence.questDistanceMeters)
        assertEquals("location_independent", evidence.questDistanceBand)
        assertFalse(evidence.hadPhotoProof)
    }

    private fun quest(
        distance: String,
        location: LocationMode = LocationMode.Gps,
    ) = Quest(
        id = "test-quest",
        title = "Test quest",
        category = "Outdoors",
        emoji = "🧭",
        duration = "45 min",
        durationMinutes = 45,
        distance = distance,
        budget = "\$12",
        budgetAmount = 12,
        difficulty = QuestDifficulty.Easy,
        location = location,
        socialLevel = SocialLevel.Solo,
        description = "Test",
        steps = List(4) { index -> QuestStep("Step $index", "Test") },
        tags = emptyList(),
    )
}
