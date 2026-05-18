package com.bina.ai.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.ui.res.stringResource
import com.bina.ai.R
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bina.ai.analytics.tracking.AnalyticsPinger
import com.bina.ai.analytics.tracking.EventTracker
import com.bina.ai.hub.FirestoreRecipeSource
import com.bina.ai.inference.InferenceEngine
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.ui.MiniAppScreen
import com.bina.ai.analytics.ui.AnalyticsScreen
import com.bina.ai.ui.screens.hub.HubScreen
import com.bina.ai.ui.screens.pocket.MyPocketScreen
import com.bina.ai.ui.screens.sync.OfflineSyncScreen

@Composable
fun BinaNavGraph(
    navController: NavHostController,
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,
    inferenceEngine: InferenceEngine? = null,
    eventTracker: EventTracker,
    analyticsRepository: com.bina.ai.analytics.data.AnalyticsRepository,
    firestoreRecipeSource: FirestoreRecipeSource? = null,
    analyticsPinger: AnalyticsPinger? = null
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
                installStore = installStore,
                firestoreRecipeSource = firestoreRecipeSource,
                onOpenRecipe = { id ->
                    navController.navigate(Screen.MiniAppView.createRoute(id))
                }
            )
        }

        composable(Screen.MyPocket.route) {
            MyPocketScreen(
                miniAppRepository = miniAppRepository,
                installStore = installStore,
                onMiniAppClick = { miniAppId ->
                    navController.navigate(Screen.MiniAppView.createRoute(miniAppId))
                }
            )
        }

        composable(Screen.OfflineSync.route) { backStackEntry ->
            val vm = com.bina.ai.ui.screens.sync.rememberSyncViewModel(
                miniAppRepository, installStore, owner = backStackEntry
            )
            OfflineSyncScreen(
                vm = vm,
                installStore = installStore,
                onScan = { navController.navigate(Screen.SyncScan.route) },
                onShare = { recipeId -> navController.navigate(Screen.SyncShare.createRoute(recipeId)) },
                onOpenRecipe = { id -> navController.navigate(Screen.MiniAppView.createRoute(id)) }
            )
        }

        composable(Screen.SyncScan.route) {
            val parentEntry = remember(it) { navController.getBackStackEntry(Screen.OfflineSync.route) }
            val vm = com.bina.ai.ui.screens.sync.rememberSyncViewModel(
                miniAppRepository, installStore, owner = parentEntry
            )
            com.bina.ai.ui.screens.sync.components.ScanQrScreen(
                vm = vm,
                onBack = { navController.popBackStack() }
            )
        }

        composable(
            route = Screen.SyncShare.route,
            arguments = listOf(navArgument("miniAppId") { type = NavType.StringType })
        ) { backStackEntry ->
            val recipeId = backStackEntry.arguments?.getString("miniAppId") ?: return@composable
            val parentEntry = remember(backStackEntry) { navController.getBackStackEntry(Screen.OfflineSync.route) }
            val vm = com.bina.ai.ui.screens.sync.rememberSyncViewModel(
                miniAppRepository, installStore, owner = parentEntry
            )
            com.bina.ai.ui.screens.sync.components.ShareQrScreen(
                vm = vm,
                miniAppRepository = miniAppRepository,
                recipeId = recipeId,
                onDone = { navController.popBackStack() }
            )
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                repository = analyticsRepository,
                onOpenHub = {
                    navController.navigate(Screen.Hub.route) {
                        popUpTo(Screen.Hub.route) { inclusive = false }
                        launchSingleTop = true
                    }
                }
            )
        }

        composable(
            route = Screen.MiniAppView.route,
            arguments = listOf(navArgument("miniAppId") { type = NavType.StringType })
        ) { backStackEntry ->
            val miniAppId = backStackEntry.arguments?.getString("miniAppId") ?: return@composable
            val cloudVersion by miniAppRepository.cloudVersion.collectAsState()
            val miniApp = remember(cloudVersion) { miniAppRepository.getById(miniAppId) }
            if (miniApp != null) {
                MiniAppScreen(
                    miniApp = miniApp,
                    inferenceEngine = inferenceEngine,
                    eventTracker = eventTracker,
                    analyticsPinger = analyticsPinger,
                    onBack = { navController.popBackStack() }
                )
            } else {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(stringResource(R.string.recipe_not_available))
                    TextButton(onClick = { navController.popBackStack() }) {
                        Text(stringResource(R.string.go_back))
                    }
                }
            }
        }

    }
}
