package com.bina.ai.analytics.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.bina.ai.analytics.data.AnalyticsRepository
import com.bina.ai.analytics.ui.model.AnalyticsUiState
import com.bina.ai.analytics.ui.model.TimeWindow
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn

class AnalyticsViewModel(
    private val repository: AnalyticsRepository
) : ViewModel() {

    private val _window = MutableStateFlow(TimeWindow.LAST_7D)
    val window: StateFlow<TimeWindow> = _window.asStateFlow()

    @OptIn(ExperimentalCoroutinesApi::class)
    val uiState: StateFlow<AnalyticsUiState> = combine(
        repository.observeMetrics(_window),
        repository.observeChartData(_window),
        repository.observeLeaderboard(_window),
        repository.observeAchievements(_window),
        repository.observeIsEmpty()
    ) { metrics, chart, leaderboard, achievements, isEmpty ->
        if (isEmpty) AnalyticsUiState.Empty
        else AnalyticsUiState.Loaded(
            window = _window.value,
            metrics = metrics,
            chart = chart,
            leaderboard = leaderboard,
            achievements = achievements
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(stopTimeoutMillis = 5_000),
        initialValue = AnalyticsUiState.Loading
    )

    fun setWindow(newWindow: TimeWindow) {
        _window.value = newWindow
    }
}
