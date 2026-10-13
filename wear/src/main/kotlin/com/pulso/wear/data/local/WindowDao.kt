package com.pulso.wear.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

@Dao
interface WindowDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertWindow(entity: WindowEntity): Long

    @Query("SELECT * FROM measurement_windows ORDER BY windowStart DESC")
    fun getAllWindowsFlow(): Flow<List<WindowEntity>>

    @Query("SELECT * FROM measurement_windows ORDER BY windowStart ASC")
    suspend fun getAllWindows(): List<WindowEntity>

    @Query("SELECT COUNT(*) FROM measurement_windows")
    suspend fun countWindows(): Int

    @Query("DELETE FROM measurement_windows")
    suspend fun clearAll()
}
