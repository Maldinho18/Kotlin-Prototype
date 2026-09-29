package com.sidequests.app.domain

import com.sidequests.app.model.QuestProgress
import com.sidequests.app.model.QuestStep
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class QuestStepCompletionPolicyTest {

    @Test
    fun nonPhotoStepCanCompleteWithoutProof() {
        val decision = QuestStepCompletionPolicy.evaluate(
            stepIndex = 0,
            step = QuestStep("Walk there", "Reach the location", requiresPhoto = false),
            progress = QuestProgress(),
        )

        assertTrue(decision.allowed)
        assertNull(decision.reason)
    }

    @Test
    fun photoStepCannotCompleteBeforeCameraProofExists() {
        val decision = QuestStepCompletionPolicy.evaluate(
            stepIndex = 1,
            step = QuestStep("Take a photo", "Capture proof", requiresPhoto = true),
            progress = QuestProgress(),
        )

        assertFalse(decision.allowed)
    }

    @Test
    fun photoStepCanCompleteAfterCameraProofExists() {
        val decision = QuestStepCompletionPolicy.evaluate(
            stepIndex = 1,
            step = QuestStep("Take a photo", "Capture proof", requiresPhoto = true),
            progress = QuestProgress(
                localPhotoProofByStep = mapOf(1 to "/tmp/proof.jpg"),
            ),
        )

        assertTrue(decision.allowed)
    }
}
