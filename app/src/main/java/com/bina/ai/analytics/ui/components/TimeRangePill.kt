package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.ui.model.TimeWindow
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun TimeRangePill(
    selected: TimeWindow,
    onSelect: (TimeWindow) -> Unit,
    modifier: Modifier = Modifier
) {
    val options = TimeWindow.entries
    val pillWidth = 64.dp
    val pillHeight = 32.dp

    Box(
        modifier = modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.7f))
            .padding(4.dp)
            .height(pillHeight)
    ) {
        val selectedIndex = options.indexOf(selected)
        val indicatorOffsetX by animateDpAsState(
            targetValue = pillWidth * selectedIndex,
            animationSpec = spring(stiffness = 400f, dampingRatio = 0.8f),
            label = "indicator"
        )

        // Sliding indicator
        Box(
            modifier = Modifier
                .width(pillWidth)
                .fillMaxHeight()
                .padding(start = indicatorOffsetX)
                .clip(RoundedCornerShape(16.dp))
                .background(BinaPrimary)
        )

        // Labels
        androidx.compose.foundation.layout.Row {
            options.forEach { window ->
                val isSelected = window == selected
                val textAlpha by animateFloatAsState(if (isSelected) 1f else 0.6f, label = "alpha")
                Box(
                    modifier = Modifier
                        .width(pillWidth)
                        .fillMaxHeight()
                        .clickable { onSelect(window) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = window.label,
                        fontSize = 12.sp,
                        fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
                        color = if (isSelected) Color.White else BinaPrimary.copy(alpha = textAlpha)
                    )
                }
            }
        }
    }
}
