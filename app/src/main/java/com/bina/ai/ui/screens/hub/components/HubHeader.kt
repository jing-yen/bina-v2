package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.navigation.UserMode
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun HubHeader(
    mode: UserMode,
    modifier: Modifier = Modifier
) {
    val (title, subtitle) = when (mode) {
        UserMode.BUILDER -> "Discover AI Recipes" to "Edge-native AI for every domain"
        UserMode.ARCHITECT -> "Recipe Marketplace" to "Author and discover recipes"
    }
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(title, fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BinaPrimary)
                if (mode == UserMode.ARCHITECT) CreatorPill()
            }
            Text(subtitle, fontSize = 12.sp, color = BinaGrayText)
        }
    }
}

@Composable
private fun CreatorPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(10.dp))
            .background(BinaGreen.copy(alpha = 0.18f))
            .padding(horizontal = 8.dp, vertical = 2.dp)
    ) {
        Text("CREATOR", fontSize = 9.sp, fontWeight = FontWeight.Bold, color = BinaGreen)
    }
}
