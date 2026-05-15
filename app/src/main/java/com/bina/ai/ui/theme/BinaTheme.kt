package com.bina.ai.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import com.bina.ai.R

val OutfitFamily = FontFamily(
    Font(R.font.outfit_regular, FontWeight.Normal),
    Font(R.font.outfit_medium, FontWeight.Medium),
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
)

// Community Workshop palette — warm stone base + four vibrant roles
val BinaPrimary = Color(0xFF78350F)      // Deep amber — dense emphasis
val BinaAccent = Color(0xFFC45A3A)       // Terracotta — CTAs, active states
val BinaTurmeric = Color(0xFFC98A1A)     // AI/knowledge features, ratings
val BinaTeal = Color(0xFF1A8A6A)         // Success, downloads, growth
val BinaIndigo = Color(0xFF5B6ABF)       // Analytics, data, info
val BinaError = Color(0xFFBE3554)        // Destructive actions

val BinaGreen = BinaTeal
val BinaDarkGreen = Color(0xFF15705A)
val BinaAmber = BinaTurmeric
val BinaRed = BinaError
val BinaDarkRed = Color(0xFFA12D48)
val BinaBlue = BinaIndigo
val BinaSecondary = Color(0xFFE8DDD3)    // Warm sand — subtle fills

val BinaStone950 = Color(0xFF1C1917)
val BinaStone700 = Color(0xFF44403C)
val BinaGrayText = Color(0xFF57534E)     // Stone 600
val BinaGrayLight = Color(0xFFA8A29E)    // Stone 400
val BinaGrayBorder = Color(0xFFE7E0D8)   // Warm border

val BinaBgMain = Color(0xFFFAF8F5)       // Warm off-white
val BinaBgCard = Color(0xFFFFFDFA)       // Card surface
val BinaScreenStart = Color(0xFFE8DDD3)  // Warm sand gradient start
val BinaScreenMid = Color(0xFFFAF8F5)    // Warm off-white gradient end

val BinaNavActive = Color(0x26C45A3A)    // Terracotta at 15%
val BinaNavSurface = Color(0xE6FFFDF8)   // Warm white at 90%

private val BinaColorScheme = lightColorScheme(
    primary = BinaAccent,
    secondary = BinaTurmeric,
    surface = BinaBgCard,
    background = BinaBgMain,
    onPrimary = Color.White,
    onSecondary = Color.White,
    onSurface = BinaStone950,
    onBackground = BinaStone950,
    error = BinaError,
)

private val defaultTypography = Typography()
private val BinaTypography = Typography(
    displayLarge = defaultTypography.displayLarge.copy(fontFamily = OutfitFamily),
    displayMedium = defaultTypography.displayMedium.copy(fontFamily = OutfitFamily),
    displaySmall = defaultTypography.displaySmall.copy(fontFamily = OutfitFamily),
    headlineLarge = defaultTypography.headlineLarge.copy(fontFamily = OutfitFamily),
    headlineMedium = defaultTypography.headlineMedium.copy(fontFamily = OutfitFamily),
    headlineSmall = defaultTypography.headlineSmall.copy(fontFamily = OutfitFamily),
    titleLarge = defaultTypography.titleLarge.copy(fontFamily = OutfitFamily),
    titleMedium = defaultTypography.titleMedium.copy(fontFamily = OutfitFamily),
    titleSmall = defaultTypography.titleSmall.copy(fontFamily = OutfitFamily),
    bodyLarge = defaultTypography.bodyLarge.copy(fontFamily = OutfitFamily),
    bodyMedium = defaultTypography.bodyMedium.copy(fontFamily = OutfitFamily),
    bodySmall = defaultTypography.bodySmall.copy(fontFamily = OutfitFamily),
    labelLarge = defaultTypography.labelLarge.copy(fontFamily = OutfitFamily),
    labelMedium = defaultTypography.labelMedium.copy(fontFamily = OutfitFamily),
    labelSmall = defaultTypography.labelSmall.copy(fontFamily = OutfitFamily),
)

@Composable
fun BinaTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = BinaColorScheme,
        typography = BinaTypography,
        content = content
    )
}
