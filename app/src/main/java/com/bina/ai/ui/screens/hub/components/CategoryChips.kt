package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaSecondary

@Composable
fun CategoryChips(
    categories: List<String>,
    selected: String,
    onSelect: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    LazyRow(
        modifier = modifier,
        contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(categories) { cat ->
            Chip(label = cat, isActive = cat == selected, onClick = { onSelect(cat) })
        }
    }
}

@Composable
private fun Chip(label: String, isActive: Boolean, onClick: () -> Unit) {
    val bg = if (isActive) {
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Brush.linearGradient(listOf(BinaPrimary, BinaSecondary)))
    } else {
        Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color.White)
    }
    Text(
        text = label,
        modifier = bg
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
        fontSize = 12.sp,
        fontWeight = FontWeight.SemiBold,
        color = if (isActive) Color.White else BinaPrimary
    )
}
