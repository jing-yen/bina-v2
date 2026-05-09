package com.bina.ai.analytics.ui.components

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountTree
import androidx.compose.material.icons.filled.CalendarMonth
import androidx.compose.material.icons.filled.MenuBook
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
import com.bina.ai.ui.theme.BinaAmber
import com.bina.ai.ui.theme.BinaBlue
import com.bina.ai.ui.theme.BinaGreen
import com.bina.ai.ui.theme.BinaPrimary
import kotlinx.coroutines.delay

enum class MetricKind { RECIPES, QUESTIONS, ACTIVE_DAYS, KNOWLEDGE }

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
                    label = "Recipes Published",
                    value = metrics.recipesPublished,
                    icon = Icons.Filled.AccountTree,
                    accentColor = BinaPrimary,
                    onClick = { onTap(MetricKind.RECIPES) }
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
                    accentColor = BinaBlue,
                    onClick = { onTap(MetricKind.ACTIVE_DAYS) }
                )
            }
            StaggeredCard(visibleAt = 3, current = visibleCount, modifier = Modifier.weight(1f)) {
                MetricCard(
                    label = "Knowledge",
                    value = (metrics.knowledgeBytes / 1024).toInt(),
                    icon = Icons.Filled.MenuBook,
                    accentColor = BinaAmber,
                    onClick = { onTap(MetricKind.KNOWLEDGE) },
                    formatter = { kb ->
                        when {
                            kb <= 0 -> "—"
                            kb >= 1024 -> "%.1f MB".format(kb / 1024f)
                            else -> "$kb KB"
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
