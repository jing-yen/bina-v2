package com.bina.ai.ui.navigation

import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.platform.LocalContext
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.bina.ai.analytics.tracking.EventTracker
import com.bina.ai.inference.InferenceEngine
import com.bina.ai.install.CapabilityChecker
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.ui.MiniAppScreen
import com.bina.ai.analytics.ui.AnalyticsScreen
import com.bina.ai.ui.screens.hub.HubScreen
import com.bina.ai.ui.screens.pocket.MyPocketScreen
import com.bina.ai.ui.screens.studio.StudioScreen
import com.bina.ai.ui.screens.sync.OfflineSyncScreen

@Composable
fun BinaNavGraph(
    navController: NavHostController,
    userMode: UserMode,
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,
    capabilityChecker: CapabilityChecker,
    inferenceEngine: InferenceEngine? = null,
    eventTracker: EventTracker,
    analyticsRepository: com.bina.ai.analytics.data.AnalyticsRepository
) {
    val coroutineScope = rememberCoroutineScope()

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
                userMode = userMode,
                onConfigureRecipe = { id ->
                    navController.navigate(Screen.Configurator.createRoute(id))
                },
                onOpenRecipe = { id ->
                    navController.navigate(Screen.MiniAppView.createRoute(id))
                },
                onOpenStudio = {
                    navController.navigate(Screen.Studio.route)
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

        composable(Screen.OfflineSync.route) {
            OfflineSyncScreen()
        }

        composable(Screen.Studio.route) {
            StudioScreen(onPublished = {
                miniAppRepository.invalidateCache()
                // TODO: when JY's StudioScreen passes the new recipe ID, auto-install here.
                // installStore.install(InstallRecord(recipeId = newRecipeId, ..., enabledFeatureIds = ...))
                navController.navigate(Screen.Hub.route) { popUpTo(Screen.Hub.route) { inclusive = true } }
            })
        }

        composable(Screen.Analytics.route) {
            AnalyticsScreen(
                repository = analyticsRepository,
                onOpenHub = {
                    navController.navigate(Screen.Hub.route) {
                        popUpTo(Screen.Hub.route) { inclusive = false }
                        launchSingleTop = true
                    }
                },
                onOpenStudio = {
                    navController.navigate(Screen.Studio.route) {
                        popUpTo(Screen.Hub.route) { saveState = true }
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

        composable(
            route = Screen.Configurator.route,
            arguments = listOf(navArgument("miniAppId") { type = NavType.StringType })
        ) { backStackEntry ->
            val miniAppId = backStackEntry.arguments?.getString("miniAppId") ?: return@composable
            val context = LocalContext.current
            val recipe = remember(miniAppId) { miniAppRepository.getById(miniAppId) }
            val baseSizeKb = remember(recipe) {
                if (recipe != null) {
                    try {
                        context.assets.openFd("miniapps/${recipe.id}.yaml").use { it.length / 1024f }
                    } catch (e: Exception) {
                        1.0f
                    }
                } else 0f
            }
            com.bina.ai.ui.screens.configurator.ConfiguratorScreen(
                miniAppId = miniAppId,
                miniAppRepository = miniAppRepository,
                installStore = installStore,
                capabilityChecker = capabilityChecker,
                baseSizeKb = baseSizeKb,
                onInstalled = { _ ->
                    navController.navigate(Screen.Hub.route) { popUpTo(Screen.Hub.route) { inclusive = true } }
                },
                onBack = { navController.popBackStack() }
            )
        }
    }
}
