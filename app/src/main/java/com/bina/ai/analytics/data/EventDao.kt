package com.bina.ai.analytics.data

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Data access for the event_log table.
 *
 * All read methods return Flow so collectors auto-update when the table changes.
 * All write methods are suspend and must be called from a coroutine.
 */
@Dao
interface EventDao {

    @Insert
    suspend fun insert(event: EventEntity)

    /** Total events of a given type within an inclusive timestamp window. */
    @Query("""
        SELECT COUNT(*) FROM event_log
        WHERE event_type = :eventType
          AND timestamp_ms >= :sinceMs
    """)
    fun observeCountSince(eventType: String, sinceMs: Long): Flow<Int>

    /** Distinct calendar days (UTC) within the window that had any event. */
    @Query("""
        SELECT COUNT(DISTINCT date(timestamp_ms / 1000, 'unixepoch')) FROM event_log
        WHERE timestamp_ms >= :sinceMs
    """)
    fun observeActiveDaysSince(sinceMs: Long): Flow<Int>

    /** All events newer than `sinceMs`, oldest-first. Used for chart bucketing. */
    @Query("""
        SELECT * FROM event_log
        WHERE timestamp_ms >= :sinceMs
        ORDER BY timestamp_ms ASC
    """)
    fun observeEventsSince(sinceMs: Long): Flow<List<EventEntity>>

    /** Per-recipe (event_type, count) for a window — used by the leaderboard. */
    @Query("""
        SELECT recipe_id, event_type, COUNT(*) AS cnt
        FROM event_log
        WHERE timestamp_ms >= :sinceMs
        GROUP BY recipe_id, event_type
    """)
    fun observeCountsByRecipe(sinceMs: Long): Flow<List<RecipeEventCount>>
}

/** Projection for [EventDao.observeCountsByRecipe]. */
data class RecipeEventCount(
    @androidx.room.ColumnInfo(name = "recipe_id") val recipeId: String,
    @androidx.room.ColumnInfo(name = "event_type") val eventType: String,
    @androidx.room.ColumnInfo(name = "cnt") val cnt: Int
)
