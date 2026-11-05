package com.pulso.mobile.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface PhoneBreathingSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(session: PhoneBreathingSessionEntity)

    @Query("SELECT * FROM phone_breathing_sessions ORDER BY completedAt DESC")
    fun getAllSessionsFlow(): Flow<List<PhoneBreathingSessionEntity>>

    @Query("SELECT * FROM phone_breathing_sessions WHERE completedAt >= :sinceTimestamp ORDER BY completedAt DESC")
    suspend fun getSessionsSince(sinceTimestamp: Long): List<PhoneBreathingSessionEntity>

    @Query("SELECT * FROM phone_breathing_sessions WHERE completedAt >= :sinceTimestamp ORDER BY completedAt DESC")
    fun getSessionsSinceFlow(sinceTimestamp: Long): Flow<List<PhoneBreathingSessionEntity>>

    @Query("SELECT * FROM phone_breathing_sessions ORDER BY completedAt DESC LIMIT 1")
    suspend fun getLatestSession(): PhoneBreathingSessionEntity?

    @Query("SELECT COUNT(*) FROM phone_breathing_sessions")
    suspend fun getCount(): Int

    /** Removes only demo breathing sessions (id prefixed with the demo marker). */
    @Query("DELETE FROM phone_breathing_sessions WHERE sessionId LIKE :prefix || '%'")
    suspend fun deleteByIdPrefix(prefix: String): Int

    @Query("DELETE FROM phone_breathing_sessions")
    suspend fun clearAll()
}
