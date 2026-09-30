package com.sidequests.app.domain

import com.sidequests.app.model.QuestProgress
import com.sidequests.app.model.QuestStep

data class StepCompletionDecision(
    val allowed: Boolean,
    val reason: String? = null,
)

object QuestStepCompletionPolicy {
    fun evaluate(
        stepIndex: Int,
        step: QuestStep,
        progress: QuestProgress,
    ): StepCompletionDecision {
        if (!step.requiresPhoto) {
            return StepCompletionDecision(allowed = true)
        }

        if (!progress.hasPhotoProof(stepIndex)) {
            return StepCompletionDecision(
                allowed = false,
                reason = "Capture the required photo proof before completing this step.",
            )
        }

        return StepCompletionDecision(allowed = true)
    }
}
