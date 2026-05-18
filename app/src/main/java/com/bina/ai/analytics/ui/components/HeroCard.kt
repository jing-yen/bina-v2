package com.bina.ai.analytics.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingFlat
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import com.bina.ai.R
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.ui.model.MetricsSnapshot
import com.bina.ai.analytics.ui.model.TimeWindow
import com.bina.ai.analytics.ui.util.plural
import com.bina.ai.ui.theme.BinaAccent
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaRed
import kotlin.math.roundToInt

@Composable
fun HeroCard(
    metrics: MetricsSnapshot,
    sparklineValues: List<Int>,
    window: TimeWindow,
    modifier: Modifier = Modifier
) {
    val deltaPct = if (metrics.totalLaunchesPrevious > 0) {
        ((metrics.totalLaunches - metrics.totalLaunchesPrevious).toFloat()
            / metrics.totalLaunchesPrevious * 100f).roundToInt()
    } else if (metrics.totalLaunches > 0) {
        100
    } else {
        0
    }

    val (deltaIcon, deltaColor) = when {
        deltaPct > 0 -> Icons.Filled.TrendingUp to BinaGreen
        deltaPct < 0 -> Icons.Filled.TrendingDown to BinaRed
        else -> Icons.Filled.TrendingFlat to Color.White.copy(alpha = 0.7f)
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(28.dp))
            .background(
                Brush.linearGradient(listOf(BinaAccent, Color(0xFFE8936E)))
            )
            .padding(20.dp)
    ) {
        Column(modifier = Modifier.fillMaxWidth()) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top
            ) {
                Column {
                    Text(
                        stringResource(R.string.analytics_total_launches),
                        fontSize = 13.sp,
                        color = Color.White.copy(alpha = 0.85f),
                        fontWeight = FontWeight.Medium
                    )
                    Spacer(Modifier.height(6.dp))
                    AnimatedCounter(
                        target = metrics.totalLaunches,
                        style = TextStyle(
                            fontSize = 40.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    )
                    Spacer(Modifier.height(2.dp))
                    val prevLabel = when (window) {
                        TimeWindow.LAST_7D -> stringResource(R.string.analytics_vs_prev_7d)
                        TimeWindow.LAST_30D -> stringResource(R.string.analytics_vs_prev_30d)
                        TimeWindow.ALL_TIME -> stringResource(R.string.analytics_all_time)
                    }
                    val comparison = if (window == TimeWindow.ALL_TIME) {
                        "${metrics.totalLaunches} ${plural(metrics.totalLaunches, "launch", "launches")} ${stringResource(R.string.analytics_all_time)}"
                    } else {
                        val n = metrics.totalLaunchesPrevious
                        "$prevLabel: $n ${plural(n, "launch", "launches")}"
                    }
                    Text(
                        comparison,
                        fontSize = 11.sp,
                        color = Color.White.copy(alpha = 0.7f),
                        fontWeight = FontWeight.Medium
                    )
                }
                Row(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color.White.copy(alpha = 0.15f))
                        .padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    Icon(deltaIcon, null, tint = deltaColor, modifier = Modifier.size(14.dp))
                    Text(
                        text = if (deltaPct >= 0) "+${deltaPct}%" else "${deltaPct}%",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = deltaColor
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Sparkline(
                values = sparklineValues,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(40.dp),
                color = Color.White.copy(alpha = 0.85f)
            )
        }
    }
}
