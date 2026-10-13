package com.pulso.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneWindowDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: PhoneWindowEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(entities: List<PhoneWindowEntity>): List<Long>

    @Query("SELECT * FROM phone_measurement_windows WHERE windowStart >= :startOfDayTimestamp ORDER BY windowStart ASC")
    fun getWindowsForTodayFlow(startOfDayTimestamp: Long): Flow<List<PhoneWindowEntity>>

    @Query("SELECT * FROM phone_measurement_windows ORDER BY windowStart DESC LIMIT 1")
    fun getLatestWindowFlow(): Flow<PhoneWindowEntity?>

    @Query("SELECT COUNT(*) FROM phone_measurement_windows")
    fun countWindowsFlow(): Flow<Int>

    @Query("SELECT COUNT(*) FROM phone_measurement_windows")
    suspend fun countWindows(): Int

    @Query("SELECT * FROM phone_measurement_windows WHERE windowStart >= :sinceTimestamp ORDER BY windowStart ASC")
    fun getWindowsSinceFlow(sinceTimestamp: Long): Flow<List<PhoneWindowEntity>>

    @Query("SELECT * FROM phone_measurement_windows WHERE windowStart >= :sinceTimestamp ORDER BY windowStart ASC")
    suspend fun getWindowsSince(sinceTimestamp: Long): List<PhoneWindowEntity>

    @Query("DELETE FROM phone_measurement_windows")
    suspend fun clearAll()
}
