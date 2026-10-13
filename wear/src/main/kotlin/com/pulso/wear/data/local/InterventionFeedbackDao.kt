package com.pulso.wear.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface InterventionFeedbackDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(entity: InterventionFeedbackEntity): Long

    @Query("SELECT * FROM intervention_feedback WHERE synced = 0 ORDER BY timestamp ASC")
    suspend fun getPendingUnsynced(): List<InterventionFeedbackEntity>

    @Query("UPDATE intervention_feedback SET synced = 1 WHERE eventId = :eventId AND action = :action")
    suspend fun markSynced(eventId: String, action: String)

    @Query("SELECT * FROM intervention_feedback ORDER BY timestamp DESC")
    fun getAllFlow(): Flow<List<InterventionFeedbackEntity>>

    @Query("SELECT COUNT(*) FROM intervention_feedback")
    suspend fun countAll(): Int

    @Query("DELETE FROM intervention_feedback")
    suspend fun clearAll()
}
