package com.hermes.android.ui.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import com.hermes.android.ui.onboarding.OnboardingScreen
import com.hermes.android.ui.sessions.SessionListScreen
import com.hermes.android.ui.chat.ChatScreen

object Routes {
    const val ONBOARDING = "onboarding"
    const val SESSIONS = "sessions"
    const val CHAT = "chat/{sessionId}"
    fun chat(sessionId: String) = "chat/$sessionId"
}

@Composable
fun HermesNavHost(navController: NavHostController) {
    NavHost(navController = navController, startDestination = Routes.ONBOARDING) {
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
                onSessionClick = { sessionId ->
                    navController.navigate(Routes.chat(sessionId))
                },
                onNewSession = { sessionId ->
                    navController.navigate(Routes.chat(sessionId))
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
    }
}