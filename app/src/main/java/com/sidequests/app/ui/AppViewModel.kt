package com.sidequests.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sidequests.app.analytics.ContextAnalytics
import com.sidequests.app.context.ContextManager
import com.sidequests.app.context.QuestLocationMode
import com.sidequests.app.data.InMemorySidequestsRepository
import com.sidequests.app.data.SidequestsRepository
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
    private val repository: SidequestsRepository = InMemorySidequestsRepository(),
    private val contextManager: ContextManager? = null,
    private val analytics: ContextAnalytics? = null,
) : ViewModel() {

    private val _uiState = MutableStateFlow(SidequestsUiState())

    /**
     * One id per app ViewModel/session. BQ10 uses distinct session ids
     * to calculate the proportion of sessions using each location mode.
     */
    val sessionId: String = UUID.randomUUID().toString()

    private var latestContext: com.sidequests.app.context.UserContext? = null

    init {
        refreshContext(trackSessionStart = true)
    }

    private fun refreshContext(trackSessionStart: Boolean) {
        val manager = contextManager ?: return

        viewModelScope.launch {
            latestContext = manager.getCurrentContext()

            if (trackSessionStart) {
                val context = latestContext ?: return@launch
                analytics?.trackSessionStarted(
                    sessionId = sessionId,
                    timeOfDay = context.timeOfDay,
                    weatherCondition = context.weather?.condition,
                )
            }
        }
    }

    fun refreshContextAfterPermission() {
        refreshContext(trackSessionStart = false)
    }
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

    fun navigate(screen: AppScreen) {
        _uiState.update { it.copy(screen = screen) }
    }

    fun openQuestDetail(questId: String) {
        _uiState.update { it.copy(selectedQuestId = questId, screen = AppScreen.QuestDetail) }
    }

    fun acceptQuest(questId: String) {
        val quest = repository.questById(questId)
        _uiState.update { state ->
            val progressMap = if (questId in state.progressByQuest) state.progressByQuest
            else state.progressByQuest + (questId to QuestProgress())
            state.copy(
                activeQuestId = questId,
                progressByQuest = progressMap,
                screen = if (quest.isGroup) AppScreen.GroupQuest else AppScreen.ActiveQuest,
            )
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
        _uiState.update {
            it.copy(preferences = it.preferences.copy(locationMode = mode))
        }

        val contextManager = contextManager ?: return
        val analytics = analytics ?: return

        viewModelScope.launch {
            val context = contextManager.getCurrentContext()
            latestContext = context

            val eventMode = when (mode) {
                "gps" -> QuestLocationMode.LOCATION_BASED
                "anywhere" -> QuestLocationMode.LOCATION_INDEPENDENT
                else -> QuestLocationMode.ALL
            }

            analytics.trackLocationModeSelected(
                sessionId = sessionId,
                mode = eventMode,
                timeOfDay = context.timeOfDay,
                weatherCondition = context.weather?.condition,
            )
        }
    }

    fun completeCurrentStep() {
        val state = _uiState.value
        val questId = state.activeQuestId
        val current = state.progressByQuest[questId] ?: QuestProgress()
        val nextCompleted = current.completedSteps + current.currentStep
        val nextStep = if (current.currentStep < 3) current.currentStep + 1 else current.currentStep
        _uiState.update {
            it.copy(
                progressByQuest = it.progressByQuest + (
                    questId to current.copy(currentStep = nextStep, completedSteps = nextCompleted)
                )
            )
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
                progressByQuest = it.progressByQuest + (questId to current.copy(abandonReason = reason)),
                screen = AppScreen.Explorer,
            )
        }
    }

    fun startAnotherQuest() {
        val state = _uiState.value
        val questId = state.activeQuestId
        val current = state.progressByQuest[questId] ?: QuestProgress()
        _uiState.update {
            it.copy(
                progressByQuest = it.progressByQuest + (questId to current.copy(abandoned = true)),
                screen = AppScreen.Explorer,
            )
        }
    }

    fun submitRating(stars: Int, tags: Set<String>) {
        val state = _uiState.value
        _uiState.update {
            it.copy(
                ratingsByQuest = it.ratingsByQuest + (state.activeQuestId to QuestRating(stars, tags)),
                screen = AppScreen.Explorer,
            )
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
