package com.sidequests.app.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sidequests.app.BuildConfig
import com.sidequests.app.data.InMemorySidequestsRepository
import com.sidequests.app.data.SidequestsRepository
import com.sidequests.app.data.SupabaseSidequestsRepository
import com.sidequests.app.data.remote.SupabaseProvider
import com.sidequests.app.data.progress.QuestProgressRepository
import com.sidequests.app.data.progress.SupabaseQuestProgressRepository
import com.sidequests.app.data.analytics.AnalyticsRepository
import com.sidequests.app.data.analytics.buildBq6AbandonmentEvidence
import com.sidequests.app.data.analytics.SupabaseAnalyticsRepository
import com.sidequests.app.data.photo.PhotoProofRepository
import com.sidequests.app.data.photo.SupabasePhotoProofRepository
import com.sidequests.app.data.recommendation.RecommendationRepository
import com.sidequests.app.data.recommendation.SupabaseRecommendationRepository
import com.sidequests.app.model.AppScreen
import com.sidequests.app.model.AbandonmentReason
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
import java.io.File
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

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
    private val analyticsRepository: AnalyticsRepository? =
        if (SupabaseProvider.isConfigured) {
            SupabaseAnalyticsRepository(SupabaseProvider.client)
        } else {
            null
        },
    private val recommendationRepository: RecommendationRepository? =
        if (SupabaseProvider.isConfigured) {
            SupabaseRecommendationRepository(SupabaseProvider.client)
        } else {
            null
        },
    private val photoProofRepository: PhotoProofRepository? =
        if (SupabaseProvider.isConfigured) {
            SupabasePhotoProofRepository(
                client = SupabaseProvider.client,
                bucketName = BuildConfig.SUPABASE_QUEST_PROOFS_BUCKET,
            )
        } else {
            null
        },
) : ViewModel() {

    private val analyticsSessionId = UUID.randomUUID().toString()

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

        if (state.remoteRecommendationIds.isNotEmpty()) {
            return state.remoteRecommendationIds.mapNotNull { id ->
                repository.allQuests().firstOrNull { it.id == id }
            }
        }

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
                    refreshRecommendations()
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

    fun refreshRecommendations() {
        val remote = recommendationRepository ?: return
        val state = _uiState.value
        if (state.recommendationLoading) return

        viewModelScope.launch {
            _uiState.update {
                it.copy(recommendationLoading = true, recommendationError = null)
            }

            remote.recommend(
                availableMinutes = state.availableTime,
                preferences = state.preferences,
                excludedQuestIds = state.skippedQuestIds,
                limit = 3,
            )
                .onSuccess { results ->
                    val ids = results.map { it.questId }
                    _uiState.update {
                        it.copy(
                            recommendationLoading = false,
                            remoteRecommendationIds = ids,
                            recommendationSource = "Supabase BQ5 · " + ids.size + " quests",
                            recommendationError = null,
                        )
                    }

                    ids.forEachIndexed { index, id ->
                        repository.allQuests().firstOrNull { it.id == id }?.let { quest ->
                            trackEvent(
                                eventType = "recommendation_shown",
                                quest = quest,
                                metadata = buildJsonObject {
                                    put("rank", index + 1)
                                    put("source", "bq5_rpc")
                                },
                            )
                        }
                    }
                }
                .onFailure { throwable ->
                    _uiState.update {
                        it.copy(
                            recommendationLoading = false,
                            remoteRecommendationIds = emptyList(),
                            recommendationSource = "Local fallback",
                            recommendationError = throwable.message ?: "Shared recommendation unavailable.",
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
        val currentState = _uiState.value
        val existingProgress = currentState.progressByQuest[questId]
        val previousAttemptFinished = existingProgress?.let { progress ->
            progress.abandoned || progress.completedSteps.size >= quest.steps.size
        } ?: false
        val existingAttemptId = currentState.attemptIdByQuest[questId]
        val createsNewAttempt = existingAttemptId == null || previousAttemptFinished
        val attemptId = if (createsNewAttempt) {
            UUID.randomUUID().toString()
        } else {
            requireNotNull(existingAttemptId)
        }

        _uiState.update { state ->
            val progressMap = if (questId in state.progressByQuest && !previousAttemptFinished) {
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

        if (createsNewAttempt) {
            syncProgress("Saving accepted quest…") {
                progressRepository?.acceptQuest(questId, attemptId)
                    ?: Result.success(Unit)
            }
            trackEvent("quest_accepted", quest)
        }
    }

    fun skipQuest(questId: String) {
        val quest = repository.questById(questId)
        _uiState.update {
            it.copy(
                skippedQuestIds = it.skippedQuestIds + questId,
                remoteRecommendationIds = it.remoteRecommendationIds.filterNot { id -> id == questId },
            )
        }
        trackEvent("recommendation_skipped", quest)
        refreshRecommendations()
    }

    fun toggleDarkMode() {
        _uiState.update { it.copy(darkMode = !it.darkMode) }
    }

    fun setAvailableTime(minutes: Int) {
        _uiState.update { it.copy(availableTime = minutes, remoteRecommendationIds = emptyList()) }
        refreshRecommendations()
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
            state.copy(
                preferences = state.preferences.copy(interests = next),
                remoteRecommendationIds = emptyList(),
            )
        }
        refreshRecommendations()
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
        _uiState.update {
            it.copy(
                preferences = it.preferences.copy(socialLevel = level),
                remoteRecommendationIds = emptyList(),
            )
        }
        refreshRecommendations()
    }

    fun setLocationMode(mode: String) {
        _uiState.update {
            it.copy(
                preferences = it.preferences.copy(locationMode = mode),
                remoteRecommendationIds = emptyList(),
            )
        }
        trackEvent(
            eventType = "location_mode_selected",
            metadata = buildJsonObject { put("mode", mode) },
        )
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

        if (current.completedSteps.isEmpty() && nextCompleted.isNotEmpty()) {
            trackEvent("quest_started", quest)
        }
        if (completed) {
            trackEvent("quest_completed", quest)
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

    fun markPhotoProofCaptured(stepIndex: Int, localPath: String) {
        val state = _uiState.value
        val questId = state.activeQuestId
        val quest = repository.questById(questId)
        val current = state.progressByQuest[questId] ?: QuestProgress()
        _uiState.update {
            it.copy(
                progressByQuest = it.progressByQuest + (
                    questId to current.copy(
                        localPhotoProofByStep = current.localPhotoProofByStep + (stepIndex to localPath),
                        uploadedPhotoProofByStep = current.uploadedPhotoProofByStep - stepIndex,
                        photoProofError = null,
                    )
                )
            )
        }

        val attemptId = state.attemptIdByQuest[questId]
        val photoFile = File(localPath)
        trackEvent(
            eventType = "photo_proof_captured",
            quest = quest,
            metadata = buildJsonObject {
                put("step_index", stepIndex)
                put("byte_count", photoFile.length())
                put("verification_type", "photo")
                put("stored_locally", true)
                if (attemptId != null) put("attempt_id", attemptId)
            },
        )

        val remote = photoProofRepository
        if (attemptId != null && remote != null) {
            uploadPhotoProof(
                quest = quest,
                attemptId = attemptId,
                stepIndex = stepIndex,
                photoFile = photoFile,
                remote = remote,
            )
        }
    }

    fun retryPhotoProofUpload(stepIndex: Int) {
        val state = _uiState.value
        val questId = state.activeQuestId
        val attemptId = state.attemptIdByQuest[questId] ?: return
        val localPath = state.progressByQuest[questId]
            ?.localPhotoProofByStep
            ?.get(stepIndex)
            ?: return
        val remote = photoProofRepository ?: return

        uploadPhotoProof(
            quest = repository.questById(questId),
            attemptId = attemptId,
            stepIndex = stepIndex,
            photoFile = File(localPath),
            remote = remote,
        )
    }

    fun reportPhotoProofError(message: String) {
        val state = _uiState.value
        val questId = state.activeQuestId
        val current = state.progressByQuest[questId] ?: QuestProgress()
        _uiState.update {
            it.copy(
                progressByQuest = it.progressByQuest + (
                    questId to current.copy(photoProofError = message)
                )
            )
        }
    }

    private fun uploadPhotoProof(
        quest: Quest,
        attemptId: String,
        stepIndex: Int,
        photoFile: File,
        remote: PhotoProofRepository,
    ) {
        val questId = quest.id
        _uiState.update { state ->
            val current = state.progressByQuest[questId] ?: QuestProgress()
            state.copy(
                progressByQuest = state.progressByQuest + (
                    questId to current.copy(
                        photoProofUploadingStep = stepIndex,
                        photoProofError = null,
                    )
                )
            )
        }

        viewModelScope.launch {
            remote.upload(
                attemptId = attemptId,
                questId = questId,
                stepIndex = stepIndex,
                photoFile = photoFile,
            )
                .onSuccess { upload ->
                    _uiState.update { state ->
                        val current = state.progressByQuest[questId] ?: QuestProgress()
                        state.copy(
                            progressByQuest = state.progressByQuest + (
                                questId to current.copy(
                                    uploadedPhotoProofByStep = current.uploadedPhotoProofByStep +
                                        (stepIndex to upload.storagePath),
                                    photoProofUploadingStep = null,
                                    photoProofError = null,
                                )
                            )
                        )
                    }
                    trackEvent(
                        eventType = "photo_proof_uploaded",
                        quest = quest,
                        metadata = buildJsonObject {
                            put("attempt_id", attemptId)
                            put("step_index", stepIndex)
                            put("byte_count", upload.byteCount)
                            put("storage_path", upload.storagePath)
                            put("verification_type", "photo")
                        },
                    )
                }
                .onFailure {
                    _uiState.update { state ->
                        val current = state.progressByQuest[questId] ?: QuestProgress()
                        state.copy(
                            progressByQuest = state.progressByQuest + (
                                questId to current.copy(
                                    photoProofUploadingStep = null,
                                    photoProofError = "Photo captured locally, but upload failed. Retry when connected.",
                                )
                            )
                        )
                    }
                    trackEvent(
                        eventType = "photo_proof_upload_failed",
                        quest = quest,
                        metadata = buildJsonObject {
                            put("attempt_id", attemptId)
                            put("step_index", stepIndex)
                            put("verification_type", "photo")
                        },
                    )
                }
        }
    }

    fun saveAndExit() {
        val state = _uiState.value
        val questId = state.activeQuestId
        val current = state.progressByQuest[questId] ?: QuestProgress()

        _uiState.update {
            it.copy(
                progressByQuest = it.progressByQuest + (
                    questId to current.copy(abandonReason = null)
                ),
                screen = AppScreen.Explorer,
                progressSyncError = null,
            )
        }

        trackEvent(
            eventType = "quest_progress_saved",
            quest = repository.questById(questId),
            metadata = buildJsonObject {
                state.attemptIdByQuest[questId]?.let { put("attempt_id", it) }
                put("current_step_index", current.currentStep)
                put("completed_step_count", current.completedSteps.size)
            },
        )

        val attemptId = state.attemptIdByQuest[questId] ?: return
        syncProgress("Saving quest progress…") {
            progressRepository?.saveProgress(
                attemptId = attemptId,
                currentStep = current.currentStep,
                completedSteps = current.completedSteps,
                reason = null,
            ) ?: Result.success(Unit)
        }
    }

    fun startAnotherQuest(reason: AbandonmentReason) {
        val state = _uiState.value
        val questId = state.activeQuestId
        val quest = repository.questById(questId)
        val current = state.progressByQuest[questId] ?: QuestProgress()
        val evidence = buildBq6AbandonmentEvidence(reason, quest, current)
        val attemptId = state.attemptIdByQuest[questId]

        _uiState.update {
            it.copy(
                progressByQuest = it.progressByQuest + (
                    questId to current.copy(
                        abandoned = true,
                        abandonReason = reason,
                    )
                ),
                skippedQuestIds = it.skippedQuestIds + questId,
                attemptIdByQuest = it.attemptIdByQuest - questId,
                screen = AppScreen.Explorer,
                progressSyncError = null,
            )
        }

        trackEvent(
            eventType = "quest_abandoned",
            quest = quest,
            metadata = buildJsonObject {
                put("schema_version", 1)
                put("reason", evidence.reasonCode)
                put("reason_code", evidence.reasonCode)
                put("reason_label", evidence.reasonLabel)
                put("quest_duration_minutes", evidence.questDurationMinutes)
                put("estimated_cost", evidence.estimatedCost)
                put("quest_distance_label", evidence.questDistanceLabel)
                evidence.questDistanceMeters?.let { put("quest_distance_meters", it) }
                put("quest_distance_band", evidence.questDistanceBand)
                put("current_step_index", evidence.currentStepIndex)
                put("completed_step_count", evidence.completedStepCount)
                put("total_step_count", evidence.totalStepCount)
                put("progress_percent", evidence.progressPercent)
                put("had_photo_proof", evidence.hadPhotoProof)
                put("uploaded_photo_proof_count", evidence.uploadedPhotoProofCount)
                if (attemptId != null) put("attempt_id", attemptId)
            },
        )

        if (attemptId == null) return
        syncProgress("Saving abandonment…") {
            progressRepository?.abandonQuest(
                attemptId = attemptId,
                currentStep = current.currentStep,
                completedSteps = current.completedSteps,
                reason = reason.code,
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

    private fun trackEvent(
        eventType: String,
        quest: Quest? = null,
        metadata: kotlinx.serialization.json.JsonObject = kotlinx.serialization.json.JsonObject(emptyMap()),
    ) {
        val analytics = analyticsRepository ?: return
        val state = _uiState.value

        viewModelScope.launch {
            analytics.track(
                sessionId = analyticsSessionId,
                eventType = eventType,
                availableMinutes = state.availableTime,
                preferences = state.preferences,
                quest = quest,
                metadata = metadata,
            )
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
