package com.bina.ai.analytics.tracking

import com.bina.ai.analytics.data.EventDao
import com.bina.ai.analytics.data.EventEntity
import com.bina.ai.analytics.data.EventType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Single instance per app lifecycle. Used by:
 *  - MiniAppScreen (logs LAUNCH on first composition)
 *  - ActionDispatcher (logs ASK after safety check passes, via callback)
 *
 * `dao.insert` is non-suspend (Room+KSP+Kotlin 2.2.21 has a compat bug with
 * suspend insert: "unexpected jvm signature V"). We wrap calls in
 * Dispatchers.IO so the SQLite write doesn't run on the main thread.
 */
class EventTracker(private val dao: EventDao) {

    suspend fun logLaunch(recipeId: String) = withContext(Dispatchers.IO) {
        dao.insert(
            EventEntity(
                recipeId = recipeId,
                eventType = EventType.LAUNCH.name,
                timestampMs = System.currentTimeMillis()
            )
        )
    }

    suspend fun logAsk(recipeId: String) = withContext(Dispatchers.IO) {
        dao.insert(
            EventEntity(
                recipeId = recipeId,
                eventType = EventType.ASK.name,
                timestampMs = System.currentTimeMillis()
            )
        )
    }
}
