package com.pulso.wear.domain.repository

import com.pulso.core.model.MeasurementWindow
import com.pulso.wear.domain.model.LiveHeartRateState
import com.pulso.wear.domain.model.SensorCapabilityReport
import kotlinx.coroutines.flow.Flow
import java.io.File

interface SensorRepository {
    suspend fun getCapabilityReport(): SensorCapabilityReport
    fun observeLiveHeartRate(): Flow<LiveHeartRateState>
    fun observeSavedWindows(): Flow<List<MeasurementWindow>>
    suspend fun saveWindow(window: MeasurementWindow): Long
    suspend fun exportWindowsToCsv(): File
    suspend fun startBackgroundMeasurementService()
    suspend fun stopBackgroundMeasurementService()
}
