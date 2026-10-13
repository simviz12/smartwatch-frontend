package com.pulso.wear.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WindowEntity::class, StressEventEntity::class, InterventionFeedbackEntity::class, BreathingSessionEntity::class], version = 4, exportSchema = false)
abstract class PulsoWearDatabase : RoomDatabase() {
    abstract fun windowDao(): WindowDao
    abstract fun stressEventDao(): StressEventDao
    abstract fun interventionFeedbackDao(): InterventionFeedbackDao
    abstract fun breathingSessionDao(): BreathingSessionDao

    companion object {
        @Volatile
        private var INSTANCE: PulsoWearDatabase? = null

        fun getInstance(context: Context): PulsoWearDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PulsoWearDatabase::class.java,
                    "pulso_wear.db"
                ).fallbackToDestructiveMigration().build()
                INSTANCE = instance
                instance
            }
        }
    }
}
