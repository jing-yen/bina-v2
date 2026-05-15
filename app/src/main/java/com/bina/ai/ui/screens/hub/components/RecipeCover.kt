package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.bina.ai.miniapp.model.MiniApp
import com.bina.ai.ui.theme.BinaAccent
import com.bina.ai.ui.theme.BinaSecondary

/**
 * Renders a recipe's cover. If `coverImage` is set, loads via Coil with the gradient as
 * a fallback during load and on error. If empty, renders only the gradient + emoji icon.
 */
@Composable
fun RecipeCover(
    miniApp: MiniApp,
    modifier: Modifier = Modifier,
    cornerRadius: Dp = 12.dp,
    showEmoji: Boolean = true,
    emojiFontSize: Int = 36
) {
    val primary = parseHex(miniApp.theme.primary, BinaAccent)
    val secondary = parseHex(miniApp.theme.secondary, BinaSecondary)

    Box(modifier = modifier.clip(RoundedCornerShape(cornerRadius))) {
        // Gradient always — image overlays it when present.
        Box(
            Modifier
                .fillMaxSize()
                .background(Brush.linearGradient(listOf(primary, secondary)))
        )
        if (showEmoji && miniApp.icon.isNotBlank()) {
            Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(miniApp.icon, fontSize = emojiFontSize.sp)
            }
        }
        if (miniApp.coverImage.isNotBlank()) {
            AsyncImage(
                model = resolveCoverPath(miniApp.coverImage),
                contentDescription = miniApp.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
    }
}

private fun resolveCoverPath(raw: String): String = when {
    raw.startsWith("http://") || raw.startsWith("https://") -> raw
    raw.startsWith("/") || raw.startsWith("file://") -> raw
    else -> "file:///android_asset/miniapps/$raw"
}

private fun parseHex(hex: String, fallback: Color): Color =
    runCatching { Color(android.graphics.Color.parseColor(hex)) }.getOrDefault(fallback)
