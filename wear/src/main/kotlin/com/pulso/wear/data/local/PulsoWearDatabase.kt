package com.pulso.wear.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [WindowEntity::class], version = 1, exportSchema = false)
abstract class PulsoWearDatabase : RoomDatabase() {
    abstract fun windowDao(): WindowDao

    companion object {
        @Volatile
        private var INSTANCE: PulsoWearDatabase? = null

        fun getInstance(context: Context): PulsoWearDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PulsoWearDatabase::class.java,
                    "pulso_wear.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
