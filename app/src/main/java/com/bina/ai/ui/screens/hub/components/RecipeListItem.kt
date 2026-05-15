package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
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
import com.bina.ai.ui.theme.BinaIndigo
import com.bina.ai.ui.theme.BinaStone950

@Composable
fun RecipeListItem(
    miniApp: MiniApp,
    isInstalled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(BinaBgCard)
            .clickable { onClick() }
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        RecipeCover(miniApp, modifier = Modifier.size(64.dp), cornerRadius = 12.dp, emojiFontSize = 28)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(miniApp.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BinaStone950, maxLines = 1)
                if (miniApp.author.verified) {
                    Text("✓", fontSize = 12.sp, color = BinaGreen)
                }
            }
            if (miniApp.description.isNotBlank()) {
                Text(miniApp.description, fontSize = 11.sp, color = BinaGrayText, maxLines = 2)
            }
            val meta = listOfNotNull(
                miniApp.category.takeIf { it.isNotBlank() },
                miniApp.dialect.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, fontSize = 10.sp, color = BinaGrayText)
            }
            if (miniApp.tags.isNotEmpty()) {
                Spacer(Modifier.height(4.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    miniApp.tags.take(2).forEach { tag ->
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(BinaIndigo.copy(alpha = 0.08f))
                                .padding(horizontal = 5.dp, vertical = 1.dp)
                        ) {
                            Text("#$tag", fontSize = 9.sp, color = BinaIndigo)
                        }
                    }
                    if (miniApp.tags.size > 2) {
                        Text("+${miniApp.tags.size - 2}", fontSize = 9.sp, color = BinaGrayText)
                    }
                }
            }
        }
        if (isInstalled) {
            Text("✓ Installed", fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = BinaGreen)
        }
    }
}
