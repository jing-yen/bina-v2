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
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.Widgets
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.MiniAppRepository
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun MyPocketScreen(
    miniAppRepository: MiniAppRepository,
    onMiniAppClick: (String) -> Unit = {}
) {
    val miniApps = remember { miniAppRepository.loadAll() }
    val totalScreens = miniApps.sumOf { it.screens.size }
    val totalWidgets = miniApps.sumOf { app -> app.screens.sumOf { it.body.size } }

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
                color = BinaPrimary
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
                    color = BinaPrimary,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Screens",
                    value = totalScreens.toString(),
                    icon = Icons.Filled.Widgets,
                    color = BinaGreen,
                    modifier = Modifier.weight(1f)
                )
                StatCard(
                    label = "Status",
                    value = "Ready",
                    icon = Icons.Filled.CloudDone,
                    color = Color(0xFF6366F1),
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
                color = Color(0xFF1A1A2E)
            )
        }

        if (miniApps.isEmpty()) {
            item {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.9f))
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
            PocketMiniAppCard(app, onClick = { onMiniAppClick(app.id) })
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
            .background(Color.White.copy(alpha = 0.9f))
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
private fun PocketMiniAppCard(app: MiniApp, onClick: () -> Unit) {
    val themeColor = try {
        Color(android.graphics.Color.parseColor(app.theme.primary))
    } catch (_: Exception) {
        BinaPrimary
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.9f))
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
                    color = Color(0xFF1A1A2E)
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

            Icon(
                Icons.Filled.CloudDone,
                contentDescription = "Saved",
                tint = BinaGreen,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}
