package com.bina.ai.ui.screens.recipe_detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.model.Feature
import com.bina.ai.ui.theme.BinaPrimary

/**
 * Read-only preview of features. Shows icon + name as small chips. Full toggling lives
 * in the Configurator.
 */
@Composable
fun FeaturePreviewList(
    features: List<Feature>,
    modifier: Modifier = Modifier
) {
    if (features.isEmpty()) return
    LazyRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        items(features, key = { it.id }) { feature ->
            FeatureChip(feature)
        }
    }
}

@Composable
private fun FeatureChip(feature: Feature) {
    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(20.dp))
            .background(Color(0xFFF1F5F9))
            .padding(horizontal = 10.dp, vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        Icon(Icons.Filled.Bolt, null, tint = BinaPrimary, modifier = Modifier.size(12.dp))
        Text(feature.name, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = BinaPrimary)
    }
}
