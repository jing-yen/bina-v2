package com.bina.ai.analytics.data

import com.bina.ai.analytics.ui.model.Achievement
import com.bina.ai.analytics.ui.model.AchievementId
import com.bina.ai.analytics.ui.model.DailyBucket
import com.bina.ai.analytics.ui.model.MetricsSnapshot
import com.bina.ai.analytics.ui.model.RecipeStats
import com.bina.ai.analytics.ui.model.TimeWindow
import com.bina.ai.R
import com.bina.ai.install.InstallStore
import com.bina.ai.miniapp.MiniAppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.util.Calendar
import java.util.TimeZone

/**
 * Builder-side analytics: combines the Room event_log Flows with installed-recipe state
 * from [InstallStore]. All metrics describe the user's own recipe usage on this device.
 *
 * `miniAppRepository` resolves display names and icons for the leaderboard.
 */
class AnalyticsRepository(
    private val dao: EventDao,
    private val miniAppRepository: MiniAppRepository,
    private val installStore: InstallStore
) {

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeMetrics(window: Flow<TimeWindow>): Flow<MetricsSnapshot> =
        window.flatMapLatest { tw ->
            val now = System.currentTimeMillis()
            val sinceMs = tw.sinceMs(now)
            val previousSinceMs = previousWindowStart(tw, now)

            combine(
                dao.observeCountSince("LAUNCH", sinceMs),
                dao.observeCountSince("LAUNCH", previousSinceMs),
                dao.observeCountSince("ASK", sinceMs),
                dao.observeActiveDaysSince(sinceMs),
                combine(
                    dao.observeEventsSince(sinceMs),
                    installStore.installs
                ) { events, installs -> events to installs }
            ) { launches, prevLaunches, asks, activeDays, eventsAndInstalls ->
                val (events, installs) = eventsAndInstalls
                MetricsSnapshot(
                    totalLaunches = launches,
                    totalLaunchesPrevious = (prevLaunches - launches).coerceAtLeast(0),
                    recipesInstalled = installs.size,
                    questionsAsked = asks,
                    activeDays = activeDays,
                    currentStreak = computeCurrentStreak(events, now)
                )
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeChartData(window: Flow<TimeWindow>): Flow<List<DailyBucket>> =
        window.flatMapLatest { tw ->
            val now = System.currentTimeMillis()
            dao.observeEventsSince(tw.sinceMs(now)).map { events ->
                bucketizeByDay(events, tw, now)
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeLeaderboard(window: Flow<TimeWindow>): Flow<List<RecipeStats>> =
        window.flatMapLatest { tw ->
            dao.observeCountsByRecipe(tw.sinceMs()).map { rows ->
                rows.groupBy { it.recipeId }.map { (recipeId, eventRows) ->
                    val launches = eventRows.firstOrNull { it.eventType == "LAUNCH" }?.cnt ?: 0
                    val asks = eventRows.firstOrNull { it.eventType == "ASK" }?.cnt ?: 0
                    val app = miniAppRepository.getById(recipeId)
                    RecipeStats(
                        recipeId = recipeId,
                        displayName = app?.name ?: recipeId,
                        icon = app?.icon ?: "📦",
                        launches = launches,
                        asks = asks
                    )
                }.sortedByDescending { it.total }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAchievements(window: Flow<TimeWindow>): Flow<List<Achievement>> =
        window.flatMapLatest { tw ->
            combine(
                dao.observeCountSince("ASK", 0L),                    // all-time asks
                dao.observeEventsSince(tw.sinceMs()),                // recent events for streak + distinct
                installStore.installs
            ) { totalAsks, recentEvents, installs ->
                val streak = computeMaxConsecutiveActiveDays(recentEvents, System.currentTimeMillis())
                val distinctRecipes = recentEvents.map { it.recipeId }.toSet().size
                val installedCount = installs.size

                listOf(
                    Achievement(
                        id = AchievementId.COLLECTOR,
                        titleResId = R.string.achievement_collector_title,
                        descriptionResId = R.string.achievement_collector_desc,
                        emoji = "📦",
                        unlocked = installedCount >= 3,
                        progress = (installedCount / 3f).coerceIn(0f, 1f)
                    ),
                    Achievement(
                        id = AchievementId.CURIOUS,
                        titleResId = R.string.achievement_curious_title,
                        descriptionResId = R.string.achievement_curious_desc,
                        emoji = "💬",
                        unlocked = totalAsks >= 10,
                        progress = (totalAsks / 10f).coerceIn(0f, 1f)
                    ),
                    Achievement(
                        id = AchievementId.STREAK,
                        titleResId = R.string.achievement_streak_title,
                        descriptionResId = R.string.achievement_streak_desc,
                        emoji = "🔥",
                        unlocked = streak >= 3,
                        progress = (streak / 3f).coerceIn(0f, 1f)
                    ),
                    Achievement(
                        id = AchievementId.EXPLORER,
                        titleResId = R.string.achievement_explorer_title,
                        descriptionResId = R.string.achievement_explorer_desc,
                        emoji = "🧭",
                        unlocked = distinctRecipes >= 3,
                        progress = (distinctRecipes / 3f).coerceIn(0f, 1f)
                    )
                )
            }
        }

    /** True when the user has zero events. */
    fun observeIsEmpty(): Flow<Boolean> =
        combine(
            dao.observeCountSince("LAUNCH", 0L),
            dao.observeCountSince("ASK", 0L)
        ) { launches, asks ->
            launches == 0 && asks == 0
        }

    // ---- helpers ----------------------------------------------------------

    private fun previousWindowStart(window: TimeWindow, now: Long): Long {
        val span = now - window.sinceMs(now)
        return (window.sinceMs(now) - span).coerceAtLeast(0L)
    }

    private fun bucketizeByDay(
        events: List<EventEntity>,
        window: TimeWindow,
        now: Long
    ): List<DailyBucket> {
        val tz = TimeZone.getDefault()
        val cal = Calendar.getInstance(tz)
        cal.timeInMillis = now
        cal.set(Calendar.HOUR_OF_DAY, 0)
        cal.set(Calendar.MINUTE, 0)
        cal.set(Calendar.SECOND, 0)
        cal.set(Calendar.MILLISECOND, 0)
        val todayStart = cal.timeInMillis

        val buckets = (0 until window.chartBucketCount).map { i ->
            todayStart - (window.chartBucketCount - 1 - i) * 86_400_000L
        }
        val bucketLaunches = IntArray(buckets.size)
        val bucketAsks = IntArray(buckets.size)

        events.forEach { ev ->
            val idx = ((ev.timestampMs - buckets.first()) / 86_400_000L).toInt()
            if (idx in buckets.indices) {
                if (ev.eventType == "LAUNCH") bucketLaunches[idx]++
                else if (ev.eventType == "ASK") bucketAsks[idx]++
            }
        }
        return buckets.indices.map { i ->
            DailyBucket(buckets[i], bucketLaunches[i], bucketAsks[i])
        }
    }

    private fun computeMaxConsecutiveActiveDays(events: List<EventEntity>, now: Long): Int {
        if (events.isEmpty()) return 0
        val daysWithActivity = activeDayStarts(events)

        var maxRun = 0
        var run = 0
        var prev: Long? = null
        for (day in daysWithActivity) {
            run = if (prev != null && day - prev == 86_400_000L) run + 1 else 1
            if (run > maxRun) maxRun = run
            prev = day
        }
        return maxRun
    }

    /** Streak counted backward from today (or yesterday if today has no activity). */
    private fun computeCurrentStreak(events: List<EventEntity>, now: Long): Int {
        if (events.isEmpty()) return 0
        val days = activeDayStarts(events)
        val tz = TimeZone.getDefault()
        val cal = Calendar.getInstance(tz).apply {
            timeInMillis = now
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val today = cal.timeInMillis
        var cursor = if (today in days) today else today - 86_400_000L
        var streak = 0
        while (cursor in days) {
            streak++
            cursor -= 86_400_000L
        }
        return streak
    }

    private fun activeDayStarts(events: List<EventEntity>): Set<Long> {
        val tz = TimeZone.getDefault()
        return events.map { ev ->
            Calendar.getInstance(tz).apply {
                timeInMillis = ev.timestampMs
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.toSortedSet()
    }
}
