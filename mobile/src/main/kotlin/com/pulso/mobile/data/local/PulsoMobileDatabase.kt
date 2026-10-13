package com.pulso.mobile.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase

@Database(entities = [PhoneWindowEntity::class], version = 1, exportSchema = false)
abstract class PulsoMobileDatabase : RoomDatabase() {
    abstract fun phoneWindowDao(): PhoneWindowDao

    companion object {
        @Volatile
        private var INSTANCE: PulsoMobileDatabase? = null

        fun getInstance(context: Context): PulsoMobileDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    PulsoMobileDatabase::class.java,
                    "pulso_mobile.db"
                ).build()
                INSTANCE = instance
                instance
            }
        }
    }
}
