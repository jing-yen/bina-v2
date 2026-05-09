package com.bina.ai.analytics.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(
    entities = [EventEntity::class],
    version = 1,
    exportSchema = false  // disable schema dump (we don't need migrations for hackathon)
)
abstract class AnalyticsDatabase : RoomDatabase() {

    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AnalyticsDatabase? = null

        fun get(context: Context): AnalyticsDatabase =
            INSTANCE ?: synchronized(this) {
                INSTANCE ?: Room.databaseBuilder(
                    context.applicationContext,
                    AnalyticsDatabase::class.java,
                    "bina_analytics.db"
                )
                    // Hackathon: schema changes wipe the DB. Acceptable.
                    .fallbackToDestructiveMigration()
                    .build()
                    .also { INSTANCE = it }
            }
    }
}
