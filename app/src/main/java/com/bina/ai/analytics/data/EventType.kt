package com.bina.ai.analytics.data

/**
 * Discrete event categories logged in event_log.
 * Stored as the enum name string; new types can be appended without schema migration.
 */
enum class EventType {
    LAUNCH,  // user opened a recipe (MiniAppScreen entered)
    ASK      // user sent an `ask:` action that passed safety checks
}
