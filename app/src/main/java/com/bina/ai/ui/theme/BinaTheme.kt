package com.bina.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val BinaPrimary = Color(0xFF091A7A)
val BinaSecondary = Color(0xFF1E3A8A)
val BinaGreen = Color(0xFF10B981)
val BinaDarkGreen = Color(0xFF059669)
val BinaAmber = Color(0xFFF59E0B)
val BinaRed = Color(0xFFEF4444)
val BinaDarkRed = Color(0xFFDC2626)
val BinaBlue = Color(0xFF3B82F6)

val BinaGrayText = Color(0xFF6B7280)
val BinaGrayLight = Color(0xFF9CA3AF)
val BinaGrayBorder = Color(0xFFE5E7EB)

val BinaScreenStart = Color(0xFFADC8FF)
val BinaScreenMid = Color(0xFFE8F2FF)

val BinaNavActive = Color(0x4DADC8FF)   // rgba(173, 200, 255, 0.3)
val BinaNavSurface = Color(0xE6FFFFFF)  // rgba(255, 255, 255, 0.9)

private val BinaColorScheme = lightColorScheme(
    primary = BinaPrimary,
    secondary = BinaSecondary,
    surface = Color.White,
    background = Color(0xFFF5F7FA),
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = Color(0xFF1A1A2E),
    onBackground = Color(0xFF1A1A2E),
    error = BinaRed,
)

@Composable
fun BinaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BinaColorScheme,
        content = content
    )
}
