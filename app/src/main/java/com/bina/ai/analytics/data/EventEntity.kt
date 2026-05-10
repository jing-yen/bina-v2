package com.bina.ai.analytics.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "event_log",
    indices = [Index(value = ["timestamp_ms", "recipe_id"])]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0L,

    @ColumnInfo(name = "recipe_id")
    val recipeId: String,

    @ColumnInfo(name = "event_type")
    val eventType: String,  // EventType.name

    @ColumnInfo(name = "timestamp_ms")
    val timestampMs: Long
)
