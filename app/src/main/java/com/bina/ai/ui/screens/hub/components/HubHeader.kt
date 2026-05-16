package com.bina.ai.ui.screens.hub.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.R
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaStone950

@Composable
fun HubHeader(modifier: Modifier = Modifier) {
    Row(
        modifier = modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Column {
            Text(stringResource(R.string.hub_title), fontWeight = FontWeight.Bold, fontSize = 22.sp, color = BinaStone950)
            Text(stringResource(R.string.hub_subtitle), fontSize = 12.sp, color = BinaGrayText)
        }
    }
}
