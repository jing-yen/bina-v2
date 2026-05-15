package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.lerp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.ui.model.TimeWindow
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaStone950

/**
 * @param collapseFraction 0f = expanded (full title + subtitle), 1f = collapsed (compact).
 *   The screen feeds this from its scroll state.
 */
@Composable
fun AnalyticsHeader(
    selectedWindow: TimeWindow,
    onSelectWindow: (TimeWindow) -> Unit,
    collapseFraction: Float = 0f,
    modifier: Modifier = Modifier
) {
    val animated by animateFloatAsState(
        targetValue = collapseFraction,
        animationSpec = tween(180),
        label = "header-collapse"
    )

    val titleSize = lerp(22.dp, 18.dp, animated).value.sp
    val verticalPad = lerp(12.dp, 6.dp, animated)
    val subtitleAlpha = (1f - animated * 1.4f).coerceIn(0f, 1f)
    val subtitleHeight = lerp(36.dp, 0.dp, animated)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = verticalPad),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(
                "Analytics",
                fontWeight = FontWeight.Bold,
                fontSize = titleSize,
                color = BinaStone950
            )
            Box(modifier = Modifier.height(subtitleHeight).alpha(subtitleAlpha)) {
                Column {
                    Text(
                        "Your authoring and on-device usage",
                        fontSize = 12.sp,
                        color = BinaGrayText
                    )
                    Spacer(Modifier.height(4.dp))
                    LiveBadge()
                }
            }
        }
        TimeRangePill(selected = selectedWindow, onSelect = onSelectWindow)
    }
}

@Composable
private fun LiveBadge() {
    val pulse by rememberInfiniteTransition(label = "live-pulse").animateFloat(
        initialValue = 0.45f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(900),
            repeatMode = RepeatMode.Reverse
        ),
        label = "live-pulse-alpha"
    )
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Box(
            Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(BinaGreen.copy(alpha = pulse))
        )
        Text(
            "Live · on-device",
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = BinaGreen
        )
    }
}
