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
import androidx.compose.ui.res.stringResource
import com.bina.ai.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.ui.model.RecipeStats
import com.bina.ai.ui.theme.BinaAmber
import com.bina.ai.ui.theme.BinaBgCard
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaStone950

@Composable
fun RecipeLeaderboard(
    rows: List<RecipeStats>,
    modifier: Modifier = Modifier
) {
    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(
            stringResource(R.string.analytics_most_used),
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = BinaStone950,
            modifier = Modifier.padding(start = 4.dp, bottom = 4.dp)
        )
        if (rows.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(BinaBgCard.copy(alpha = 0.92f))
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    stringResource(R.string.analytics_no_activity_window),
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
            .background(BinaBgCard.copy(alpha = 0.92f))
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
            Text(row.displayName, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BinaStone950)
            Text(
                stringResource(R.string.analytics_launches_asks, row.launches, row.asks),
                fontSize = 11.sp,
                color = BinaGrayText
            )
        }
        Text(
            "${row.total}",
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = BinaStone950
        )
    }
}

private fun rankColor(rank: Int) = when (rank) {
    1 -> BinaAmber
    2 -> Color(0xFF94A3B8)  // silver
    3 -> Color(0xFFB45309)  // bronze
    else -> BinaStone950
}
