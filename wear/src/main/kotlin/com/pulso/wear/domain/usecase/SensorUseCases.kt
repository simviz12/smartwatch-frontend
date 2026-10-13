package com.pulso.wear.domain.usecase

import com.pulso.core.model.MeasurementWindow
import com.pulso.wear.domain.model.LiveHeartRateState
import com.pulso.wear.domain.model.SensorCapabilityReport
import com.pulso.wear.domain.repository.SensorRepository
import kotlinx.coroutines.flow.Flow
import java.io.File

class GetCapabilityReportUseCase(private val repository: SensorRepository) {
    suspend operator fun invoke(): SensorCapabilityReport = repository.getCapabilityReport()
}

class ObserveLiveHeartRateUseCase(private val repository: SensorRepository) {
    operator fun invoke(): Flow<LiveHeartRateState> = repository.observeLiveHeartRate()
}

class ObserveWindowsUseCase(private val repository: SensorRepository) {
    operator fun invoke(): Flow<List<MeasurementWindow>> = repository.observeSavedWindows()
}

class ExportWindowsCsvUseCase(private val repository: SensorRepository) {
    suspend operator fun invoke(): File = repository.exportWindowsToCsv()
}

class ManageBackgroundSamplingUseCase(private val repository: SensorRepository) {
    suspend fun start() = repository.startBackgroundMeasurementService()
    suspend fun stop() = repository.stopBackgroundMeasurementService()
}
