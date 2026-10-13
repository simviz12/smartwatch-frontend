package com.pulso.wear.data.sync

import android.content.Context
import com.pulso.core.model.ConfigSync
import com.pulso.wear.domain.repository.SyncRepository
import kotlinx.coroutines.flow.StateFlow

class AndroidSyncRepository(
    context: Context
) : SyncRepository {
    private val syncManager = SyncManager.getInstance(context)

    override val currentConfig: StateFlow<ConfigSync?> = syncManager.currentConfig

    override suspend fun triggerSyncNow(): Int {
        return syncManager.syncUnsyncedWindowsNow()
    }
}
