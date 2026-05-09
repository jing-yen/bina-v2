package com.bina.ai.analytics.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.ui.model.TimeWindow
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun AnalyticsHeader(
    selectedWindow: TimeWindow,
    onSelectWindow: (TimeWindow) -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text("Analytics", fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BinaPrimary)
            Text("Your authoring and on-device usage", fontSize = 12.sp, color = BinaGrayText)
        }
        TimeRangePill(selected = selectedWindow, onSelect = onSelectWindow)
    }
}
