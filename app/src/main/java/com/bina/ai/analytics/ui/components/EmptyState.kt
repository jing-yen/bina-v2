package com.bina.ai.analytics.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary
import com.bina.ai.ui.theme.BinaSecondary

@Composable
fun EmptyState(
    onOpenHub: () -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Compose-drawn illustration: three concentric circles with icon
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(160.dp)
                    .clip(CircleShape)
                    .background(BinaPrimary.copy(alpha = 0.05f))
            )
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .clip(CircleShape)
                    .background(BinaPrimary.copy(alpha = 0.1f))
            )
            Box(
                modifier = Modifier
                    .size(72.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.linearGradient(listOf(BinaPrimary, BinaSecondary))
                    ),
                contentAlignment = Alignment.Center
            ) {
                Text("📊", fontSize = 32.sp)
            }
        }
        Spacer(Modifier.height(20.dp))
        Text(
            "No activity yet",
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold,
            color = BinaPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            "Open a recipe from the Hub to start seeing analytics.",
            fontSize = 13.sp,
            color = BinaGrayText,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(24.dp))
        Button(
            onClick = onOpenHub,
            shape = RoundedCornerShape(14.dp),
            colors = ButtonDefaults.buttonColors(containerColor = BinaPrimary)
        ) {
            Text("Open Hub", fontWeight = FontWeight.SemiBold)
        }
    }
}
