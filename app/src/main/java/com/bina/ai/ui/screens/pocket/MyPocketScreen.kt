package com.bina.ai.ui.screens.pocket

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material.icons.outlined.AddToHomeScreen
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.platform.LocalContext
import androidx.compose.runtime.collectAsState
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.bina.ai.install.InstallStore
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.R
import com.bina.ai.install.ShortcutHelper
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.theme.BinaAccent
import com.bina.ai.ui.theme.BinaBgCard
import com.bina.ai.ui.theme.BinaGrayBorder
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
    val context = LocalContext.current

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        item {
            Text(
                stringResource(R.string.pocket_title),
                fontWeight = FontWeight.Bold,
                fontSize = 20.sp,
                lineHeight = 24.sp,
                color = BinaStone950
            )
            Spacer(Modifier.height(2.dp))
            Text(
                stringResource(R.string.pocket_subtitle),
                fontSize = 13.sp,
                lineHeight = 17.sp,
                color = BinaGrayText
            )
        }

        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                StatCard(
                    label = stringResource(R.string.pocket_stat_saved),
                    value = miniApps.size.toString(),
                    icon = Icons.Filled.Inventory2,
                    color = BinaAccent,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.pocket_stat_screens),
                    value = totalScreens.toString(),
                    icon = Icons.Filled.Widgets,
                    color = BinaTurmeric,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = stringResource(R.string.pocket_stat_status),
                    value = stringResource(R.string.pocket_stat_ready),
                    icon = Icons.Filled.CloudDone,
                    color = BinaIndigo,
                    modifier = Modifier.weight(1f)
                )
            }
        }

        item {
            Text(
                stringResource(R.string.pocket_saved_recipes),
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
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
                        Text(stringResource(R.string.pocket_empty_title), color = BinaGrayText, fontSize = 15.sp)
                        Spacer(Modifier.height(4.dp))
                        Text(
                            stringResource(R.string.pocket_empty_subtitle),
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
                onAddToHome = {
                    ShortcutHelper.pinRecipeToHomeScreen(
                        context = context,
                        recipeId = app.id,
                        recipeName = app.name,
                        emoji = app.icon
                    )
                }
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
            .padding(10.dp)
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.fillMaxWidth()) {
            Icon(
                icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.height(4.dp))
            Text(
                value,
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp,
                lineHeight = 20.sp,
                color = color
            )
            Text(
                label,
                fontSize = 10.sp,
                lineHeight = 13.sp,
                color = BinaGrayText
            )
        }
    }
}

@Composable
private fun PocketMiniAppCard(app: MiniApp, onClick: () -> Unit, onAddToHome: () -> Unit = {}) {
    val themeColor = try {
        Color(android.graphics.Color.parseColor(app.theme.primary))
    } catch (_: Exception) {
        BinaAccent
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, BinaGrayBorder, RoundedCornerShape(16.dp))
            .background(BinaBgCard.copy(alpha = 0.92f))
            .clickable(onClick = onClick)
            .padding(12.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .clip(RoundedCornerShape(14.dp))
                    .background(themeColor.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Text(app.icon, fontSize = 24.sp)
            }

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    app.name,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    lineHeight = 18.sp,
                    color = BinaStone950
                )
                Text(
                    app.description,
                    fontSize = 12.sp,
                    lineHeight = 16.sp,
                    color = BinaGrayText,
                    maxLines = 2
                )
                Spacer(Modifier.height(3.dp))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .clip(RoundedCornerShape(6.dp))
                            .background(BinaGreen.copy(alpha = 0.12f))
                            .padding(horizontal = 6.dp, vertical = 1.dp)
                    ) {
                        Text(stringResource(R.string.pocket_offline_ready), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = BinaGreen)
                    }
                    Text(
                        stringResource(R.string.pocket_screens_count, app.screens.size),
                        fontSize = 10.sp,
                        color = BinaGrayText
                    )
                }
            }

            IconButton(onClick = onAddToHome, modifier = Modifier.size(32.dp)) {
                Icon(
                    Icons.Outlined.AddToHomeScreen,
                    contentDescription = stringResource(R.string.pocket_add_home),
                    tint = BinaAccent,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}
