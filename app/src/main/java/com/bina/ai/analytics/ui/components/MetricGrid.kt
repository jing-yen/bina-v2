package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.Inventory2
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.QuestionAnswer
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.bina.ai.analytics.ui.model.MetricsSnapshot
import com.bina.ai.ui.theme.BinaAccent
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaIndigo
import com.bina.ai.ui.theme.BinaTurmeric
import kotlinx.coroutines.delay

enum class MetricKind { INSTALLED, QUESTIONS, ACTIVE_DAYS, STREAK }

@Composable
fun MetricGrid(
    metrics: MetricsSnapshot,
    onTap: (MetricKind) -> Unit,
    modifier: Modifier = Modifier
) {
    var visibleCount by remember { mutableStateOf(0) }
    LaunchedEffect(Unit) {
        for (i in 1..4) { delay(80); visibleCount = i }
    }

    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(10.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StaggeredCard(visibleAt = 0, current = visibleCount, modifier = Modifier.weight(1f)) {
                MetricCard(
                    label = "Recipes Installed",
                    value = metrics.recipesInstalled,
                    icon = Icons.Filled.Inventory2,
                    accentColor = BinaAccent,
                    onClick = { onTap(MetricKind.INSTALLED) }
                )
            }
            StaggeredCard(visibleAt = 1, current = visibleCount, modifier = Modifier.weight(1f)) {
                MetricCard(
                    label = "Questions Asked",
                    value = metrics.questionsAsked,
                    icon = Icons.Filled.QuestionAnswer,
                    accentColor = BinaGreen,
                    onClick = { onTap(MetricKind.QUESTIONS) }
                )
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp), modifier = Modifier.fillMaxWidth()) {
            StaggeredCard(visibleAt = 2, current = visibleCount, modifier = Modifier.weight(1f)) {
                MetricCard(
                    label = "Active Days",
                    value = metrics.activeDays,
                    icon = Icons.Filled.CalendarMonth,
                    accentColor = BinaIndigo,
                    onClick = { onTap(MetricKind.ACTIVE_DAYS) }
                )
            }
            StaggeredCard(visibleAt = 3, current = visibleCount, modifier = Modifier.weight(1f)) {
                MetricCard(
                    label = "Streak",
                    value = metrics.currentStreak,
                    icon = Icons.Filled.LocalFireDepartment,
                    accentColor = BinaTurmeric,
                    onClick = { onTap(MetricKind.STREAK) },
                    formatter = { days ->
                        when {
                            days <= 0 -> "0"
                            days == 1 -> "1 day"
                            else -> "$days days"
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun StaggeredCard(
    visibleAt: Int,
    current: Int,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    androidx.compose.animation.AnimatedVisibility(
        visible = current > visibleAt,
        enter = fadeIn(tween(200)) + slideInVertically(tween(200)) { it / 4 },
        modifier = modifier
    ) {
        content()
    }
}
