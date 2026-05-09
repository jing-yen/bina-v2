package com.bina.ai.analytics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
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
import com.bina.ai.analytics.ui.model.RecipeStats
import com.bina.ai.ui.theme.BinaAmber
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun RecipeLeaderboard(
    rows: List<RecipeStats>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            "Most-Used Recipes",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = BinaPrimary,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        if (rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.9f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "No recipe activity in this window yet",
                    fontSize = 12.sp,
                    color = BinaGrayText
                )
            }
        } else {
            rows.take(8).forEachIndexed { index, row ->
                LeaderboardRow(rank = index + 1, row = row)
            }
        }
    }
}

@Composable
private fun LeaderboardRow(rank: Int, row: RecipeStats) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(28.dp)
                .clip(CircleShape)
                .background(rankColor(rank).copy(alpha = 0.18f)),
            contentAlignment = Alignment.Center
        ) {
            Text("$rank", fontSize = 12.sp, fontWeight = FontWeight.Bold, color = rankColor(rank))
        }
        Text(row.icon, fontSize = 22.sp)
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(row.displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BinaPrimary)
                AuthoredBadge(authored = row.isAuthored)
            }
            Text(
                "${row.launches} launches · ${row.asks} asks",
                fontSize = 11.sp,
                color = BinaGrayText
            )
        }
        Text(
            "${row.total}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = BinaPrimary
        )
    }
}

@Composable
private fun AuthoredBadge(authored: Boolean) {
    val (label, color) = if (authored) "Authored" to BinaGreen else "Bundled" to BinaGrayText
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(6.dp))
            .background(color.copy(alpha = 0.15f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(label, fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = color)
    }
}

private fun rankColor(rank: Int) = when (rank) {
    1 -> BinaAmber
    2 -> Color(0xFF94A3B8)  // silver
    3 -> Color(0xFFB45309)  // bronze
    else -> BinaPrimary
}
