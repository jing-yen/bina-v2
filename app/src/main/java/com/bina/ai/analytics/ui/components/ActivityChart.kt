package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.LinearOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.ui.model.DailyBucket
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun ActivityChart(
    buckets: List<DailyBucket>,
    modifier: Modifier = Modifier
) {
    var animationProgress by remember { mutableStateOf(0f) }
    LaunchedEffect(buckets) {
        animationProgress = 0f
        animationProgress = 1f
    }
    val animProgress by animateFloatAsState(
        targetValue = animationProgress,
        animationSpec = tween(700, easing = LinearOutSlowInEasing),
        label = "chart-rise"
    )

    var selectedIndex by remember { mutableStateOf(-1) }
    val selectedLabel = if (selectedIndex in buckets.indices) {
        val b = buckets[selectedIndex]
        val day = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(b.dayStartMs))
        "$day · ${b.launches} launches · ${b.asks} asks"
    } else null

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        Text("Daily Activity", fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = BinaPrimary)
        Spacer(Modifier.height(4.dp))
        Text(
            selectedLabel ?: "Tap a bar for details",
            fontSize = 11.sp,
            color = BinaGrayText
        )
        Spacer(Modifier.height(12.dp))

        Canvas(
            modifier = Modifier
                .fillMaxWidth()
                .height(140.dp)
                .pointerInput(buckets) {
                    detectTapGestures { tap ->
                        if (buckets.isEmpty()) return@detectTapGestures
                        val barTotalWidth = size.width / buckets.size
                        val idx = (tap.x / barTotalWidth).toInt().coerceIn(0, buckets.size - 1)
                        selectedIndex = if (selectedIndex == idx) -1 else idx
                    }
                }
        ) {
            if (buckets.isEmpty()) return@Canvas
            val maxTotal = (buckets.maxOfOrNull { it.total } ?: 1).coerceAtLeast(1)
            val barTotalWidth = size.width / buckets.size
            val barWidth = barTotalWidth * 0.6f
            val barOffsetX = (barTotalWidth - barWidth) / 2

            buckets.forEachIndexed { i, b ->
                val staggerStart = i.toFloat() / buckets.size * 0.3f
                val barProgress = ((animProgress - staggerStart) / (1f - staggerStart))
                    .coerceIn(0f, 1f)

                val totalH = (b.total.toFloat() / maxTotal) * size.height * barProgress
                val launchH = (b.launches.toFloat() / maxTotal) * size.height * barProgress
                val askH = (b.asks.toFloat() / maxTotal) * size.height * barProgress

                val x = i * barTotalWidth + barOffsetX
                val isSelected = i == selectedIndex
                val alpha = if (selectedIndex == -1 || isSelected) 1f else 0.4f

                // Launches segment (bottom)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(BinaPrimary.copy(alpha = alpha), BinaPrimary.copy(alpha = alpha * 0.7f))
                    ),
                    topLeft = Offset(x, size.height - launchH),
                    size = Size(barWidth, launchH)
                )
                // Asks segment (stacked on top)
                drawRect(
                    brush = Brush.verticalGradient(
                        colors = listOf(BinaGreen.copy(alpha = alpha), BinaGreen.copy(alpha = alpha * 0.7f))
                    ),
                    topLeft = Offset(x, size.height - launchH - askH),
                    size = Size(barWidth, askH)
                )
            }
        }
    }
}
