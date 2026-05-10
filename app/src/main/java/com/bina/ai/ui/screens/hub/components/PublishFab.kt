package com.bina.ai.ui.screens.hub.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExtendedFloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun PublishFab(
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ExtendedFloatingActionButton(
        onClick = onClick,
        modifier = modifier,
        containerColor = BinaPrimary,
        contentColor = Color.White,
        icon = { Icon(Icons.Filled.Add, contentDescription = null) },
        text = { Text("Publish new", fontWeight = FontWeight.SemiBold) }
    )
}
