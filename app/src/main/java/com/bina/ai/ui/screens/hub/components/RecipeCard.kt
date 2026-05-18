package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.bina.ai.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.theme.BinaAccent
import com.bina.ai.ui.theme.BinaBgCard
import com.bina.ai.ui.theme.BinaGrayBorder
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaRed
import com.bina.ai.ui.theme.BinaStone950

private val LANG_TO_FLAG = mapOf(
    "ms" to "🇲🇾",
    "en" to "🇬🇧",
    "zh" to "🇨🇳",
    "ta" to "🇮🇳",
    "id" to "🇮🇩",
    "th" to "🇹🇭",
    "km" to "🇰🇭",
    "my" to "🇲🇲",
    "vi" to "🇻🇳",
    "tl" to "🇵🇭",
)

@Composable
fun RecipeCard(
    miniApp: MiniApp,
    isInstalled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val originFlag = LANG_TO_FLAG[miniApp.localisation.defaultLanguage]

    Column(
        modifier = modifier
            .width(160.dp)
            .clip(RoundedCornerShape(14.dp))
            .border(1.dp, BinaGrayBorder, RoundedCornerShape(14.dp))
            .background(BinaBgCard)
            .clickable { onClick() }
            .padding(6.dp)
    ) {
        Box(modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f)) {
            RecipeCover(miniApp, modifier = Modifier.fillMaxWidth().aspectRatio(4f / 3f), cornerRadius = 10.dp, emojiFontSize = 32)
            if (miniApp.emergency) {
                Box(
                    modifier = Modifier
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(BinaRed)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(stringResource(R.string.sos_badge), fontSize = 7.sp, fontWeight = FontWeight.Bold, color = Color.White)
                }
            }
            if (originFlag != null) {
                Box(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(Color.White.copy(alpha = 0.92f))
                        .padding(horizontal = 4.dp, vertical = 1.dp)
                ) {
                    Text(originFlag, fontSize = 10.sp)
                }
            }
            Box(
                modifier = Modifier.align(Alignment.TopEnd).padding(4.dp)
            ) {
                BadgePill("✓")
            }
        }
        Column(modifier = Modifier.padding(top = 3.dp, start = 2.dp, end = 2.dp, bottom = 2.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                Text(com.bina.ai.ui.localizedName(miniApp), fontSize = 13.sp, lineHeight = 15.sp, fontWeight = FontWeight.SemiBold, color = BinaStone950, maxLines = 1, modifier = Modifier.weight(1f, fill = false))
                Icon(Icons.Filled.Verified, contentDescription = null, tint = BinaAccent, modifier = Modifier.size(13.dp))
            }
            val translatedCategory = com.bina.ai.ui.localizedCategory(miniApp.category)
            val meta = listOfNotNull(
                translatedCategory.takeIf { it.isNotBlank() },
                miniApp.dialect.takeIf { it.isNotBlank() }
            ).joinToString(" · ")
            if (meta.isNotBlank()) {
                Text(meta, fontSize = 10.sp, lineHeight = 12.sp, color = BinaGrayText, maxLines = 1)
            }
            if (isInstalled) {
                Text("✓ ${stringResource(R.string.hub_installed)}", fontSize = 9.sp, lineHeight = 11.sp, fontWeight = FontWeight.SemiBold, color = BinaGreen)
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
