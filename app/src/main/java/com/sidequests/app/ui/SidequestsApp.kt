package com.sidequests.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sidequests.app.model.AppScreen
import com.sidequests.app.ui.auth.AuthScreen
import com.sidequests.app.ui.auth.AuthStage
import com.sidequests.app.ui.auth.AuthViewModel
import com.sidequests.app.ui.auth.MissingSupabaseConfigurationScreen
import com.sidequests.app.ui.theme.SidequestsTheme
import kotlinx.coroutines.delay

@Composable
fun SidequestsApp(
    appViewModel: AppViewModel = viewModel(),
    authViewModel: AuthViewModel = viewModel(),
) {
    val appState by appViewModel.uiState.collectAsStateWithLifecycle()
    val authState by authViewModel.uiState.collectAsStateWithLifecycle()

    SidequestsTheme(darkTheme = appState.darkMode) {
        when (authState.stage) {
            AuthStage.Initializing -> {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(MaterialTheme.colorScheme.background),
                    contentAlignment = Alignment.Center,
                ) {
                    CircularProgressIndicator()
                }
            }

            AuthStage.ConfigurationMissing -> {
                MissingSupabaseConfigurationScreen()
            }

            AuthStage.SignedOut -> {
                AuthScreen(
                    state = authState,
                    onSignIn = authViewModel::signIn,
                    onSignUp = authViewModel::signUp,
                    onClearFeedback = authViewModel::clearFeedback,
                )
            }

            AuthStage.SignedIn -> {
                AuthenticatedSidequestsContent(
                    state = appState,
                    viewModel = appViewModel,
                    onSignOut = authViewModel::signOut,
                )
            }
        }
    }
}

@Composable
private fun AuthenticatedSidequestsContent(
    state: com.sidequests.app.model.SidequestsUiState,
    viewModel: AppViewModel,
    onSignOut: () -> Unit,
) {
    LaunchedEffect(state.screen, state.contextualNotificationShown) {
        if (state.screen == AppScreen.Explorer && !state.contextualNotificationShown) {
            delay(4_000)
            viewModel.showContextualNotification()
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            Box(modifier = Modifier.weight(1f)) {
                when (state.screen) {
                    AppScreen.Onboarding -> OnboardingScreen(state, viewModel)
                    AppScreen.Explorer -> ExplorerScreen(state, viewModel)
                    AppScreen.QuestDetail -> QuestDetailScreen(
                        viewModel.selectedQuest(),
                        viewModel,
                    )
                    AppScreen.ActiveQuest -> ActiveQuestScreen(
                        quest = viewModel.activeQuest(),
                        progress = viewModel.activeProgress(),
                        viewModel = viewModel,
                    )
                    AppScreen.ExitFlow -> ExitFlowScreen(
                        quest = viewModel.activeQuest(),
                        progress = viewModel.activeProgress(),
                        viewModel = viewModel,
                    )
                    AppScreen.Rating -> RatingScreen(
                        viewModel.activeQuest(),
                        viewModel,
                    )
                    AppScreen.GroupQuest -> GroupQuestScreen(
                        viewModel.activeQuest(),
                        viewModel,
                    )
                    AppScreen.Profile -> ProfileScreen(
                        state = state,
                        viewModel = viewModel,
                        onSignOut = onSignOut,
                    )
                }
            }

            if (state.screen !in setOf(
                    AppScreen.Onboarding,
                    AppScreen.ExitFlow,
                    AppScreen.Rating,
                )
            ) {
                BottomBar(
                    screen = state.screen,
                    onNavigate = { target ->
                        if (target == AppScreen.ActiveQuest) {
                            viewModel.navigate(
                                if (viewModel.activeQuest().isGroup) {
                                    AppScreen.GroupQuest
                                } else {
                                    AppScreen.ActiveQuest
                                }
                            )
                        } else {
                            viewModel.navigate(target)
                        }
                    },
                )
            }
        }

        state.notificationQuestId?.let { questId ->
            ContextNotificationBanner(
                quest = viewModel.allQuests().first { it.id == questId },
                onOpen = viewModel::openNotificationQuest,
                onDismiss = viewModel::dismissNotification,
                modifier = Modifier
                    .zIndex(10f)
                    .padding(top = 28.dp),
            )
        }
    }
}
