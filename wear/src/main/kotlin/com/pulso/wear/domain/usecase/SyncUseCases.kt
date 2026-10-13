package com.pulso.wear.domain.usecase

import com.pulso.core.model.ConfigSync
import com.pulso.wear.domain.repository.SyncRepository
import kotlinx.coroutines.flow.StateFlow

class ObserveConfigUseCase(private val syncRepository: SyncRepository) {
    operator fun invoke(): StateFlow<ConfigSync?> = syncRepository.currentConfig
}

class TriggerSyncUseCase(private val syncRepository: SyncRepository) {
    suspend operator fun invoke(): Int = syncRepository.triggerSyncNow()
}
