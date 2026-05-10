package com.bina.ai.analytics.ui.model

import java.util.concurrent.TimeUnit

enum class TimeWindow(val label: String) {
    LAST_7D("7d"),
    LAST_30D("30d"),
    ALL_TIME("All");

    /** Inclusive lower bound for SQL queries. ALL_TIME returns 0 (epoch). */
    fun sinceMs(now: Long = System.currentTimeMillis()): Long = when (this) {
        LAST_7D -> now - TimeUnit.DAYS.toMillis(7)
        LAST_30D -> now - TimeUnit.DAYS.toMillis(30)
        ALL_TIME -> 0L
    }

    /** Number of days in the window for chart bucketing. ALL_TIME defaults to 30. */
    val chartBucketCount: Int get() = when (this) {
        LAST_7D -> 7
        LAST_30D -> 30
        ALL_TIME -> 30
    }
}
