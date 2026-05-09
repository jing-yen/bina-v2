package com.bina.ai

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.lifecycleScope
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import com.bina.ai.inference.InferenceEngine
import com.bina.ai.inference.LiteRtLmEngine
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.ui.components.BinaBottomNav
import com.bina.ai.ui.components.BinaTopBar
import com.bina.ai.ui.navigation.BinaNavGraph
import com.bina.ai.ui.navigation.Screen
import com.bina.ai.ui.navigation.UserMode
import com.bina.ai.ui.theme.BinaScreenMid
import com.bina.ai.ui.theme.BinaScreenStart
import com.bina.ai.ui.theme.BinaTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var inferenceEngine: InferenceEngine

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val miniAppRepository = MiniAppRepository {
            val assetFiles = applicationContext.assets.list("miniapps") ?: emptyArray()
            val fromAssets = assetFiles.filter { it.endsWith(".yaml") || it.endsWith(".yml") }
                .map { it to applicationContext.assets.open("miniapps/$it").bufferedReader().readText() }

            val userDir = java.io.File(applicationContext.filesDir, "miniapps")
            val fromUser = if (userDir.isDirectory) {
                userDir.listFiles()
                    ?.filter { it.extension in listOf("yaml", "yml") }
                    ?.map { it.name to it.readText() }
                    ?: emptyList()
            } else emptyList()

            fromAssets + fromUser
        }

        inferenceEngine = LiteRtLmEngine(applicationContext)
        lifecycleScope.launch { inferenceEngine.initialize() }

        setContent {
            BinaTheme {
                var userMode by remember { mutableStateOf(UserMode.BUILDER) }
                val navController = rememberNavController()
                val navBackStackEntry by navController.currentBackStackEntryAsState()
                val currentRoute = navBackStackEntry?.destination?.route
                val showShell = currentRoute in listOf(
                    Screen.Hub.route,
                    Screen.MyPocket.route,
                    Screen.OfflineSync.route,
                    Screen.Studio.route,
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
                                    Color.White
                                )
                            )
                        )
                        .statusBarsPadding()
                ) {
                    if (showShell) {
                        BinaTopBar(
                            userMode = userMode,
                            onToggleMode = {
                                userMode = if (userMode == UserMode.BUILDER)
                                    UserMode.ARCHITECT else UserMode.BUILDER
                                navController.navigate(Screen.Hub.route) {
                                    popUpTo(0) { inclusive = true }
                                }
                            }
                        )
                    }

                    Box(modifier = Modifier.weight(1f)) {
                        BinaNavGraph(
                            navController = navController,
                            userMode = userMode,
                            miniAppRepository = miniAppRepository,
                            inferenceEngine = inferenceEngine
                        )
                    }

                    if (showShell) {
                        BinaBottomNav(
                            userMode = userMode,
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

    override fun onDestroy() {
        super.onDestroy()
        inferenceEngine.close()
    }
}
