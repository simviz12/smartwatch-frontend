package com.pulso.wear.data.sync

import android.util.Log
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.WearableListenerService
import com.pulso.core.contract.DataLayerContracts
import com.pulso.core.model.ConfigSync
import com.pulso.core.model.SamplesAck
import com.pulso.wear.data.local.PulsoWearDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class WearDataListenerService : WearableListenerService() {

    private val serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    private val database by lazy { PulsoWearDatabase.getInstance(this) }
    private val syncManager by lazy { SyncManager.getInstance(this) }

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        syncManager.onDataChanged(dataEvents)
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        syncManager.onMessageReceived(messageEvent)
    }

    companion object {
        private const val TAG = "WearDataListener"
    }
}
