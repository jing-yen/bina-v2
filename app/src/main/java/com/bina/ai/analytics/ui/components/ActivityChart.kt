package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.ui.model.DailyBucket
import com.bina.ai.analytics.ui.util.plural
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val COLS = 7

@Composable
fun ActivityChart(
    buckets: List<DailyBucket>,
    modifier: Modifier = Modifier
) {
    var selectedIndex by remember { mutableStateOf(-1) }
    val selected = buckets.getOrNull(selectedIndex)

    val maxTotal = (buckets.maxOfOrNull { it.total } ?: 0).coerceAtLeast(1)
    val rowCount = (buckets.size + COLS - 1) / COLS
    val totalCells = rowCount * COLS
    // Pad leading cells so "today" lands at bottom-right (natural reading order).
    val padCount = totalCells - buckets.size

    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White.copy(alpha = 0.9f))
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                "Daily Activity",
                fontWeight = FontWeight.SemiBold,
                fontSize = 14.sp,
                color = BinaPrimary
            )
            HeatmapLegend()
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = selected?.let {
                val date = SimpleDateFormat("MMM d", Locale.getDefault()).format(Date(it.dayStartMs))
                "$date · ${it.launches} ${plural(it.launches, "launch", "launches")} · " +
                    "${it.asks} ${plural(it.asks, "ask", "asks")}"
            } ?: "Tap a day for details",
            fontSize = 11.sp,
            color = BinaGrayText
        )
        Spacer(Modifier.height(12.dp))

        Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
            for (r in 0 until rowCount) {
                Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                    for (c in 0 until COLS) {
                        val cellIdx = r * COLS + c
                        val bucketIdx = cellIdx - padCount
                        if (bucketIdx in buckets.indices) {
                            HeatmapCell(
                                bucket = buckets[bucketIdx],
                                maxTotal = maxTotal,
                                isSelected = bucketIdx == selectedIndex,
                                hasSelection = selectedIndex != -1,
                                isToday = bucketIdx == buckets.lastIndex,
                                animationDelayMs = bucketIdx * 30L,
                                onTap = {
                                    selectedIndex = if (selectedIndex == bucketIdx) -1 else bucketIdx
                                },
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f).aspectRatio(1f))
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun HeatmapCell(
    bucket: DailyBucket,
    maxTotal: Int,
    isSelected: Boolean,
    hasSelection: Boolean,
    isToday: Boolean,
    animationDelayMs: Long,
    onTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    var visible by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(animationDelayMs)
        visible = true
    }
    val alpha by animateFloatAsState(
        targetValue = if (visible) 1f else 0f,
        animationSpec = tween(200),
        label = "heatmap-cell-alpha"
    )

    val intensity = bucket.total.toFloat() / maxTotal
    val baseAlpha = when {
        bucket.total == 0 -> 0.08f
        intensity < 0.34f -> 0.35f
        intensity < 0.67f -> 0.65f
        else -> 1f
    }
    val baseColor = if (bucket.total == 0) {
        Color(0xFFE5E7EB)
    } else {
        BinaGreen.copy(alpha = baseAlpha)
    }

    val cellAlpha = if (hasSelection && !isSelected) 0.35f else 1f

    Box(
        modifier = modifier
            .aspectRatio(1f)
            .clip(RoundedCornerShape(6.dp))
            .background(baseColor.copy(alpha = baseColor.alpha * cellAlpha * alpha))
            .then(
                if (isSelected) {
                    Modifier.border(2.dp, BinaPrimary, RoundedCornerShape(6.dp))
                } else if (isToday) {
                    Modifier.border(1.5.dp, BinaPrimary.copy(alpha = 0.45f), RoundedCornerShape(6.dp))
                } else Modifier
            )
            .clickable { onTap() }
    )
}

@Composable
private fun HeatmapLegend(modifier: Modifier = Modifier) {
    val steps = listOf(
        Color(0xFFE5E7EB),
        BinaGreen.copy(alpha = 0.35f),
        BinaGreen.copy(alpha = 0.65f),
        BinaGreen
    )
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp)
    ) {
        Text("less", fontSize = 9.sp, color = BinaGrayText)
        Spacer(Modifier.size(4.dp))
        steps.forEach { c ->
            Box(
                Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(c)
            )
        }
        Spacer(Modifier.size(4.dp))
        Text("more", fontSize = 9.sp, color = BinaGrayText)
    }
}
