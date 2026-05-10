package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun Sparkline(
    values: List<Int>,
    modifier: Modifier = Modifier,
    color: Color = Color.White
) {
    if (values.size < 2) {
        Canvas(modifier) { /* nothing to draw */ }
        return
    }

    var animationKey by remember { mutableStateOf(values) }
    LaunchedEffect(values) { animationKey = values }

    val drawProgress by animateFloatAsState(
        targetValue = 1f,
        animationSpec = tween(800, easing = LinearEasing),
        label = "sparkline-draw"
    )

    Canvas(modifier = modifier) {
        val maxValue = (values.maxOrNull() ?: 1).coerceAtLeast(1)
        val stepX = size.width / (values.size - 1).coerceAtLeast(1)

        val path = Path()
        values.forEachIndexed { i, v ->
            val x = stepX * i
            val y = size.height * (1f - v.toFloat() / maxValue) * 0.9f + size.height * 0.05f
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }

        drawPath(
            path = path,
            color = color,
            style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
