package com.hermes.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hermes.android.HermesApp
import com.hermes.android.data.auth.AuthStore
import com.hermes.android.ui.chat.ChatScreen
import com.hermes.android.ui.memory.MemoryViewerScreen
import com.hermes.android.ui.onboarding.OnboardingScreen
import com.hermes.android.ui.sessions.SessionListScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val SESSIONS = "sessions"
    const val CHAT = "chat/{sessionId}"
    const val MEMORY = "memory"
    fun chat(sessionId: String) = "chat/$sessionId"
}

@Composable
fun HermesNavHost(navController: NavHostController) {
    val authStore = remember { AuthStore(HermesApp.instance) }
    val isLoggedIn = authStore.isLoggedIn && authStore.serverUrl != null

    val startDestination = if (isLoggedIn) Routes.SESSIONS else Routes.ONBOARDING

    NavHost(navController = navController, startDestination = startDestination) {
        composable(Routes.ONBOARDING) {
            OnboardingScreen(
                onConnected = {
                    navController.navigate(Routes.SESSIONS) {
                        popUpTo(Routes.ONBOARDING) { inclusive = true }
                    }
                }
            )
        }
        composable(Routes.SESSIONS) {
            SessionListScreen(
                onSessionClick = { sessionId -> navController.navigate(Routes.chat(sessionId)) },
                onNewSession = { sessionId -> navController.navigate(Routes.chat(sessionId)) },
                onDisconnect = {
                    authStore.clear()
                    navController.navigate(Routes.ONBOARDING) {
                        popUpTo(Routes.SESSIONS) { inclusive = true }
                    }
                },
                onNavigateToMemory = {
                    navController.navigate(Routes.MEMORY)
                }
            )
        }
        composable(Routes.CHAT) { backStackEntry ->
            val sessionId = backStackEntry.arguments?.getString("sessionId") ?: ""
            ChatScreen(
                sessionId = sessionId,
                onBack = { navController.popBackStack() }
            )
        }
        composable(Routes.MEMORY) {
            MemoryViewerScreen(onBack = { navController.popBackStack() })
        }
    }
}