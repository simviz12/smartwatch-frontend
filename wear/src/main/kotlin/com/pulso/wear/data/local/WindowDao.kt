package com.pulso.wear.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WindowDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertWindow(entity: WindowEntity): Long

    @Query("SELECT * FROM measurement_windows ORDER BY windowStart DESC")
    fun getAllWindowsFlow(): Flow<List<WindowEntity>>

    @Query("SELECT * FROM measurement_windows ORDER BY windowStart ASC")
    suspend fun getAllWindows(): List<WindowEntity>

    @Query("SELECT * FROM measurement_windows WHERE synced = 0 ORDER BY windowStart ASC LIMIT :limit")
    suspend fun getUnsyncedWindows(limit: Int = 100): List<WindowEntity>

    @Query("UPDATE measurement_windows SET batchId = :batchId WHERE id IN (:ids)")
    suspend fun markWindowsWithBatchId(ids: List<Long>, batchId: String)

    @Query("DELETE FROM measurement_windows WHERE batchId = :batchId")
    suspend fun deleteWindowsByBatchId(batchId: String): Int

    @Query("DELETE FROM measurement_windows WHERE windowStart < :cutoffTimestamp")
    suspend fun deleteExpiredWindows(cutoffTimestamp: Long): Int

    @Query("SELECT COUNT(*) FROM measurement_windows")
    suspend fun countWindows(): Int

    @Query("SELECT COUNT(*) FROM measurement_windows WHERE synced = 0")
    suspend fun countUnsyncedWindows(): Int

    @Query("DELETE FROM measurement_windows")
    suspend fun clearAll()
}
