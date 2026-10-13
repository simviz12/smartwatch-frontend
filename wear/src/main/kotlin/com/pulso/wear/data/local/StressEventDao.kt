package com.pulso.wear.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query

@Dao
interface StressEventDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertEvent(entity: StressEventEntity): Long

    @Query("SELECT * FROM wear_stress_events WHERE isSynced = 0 ORDER BY time ASC")
    suspend fun getUnsyncedEvents(): List<StressEventEntity>

    @Query("UPDATE wear_stress_events SET isSynced = 1 WHERE id IN (:eventIds)")
    suspend fun markEventsAsSynced(eventIds: List<String>): Int

    @Query("SELECT COUNT(*) FROM wear_stress_events WHERE time >= :startOfDayTimestamp")
    suspend fun countTriggersToday(startOfDayTimestamp: Long): Int

    @Query("SELECT time FROM wear_stress_events ORDER BY time DESC LIMIT 1")
    suspend fun getLastTriggerTime(): Long?

    @Query("SELECT * FROM wear_stress_events ORDER BY time DESC LIMIT :limit")
    suspend fun getRecentEvents(limit: Int): List<StressEventEntity>

    @Query("DELETE FROM wear_stress_events")
    suspend fun clearAll()
}
