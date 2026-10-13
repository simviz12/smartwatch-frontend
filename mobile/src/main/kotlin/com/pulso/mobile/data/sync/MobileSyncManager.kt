package com.pulso.mobile.data.sync

import android.content.Context
import android.util.Log
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService
import com.pulso.core.contract.DataLayerContracts
import com.pulso.core.model.ConfigSync
import com.pulso.core.model.MeasurementWindow
import com.pulso.core.model.PingMessage
import com.pulso.core.model.SamplesAck
import com.pulso.core.model.SamplesBatch
import com.pulso.core.model.WatchStatus
import com.pulso.mobile.data.local.PhoneWindowDao
import com.pulso.mobile.data.local.PhoneWindowEntity
import com.pulso.mobile.data.local.PulsoMobileDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

data class MobileDiagnosticsState(
    val consentGranted: Boolean = false,
    val consentVersion: String? = null,
    val consentTimestamp: Long? = null,
    val watchConnected: Boolean = false,
    val connectedWatchNodeName: String? = null,
    val lastWatchStatus: WatchStatus? = null,
    val lastSampleReceived: MeasurementWindow? = null,
    val totalWindowsStored: Int = 0,
    val lastSyncBatchId: String? = null,
    val lastSyncAcceptedCount: Int = 0,
    val lastPingRoundtripMs: Long? = null,
    val lastError: String? = null,
    val baselineMismatch: String? = null
)

class MobileSyncManager private constructor(private val context: Context) :
    MessageClient.OnMessageReceivedListener {

    private val sharedPrefs = context.getSharedPreferences("pulso_mobile_prefs", Context.MODE_PRIVATE)
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val database = PulsoMobileDatabase.getInstance(context)
    private val windowDao = database.phoneWindowDao()
    private val scope = CoroutineScope(Dispatchers.IO + SupervisorJob())

    private val _diagnosticsState = MutableStateFlow(loadInitialDiagnostics())
    val diagnosticsState: StateFlow<MobileDiagnosticsState> = _diagnosticsState.asStateFlow()

    init {
        messageClient.addListener(this)
        scope.launch {
            checkWatchConnection()
            observeDatabaseCount()
        }
    }

    private fun loadInitialDiagnostics(): MobileDiagnosticsState {
        val granted = sharedPrefs.getBoolean(KEY_CONSENT_GRANTED, false)
        val version = sharedPrefs.getString(KEY_CONSENT_VERSION, null)
        val timestamp = if (sharedPrefs.contains(KEY_CONSENT_TIMESTAMP)) sharedPrefs.getLong(KEY_CONSENT_TIMESTAMP, 0L) else null
        return MobileDiagnosticsState(
            consentGranted = granted,
            consentVersion = version,
            consentTimestamp = timestamp
        )
    }

    private suspend fun observeDatabaseCount() {
        val count = windowDao.countWindows()
        _diagnosticsState.value = _diagnosticsState.value.copy(totalWindowsStored = count)
    }

    fun isConsentGranted(): Boolean = _diagnosticsState.value.consentGranted

    suspend fun setConsent(granted: Boolean, version: String = "1.0-es") = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        sharedPrefs.edit()
            .putBoolean(KEY_CONSENT_GRANTED, granted)
            .putString(KEY_CONSENT_VERSION, if (granted) version else null)
            .putLong(KEY_CONSENT_TIMESTAMP, now)
            .apply()

        _diagnosticsState.value = _diagnosticsState.value.copy(
            consentGranted = granted,
            consentVersion = if (granted) version else null,
            consentTimestamp = now
        )

        val config = ConfigSync(
            collectionEnabled = granted,
            consentVersion = if (granted) version else null,
            windowMinutes = 5,
            updatedAt = now
        )
        sendConfigToWatch(config)
    }

    suspend fun sendConfigToWatch(config: ConfigSync): Boolean = withContext(Dispatchers.IO) {
        try {
            val putDataMapReq = PutDataMapRequest.create(DataLayerContracts.CONFIG_PATH).apply {
                dataMap.putByteArray(DataLayerContracts.CONFIG_KEY, config.toByteArray())
            }
            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            Tasks.await(dataClient.putDataItem(putDataReq))
            Log.i(TAG, "Successfully synchronized /pulso/v1/config to Wearable DataLayer: enabled=${config.collectionEnabled}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to synchronize /pulso/v1/config", e)
            _diagnosticsState.value = _diagnosticsState.value.copy(lastError = "Error enviando configuración: ${e.message}")
            false
        }
    }

    suspend fun checkWatchConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = Tasks.await(nodeClient.connectedNodes)
            val watchNode = nodes.firstOrNull()
            val isConnected = watchNode != null
            _diagnosticsState.value = _diagnosticsState.value.copy(
                watchConnected = isConnected,
                connectedWatchNodeName = watchNode?.displayName
            )
            if (isConnected) {
                // Also ensure config is synced on connect
                val config = ConfigSync(
                    collectionEnabled = isConsentGranted(),
                    consentVersion = _diagnosticsState.value.consentVersion,
                    updatedAt = System.currentTimeMillis()
                )
                sendConfigToWatch(config)
            }
            isConnected
        } catch (e: Exception) {
            Log.e(TAG, "Error checking watch connection", e)
            false
        }
    }

    suspend fun testPing(): Long? = withContext(Dispatchers.IO) {
        val start = System.currentTimeMillis()
        try {
            val nodes = Tasks.await(nodeClient.connectedNodes)
            val watchNode = nodes.firstOrNull()
            if (watchNode == null) {
                _diagnosticsState.value = _diagnosticsState.value.copy(lastError = "No hay reloj conectado")
                return@withContext null
            }

            val pingMsg = PingMessage(
                from = DataLayerContracts.Source.PHONE,
                sentAt = start,
                counter = 1
            )
            Tasks.await(
                messageClient.sendMessage(
                    watchNode.id,
                    DataLayerContracts.PING_PATH,
                    pingMsg.toByteArray()
                )
            )
            val duration = System.currentTimeMillis() - start
            _diagnosticsState.value = _diagnosticsState.value.copy(
                lastPingRoundtripMs = duration,
                lastError = null
            )
            duration
        } catch (e: Exception) {
            Log.e(TAG, "Ping test failed", e)
            _diagnosticsState.value = _diagnosticsState.value.copy(lastError = "Fallo de ping: ${e.message}")
            null
        }
    }

    suspend fun sendResetBaselineCommand(): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = Tasks.await(nodeClient.connectedNodes)
            val watchNode = nodes.firstOrNull()
            if (watchNode == null) {
                _diagnosticsState.value = _diagnosticsState.value.copy(lastError = "No se puede reiniciar en el reloj: reloj no conectado")
                return@withContext false
            }

            val cmd = com.pulso.core.model.WatchCommand(
                command = com.pulso.core.model.WatchCommand.CMD_RESET_BASELINE,
                sentAt = System.currentTimeMillis()
            )

            Tasks.await(
                messageClient.sendMessage(
                    watchNode.id,
                    DataLayerContracts.COMMANDS_PATH,
                    cmd.toByteArray()
                )
            )
            Log.i(TAG, "Dispatched CMD_RESET_BASELINE to watch ${watchNode.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send reset baseline command", e)
            _diagnosticsState.value = _diagnosticsState.value.copy(lastError = "Fallo enviando comando de reinicio: ${e.message}")
            false
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        when (messageEvent.path) {
            DataLayerContracts.SAMPLES_PATH -> handleSamplesMessage(messageEvent)
            DataLayerContracts.STATUS_PATH -> handleStatusMessage(messageEvent)
        }
    }

    fun handleSamplesMessage(messageEvent: MessageEvent) {
        scope.launch {
            try {
                val batch = SamplesBatch.fromByteArray(messageEvent.data)
                Log.i(TAG, "Received SamplesBatch ${batch.batchId} from watch ${batch.watchId} with ${batch.windows.size} windows")

                // Validate ranges & physiological boundaries
                val validEntities = mutableListOf<PhoneWindowEntity>()
                for (w in batch.windows) {
                    if (w.windowStart > 0 && w.windowEnd >= w.windowStart && w.meanHr in 30.0..220.0) {
                        validEntities.add(PhoneWindowEntity.fromDomain(w, batch.watchId))
                    } else {
                        Log.w(TAG, "Skipping corrupted window outside physiological ranges: $w")
                    }
                }

                // Idempotent upsert
                windowDao.insertOrUpdateAll(validEntities)
                val newCount = windowDao.countWindows()

                _diagnosticsState.value = _diagnosticsState.value.copy(
                    lastSampleReceived = batch.windows.lastOrNull(),
                    totalWindowsStored = newCount,
                    lastSyncBatchId = batch.batchId,
                    lastSyncAcceptedCount = validEntities.size,
                    lastError = null
                )

                // Dispatch ACK
                val ack = SamplesAck(
                    batchId = batch.batchId,
                    accepted = validEntities.size
                )
                Tasks.await(
                    messageClient.sendMessage(
                        messageEvent.sourceNodeId,
                        DataLayerContracts.SAMPLES_ACK_PATH,
                        ack.toByteArray()
                    )
                )
                Log.i(TAG, "Dispatched SamplesAck for batch ${batch.batchId} accepted ${validEntities.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse or persist SamplesBatch", e)
                _diagnosticsState.value = _diagnosticsState.value.copy(lastError = "Lote inválido rechazado: ${e.message}")
            }
        }
    }

    private fun handleStatusMessage(messageEvent: MessageEvent) {
        try {
            val status = WatchStatus.fromByteArray(messageEvent.data)
            Log.d(TAG, "Received WatchStatus: battery=${status.batteryPct}%, model=${status.watchModel}, serviceRunning=${status.serviceRunning}")
            _diagnosticsState.value = _diagnosticsState.value.copy(
                lastWatchStatus = status,
                watchConnected = true
            )
        } catch (e: Exception) {
            Log.e(TAG, "Failed to parse WatchStatus", e)
        }
    }

    companion object {
        private const val TAG = "PulsoMobileSync"
        private const val KEY_CONSENT_GRANTED = "consent_granted"
        private const val KEY_CONSENT_VERSION = "consent_version"
        private const val KEY_CONSENT_TIMESTAMP = "consent_timestamp"

        @Volatile
        private var INSTANCE: MobileSyncManager? = null

        fun getInstance(context: Context): MobileSyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = MobileSyncManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

class PhoneWearableListenerService : WearableListenerService() {
    override fun onMessageReceived(messageEvent: MessageEvent) {
        val manager = MobileSyncManager.getInstance(this)
        manager.onMessageReceived(messageEvent)
    }
}
