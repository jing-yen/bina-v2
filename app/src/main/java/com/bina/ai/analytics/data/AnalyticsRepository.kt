package com.bina.ai.analytics.data

import com.bina.ai.analytics.ui.model.Achievement
import com.bina.ai.analytics.ui.model.AchievementId
import com.bina.ai.analytics.ui.model.DailyBucket
import com.bina.ai.analytics.ui.model.MetricsSnapshot
import com.bina.ai.analytics.ui.model.RecipeStats
import com.bina.ai.analytics.ui.model.TimeWindow
import com.bina.ai.miniapp.MiniAppRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.ExperimentalCoroutinesApi
import java.io.File
import java.util.Calendar
import java.util.TimeZone

/**
 * Combines the Room event_log Flows with filesystem-derived authoring stats.
 *
 * - Usage queries (launches, asks, active days, chart, leaderboard) span ALL recipes.
 * - Authoring queries (recipesPublished, knowledgeBytes) are scoped to filesDir.
 *
 * The `miniAppRepository` is used to resolve display names and icons for the leaderboard.
 */
class AnalyticsRepository(
    private val dao: EventDao,
    private val miniAppRepository: MiniAppRepository,
    private val filesDir: File
) {

    private val miniappsDir = File(filesDir, "miniapps")
    private val knowledgeDir = File(filesDir, "knowledge")

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
                dao.observeActiveDaysSince(sinceMs)
            ) { launches, prevLaunches, asks, activeDays ->
                MetricsSnapshot(
                    totalLaunches = launches,
                    totalLaunchesPrevious = (prevLaunches - launches).coerceAtLeast(0),
                    recipesPublished = scanAuthoredRecipeIds().size,
                    questionsAsked = asks,
                    activeDays = activeDays,
                    knowledgeBytes = scanKnowledgeBytes()
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
                val authoredIds = scanAuthoredRecipeIds()
                rows.groupBy { it.recipeId }.map { (recipeId, eventRows) ->
                    val launches = eventRows.firstOrNull { it.eventType == "LAUNCH" }?.cnt ?: 0
                    val asks = eventRows.firstOrNull { it.eventType == "ASK" }?.cnt ?: 0
                    val app = miniAppRepository.getById(recipeId)
                    RecipeStats(
                        recipeId = recipeId,
                        displayName = app?.name ?: recipeId,
                        icon = app?.icon ?: "📦",
                        launches = launches,
                        asks = asks,
                        isAuthored = recipeId in authoredIds
                    )
                }.sortedByDescending { it.total }
            }
        }

    @OptIn(ExperimentalCoroutinesApi::class)
    fun observeAchievements(window: Flow<TimeWindow>): Flow<List<Achievement>> =
        window.flatMapLatest { tw ->
            combine(
                dao.observeCountSince("ASK", 0L),                    // all-time asks
                dao.observeEventsSince(tw.sinceMs())                 // recent events for streak calc
            ) { totalAsks, recentEvents ->
                val authored = scanAuthoredRecipeIds()
                val knowledgeFiles = scanKnowledgeFileCount()
                val streak = computeMaxConsecutiveActiveDays(recentEvents, System.currentTimeMillis())

                listOf(
                    Achievement(
                        id = AchievementId.FIRST_AUTHOR,
                        title = "First Author",
                        description = "Publish your first recipe in Studio",
                        emoji = "📜",
                        unlocked = authored.isNotEmpty(),
                        progress = if (authored.isNotEmpty()) 1f else 0f
                    ),
                    Achievement(
                        id = AchievementId.CURIOUS,
                        title = "Curious",
                        description = "Ask 10 questions across any recipes",
                        emoji = "💬",
                        unlocked = totalAsks >= 10,
                        progress = (totalAsks / 10f).coerceIn(0f, 1f)
                    ),
                    Achievement(
                        id = AchievementId.STREAK,
                        title = "Streak",
                        description = "Use Bina 3 consecutive days",
                        emoji = "🔥",
                        unlocked = streak >= 3,
                        progress = (streak / 3f).coerceIn(0f, 1f)
                    ),
                    Achievement(
                        id = AchievementId.KNOWLEDGE_ARCHITECT,
                        title = "Knowledge Architect",
                        description = "Upload 5 files across your recipes",
                        emoji = "📚",
                        unlocked = knowledgeFiles >= 5,
                        progress = (knowledgeFiles / 5f).coerceIn(0f, 1f)
                    )
                )
            }
        }

    /** True when the user has zero events AND zero authored recipes. */
    fun observeIsEmpty(): Flow<Boolean> =
        combine(
            dao.observeCountSince("LAUNCH", 0L),
            dao.observeCountSince("ASK", 0L)
        ) { launches, asks ->
            launches == 0 && asks == 0 && scanAuthoredRecipeIds().isEmpty()
        }

    // ---- helpers ----------------------------------------------------------

    private fun scanAuthoredRecipeIds(): Set<String> {
        if (!miniappsDir.isDirectory) return emptySet()
        return miniappsDir.listFiles()
            ?.filter { it.isFile && (it.extension == "yaml" || it.extension == "yml") }
            ?.mapNotNull { extractIdFromYaml(it) }
            ?.toSet()
            ?: emptySet()
    }

    private fun extractIdFromYaml(file: File): String? = runCatching {
        file.useLines { lines ->
            lines
                .firstOrNull { it.trimStart().startsWith("id:") }
                ?.substringAfter(":")
                ?.trim()
                ?.trim('"', '\'')
                ?.takeIf { it.isNotEmpty() }
        }
    }.getOrNull()

    private fun scanKnowledgeBytes(): Long {
        if (!knowledgeDir.isDirectory) return 0L
        return knowledgeDir.walkTopDown()
            .filter { it.isFile }
            .sumOf { it.length() }
    }

    private fun scanKnowledgeFileCount(): Int {
        if (!knowledgeDir.isDirectory) return 0
        return knowledgeDir.walkTopDown().count { it.isFile }
    }

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
        val tz = TimeZone.getDefault()
        val daysWithActivity = events.map { ev ->
            Calendar.getInstance(tz).apply {
                timeInMillis = ev.timestampMs
                set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
            }.timeInMillis
        }.toSortedSet()

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
}
