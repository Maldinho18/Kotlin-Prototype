package com.sidequests.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sidequests.app.data.InMemorySidequestsRepository
import com.sidequests.app.data.SidequestsRepository
import com.sidequests.app.data.SupabaseSidequestsRepository
import com.sidequests.app.data.remote.SupabaseProvider
import com.sidequests.app.data.progress.QuestProgressRepository
import com.sidequests.app.data.progress.SupabaseQuestProgressRepository
import com.sidequests.app.model.AppScreen
import com.sidequests.app.model.Quest
import com.sidequests.app.model.QuestDifficulty
import com.sidequests.app.model.QuestProgress
import com.sidequests.app.model.QuestRating
import com.sidequests.app.model.SidequestsUiState
import com.sidequests.app.model.SocialLevel
import com.sidequests.app.model.UserPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class AppViewModel(
    private val repository: SidequestsRepository =
        if (SupabaseProvider.isConfigured) {
            SupabaseSidequestsRepository(SupabaseProvider.client)
        } else {
            InMemorySidequestsRepository()
        },
    private val progressRepository: QuestProgressRepository? =
        if (SupabaseProvider.isConfigured) {
            SupabaseQuestProgressRepository(SupabaseProvider.client)
        } else {
            null
        },
) : ViewModel() {

    private val _uiState = MutableStateFlow(SidequestsUiState())
    val uiState: StateFlow<SidequestsUiState> = _uiState.asStateFlow()

    val categories: List<String> get() = repository.categories()

    fun allQuests(): List<Quest> = repository.allQuests()

    fun selectedQuest(): Quest = repository.questById(_uiState.value.selectedQuestId)

    fun activeQuest(): Quest = repository.questById(_uiState.value.activeQuestId)

    fun activeProgress(): QuestProgress =
        _uiState.value.progressByQuest[_uiState.value.activeQuestId] ?: QuestProgress()

    fun recommendations(): List<Quest> {
        val state = _uiState.value
        val completedIds = state.progressByQuest.filterValues { it.completedSteps.size >= 4 }.keys
        val inProgressIds = state.progressByQuest.filterValues { it.completedSteps.size in 1..3 && !it.abandoned }.keys
        return repository.recommendations(
            availableMinutes = state.availableTime,
            preferences = state.preferences,
            skippedIds = state.skippedQuestIds,
            completedIds = completedIds,
            inProgressIds = inProgressIds,
        )
    }


    fun refreshQuestCatalog() {
        if (_uiState.value.catalogLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    catalogLoading = true,
                    catalogError = null,
                )
            }

            repository.refresh()
                .onSuccess { count ->
                    _uiState.update {
                        it.copy(
                            catalogLoading = false,
                            catalogSource = "Supabase · $count quests",
                            catalogError = null,
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            catalogLoading = false,
                            catalogSource = "Local fallback",
                            catalogError = throwable.message ?: "Remote catalogue unavailable.",
                        )
                    }
                }
        }
    }

    fun navigate(screen: AppScreen) {
        _uiState.update { it.copy(screen = screen) }
    }

    fun openQuestDetail(questId: String) {
        _uiState.update { it.copy(selectedQuestId = questId, screen = AppScreen.QuestDetail) }
    }

    fun acceptQuest(questId: String) {
        val quest = repository.questById(questId)
        val existingAttemptId = _uiState.value.attemptIdByQuest[questId]
        val attemptId = existingAttemptId ?: UUID.randomUUID().toString()

        _uiState.update { state ->
            val progressMap = if (questId in state.progressByQuest) {
                state.progressByQuest
            } else {
                state.progressByQuest + (questId to QuestProgress())
            }

            state.copy(
                activeQuestId = questId,
                progressByQuest = progressMap,
                attemptIdByQuest = state.attemptIdByQuest + (questId to attemptId),
                screen = if (quest.isGroup) AppScreen.GroupQuest else AppScreen.ActiveQuest,
                progressSyncError = null,
                progressSyncMessage = null,
            )
        }

        if (existingAttemptId == null) {
            syncProgress("Saving accepted quest…") {
                progressRepository?.acceptQuest(questId, attemptId)
                    ?: Result.success(Unit)
            }
        }
    }

    fun skipQuest(questId: String) {
        _uiState.update { it.copy(skippedQuestIds = it.skippedQuestIds + questId) }
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(darkMode = !it.darkMode) }
    }

    fun setAvailableTime(minutes: Int) {
        _uiState.update { it.copy(availableTime = minutes) }
    }

    fun setCategory(category: String) {
        _uiState.update { it.copy(selectedCategory = category) }
    }

    fun nextOnboardingStep() {
        _uiState.update { state ->
            if (state.onboardingStep < 3) state.copy(onboardingStep = state.onboardingStep + 1)
            else state.copy(screen = AppScreen.Explorer)
        }
    }

    fun skipOnboarding() {
        _uiState.update { it.copy(preferences = UserPreferences(), screen = AppScreen.Explorer) }
    }

    fun toggleInterest(interest: String) {
        _uiState.update { state ->
            val next = state.preferences.interests.toMutableSet().apply {
                if (!add(interest)) remove(interest)
            }
            state.copy(preferences = state.preferences.copy(interests = next))
        }
    }

    fun setDifficulty(difficulty: QuestDifficulty) {
        _uiState.update { it.copy(preferences = it.preferences.copy(difficulty = difficulty)) }
    }

    fun setTypicalTime(minutes: Int) {
        _uiState.update { state ->
            state.copy(
                availableTime = minutes,
                preferences = state.preferences.copy(typicalTime = minutes),
            )
        }
    }

    fun setBudget(amount: Int) {
        _uiState.update { it.copy(preferences = it.preferences.copy(budgetMax = amount)) }
    }

    fun setSocialLevel(level: SocialLevel) {
        _uiState.update { it.copy(preferences = it.preferences.copy(socialLevel = level)) }
    }

    fun setLocationMode(mode: String) {
        _uiState.update { it.copy(preferences = it.preferences.copy(locationMode = mode)) }
    }

    fun completeCurrentStep() {
        val state = _uiState.value
        val questId = state.activeQuestId
        val quest = repository.questById(questId)
        val current = state.progressByQuest[questId] ?: QuestProgress()
        val nextCompleted = current.completedSteps + current.currentStep
        val completed = nextCompleted.size >= quest.steps.size
        val nextStep = if (current.currentStep < quest.steps.lastIndex) {
            current.currentStep + 1
        } else {
            current.currentStep
        }

        _uiState.update {
            it.copy(
                progressByQuest = it.progressByQuest + (
                    questId to current.copy(
                        currentStep = nextStep,
                        completedSteps = nextCompleted,
                    )
                ),
                progressSyncError = null,
            )
        }

        val attemptId = _uiState.value.attemptIdByQuest[questId] ?: return
        syncProgress(if (completed) "Saving completed quest…" else "Saving progress…") {
            progressRepository?.updateProgress(
                attemptId = attemptId,
                currentStep = nextStep,
                completedSteps = nextCompleted,
                completed = completed,
            ) ?: Result.success(Unit)
        }
    }

    fun jumpToStep(step: Int) {
        val state = _uiState.value
        val questId = state.activeQuestId
        val current = state.progressByQuest[questId] ?: QuestProgress()
        _uiState.update {
            it.copy(progressByQuest = it.progressByQuest + (questId to current.copy(currentStep = step.coerceIn(0, 3))))
        }
    }

    fun markPhotoProof() {
        val state = _uiState.value
        val questId = state.activeQuestId
        val current = state.progressByQuest[questId] ?: QuestProgress()
        _uiState.update {
            it.copy(progressByQuest = it.progressByQuest + (questId to current.copy(hasPhotoProof = true)))
        }
    }

    fun saveAndExit(reason: String?) {
        val state = _uiState.value
        val questId = state.activeQuestId
        val current = state.progressByQuest[questId] ?: QuestProgress()

        _uiState.update {
            it.copy(
                progressByQuest = it.progressByQuest + (
                    questId to current.copy(abandonReason = reason)
                ),
                screen = AppScreen.Explorer,
                progressSyncError = null,
            )
        }

        val attemptId = state.attemptIdByQuest[questId] ?: return
        syncProgress("Saving quest progress…") {
            progressRepository?.saveProgress(
                attemptId = attemptId,
                currentStep = current.currentStep,
                completedSteps = current.completedSteps,
                reason = reason,
            ) ?: Result.success(Unit)
        }
    }

    fun startAnotherQuest() {
        val state = _uiState.value
        val questId = state.activeQuestId
        val current = state.progressByQuest[questId] ?: QuestProgress()

        _uiState.update {
            it.copy(
                progressByQuest = it.progressByQuest + (
                    questId to current.copy(abandoned = true)
                ),
                screen = AppScreen.Explorer,
                progressSyncError = null,
            )
        }

        val attemptId = state.attemptIdByQuest[questId] ?: return
        syncProgress("Saving abandonment…") {
            progressRepository?.abandonQuest(
                attemptId = attemptId,
                currentStep = current.currentStep,
                completedSteps = current.completedSteps,
                reason = current.abandonReason,
            ) ?: Result.success(Unit)
        }
    }

    fun submitRating(stars: Int, tags: Set<String>) {
        val state = _uiState.value
        val questId = state.activeQuestId

        _uiState.update {
            it.copy(
                ratingsByQuest = it.ratingsByQuest + (
                    questId to QuestRating(stars, tags)
                ),
                screen = AppScreen.Explorer,
                progressSyncError = null,
            )
        }

        val attemptId = state.attemptIdByQuest[questId] ?: return
        syncProgress("Saving rating…") {
            progressRepository?.rateQuest(
                attemptId = attemptId,
                stars = stars,
                tags = tags,
            ) ?: Result.success(Unit)
        }
    }

    private fun syncProgress(
        message: String,
        operation: suspend () -> Result<Unit>,
    ) {
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    progressSyncing = true,
                    progressSyncError = null,
                    progressSyncMessage = message,
                )
            }

            operation()
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            progressSyncing = false,
                            progressSyncError = null,
                            progressSyncMessage = "Progress synced with Supabase",
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            progressSyncing = false,
                            progressSyncError = throwable.message ?: "Progress could not be synced.",
                            progressSyncMessage = "Saved locally; remote sync failed",
                        )
                    }
                }
        }
    }

    fun showContextualNotification() {
        _uiState.update {
            if (it.contextualNotificationShown) it
            else it.copy(notificationQuestId = "sponsored-climbing", contextualNotificationShown = true)
        }
    }

    fun dismissNotification() {
        _uiState.update { it.copy(notificationQuestId = null) }
    }

    fun openNotificationQuest() {
        val questId = _uiState.value.notificationQuestId ?: return
        _uiState.update {
            it.copy(
                selectedQuestId = questId,
                notificationQuestId = null,
                screen = AppScreen.QuestDetail,
            )
        }
    }
}
