package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.theme.BinaAccent
import com.bina.ai.ui.theme.BinaBgCard
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaRed
import com.bina.ai.ui.theme.BinaStone950

@Composable
fun RecipeCard(
    miniApp: MiniApp,
    isInstalled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .width(150.dp)
            .clip(RoundedCornerShape(16.dp))
            .background(BinaBgCard)
            .clickable { onClick() }
            .padding(8.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(1f)) {
            RecipeCover(miniApp, modifier = Modifier.fillMaxWidth().aspectRatio(1f), cornerRadius = 12.dp, emojiFontSize = 38)
            if (miniApp.emergency) {
                Box(
                    modifier = Modifier
                        .padding(6.dp)
                        .clip(RoundedCornerShape(8.dp))
                        .background(BinaRed)
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text("EMERGENCY", fontSize = 8.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            if (miniApp.author.verified) {
                Box(
                    modifier = Modifier.align(Alignment.TopEnd).padding(6.dp)
                ) {
                    BadgePill("✓ Verified")
                }
            }
        }
        Column {
            Text(miniApp.name, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = BinaStone950, maxLines = 1)
            val meta = listOfNotNull(
                miniApp.category.takeIf { it.isNotBlank() },
                miniApp.dialect.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, fontSize = 10.sp, color = BinaGrayText, maxLines = 1)
            }
            if (isInstalled) {
                Spacer(Modifier.height(2.dp))
                Text("✓ Installed", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = BinaGreen)
            }
        }
    }
}

@Composable
private fun BadgePill(text: String) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(Color.White.copy(alpha = 0.92f))
            .padding(horizontal = 5.dp, vertical = 1.dp)
    ) {
        Text(text, fontSize = 8.sp, color = BinaAccent, fontWeight = FontWeight.SemiBold)
    }
}
