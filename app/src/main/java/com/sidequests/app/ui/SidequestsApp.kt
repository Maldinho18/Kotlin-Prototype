package com.sidequests.app.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.zIndex
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.sidequests.app.model.AppScreen
import com.sidequests.app.ui.theme.SidequestsTheme
import kotlinx.coroutines.delay

@Composable
fun SidequestsApp(
    appViewModel: AppViewModel = viewModel(),
) {
    val state by appViewModel.uiState.collectAsStateWithLifecycle()

    SidequestsTheme(darkTheme = state.darkMode) {
        LaunchedEffect(state.screen, state.contextualNotificationShown) {
            if (state.screen == AppScreen.Explorer && !state.contextualNotificationShown) {
                delay(4_000)
                appViewModel.showContextualNotification()
            }
        }

        Box(modifier = Modifier.fillMaxSize().background(MaterialTheme.colorScheme.background)) {
            Column(modifier = Modifier.fillMaxSize()) {
                Box(modifier = Modifier.weight(1f)) {
                    when (state.screen) {
                        AppScreen.Onboarding -> OnboardingScreen(state, appViewModel)
                        AppScreen.Explorer -> ExplorerScreen(state, appViewModel)
                        AppScreen.QuestDetail -> QuestDetailScreen(appViewModel.selectedQuest(), appViewModel)
                        AppScreen.ActiveQuest -> ActiveQuestScreen(
                            quest = appViewModel.activeQuest(),
                            progress = appViewModel.activeProgress(),
                            viewModel = appViewModel,
                        )
                        AppScreen.ExitFlow -> ExitFlowScreen(
                            quest = appViewModel.activeQuest(),
                            progress = appViewModel.activeProgress(),
                            viewModel = appViewModel,
                        )
                        AppScreen.Rating -> RatingScreen(appViewModel.activeQuest(), appViewModel)
                        AppScreen.GroupQuest -> GroupQuestScreen(appViewModel.activeQuest(), appViewModel)
                        AppScreen.Profile -> ProfileScreen(state, appViewModel)
                    }
                }

                if (state.screen !in setOf(AppScreen.Onboarding, AppScreen.ExitFlow, AppScreen.Rating)) {
                    BottomBar(
                        screen = state.screen,
                        onNavigate = { target ->
                            if (target == AppScreen.ActiveQuest) {
                                appViewModel.navigate(
                                    if (appViewModel.activeQuest().isGroup) AppScreen.GroupQuest else AppScreen.ActiveQuest
                                )
                            } else {
                                appViewModel.navigate(target)
                            }
                        },
                    )
                }
            }

            state.notificationQuestId?.let { questId ->
                ContextNotificationBanner(
                    quest = appViewModel.allQuests().first { it.id == questId },
                    onOpen = appViewModel::openNotificationQuest,
                    onDismiss = appViewModel::dismissNotification,
                    modifier = Modifier.zIndex(10f).padding(top = 28.dp),
                )
            }
        }
    }
}
