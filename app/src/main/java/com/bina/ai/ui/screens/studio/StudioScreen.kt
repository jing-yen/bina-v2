package com.bina.ai.ui.screens.studio

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun StudioScreen() {
    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        Text("Recipe Studio", fontSize = 20.sp, fontWeight = FontWeight.SemiBold, color = BinaPrimary)
    }
}
