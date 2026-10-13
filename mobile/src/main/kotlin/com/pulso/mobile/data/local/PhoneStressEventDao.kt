package com.pulso.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneStressEventDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: PhoneStressEventEntity): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(entities: List<PhoneStressEventEntity>): List<Long>

    @Query("SELECT * FROM phone_stress_events ORDER BY time DESC")
    fun getAllEventsFlow(): Flow<List<PhoneStressEventEntity>>

    @Query("SELECT * FROM phone_stress_events ORDER BY time DESC LIMIT 1")
    fun getLatestEventFlow(): Flow<PhoneStressEventEntity?>

    @Query("SELECT COUNT(*) FROM phone_stress_events")
    suspend fun countEvents(): Int

    @Query("DELETE FROM phone_stress_events")
    suspend fun clearAll()
}
