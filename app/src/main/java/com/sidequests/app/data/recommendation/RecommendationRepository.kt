package com.sidequests.app.data.recommendation

import com.sidequests.app.model.UserPreferences
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.postgrest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

interface RecommendationRepository {
    suspend fun recommend(
        availableMinutes: Int,
        preferences: UserPreferences,
        limit: Int = 3,
    ): Result<List<RecommendationResult>>
}

data class RecommendationResult(
    val questId: String,
    val score: Double,
)

@Serializable
private data class RecommendQuestParams(
    @SerialName("p_available_minutes")
    val availableMinutes: Int,
    @SerialName("p_social_level")
    val socialLevel: String,
    @SerialName("p_interests")
    val interests: List<String>,
    @SerialName("p_location_mode")
    val locationMode: String,
    @SerialName("p_limit")
    val limit: Int,
)

@Serializable
private data class RecommendationRowDto(
    @SerialName("quest_id")
    val questId: String,
    val score: Double,
)

class SupabaseRecommendationRepository(
    private val client: SupabaseClient,
) : RecommendationRepository {

    override suspend fun recommend(
        availableMinutes: Int,
        preferences: UserPreferences,
        limit: Int,
    ): Result<List<RecommendationResult>> = runCatching {
        withContext(Dispatchers.IO) {
            client.postgrest
                .rpc(
                    function = "recommend_quests",
                    parameters = RecommendQuestParams(
                        availableMinutes = availableMinutes,
                        socialLevel = preferences.socialLevel.name.lowercase(),
                        interests = preferences.interests.sorted(),
                        locationMode = preferences.locationMode,
                        limit = limit,
                    ),
                )
                .decodeList<RecommendationRowDto>()
                .map { RecommendationResult(it.questId, it.score) }
        }
    }
}
