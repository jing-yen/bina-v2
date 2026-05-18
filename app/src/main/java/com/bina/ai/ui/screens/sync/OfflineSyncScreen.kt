package com.bina.ai.ui.screens.sync

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.launch
import com.bina.ai.R
import com.bina.ai.install.InstallStore
import com.bina.ai.ui.screens.recipe_detail.RecipeDetailSheet
import com.bina.ai.ui.screens.sync.components.ShareRecipePickerSheet
import com.bina.ai.ui.screens.sync.components.SyncActionCard
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaStone950

@Composable
fun OfflineSyncScreen(
    vm: SyncViewModel,
    installStore: InstallStore,
    onScan: () -> Unit,
    onShare: (String) -> Unit,
    onOpenRecipe: (String) -> Unit
) {
    val installed by vm.installedRecipesForShare.collectAsStateWithLifecycle()
    val incoming by vm.incoming.collectAsStateWithLifecycle()
    val installedIds by installStore.installs.collectAsStateWithLifecycle(initialValue = emptyMap())

    val scope = rememberCoroutineScope()
    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(stringResource(R.string.sync_title), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BinaStone950)
        Text(
            stringResource(R.string.sync_subtitle),
            fontSize = 12.sp, color = BinaGrayText
        )
        Spacer(Modifier.height(8.dp))
        SyncActionCard(
            title = stringResource(R.string.sync_scan_title),
            subtitle = stringResource(R.string.sync_scan_subtitle),
            icon = Icons.Filled.QrCodeScanner,
            accentColor = BinaGreen,
            onClick = onScan
        )
        SyncActionCard(
            title = stringResource(R.string.sync_share_title),
            subtitle = stringResource(R.string.sync_share_subtitle),
            icon = Icons.Filled.Share,
            accentColor = BinaGreen,
            onClick = { showPicker = true }
        )
    }

    if (showPicker) {
        ShareRecipePickerSheet(
            recipes = installed,
            onPick = { recipe ->
                showPicker = false
                onShare(recipe.id)
            },
            onDismiss = { showPicker = false }
        )
    }

    val ready = incoming as? IncomingState.Ready
    if (ready != null) {
        RecipeDetailSheet(
            miniApp = ready.miniApp,
            isInstalled = ready.miniApp.id in installedIds.keys,
            sizeKb = ready.yaml.length / 1024f,
            onInstall = {
                val ready2 = vm.incoming.value as? IncomingState.Ready
                val id = vm.confirmInstall()
                if (id != null && ready2 != null) {
                    scope.launch {
                        try {
                            installStore.install(com.bina.ai.install.model.InstallRecord(
                                recipeId = id,
                                installedAt = System.currentTimeMillis(),
                                enabledFeatureIds = ready2.miniApp.features.map { it.id }.toSet()
                            ))
                        } catch (_: Exception) { }
                        onOpenRecipe(id)
                    }
                }
            },
            onOpen = { vm.dismissPreview() },
            onDismiss = { vm.dismissPreview() }
        )
    }
}
