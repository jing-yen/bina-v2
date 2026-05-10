package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.tween
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.TextStyle

/**
 * Counts up smoothly from the current displayed value to [target].
 *
 * The Text uses tabular-monospace digits via fontFeatureSettings so digits
 * don't jitter horizontally as they animate.
 */
@Composable
fun AnimatedCounter(
    target: Int,
    modifier: Modifier = Modifier,
    style: TextStyle = TextStyle.Default,
    durationMs: Int = 600,
    formatter: (Int) -> String = { it.toString() }
) {
    val animated by animateIntAsState(
        targetValue = target,
        animationSpec = tween(durationMillis = durationMs),
        label = "AnimatedCounter"
    )
    Text(
        text = formatter(animated),
        modifier = modifier,
        style = style.copy(fontFeatureSettings = "tnum")
    )
}
