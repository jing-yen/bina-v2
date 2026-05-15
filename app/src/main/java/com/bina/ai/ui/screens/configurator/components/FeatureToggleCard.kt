package com.bina.ai.ui.screens.configurator.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Camera
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.Mic
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Storage
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.miniapp.model.Feature
import com.bina.ai.ui.theme.BinaBgCard
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaStone950

@Composable
fun FeatureToggleCard(
    feature: Feature,
    isEnabled: Boolean,
    isToggleable: Boolean,
    accentColor: Color,
    onToggle: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    val rowAlpha = if (isToggleable) 1f else 0.4f
    Row(
        modifier = modifier
            .fillMaxWidth()
            .alpha(rowAlpha)
            .clip(RoundedCornerShape(16.dp))
            .background(BinaBgCard)
            .padding(14.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(accentColor.copy(alpha = 0.15f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(iconFor(feature.icon), null, tint = accentColor, modifier = Modifier.size(22.dp))
        }
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Text(feature.name, fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = BinaStone950)
                if (feature.recommended) RecommendedPill()
            }
            if (feature.description.isNotBlank()) {
                Spacer(Modifier.height(2.dp))
                Text(feature.description, fontSize = 11.sp, color = BinaGrayText)
            }
            Spacer(Modifier.height(2.dp))
            Text("+${"%.1f".format(feature.sizeKb)} KB", fontSize = 10.sp, color = BinaGrayText, fontWeight = FontWeight.Medium)
        }
        Switch(
            checked = isEnabled,
            onCheckedChange = if (isToggleable) onToggle else null,
            enabled = isToggleable
        )
    }
}

@Composable
private fun RecommendedPill() {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(8.dp))
            .background(BinaGreen.copy(alpha = 0.18f))
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text("RECOMMENDED", fontSize = 9.sp, fontWeight = FontWeight.SemiBold, color = BinaGreen)
    }
}

private fun iconFor(name: String): ImageVector = when (name) {
    "camera" -> Icons.Filled.Camera
    "mic" -> Icons.Filled.Mic
    "gps_fixed" -> Icons.Filled.GpsFixed
    "storage" -> Icons.Filled.Storage
    "sms" -> Icons.Filled.Sms
    "share" -> Icons.Filled.Share
    "notifications" -> Icons.Filled.Notifications
    "warning" -> Icons.Filled.Warning
    else -> Icons.Filled.Bolt
}
