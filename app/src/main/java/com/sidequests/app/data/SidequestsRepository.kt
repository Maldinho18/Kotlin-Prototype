package com.sidequests.app.data

import com.sidequests.app.model.LocationMode
import com.sidequests.app.model.Quest
import com.sidequests.app.model.UserPreferences

interface SidequestsRepository {
    fun allQuests(): List<Quest>
    fun questById(id: String): Quest
    fun categories(): List<String>
    fun recommendations(
        availableMinutes: Int,
        preferences: UserPreferences,
        skippedIds: Set<String>,
        completedIds: Set<String>,
        inProgressIds: Set<String>,
    ): List<Quest>
}

class InMemorySidequestsRepository : SidequestsRepository {
    override fun allQuests(): List<Quest> = QuestSeed.quests

    override fun questById(id: String): Quest =
        QuestSeed.quests.firstOrNull { it.id == id } ?: QuestSeed.quests.first()

    override fun categories(): List<String> = QuestSeed.categories

    override fun recommendations(
        availableMinutes: Int,
        preferences: UserPreferences,
        skippedIds: Set<String>,
        completedIds: Set<String>,
        inProgressIds: Set<String>,
    ): List<Quest> {
        return QuestSeed.quests
            .asSequence()
            .filterNot { it.id in completedIds || it.id in skippedIds || it.id in inProgressIds }
            .map { quest ->
                var score = 0.0
                if (quest.durationMinutes <= availableMinutes) score += 3
                if (
                    preferences.interests.isEmpty() ||
                    quest.category in preferences.interests ||
                    quest.tags.any { it.replaceFirstChar(Char::uppercase) in preferences.interests }
                ) score += 2
                if (quest.difficulty == preferences.difficulty) score += 2
                if (
                    preferences.locationMode == "all" ||
                    (preferences.locationMode == "gps" && quest.location == LocationMode.Gps) ||
                    (preferences.locationMode == "anywhere" && quest.location == LocationMode.Anywhere)
                ) score += 2
                if (quest.budgetAmount <= preferences.budgetMax) score += 1
                if (quest.isNew) score += 1
                if (quest.isSponsored) score += 0.5
                quest to score
            }
            .sortedByDescending { it.second }
            .take(4)
            .map { it.first }
            .toList()
    }
}
