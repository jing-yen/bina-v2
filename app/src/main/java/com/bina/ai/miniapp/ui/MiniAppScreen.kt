package com.bina.ai.miniapp.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.inference.InferenceEngine
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.miniapp.runtime.ActionDispatcher
import com.bina.ai.miniapp.runtime.FormulaEngine
import com.bina.ai.miniapp.runtime.VariableStore
import com.bina.ai.miniapp.widgets.RenderWidget
import com.bina.ai.miniapp.widgets.parseColor
import com.bina.ai.platform.AndroidLocationProvider
import kotlinx.coroutines.launch

@Composable
fun MiniAppScreen(
    miniApp: MiniApp,
    inferenceEngine: InferenceEngine? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val backingMap = remember { mutableStateMapOf<String, String>() }
    val store = remember { VariableStore(miniApp.variables, backingMap) }
    val formulaEngine = remember { FormulaEngine(miniApp.formulas) }
    var currentScreenId by remember { mutableStateOf(miniApp.screens.firstOrNull()?.id ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    val themeColor = parseColor(miniApp.theme.primary)

    val dispatcher = remember {
        ActionDispatcher(
            store = store,
            miniApp = miniApp,
            formulaEngine = formulaEngine,
            locationProvider = AndroidLocationProvider(context),
            inferenceEngine = inferenceEngine,
            onNavigate = { screenId ->
                if (screenId == "home" || screenId == "back") {
                    currentScreenId = miniApp.screens.first().id
                } else {
                    currentScreenId = screenId
                }
            }
        )
    }

    val currentScreen = miniApp.screens.find { it.id == currentScreenId }
        ?: miniApp.screens.firstOrNull()

    if (currentScreen == null) return

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White.copy(alpha = 0.85f))
                .padding(horizontal = 12.dp, vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(40.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(themeColor.copy(alpha = 0.1f))
                    .clickable {
                        if (currentScreenId != miniApp.screens.first().id) {
                            currentScreenId = miniApp.screens.first().id
                        } else {
                            onBack()
                        }
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = themeColor,
                    modifier = Modifier.size(20.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Text(
                miniApp.icon,
                fontSize = 22.sp
            )
            Spacer(Modifier.width(8.dp))
            Column {
                Text(
                    currentScreen.title.ifEmpty { miniApp.name },
                    fontSize = 18.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF1A1A2E)
                )
                if (miniApp.safety.disclaimer.isNotEmpty()) {
                    Text(
                        miniApp.safety.disclaimer,
                        fontSize = 10.sp,
                        color = Color(0xFF9CA3AF)
                    )
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            currentScreen.body.forEach { widget ->
                RenderWidget(
                    widget = widget,
                    store = store,
                    themeColor = themeColor,
                    isLoading = isLoading,
                    dataSets = miniApp.data,
                    onAction = { action ->
                        scope.launch {
                            isLoading = true
                            dispatcher.dispatch(action)
                            isLoading = false
                        }
                    }
                )
            }
        }
    }
}
