package com.bina.ai.analytics.ui.model

/** Top-level UI state for AnalyticsScreen. */
sealed interface AnalyticsUiState {
    data object Loading : AnalyticsUiState
    data object Empty : AnalyticsUiState
    data class Loaded(
        val window: TimeWindow,
        val metrics: MetricsSnapshot,
        val chart: List<DailyBucket>,
        val leaderboard: List<RecipeStats>,
        val achievements: List<Achievement>
    ) : AnalyticsUiState
}

/** Numbers for the hero card + 2x2 metric grid. */
data class MetricsSnapshot(
    val totalLaunches: Int,
    val totalLaunchesPrevious: Int,   // for delta calculation
    val recipesInstalled: Int,
    val questionsAsked: Int,
    val activeDays: Int,
    val currentStreak: Int
)

/** One day's worth of stacked-bar data for the activity chart. */
data class DailyBucket(
    val dayStartMs: Long,
    val launches: Int,
    val asks: Int
) {
    val total: Int get() = launches + asks
}

/** A row in the recipe leaderboard. */
data class RecipeStats(
    val recipeId: String,
    val displayName: String,   // resolved from MiniAppRepository, fallback to id
    val icon: String,          // emoji from recipe, fallback "📦"
    val launches: Int,
    val asks: Int
) {
    val total: Int get() = launches + asks
}

/** Achievement state shown in the bottom card. */
data class Achievement(
    val id: AchievementId,
    val title: String,
    val description: String,
    val emoji: String,
    val unlocked: Boolean,
    val progress: Float       // 0f..1f for progress bars on locked items
)

enum class AchievementId { COLLECTOR, CURIOUS, STREAK, EXPLORER }
