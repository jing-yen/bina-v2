package com.bina.ai.ui.screens.hub

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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
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
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaGrayText

@Composable
fun HubScreen(
    miniAppRepository: MiniAppRepository,
    onMiniAppClick: (String) -> Unit = {}
) {
    val miniApps = remember { miniAppRepository.loadAll() }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = PaddingValues(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            Text(
                "Discover AI Recipes",
                fontWeight = FontWeight.Bold,
                fontSize = 22.sp,
                color = BinaPrimary,
                modifier = Modifier.padding(bottom = 8.dp)
            )
        }

        if (miniApps.isEmpty()) {
            item {
                Box(
                    Modifier.fillMaxWidth().padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text("No miniapps found", color = BinaGrayText)
                }
            }
        }

        items(miniApps, key = { it.id }) { app ->
            MiniAppCard(app, onClick = { onMiniAppClick(app.id) })
        }
    }
}

@Composable
private fun MiniAppCard(app: MiniApp, onClick: () -> Unit) {
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
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    CategoryBadge(app.category, themeColor)
                    if (app.author.verified) {
                        Text("✓ Verified", fontSize = 11.sp, color = themeColor)
                    }
                }
                Text(
                    "${app.screens.size} screens · ${countWidgets(app)} widgets",
                    fontSize = 11.sp,
                    color = BinaGrayText,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun CategoryBadge(category: String, color: Color) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.12f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text(category, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = color)
    }
}

private fun countWidgets(app: MiniApp): Int =
    app.screens.sumOf { it.body.size }
