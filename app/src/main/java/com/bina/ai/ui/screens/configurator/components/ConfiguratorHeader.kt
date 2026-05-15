package com.bina.ai.ui.screens.configurator.components

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaStone950

@Composable
fun ConfiguratorHeader(
    totalSizeKb: Float,
    activeCount: Int,
    totalCount: Int,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(Color(0xFFF5F0EB))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text("Total Download Size", fontSize = 11.sp, color = BinaGrayText, fontWeight = FontWeight.Medium)
                AnimatedContent(
                    targetState = totalSizeKb,
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "size"
                ) { size ->
                    Text(
                        text = "%.1f KB".format(size),
                        fontSize = 24.sp,
                        fontWeight = FontWeight.Bold,
                        color = BinaStone950
                    )
                }
            }
            Column(horizontalAlignment = Alignment.End) {
                Text("Active Features", fontSize = 11.sp, color = BinaGrayText, fontWeight = FontWeight.Medium)
                AnimatedContent(
                    targetState = "$activeCount/$totalCount",
                    transitionSpec = { fadeIn(tween(200)) togetherWith fadeOut(tween(200)) },
                    label = "count"
                ) { txt ->
                    Text(txt, fontSize = 24.sp, fontWeight = FontWeight.Bold, color = BinaGreen)
                }
            }
        }
    }
}
