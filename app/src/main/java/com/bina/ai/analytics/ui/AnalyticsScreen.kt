package com.bina.ai.analytics.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.ViewModelProvider
import com.bina.ai.analytics.data.AnalyticsRepository
import com.bina.ai.analytics.ui.components.AchievementCard
import com.bina.ai.analytics.ui.components.ActivityChart
import com.bina.ai.analytics.ui.components.AnalyticsHeader
import com.bina.ai.analytics.ui.components.EmptyState
import com.bina.ai.analytics.ui.components.HeroCard
import com.bina.ai.analytics.ui.components.MetricDetailSheet
import com.bina.ai.analytics.ui.components.MetricGrid
import com.bina.ai.analytics.ui.components.MetricKind
import com.bina.ai.analytics.ui.components.RecipeLeaderboard
import com.bina.ai.analytics.ui.model.AnalyticsUiState
import com.bina.ai.analytics.viewmodel.AnalyticsViewModel

@Composable
fun AnalyticsScreen(
    repository: AnalyticsRepository,
    onOpenHub: () -> Unit,
    onOpenStudio: () -> Unit,
    modifier: Modifier = Modifier
) {
    val factory = remember(repository) {
        object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T =
                AnalyticsViewModel(repository) as T
        }
    }
    val viewModel: AnalyticsViewModel = viewModel(factory = factory)
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val window by viewModel.window.collectAsStateWithLifecycle()

    when (val s = state) {
        AnalyticsUiState.Loading -> {
            Column(modifier = modifier.fillMaxSize()) {
                AnalyticsHeader(
                    selectedWindow = window,
                    onSelectWindow = viewModel::setWindow
                )
            }
        }
        AnalyticsUiState.Empty -> {
            Column(modifier = modifier.fillMaxSize()) {
                AnalyticsHeader(
                    selectedWindow = window,
                    onSelectWindow = viewModel::setWindow
                )
                EmptyState(onOpenHub = onOpenHub, onOpenStudio = onOpenStudio)
            }
        }
        is AnalyticsUiState.Loaded -> {
            val listState = rememberLazyListState()
            val density = LocalDensity.current
            val collapseRangePx = remember(density) { with(density) { 120.dp.toPx() } }
            val collapseFraction by remember(collapseRangePx) {
                derivedStateOf {
                    if (listState.firstVisibleItemIndex > 0) 1f
                    else (listState.firstVisibleItemScrollOffset / collapseRangePx).coerceIn(0f, 1f)
                }
            }

            var openedKind by remember { mutableStateOf<MetricKind?>(null) }

            Column(modifier = modifier.fillMaxSize()) {
                AnalyticsHeader(
                    selectedWindow = s.window,
                    onSelectWindow = viewModel::setWindow,
                    collapseFraction = collapseFraction
                )
                LazyColumn(
                    state = listState,
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = 24.dp),
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    item {
                        HeroCard(
                            metrics = s.metrics,
                            sparklineValues = s.chart.map { it.total },
                            window = s.window,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item {
                        MetricGrid(
                            metrics = s.metrics,
                            onTap = { openedKind = it },
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item {
                        ActivityChart(
                            buckets = s.chart,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item {
                        RecipeLeaderboard(
                            rows = s.leaderboard,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item {
                        AchievementCard(
                            achievements = s.achievements,
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                    item { Spacer(Modifier.height(8.dp)) }
                }
            }

            openedKind?.let { kind ->
                MetricDetailSheet(
                    kind = kind,
                    metrics = s.metrics,
                    leaderboard = s.leaderboard,
                    chart = s.chart,
                    onDismiss = { openedKind = null }
                )
            }
        }
    }
}
