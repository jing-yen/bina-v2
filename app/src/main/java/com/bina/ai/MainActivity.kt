package com.bina.ai

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bina.ai.analytics.tracking.AnalyticsPinger
import com.bina.ai.hub.FirestoreRecipeSource
import com.bina.ai.install.ShortcutHelper
import com.bina.ai.inference.InferenceEngine
import com.bina.ai.inference.LiteRtLmEngine
import com.bina.ai.inference.ModelDownloadManager
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.ui.components.BinaBottomNav
import com.bina.ai.ui.components.BinaTopBar
import com.bina.ai.ui.navigation.BinaNavGraph
import com.bina.ai.ui.navigation.Screen
import com.bina.ai.ui.screens.ModelDownloadScreen
import com.bina.ai.ui.theme.BinaBgMain
import com.bina.ai.ui.theme.BinaScreenMid
import com.bina.ai.ui.theme.BinaScreenStart
import com.bina.ai.ui.theme.BinaTheme
import kotlinx.coroutines.launch

class MainActivity : AppCompatActivity() {

    private lateinit var inferenceEngine: LiteRtLmEngine
    private lateinit var downloadManager: ModelDownloadManager

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val userDir = java.io.File(applicationContext.filesDir, "miniapps")
        val miniAppRepository = MiniAppRepository(
            loadYamlFiles = {
                if (userDir.isDirectory) {
                    userDir.listFiles()
                        ?.filter { it.extension in listOf("yaml", "yml") }
                        ?.map { it.name to it.readText() }
                        ?: emptyList()
                } else emptyList()
            },
            persistYaml = { filename, yamlText ->
                userDir.mkdirs()
                java.io.File(userDir, filename).writeText(yamlText)
            }
        )

        inferenceEngine = LiteRtLmEngine(applicationContext)
        downloadManager = ModelDownloadManager(applicationContext)

        val modelAlreadyExists = inferenceEngine.findModelFile() != null || downloadManager.getModelPath() != null
        if (modelAlreadyExists) {
            lifecycleScope.launch { inferenceEngine.initialize() }
        }

        val installStore = InstallStore.create(applicationContext)
        val firestoreRecipeSource = try { FirestoreRecipeSource(filesDir) } catch (_: Exception) { null }
        val analyticsPinger = try { AnalyticsPinger(applicationContext) } catch (_: Exception) { null }

        if (firestoreRecipeSource != null) {
            val cached = firestoreRecipeSource.loadCached()
            if (cached.isNotEmpty()) {
                miniAppRepository.registerCloudRecipesWithYaml(cached)
            }
            lifecycleScope.launch {
                try {
                    val recipes = firestoreRecipeSource.fetchRecipesWithYaml()
                    miniAppRepository.registerCloudRecipesWithYaml(recipes)
                } catch (_: Exception) { }
            }
        }

        // Analytics infrastructure
        val analyticsDb = com.bina.ai.analytics.data.AnalyticsDatabase.get(applicationContext)
        val eventTracker = com.bina.ai.analytics.tracking.EventTracker(analyticsDb.eventDao())
        val analyticsRepository = com.bina.ai.analytics.data.AnalyticsRepository(
            dao = analyticsDb.eventDao(),
            miniAppRepository = miniAppRepository,
            installStore = installStore
        )

        setContent {
            BinaTheme {
                var modelReady by remember { mutableStateOf(modelAlreadyExists) }
                val navController = rememberNavController()
                val shortcutRecipeId = remember {
                    intent?.getStringExtra(ShortcutHelper.EXTRA_RECIPE_ID)
                }
                LaunchedEffect(shortcutRecipeId) {
                    if (!shortcutRecipeId.isNullOrEmpty()) {
                        navController.navigate(Screen.MiniAppView.createRoute(shortcutRecipeId)) {
                            launchSingleTop = true
                        }
                    }
                }
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showShell = modelReady && currentRoute in listOf(
                    Screen.Hub.route,
                    Screen.MyPocket.route,
                    Screen.OfflineSync.route,
                    Screen.Analytics.route
                )

                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                colors = listOf(
                                    BinaScreenStart,
                                    BinaScreenMid,
                                    BinaBgMain
                                )
                            )
                        )
                        .statusBarsPadding()
                ) {
                    if (!modelReady) {
                        ModelDownloadScreen(
                            downloadManager = downloadManager,
                            onModelReady = {
                                lifecycleScope.launch {
                                    inferenceEngine.initialize()
                                    modelReady = true
                                }
                            },
                            modifier = Modifier.weight(1f)
                        )
                    } else {
                        if (showShell) {
                            BinaTopBar()
                        }

                        Box(modifier = Modifier.weight(1f)) {
                            BinaNavGraph(
                                navController = navController,
                                miniAppRepository = miniAppRepository,
                                installStore = installStore,
                                inferenceEngine = inferenceEngine,
                                eventTracker = eventTracker,
                                analyticsRepository = analyticsRepository,
                                firestoreRecipeSource = firestoreRecipeSource,
                                analyticsPinger = analyticsPinger
                            )
                        }

                        if (showShell) {
                            BinaBottomNav(
                                currentRoute = currentRoute,
                                onTabClick = { screen ->
                                    navController.navigate(screen.route) {
                                        popUpTo(Screen.Hub.route) { saveState = true }
                                        launchSingleTop = true
                                        restoreState = true
                                    }
                                },
                                modifier = Modifier.navigationBarsPadding()
                            )
                        }
                    }
                }
            }
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        inferenceEngine.close()
    }
}
