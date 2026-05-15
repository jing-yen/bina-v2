package com.bina.ai.ui.screens.configurator

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.bina.ai.install.CapabilityChecker
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.screens.configurator.components.ConfiguratorHeader
import com.bina.ai.ui.screens.configurator.components.FeatureToggleCard
import com.bina.ai.ui.theme.BinaAccent
import com.bina.ai.ui.theme.BinaGrayText

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ConfiguratorScreen(
    miniAppId: String,
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,
    capabilityChecker: CapabilityChecker,
    baseSizeKb: Float,
    onInstalled: (recipeName: String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val cv by miniAppRepository.cloudVersion.collectAsState()
    val recipe = remember(miniAppId, cv) { miniAppRepository.getById(miniAppId) }

    if (recipe == null) {
        UnavailableScaffold(onBack = onBack, modifier = modifier)
        return
    }

    if (recipe.features.isEmpty()) {
        EmptyFeaturesScaffold(
            recipe = recipe,
            installStore = installStore,
            capabilityChecker = capabilityChecker,
            miniAppRepository = miniAppRepository,
            onInstalled = onInstalled,
            onBack = onBack,
            modifier = modifier
        )
        return
    }

    val factory = remember(recipe, capabilityChecker, baseSizeKb, installStore) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ConfiguratorViewModel(
                    initialState = ConfiguratorState.initial(recipe, capabilityChecker),
                    baseSizeKb = baseSizeKb,
                    installStore = installStore,
                    miniAppRepository = miniAppRepository
                ) as T
        }
    }
    val vm: ConfiguratorViewModel = viewModel(key = recipe.id, factory = factory)
    val state by vm.state.collectAsStateWithLifecycle()
    val snackHost = remember { SnackbarHostState() }
    val accent = parseHexColor(recipe.theme.primary, fallback = BinaAccent)

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is ConfiguratorEvent.Installed -> onInstalled(event.recipeName)
                is ConfiguratorEvent.AlreadyInstalled -> {
                    snackHost.showSnackbar("Already installed — opening MyPocket.")
                    onInstalled(recipe.name)
                }
                is ConfiguratorEvent.Failed -> {
                    snackHost.showSnackbar("Couldn't save install: ${event.message}")
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = {
                    Column {
                        Text(recipe.name, fontWeight = FontWeight.SemiBold, fontSize = 18.sp)
                        Text("Choose features you need", fontSize = 11.sp, color = BinaGrayText)
                    }
                },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color.Transparent)
            )
        },
        snackbarHost = { SnackbarHost(snackHost) },
        bottomBar = {
            Column(modifier = Modifier.padding(16.dp)) {
                Button(
                    onClick = vm::install,
                    modifier = Modifier.fillMaxWidth().height(54.dp),
                    shape = RoundedCornerShape(16.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = BinaAccent)
                ) {
                    Text("Install to Pocket", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    "You can change these settings anytime by uninstalling and re-installing.",
                    fontSize = 10.sp,
                    color = BinaGrayText,
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)
                )
            }
        }
    ) { padding ->
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                top = padding.calculateTopPadding() + 8.dp,
                bottom = padding.calculateBottomPadding() + 8.dp,
                start = 16.dp,
                end = 16.dp
            ),
            verticalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            item {
                ConfiguratorHeader(
                    totalSizeKb = state.totalSizeKb(baseSizeKb),
                    activeCount = state.activeCount,
                    totalCount = state.totalCount
                )
            }
            items(recipe.features, key = { it.id }) { feature ->
                FeatureToggleCard(
                    feature = feature,
                    isEnabled = state.toggles[feature.id] == true,
                    isToggleable = state.availability[feature.id] == true,
                    accentColor = accent,
                    onToggle = { on -> vm.toggleFeature(feature.id, on) }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun UnavailableScaffold(onBack: () -> Unit, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text("Recipe unavailable") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Box(modifier = Modifier.fillMaxSize().padding(padding), contentAlignment = Alignment.Center) {
            Text("This recipe is no longer available.", color = BinaGrayText)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EmptyFeaturesScaffold(
    recipe: MiniApp,
    installStore: InstallStore,
    capabilityChecker: CapabilityChecker,
    miniAppRepository: MiniAppRepository,
    onInstalled: (String) -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val factory = remember(recipe, installStore, capabilityChecker) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                ConfiguratorViewModel(
                    initialState = ConfiguratorState.initial(recipe, capabilityChecker),
                    baseSizeKb = 0f,
                    installStore = installStore,
                    miniAppRepository = miniAppRepository
                ) as T
        }
    }
    val vm: ConfiguratorViewModel = viewModel(key = recipe.id + "_empty", factory = factory)
    val snackHost = remember { SnackbarHostState() }

    LaunchedEffect(vm) {
        vm.events.collect { event ->
            when (event) {
                is ConfiguratorEvent.Installed -> onInstalled(event.recipeName)
                is ConfiguratorEvent.AlreadyInstalled -> {
                    snackHost.showSnackbar("Already installed — opening MyPocket.")
                    onInstalled(recipe.name)
                }
                is ConfiguratorEvent.Failed -> {
                    snackHost.showSnackbar("Couldn't save install: ${event.message}")
                }
            }
        }
    }

    Scaffold(
        modifier = modifier,
        topBar = {
            TopAppBar(
                title = { Text(recipe.name) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        },
        snackbarHost = { SnackbarHost(snackHost) }
    ) { padding ->
        Column(
            modifier = Modifier.fillMaxSize().padding(padding).padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text("This recipe has no configurable features.", fontSize = 14.sp, color = BinaGrayText)
            Spacer(Modifier.height(16.dp))
            Button(onClick = vm::install, colors = ButtonDefaults.buttonColors(containerColor = BinaAccent)) {
                Text("Install with defaults", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

private fun parseHexColor(hex: String, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
