package com.pulso.wear.data.sync

import android.content.Context
import android.util.Log
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

class SyncWorker(
    appContext: Context,
    workerParams: WorkerParameters
) : CoroutineWorker(appContext, workerParams) {

    override suspend fun doWork(): Result {
        Log.i(TAG, "SyncWorker triggered: syncing unsynced windows to phone")
        return try {
            val syncManager = SyncManager.getInstance(applicationContext)
            val sentCount = syncManager.syncUnsyncedWindowsNow()
            Log.i(TAG, "SyncWorker finished successfully. Dispatched $sentCount windows.")
            Result.success()
        } catch (e: Exception) {
            Log.e(TAG, "SyncWorker failed to sync windows", e)
            Result.retry()
        }
    }

    companion object {
        private const val TAG = "PulsoSyncWorker"
    }
}
