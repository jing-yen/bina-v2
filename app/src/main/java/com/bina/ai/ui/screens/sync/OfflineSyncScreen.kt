package com.bina.ai.ui.screens.sync

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.QrCodeScanner
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bina.ai.install.InstallStore
import com.bina.ai.ui.screens.recipe_detail.RecipeDetailSheet
import com.bina.ai.ui.screens.sync.components.ShareRecipePickerSheet
import com.bina.ai.ui.screens.sync.components.SyncActionCard
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun OfflineSyncScreen(
    vm: SyncViewModel,
    installStore: InstallStore,
    onScan: () -> Unit,
    onShare: (String) -> Unit,
    onConfigureRecipe: (String) -> Unit
) {
    val installed by vm.installedRecipesForShare.collectAsStateWithLifecycle()
    val incoming by vm.incoming.collectAsStateWithLifecycle()
    val installedIds by installStore.installs.collectAsStateWithLifecycle(initialValue = emptyMap())

    var showPicker by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier.fillMaxSize().padding(horizontal = 20.dp, vertical = 16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text("Offline Sync", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BinaPrimary)
        Text(
            "Share recipes phone-to-phone, no internet needed.",
            fontSize = 12.sp, color = BinaGrayText
        )
        Spacer(Modifier.height(8.dp))
        SyncActionCard(
            title = "Scan to Receive",
            subtitle = "Scan another phone's QR or paste a YAML to install.",
            icon = Icons.Filled.QrCodeScanner,
            accentColor = BinaPrimary,
            onClick = onScan
        )
        SyncActionCard(
            title = "Share a Recipe",
            subtitle = "Pick an installed recipe to share via QR.",
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

    // Receive preview — wired in Task 10. Stub here so scan/paste can flow later.
    val ready = incoming as? IncomingState.Ready
    if (ready != null) {
        RecipeDetailSheet(
            miniApp = ready.miniApp,
            isInstalled = ready.miniApp.id in installedIds.keys,
            sizeKb = ready.yaml.length / 1024f,
            onConfigureInstall = {
                val id = vm.confirmInstall()
                if (id != null) onConfigureRecipe(id)
            },
            onOpen = { vm.dismissPreview() },
            onDismiss = { vm.dismissPreview() }
        )
    }
}
