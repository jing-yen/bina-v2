package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import kotlinx.coroutines.delay
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.ProgressIndicatorDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.bina.ai.analytics.ui.model.Achievement
import com.bina.ai.ui.theme.BinaAmber
import com.bina.ai.ui.theme.BinaGrayBorder
import com.bina.ai.ui.theme.BinaGrayText
import com.bina.ai.ui.theme.BinaPrimary

@Composable
fun AchievementCard(
    achievements: List<Achievement>,
    modifier: Modifier = Modifier
) {
    val featured = achievements.firstOrNull { it.unlocked } ?: achievements.firstOrNull()
    val locked = achievements.filter { it != featured }

    Column(modifier = modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Text(
            "Achievements",
            fontWeight = FontWeight.SemiBold,
            fontSize = 14.sp,
            color = BinaPrimary,
            modifier = Modifier.padding(start = 4.dp)
        )
        if (featured != null) FeaturedAchievement(featured)
        if (locked.isNotEmpty()) {
            Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                locked.forEach { LockedAchievement(it) }
            }
        }
    }
}

@Composable
private fun FeaturedAchievement(a: Achievement) {
    var pulseTarget by remember { mutableStateOf(1.08f) }
    LaunchedEffect(Unit) {
        while (true) {
            pulseTarget = 1.08f
            delay(600)
            pulseTarget = 1f
            delay(600)
        }
    }
    val pulse by animateFloatAsState(
        targetValue = pulseTarget,
        animationSpec = tween(600),
        label = "pulse"
    )
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(
                Brush.linearGradient(listOf(BinaAmber, Color(0xFFFFC56C)))
            )
            .padding(16.dp)
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(14.dp)) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .scale(if (a.unlocked) pulse else 1f)
                    .clip(RoundedCornerShape(16.dp))
                    .background(Color.White.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Text(a.emoji, fontSize = 28.sp)
            }
            Column(modifier = Modifier.weight(1f)) {
                Text(a.title, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Color.White)
                Text(
                    if (a.unlocked) "Unlocked" else a.description,
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.85f)
                )
                if (!a.unlocked) {
                    Spacer(Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { a.progress },
                        color = Color.White,
                        trackColor = Color.White.copy(alpha = 0.3f),
                        modifier = Modifier.fillMaxWidth().height(4.dp).clip(RoundedCornerShape(2.dp))
                    )
                }
            }
        }
    }
}

@Composable
private fun LockedAchievement(a: Achievement) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.85f))
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(12.dp))
                .background(BinaGrayBorder),
            contentAlignment = Alignment.Center
        ) {
            Text(a.emoji, fontSize = 20.sp)
        }
        Column(modifier = Modifier.weight(1f)) {
            Text(a.title, fontWeight = FontWeight.SemiBold, fontSize = 13.sp, color = BinaPrimary)
            Text(a.description, fontSize = 11.sp, color = BinaGrayText)
            Spacer(Modifier.height(4.dp))
            LinearProgressIndicator(
                progress = { a.progress },
                color = BinaPrimary,
                trackColor = BinaGrayBorder,
                modifier = Modifier.fillMaxWidth().height(3.dp).clip(RoundedCornerShape(2.dp))
            )
        }
    }
}
