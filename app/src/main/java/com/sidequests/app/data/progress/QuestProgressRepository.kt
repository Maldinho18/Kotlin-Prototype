package com.sidequests.app.data.progress

import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import java.time.Instant

interface QuestProgressRepository {
    suspend fun acceptQuest(questId: String, attemptId: String): Result<Unit>

    suspend fun updateProgress(
        attemptId: String,
        currentStep: Int,
        completedSteps: Set<Int>,
        completed: Boolean,
    ): Result<Unit>

    suspend fun saveProgress(
        attemptId: String,
        currentStep: Int,
        completedSteps: Set<Int>,
        reason: String?,
    ): Result<Unit>

    suspend fun abandonQuest(
        attemptId: String,
        currentStep: Int,
        completedSteps: Set<Int>,
        reason: String?,
    ): Result<Unit>

    suspend fun rateQuest(
        attemptId: String,
        stars: Int,
        tags: Set<String>,
    ): Result<Unit>
}

@Serializable
private data class UserQuestInsertDto(
    val id: String,
    @SerialName("user_id")
    val userId: String,
    @SerialName("quest_id")
    val questId: String,
    val status: String = "accepted",
    @SerialName("current_step")
    val currentStep: Int = 0,
    @SerialName("completed_steps")
    val completedSteps: List<Int> = emptyList(),
)

class SupabaseQuestProgressRepository(
    private val client: SupabaseClient,
) : QuestProgressRepository {

    override suspend fun acceptQuest(
        questId: String,
        attemptId: String,
    ): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            val userId = requireUserId()

            client.from("user_quests").insert(
                UserQuestInsertDto(
                    id = attemptId,
                    userId = userId,
                    questId = questId,
                )
            )
        }
    }

    override suspend fun updateProgress(
        attemptId: String,
        currentStep: Int,
        completedSteps: Set<Int>,
        completed: Boolean,
    ): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            client.from("user_quests").update({
                set("status", if (completed) "completed" else "in_progress")
                set("current_step", currentStep)
                set("completed_steps", completedSteps.sorted())
                if (completed) {
                    set("completed_at", Instant.now().toString())
                } else {
                    set("started_at", Instant.now().toString())
                }
            }) {
                filter {
                    eq("id", attemptId)
                }
            }
        }
    }

    override suspend fun saveProgress(
        attemptId: String,
        currentStep: Int,
        completedSteps: Set<Int>,
        reason: String?,
    ): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            client.from("user_quests").update({
                set("status", "in_progress")
                set("current_step", currentStep)
                set("completed_steps", completedSteps.sorted())
                set("abandon_reason", reason)
            }) {
                filter {
                    eq("id", attemptId)
                }
            }
        }
    }

    override suspend fun abandonQuest(
        attemptId: String,
        currentStep: Int,
        completedSteps: Set<Int>,
        reason: String?,
    ): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            client.from("user_quests").update({
                set("status", "abandoned")
                set("current_step", currentStep)
                set("completed_steps", completedSteps.sorted())
                set("abandon_reason", reason)
                set("abandoned_at", Instant.now().toString())
            }) {
                filter {
                    eq("id", attemptId)
                }
            }
        }
    }

    override suspend fun rateQuest(
        attemptId: String,
        stars: Int,
        tags: Set<String>,
    ): Result<Unit> = runCatching {
        withContext(Dispatchers.IO) {
            client.from("user_quests").update({
                set("rating", stars)
                set("feedback_tags", tags.sorted())
            }) {
                filter {
                    eq("id", attemptId)
                }
            }
        }
    }

    private fun requireUserId(): String =
        client.auth.currentUserOrNull()?.id
            ?: error("No authenticated Supabase user is available.")
}
