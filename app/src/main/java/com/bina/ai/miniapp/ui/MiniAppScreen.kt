package com.bina.ai.miniapp.ui

import android.app.Activity
import android.content.Intent
import android.content.res.Configuration
import android.net.Uri
import android.speech.tts.TextToSpeech
import java.util.Locale
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.tracking.AnalyticsPinger
import com.bina.ai.analytics.tracking.EventTracker
import com.bina.ai.inference.InferenceEngine
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.miniapp.model.Widget as MiniWidget
import com.bina.ai.miniapp.runtime.ActionDispatcher
import com.bina.ai.miniapp.runtime.FormulaEngine
import com.bina.ai.miniapp.runtime.TriageEngine
import com.bina.ai.miniapp.runtime.TriageResult
import com.bina.ai.miniapp.runtime.VariableStore
import androidx.compose.ui.res.stringResource
import com.bina.ai.R
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
                    if (k !in VariableStore.TRANSIENT_VARS && !k.startsWith("l10n.")) {
                        editor.putString(k, v)
                    }
                }
                editor.apply()
            }
            val defaultLang = miniApp.localisation.defaultLanguage.ifEmpty {
                miniApp.localisation.supported.firstOrNull() ?: "en"
            }
            miniApp.localisation.labels[defaultLang]?.forEach { (key, value) ->
                backingMap["l10n.$key"] = value
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
    val supportedLangs = miniApp.localisation.supported.ifEmpty { listOf(miniApp.localisation.defaultLanguage) }
    var currentLang by remember { mutableStateOf(com.bina.ai.ui.resolveAppLang(miniApp)) }
    fun applyLang(lang: String) {
        store["active_language"] = lang
        miniApp.localisation.labels[lang]?.forEach { (key, value) ->
            store["l10n.$key"] = value
        }
    }
    remember(currentLang) { applyLang(currentLang); true }
    val langContext = remember(currentLang) {
        val locale = langToLocale(currentLang)
        val config = Configuration(context.resources.configuration).apply { setLocale(locale) }
        context.createConfigurationContext(config)
    }
    val tts = remember {
        var engine: TextToSpeech? = null
        engine = TextToSpeech(context) { status ->
            if (status == TextToSpeech.SUCCESS) {
                engine?.language = langToLocale(currentLang)
            }
        }
        engine
    }
    LaunchedEffect(currentLang) { tts?.language = langToLocale(currentLang) }
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
                            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.share)))
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

    val view = LocalView.current
    SideEffect {
        (view.context as? Activity)?.window?.statusBarColor = secondaryColor.toArgb()
    }
    androidx.compose.runtime.DisposableEffect(Unit) {
        onDispose {
            (view.context as? Activity)?.window?.statusBarColor = android.graphics.Color.TRANSPARENT
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(secondaryColor)
            .navigationBarsPadding()
    ) {
        val isHomeScreen = currentScreenId == miniApp.screens.firstOrNull()?.id

        BackHandler(enabled = !isHomeScreen) {
            switchScreen(miniApp.screens.first().id)
        }

        if (isHomeScreen) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 14.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                Text(miniApp.icon, fontSize = 48.sp)
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f).padding(bottom = 4.dp)) {
                    val displayName = store["l10n.recipe_name"]?.takeIf { it.isNotBlank() } ?: miniApp.name
                    Text(
                        store.interpolate(currentScreen.title.ifEmpty { displayName }),
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFF1C1917)
                    )
                    if (miniApp.author.organisation.isNotBlank()) {
                        Text(
                            stringResource(R.string.recipe_by, miniApp.author.organisation),
                            fontSize = 11.sp,
                            color = Color(0xFF78716C)
                        )
                    }
                }
                if (supportedLangs.size > 1) {
                    Column(
                        modifier = Modifier.padding(bottom = 4.dp),
                        verticalArrangement = Arrangement.spacedBy(2.dp),
                        horizontalAlignment = Alignment.End
                    ) {
                        supportedLangs.chunked(5).forEach { row ->
                            Row(horizontalArrangement = Arrangement.spacedBy(0.dp)) {
                                row.forEach { lang ->
                                    val flag = LANG_FLAGS[lang] ?: lang.uppercase()
                                    val isActive = lang == currentLang
                                    Text(
                                        flag,
                                        fontSize = if (isActive) 16.sp else 13.sp,
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(6.dp))
                                            .clickable { currentLang = lang }
                                            .padding(horizontal = 3.dp, vertical = 2.dp)
                                    )
                                }
                            }
                        }
                    }
                }
            }
        } else {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = themeColor,
                    modifier = Modifier
                        .size(20.dp)
                        .clickable { switchScreen(miniApp.screens.first().id) }
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    run {
                        val screenEntry = miniApp.screenCatalog.find { it.id == currentScreenId }
                        screenEntry?.icon?.ifEmpty { null } ?: miniApp.icon
                    },
                    fontSize = 22.sp
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    store.interpolate(currentScreen.title.ifEmpty { miniApp.name }),
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1C1917),
                    modifier = Modifier.weight(1f)
                )
                Spacer(Modifier.width(8.dp))
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
            val triageDisplayName = store["l10n.recipe_name"]?.takeIf { it.isNotBlank() } ?: miniApp.name
            TriageChatContent(
                miniApp = miniApp,
                displayName = triageDisplayName,
                store = store,
                langContext = langContext,
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
                                        TriageChatMessage(context.getString(R.string.miniapp_triage_fallback), isUser = false)
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

            val completedIncrementActions = remember(currentScreenId) {
                miniApp.screens.flatMap { screen ->
                    screen.body.filterIsInstance<MiniWidget.ChecklistItems>().map { it.bind } +
                    screen.body.filterIsInstance<MiniWidget.ProgressBar>().map { it.bind }
                }
            }.filter { bindVar ->
                val current = (backingMap[bindVar] ?: "").toIntOrNull() ?: 0
                val max = miniApp.screens.flatMap { it.body }.let { widgets ->
                    widgets.filterIsInstance<MiniWidget.ChecklistItems>().find { it.bind == bindVar }?.items?.size
                        ?: widgets.filterIsInstance<MiniWidget.ProgressBar>().find { it.bind == bindVar }?.total
                }
                max != null && current >= max
            }.map { "increment:$it" }.toSet()

            LaunchedEffect(completedIncrementActions.size) {
                if (completedIncrementActions.isNotEmpty()) {
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
                        if (widget is MiniWidget.ActionButton && widget.action in completedIncrementActions) return@forEach
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
                            },
                            inferenceEngine = inferenceEngine
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
                Box(
                    modifier = Modifier
                        .height(28.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(Color.White.copy(alpha = 0.6f))
                        .clickable {
                            if (isHomeScreen) onBack()
                            else switchScreen(miniApp.screens.first().id)
                        }
                        .padding(horizontal = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = Color(0xFF44403C),
                        modifier = Modifier.size(16.dp)
                    )
                }
                miniApp.screens.forEach { screen ->
                    val isActive = screen.id == currentScreenId
                    val isHome = screen.id == miniApp.screens.first().id
                    val catalogEntry = miniApp.screenCatalog.find { it.id == screen.id }
                    val emoji = catalogEntry?.icon?.ifEmpty { null }
                    val label = if (isHome) {
                            langContext.getString(R.string.miniapp_home)
                        } else store.interpolate(screen.title.ifEmpty { catalogEntry?.title ?: screen.id })

                    Box(
                        modifier = Modifier
                            .height(28.dp)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (isActive) themeColor else Color.White.copy(alpha = 0.6f))
                            .clickable(role = androidx.compose.ui.semantics.Role.Tab) { switchScreen(screen.id) }
                            .padding(horizontal = if (isActive) 14.dp else 10.dp)
                            .animateContentSize(),
                        contentAlignment = Alignment.Center
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
    val lang = miniApp.localisation.defaultLanguage.ifEmpty { miniApp.localisation.supported.firstOrNull() ?: "en" }
    val labels = miniApp.localisation.labels[lang].orEmpty()
    val interpolate = { text: String ->
        Regex("\\{\\{(\\w+(?:\\.\\w+)?)\\}\\}").replace(text) { m ->
            val key = m.groupValues[1]
            if (key.startsWith("l10n.")) labels[key.removePrefix("l10n.")] ?: m.value
            else m.value
        }
    }

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
                                interpolate(screen.title),
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
                    intro.acceptLabel.ifEmpty { stringResource(R.string.miniapp_i_understand) },
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = Color.White
                )
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

private val LANG_FLAGS = mapOf(
    "ms" to "🇲🇾", "en" to "🇬🇧",
    "id" to "🇮🇩", "zh" to "🇨🇳",
    "ta" to "🇮🇳", "th" to "🇹🇭",
    "vi" to "🇻🇳", "tl" to "🇵🇭",
    "my" to "🇲🇲", "km" to "🇰🇭",
    "lo" to "🇱🇦", "ja" to "🇯🇵",
    "ko" to "🇰🇷", "ar" to "🇸🇦",
    "hi" to "🇮🇳", "bn" to "🇧🇩",
    "fr" to "🇫🇷", "es" to "🇪🇸",
    "pt" to "🇧🇷", "sw" to "🇰🇪",
)

private fun langToLocale(code: String): Locale = when (code) {
    "ms" -> Locale("ms", "MY")
    "id" -> Locale("in", "ID")
    "in" -> Locale("in", "ID")
    "zh" -> Locale("zh", "CN")
    "ta" -> Locale("ta", "IN")
    "th" -> Locale("th", "TH")
    "vi" -> Locale("vi", "VN")
    "tl" -> Locale("fil", "PH")
    "my" -> Locale("my", "MM")
    "km" -> Locale("km", "KH")
    "lo" -> Locale("lo", "LA")
    "ja" -> Locale("ja", "JP")
    "ko" -> Locale("ko", "KR")
    "ar" -> Locale("ar", "SA")
    "hi" -> Locale("hi", "IN")
    "bn" -> Locale("bn", "BD")
    "fr" -> Locale("fr", "FR")
    "es" -> Locale("es", "ES")
    "pt" -> Locale("pt", "BR")
    "sw" -> Locale("sw", "KE")
    else -> Locale(code)
}

data class TriageChatMessage(val text: String, val isUser: Boolean)

@Composable
private fun TriageChatContent(
    miniApp: MiniApp,
    displayName: String,
    store: VariableStore,
    langContext: android.content.Context,
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
            val greeting = store["l10n.greeting"]
                ?.let { store.interpolate(it) }
                ?: langContext.getString(R.string.triage_greeting, displayName)
            Box(
                modifier = Modifier
                    .align(Alignment.Start)
                    .clip(RoundedCornerShape(16.dp, 16.dp, 16.dp, 4.dp))
                    .background(themeColor.copy(alpha = 0.1f))
                    .padding(12.dp)
            ) {
                Text(
                    greeting,
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
                placeholder = { Text(langContext.getString(R.string.triage_placeholder), fontSize = 14.sp) },
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
                Text(langContext.getString(R.string.triage_send), color = Color.White, fontSize = 14.sp)
            }
        }
    }
}
