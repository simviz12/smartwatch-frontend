package com.pulso.wear.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface BreathingSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: BreathingSessionEntity)

    @Query("SELECT * FROM breathing_sessions WHERE isSynced = 0 ORDER BY completedAt ASC")
    suspend fun getUnsyncedSessions(): List<BreathingSessionEntity>

    @Query("UPDATE breathing_sessions SET isSynced = 1 WHERE sessionId = :sessionId")
    suspend fun markSessionSynced(sessionId: String)

    @Query("UPDATE breathing_sessions SET feeling = :feeling WHERE sessionId = :sessionId")
    suspend fun updateFeeling(sessionId: String, feeling: String)

    @Query("SELECT * FROM breathing_sessions ORDER BY completedAt DESC LIMIT 20")
    fun getRecentSessionsFlow(): Flow<List<BreathingSessionEntity>>
}
