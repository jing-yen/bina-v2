package com.bina.ai.miniapp.ui

import android.content.Intent
import android.net.Uri
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.animation.animateContentSize
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
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
import com.bina.ai.analytics.tracking.AnalyticsPinger
import com.bina.ai.analytics.tracking.EventTracker
import com.bina.ai.inference.InferenceEngine
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.miniapp.runtime.ActionDispatcher
import com.bina.ai.miniapp.runtime.FormulaEngine
import com.bina.ai.miniapp.runtime.TriageEngine
import com.bina.ai.miniapp.runtime.TriageResult
import com.bina.ai.miniapp.runtime.VariableStore
import com.bina.ai.miniapp.widgets.RenderWidget
import com.bina.ai.miniapp.widgets.parseColor
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.platform.AndroidLocationProvider
import kotlinx.coroutines.launch

@Composable
fun MiniAppScreen(
    miniApp: MiniApp,
    inferenceEngine: InferenceEngine? = null,
    eventTracker: EventTracker,
    analyticsPinger: AnalyticsPinger? = null,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val storePrefs = remember { context.getSharedPreferences("bina_vars_${miniApp.id}", android.content.Context.MODE_PRIVATE) }
    val backingMap = remember {
        val map = mutableStateMapOf<String, String>()
        storePrefs.all.forEach { (k, v) -> if (v is String) map[k] = v }
        map
    }
    val store = remember {
        VariableStore(miniApp.variables, backingMap).also { vs ->
            vs.onChange = {
                val editor = storePrefs.edit()
                backingMap.forEach { (k, v) ->
                    if (k != "ai_response" && k != "is_loading" && k != "photo_path") {
                        editor.putString(k, v)
                    }
                }
                editor.apply()
            }
        }
    }
    val formulaEngine = remember { FormulaEngine(miniApp.formulas) }
    var currentScreenId by remember { mutableStateOf(miniApp.screens.firstOrNull()?.id ?: "") }
    var isLoading by remember { mutableStateOf(false) }
    val screenResponses = remember { mutableMapOf<String, String>() }
    val switchScreen = { targetId: String ->
        screenResponses[currentScreenId] = store["ai_response"]
        store["ai_response"] = screenResponses[targetId] ?: ""
        currentScreenId = targetId
    }
    val hasIntro = miniApp.setup.introPage.disclaimer.isNotEmpty()
    val introPrefs = remember { context.getSharedPreferences("bina_intro", android.content.Context.MODE_PRIVATE) }
    val introAcceptedKey = "accepted_${miniApp.id}"
    var showIntro by remember { mutableStateOf(hasIntro && !introPrefs.getBoolean(introAcceptedKey, false)) }
    val scope = rememberCoroutineScope()
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = Locale("ms", "MY")
            }
        }
        engine
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose { tts?.shutdown() }
    }
    val themeColor = parseColor(miniApp.theme.primary)
    val secondaryColor = if (miniApp.theme.secondary.isNotEmpty()) parseColor(miniApp.theme.secondary) else themeColor.copy(alpha = 0.3f)

    val triageEngine = remember {
        TriageEngine(
            catalog = miniApp.screenCatalog,
            questions = miniApp.questions,
            config = miniApp.triage,
            inferenceEngine = inferenceEngine
        )
    }
    val triageMessages = remember { mutableStateOf(listOf<TriageChatMessage>()) }
    var triageInput by remember { mutableStateOf("") }
    var triageLoading by remember { mutableStateOf(false) }

    LaunchedEffect(miniApp.id) {
        eventTracker.logLaunch(miniApp.id)
        analyticsPinger?.onRecipeLaunched(miniApp.id)
    }

    val dispatcher = remember {
        ActionDispatcher(
            store = store,
            miniApp = miniApp,
            formulaEngine = formulaEngine,
            locationProvider = AndroidLocationProvider(context),
            inferenceEngine = inferenceEngine,
            onNavigate = { screenId ->
                if (screenId == "home" || screenId == "back") {
                    switchScreen(miniApp.screens.first().id)
                } else {
                    switchScreen(screenId)
                }
            },
            onAskLogged = {
                scope.launch { eventTracker.logAsk(miniApp.id) }
            },
            onNativeIntent = { intent ->
                when (intent) {
                    is ActionDispatcher.NativeIntent.Tts -> {
                        tts?.speak(intent.text, TextToSpeech.QUEUE_ADD, null, "bina_tts")
                    }
                    is ActionDispatcher.NativeIntent.Sms -> {
                        try {
                            val smsUri = Uri.parse("smsto:${intent.phone}")
                            val smsIntent = Intent(Intent.ACTION_SENDTO, smsUri).apply {
                                putExtra("sms_body", intent.body)
                            }
                            context.startActivity(smsIntent)
                        } catch (_: Exception) {}
                    }
                    is ActionDispatcher.NativeIntent.Tel -> {
                        try {
                            context.startActivity(Intent(Intent.ACTION_DIAL, Uri.parse("tel:${intent.phone}")))
                        } catch (_: Exception) {}
                    }
                    is ActionDispatcher.NativeIntent.Share -> {
                        try {
                            val shareIntent = Intent(Intent.ACTION_SEND).apply {
                                type = "text/plain"
                                putExtra(Intent.EXTRA_TEXT, intent.text)
                            }
                            context.startActivity(Intent.createChooser(shareIntent, "Kongsi"))
                        } catch (_: Exception) {}
                    }
                }
            }
        )
    }

    val currentScreen = miniApp.screens.find { it.id == currentScreenId }
        ?: miniApp.screens.firstOrNull()

    if (currentScreen == null) return

    if (showIntro) {
        IntroPageScreen(
            miniApp = miniApp,
            themeColor = themeColor,
            onAccept = {
                introPrefs.edit().putBoolean(introAcceptedKey, true).apply()
                showIntro = false
            },
            onBack = onBack
        )
        return
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(secondaryColor)
            .navigationBarsPadding()
    ) {
        // Nav bar — matches web preview: icon + title, back chevron on sub-screens only
        val isHomeScreen = currentScreenId == miniApp.screens.firstOrNull()?.id

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (!isHomeScreen) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = themeColor,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { switchScreen(miniApp.screens.first().id) }
                )
                Spacer(Modifier.width(8.dp))
            }
            Text(
                if (isHomeScreen) miniApp.icon else {
                    val screenEntry = miniApp.screenCatalog.find { it.id == currentScreenId }
                    screenEntry?.icon?.ifEmpty { null } ?: miniApp.icon
                },
                fontSize = 22.sp
            )
            Spacer(Modifier.width(8.dp))
            Text(
                currentScreen.title.ifEmpty { miniApp.name },
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1917),
                modifier = Modifier.weight(1f)
            )
            if (!isHomeScreen) {
                Icon(
                    Icons.Filled.Refresh,
                    contentDescription = "Reset screen",
                    tint = themeColor,
                    modifier = Modifier
                        .size(22.dp)
                        .clickable {
                            store["ai_response"] = ""
                            store["photo_path"] = ""
                            screenResponses.remove(currentScreenId)
                        }
                )
            }
        }

        val showTriage = isHomeScreen && triageEngine.isChatMode

        if (showTriage) {
            TriageChatContent(
                miniApp = miniApp,
                themeColor = themeColor,
                messages = triageMessages.value,
                input = triageInput,
                isLoading = triageLoading,
                onInputChange = { triageInput = it },
                onSend = {
                    val text = triageInput.trim()
                    if (text.isNotEmpty() && !triageLoading) {
                        triageInput = ""
                        triageMessages.value = triageMessages.value + TriageChatMessage(text, isUser = true)
                        triageLoading = true
                        scope.launch {
                            val result = triageEngine.route(text)
                            triageLoading = false
                            when (result) {
                                is TriageResult.Navigate -> {
                                    result.prefillHints.forEach { (k, v) -> store[k] = v }
                                    switchScreen(result.screenId)
                                    triageMessages.value = emptyList()
                                    triageEngine.reset()
                                }
                                is TriageResult.Clarify -> {
                                    triageMessages.value = triageMessages.value +
                                        TriageChatMessage(result.question, isUser = false)
                                }
                                is TriageResult.Fallback -> {
                                    currentScreen.body.let { /* show grid fallback */ }
                                    triageMessages.value = triageMessages.value +
                                        TriageChatMessage("Let me show you all available options.", isUser = false)
                                }
                            }
                        }
                    }
                },
                modifier = Modifier.fillMaxSize().weight(1f)
            )
        } else {
            // Content area — bottom-aligned like web preview (justify-end)
            val scrollState = rememberScrollState()
            val aiResponse = backingMap["ai_response"] ?: ""
            LaunchedEffect(aiResponse) {
                if (aiResponse.isNotEmpty()) {
                    scrollState.animateScrollTo(scrollState.maxValue)
                }
            }
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .weight(1f)
                    .verticalScroll(scrollState)
                    .padding(horizontal = 16.dp, vertical = 16.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
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
                    Spacer(Modifier.height(32.dp))
                }
            }
        }

        if (miniApp.screens.size > 1) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 12.dp, vertical = 8.dp)
                    .horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.CenterHorizontally)
            ) {
                if (!isHomeScreen) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(Color.White.copy(alpha = 0.6f))
                            .clickable { onBack() }
                            .padding(horizontal = 10.dp, vertical = 6.dp)
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color(0xFF44403C),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                miniApp.screens.forEach { screen ->
                    val isActive = screen.id == currentScreenId
                    val isHome = screen.id == miniApp.screens.first().id
                    val catalogEntry = miniApp.screenCatalog.find { it.id == screen.id }
                    val emoji = catalogEntry?.icon?.ifEmpty { null }
                    val label = if (isHome) "Home"
                        else screen.title.ifEmpty { catalogEntry?.title ?: screen.id }

                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isActive) themeColor else Color.White.copy(alpha = 0.6f))
                            .clickable(role = androidx.compose.ui.semantics.Role.Tab) { switchScreen(screen.id) }
                            .padding(horizontal = if (isActive) 14.dp else 10.dp, vertical = 6.dp)
                            .animateContentSize()
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            if (isHome) {
                                Icon(
                                    Icons.Filled.Home,
                                    contentDescription = "Home",
                                    tint = if (isActive) Color.White else Color(0xFF44403C),
                                    modifier = Modifier.size(16.dp)
                                )
                            } else if (!emoji.isNullOrEmpty()) {
                                Text(emoji, fontSize = 14.sp)
                            }
                            if (isActive) {
                                if (isHome || !emoji.isNullOrEmpty()) Spacer(Modifier.width(4.dp))
                                Text(
                                    label,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Medium,
                                    color = Color.White
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun IntroPageScreen(
    miniApp: MiniApp,
    themeColor: Color,
    onAccept: () -> Unit,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val intro = miniApp.setup.introPage
    val author = intro.author ?: miniApp.author
    val secondaryColor = if (miniApp.theme.secondary.isNotEmpty()) parseColor(miniApp.theme.secondary) else themeColor.copy(alpha = 0.3f)

    Column(
        Modifier
            .fillMaxSize()
            .background(secondaryColor)
            .navigationBarsPadding()
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Go back",
                    tint = Color(0xFF1C1917),
                    modifier = Modifier
                        .size(24.dp)
                        .clickable { onBack() }
                )
            }
            Spacer(Modifier.height(16.dp))
            Text(miniApp.icon, fontSize = 48.sp)
            Spacer(Modifier.height(12.dp))
            Text(
                miniApp.name,
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1C1917)
            )
            if (miniApp.description.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Text(
                    miniApp.description,
                    fontSize = 13.sp,
                    color = BinaGrayText,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            if (author.name.isNotEmpty()) {
                Spacer(Modifier.height(12.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(author.name, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = Color(0xFF1C1917))
                    if (author.verified) {
                        Spacer(Modifier.width(4.dp))
                        Icon(
                            Icons.Filled.Verified,
                            contentDescription = "Verified",
                            tint = themeColor,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
                if (author.organisation.isNotEmpty()) {
                    Text(author.organisation, fontSize = 13.sp, color = BinaGrayText)
                }
            }

            if (intro.links.isNotEmpty()) {
                Spacer(Modifier.height(20.dp))
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    intro.links.forEach { link ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .border(1.dp, themeColor.copy(alpha = 0.2f), RoundedCornerShape(12.dp))
                                .clickable {
                                    try {
                                        context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(link.url)))
                                    } catch (_: Exception) {}
                                }
                                .padding(12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                Icons.Filled.OpenInNew,
                                contentDescription = null,
                                tint = themeColor,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(link.label, fontSize = 14.sp, color = themeColor, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }

            // Screen list — matches web preview's screen listing
            val nonHomeScreens = miniApp.screens.drop(1)
            if (nonHomeScreens.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Column(
                    Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    nonHomeScreens.forEach { screen ->
                        val catalogEntry = miniApp.screenCatalog.find { it.id == screen.id }
                        val screenIcon = catalogEntry?.icon?.ifEmpty { null } ?: "📋"
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(12.dp))
                                .background(Color.White.copy(alpha = 0.6f))
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(screenIcon, fontSize = 16.sp)
                            Spacer(Modifier.width(10.dp))
                            Text(
                                screen.title,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium,
                                color = Color(0xFF1C1917)
                            )
                        }
                    }
                }
            }

            if (intro.disclaimer.isNotEmpty()) {
                Spacer(Modifier.height(16.dp))
                Text(
                    intro.disclaimer,
                    fontSize = 13.sp,
                    color = BinaGrayText,
                    lineHeight = 18.sp,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
            }

            Spacer(Modifier.weight(1f))
            Button(
                onClick = onAccept,
                modifier = Modifier.fillMaxWidth().height(44.dp),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor)
            ) {
                Text(
                    intro.acceptLabel.ifEmpty { "I Understand" },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

data class TriageChatMessage(val text: String, val isUser: Boolean)

@Composable
private fun TriageChatContent(
    miniApp: MiniApp,
    themeColor: Color,
    messages: List<TriageChatMessage>,
    input: String,
    isLoading: Boolean,
    onInputChange: (String) -> Unit,
    onSend: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(modifier) {
        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                    .background(themeColor.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text(
                    "Hi! I'm ${miniApp.name}. How can I help you today?",
                    fontSize = 14.sp,
                    color = Color(0xFF1C1917)
                )
            }

            messages.forEach { msg ->
                Box(
                    modifier = Modifier
                        .align(if (msg.isUser) Alignment.End else Alignment.Start)
                        .clip(
                            if (msg.isUser) RoundedCornerShape(16.dp, 16.dp, 4.dp, 16.dp)
                            else RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp)
                        )
                        .background(if (msg.isUser) themeColor else themeColor.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    Text(
                        msg.text,
                        fontSize = 14.sp,
                        color = if (msg.isUser) Color.White else Color(0xFF1C1917)
                    )
                }
            }

            if (isLoading) {
                Box(
                    modifier = Modifier
                        .align(Alignment.Start)
                        .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                        .background(themeColor.copy(alpha = 0.1f))
                        .padding(12.dp)
                ) {
                    androidx.compose.material3.CircularProgressIndicator(
                        modifier = Modifier.size(16.dp),
                        color = themeColor,
                        strokeWidth = 2.dp
                    )
                }
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color.White)
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            androidx.compose.material3.OutlinedTextField(
                value = input,
                onValueChange = onInputChange,
                placeholder = { Text("Describe what you need...", fontSize = 14.sp) },
                modifier = Modifier.weight(1f),
                shape = RoundedCornerShape(14.dp),
                singleLine = true
            )
            Button(
                onClick = onSend,
                enabled = input.isNotBlank() && !isLoading,
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(containerColor = themeColor),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp)
            ) {
                Text("Send", color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
