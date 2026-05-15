package com.bina.ai.ui.screens.pocket

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
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bina.ai.install.InstallStore
import kotlinx.coroutines.launch
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.theme.BinaAccent
import com.bina.ai.ui.theme.BinaBgCard
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaIndigo
import com.bina.ai.ui.theme.BinaStone950
import com.bina.ai.ui.theme.BinaTurmeric

@Composable
fun MyPocketScreen(
    miniAppRepository: MiniAppRepository,
    installStore: InstallStore,
    onMiniAppClick: (String) -> Unit = {}
) {
    val installs by installStore.installs.collectAsStateWithLifecycle(initialValue = emptyMap())
    val cloudVersion by miniAppRepository.cloudVersion.collectAsState()
    val miniApps = remember(installs, cloudVersion) {
        val local = miniAppRepository.loadAll()
        installs.keys.mapNotNull { id -> local.find { it.id == id } ?: miniAppRepository.getById(id) }
    }
    val totalScreens = miniApps.sumOf { it.screens.size }
    val totalWidgets = miniApps.sumOf { app -> app.screens.sumOf { it.body.size } }
    val scope = rememberCoroutineScope()
    var confirmDeleteApp by remember { mutableStateOf<MiniApp?>(null) }

    confirmDeleteApp?.let { app ->
        AlertDialog(
            onDismissRequest = { confirmDeleteApp = null },
            title = { Text("Remove ${app.name}?") },
            text = { Text("This will remove the recipe from your pocket. You can reinstall it from the Hub anytime.") },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch { installStore.uninstall(app.id) }
                    confirmDeleteApp = null
                }) { Text("Remove", color = Color(0xFFDC2626)) }
            },
            dismissButton = {
                TextButton(onClick = { confirmDeleteApp = null }) { Text("Cancel") }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "My Pocket",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = BinaStone950
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "Your saved recipes, ready offline",
                fontSize = 14.sp,
                color = BinaGrayText
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label = "Saved",
                    value = miniApps.size.toString(),
                    icon = Icons.Filled.Inventory2,
                    color = BinaAccent,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Screens",
                    value = totalScreens.toString(),
                    icon = Icons.Filled.Widgets,
                    color = BinaTurmeric,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Status",
                    value = "Ready",
                    icon = Icons.Filled.CloudDone,
                    color = BinaIndigo,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Spacer(Modifier.height(4.dp))
            Text(
                "Saved Recipes",
                fontWeight = FontWeight.SemiBold,
                fontSize = 16.sp,
                color = BinaStone950
            )
        }

        if (miniApps.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(BinaBgCard.copy(alpha = 0.92f))
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Filled.Inventory2,
                            contentDescription = null,
                            tint = BinaGrayText,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(Modifier.height(12.dp))
                        Text("No saved recipes yet", color = BinaGrayText, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            "Browse the Hub to save recipes to your pocket",
                            color = BinaGrayText,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        items(miniApps, key = { it.id }) { app ->
            PocketMiniAppCard(
                app,
                onClick = { onMiniAppClick(app.id) },
                onRemove = { confirmDeleteApp = app }
            )
        }
    }
}

@Composable
private fun StatCard(
    label: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .background(BinaBgCard.copy(alpha = 0.92f))
            .padding(12.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(22.dp)
            )
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                color = color
            )
            Text(
                label,
                fontSize = 11.sp,
                color = BinaGrayText
            )
        }
    }
}

@Composable
private fun PocketMiniAppCard(app: MiniApp, onClick: () -> Unit, onRemove: () -> Unit = {}) {
    val themeColor = try {
        Color(android.graphics.Color.parseColor(app.theme.primary))
    } catch (_: Exception) {
        BinaAccent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(BinaBgCard.copy(alpha = 0.92f))
            .clickable(onClick = onClick)
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(themeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(app.icon, fontSize = 28.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 16.sp,
                    color = BinaStone950
                )
                Text(
                    app.description,
                    fontSize = 13.sp,
                    color = BinaGrayText,
                    maxLines = 2
                )
                Spacer(Modifier.height(4.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BinaGreen.copy(alpha = 0.12f))
                            .padding(horizontal = 8.dp, vertical = 2.dp)
                    ) {
                        Text("Offline Ready", fontSize = 11.sp, fontWeight = FontWeight.Medium, color = BinaGreen)
                    }
                    Text(
                        "${app.screens.size} screens",
                        fontSize = 11.sp,
                        color = BinaGrayText
                    )
                }
            }

            IconButton(onClick = onRemove, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Filled.Delete,
                    contentDescription = "Remove recipe",
                    tint = BinaGrayText,
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }
}
