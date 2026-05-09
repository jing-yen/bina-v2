package com.bina.ai.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bina.ai.analytics.tracking.EventTracker
import com.bina.ai.inference.InferenceEngine
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.ui.MiniAppScreen
import com.bina.ai.ui.screens.analytics.AnalyticsScreen
import com.bina.ai.ui.screens.hub.HubScreen
import com.bina.ai.ui.screens.pocket.MyPocketScreen
import com.bina.ai.ui.screens.studio.StudioScreen
import com.bina.ai.ui.screens.sync.OfflineSyncScreen

@Composable
fun BinaNavGraph(
    navController: NavHostController,
    userMode: UserMode,
    miniAppRepository: MiniAppRepository,
    inferenceEngine: InferenceEngine? = null,
    eventTracker: EventTracker
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Hub.route,
        enterTransition = { fadeIn() },
        exitTransition = { fadeOut() },
        popEnterTransition = { fadeIn() },
        popExitTransition = { fadeOut() }
    ) {
        composable(Screen.Hub.route) {
            HubScreen(
                miniAppRepository = miniAppRepository,
                onMiniAppClick = { miniAppId ->
                    navController.navigate(Screen.MiniAppView.createRoute(miniAppId))
                }
            )
        }

        composable(Screen.MyPocket.route) {
            MyPocketScreen(
                miniAppRepository = miniAppRepository,
                onMiniAppClick = { miniAppId ->
                    navController.navigate(Screen.MiniAppView.createRoute(miniAppId))
                }
            )
        }

        composable(Screen.OfflineSync.route) {
            OfflineSyncScreen()
        }

        composable(Screen.Studio.route) {
            StudioScreen(onPublished = {
                miniAppRepository.invalidateCache()
                navController.navigate(Screen.Hub.route) {
                    popUpTo(Screen.Hub.route) { inclusive = true }
                }
            })
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen()
        }

        composable(
            route = Screen.MiniAppView.route,
            arguments = listOf(navArgument("miniAppId") { type = NavType.StringType })
        ) { backStackEntry ->
            val miniAppId = backStackEntry.arguments?.getString("miniAppId") ?: return@composable
            val miniApp = remember { miniAppRepository.getById(miniAppId) }
            if (miniApp != null) {
                MiniAppScreen(
                    miniApp = miniApp,
                    inferenceEngine = inferenceEngine,
                    eventTracker = eventTracker,
                    onBack = { navController.popBackStack() }
                )
            }
        }
    }
}
