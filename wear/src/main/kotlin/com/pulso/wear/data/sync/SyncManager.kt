package com.pulso.wear.data.sync

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.BatteryManager
import android.os.Build
import android.util.Log
import androidx.work.Constraints
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.NetworkType
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.google.android.gms.tasks.Tasks
import com.google.android.gms.wearable.DataClient
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.MessageClient
import com.google.android.gms.wearable.MessageEvent
import com.google.android.gms.wearable.Wearable
import com.pulso.core.contract.DataLayerContracts
import com.pulso.core.model.ConfigSync
import com.pulso.core.model.MeasurementWindow
import com.pulso.core.model.SamplesAck
import com.pulso.core.model.SamplesBatch
import com.pulso.core.model.WatchStatus
import com.pulso.wear.data.local.PulsoWearDatabase
import com.pulso.wear.data.sensor.SensorSamplingService
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.concurrent.TimeUnit

class SyncManager private constructor(private val context: Context) :
    DataClient.OnDataChangedListener,
    MessageClient.OnMessageReceivedListener {

    private val sharedPrefs = context.getSharedPreferences("pulso_sync_prefs", Context.MODE_PRIVATE)
    private val database = PulsoWearDatabase.getInstance(context)
    private val dataClient = Wearable.getDataClient(context)
    private val messageClient = Wearable.getMessageClient(context)
    private val nodeClient = Wearable.getNodeClient(context)
    private val workManager = WorkManager.getInstance(context)
    private val scope = CoroutineScope(Dispatchers.IO)

    private val _currentConfig = MutableStateFlow<ConfigSync?>(loadPersistedConfig())
    val currentConfig: StateFlow<ConfigSync?> = _currentConfig.asStateFlow()

    init {
        dataClient.addListener(this)
        messageClient.addListener(this)
        schedulePeriodicSync()
    }

    private fun loadPersistedConfig(): ConfigSync? {
        val enabled = sharedPrefs.getBoolean(KEY_COLLECTION_ENABLED, false)
        val hasConfig = sharedPrefs.getBoolean(KEY_HAS_CONFIG, false)
        if (!hasConfig) return null

        val version = sharedPrefs.getString(KEY_CONSENT_VERSION, null)
        val interval = sharedPrefs.getInt(KEY_WINDOW_MINUTES, 5)
        val updated = sharedPrefs.getLong(KEY_UPDATED_AT, 0L)
        return ConfigSync(
            collectionEnabled = enabled,
            consentVersion = version,
            windowMinutes = interval,
            updatedAt = updated
        )
    }

    private fun persistConfig(config: ConfigSync) {
        sharedPrefs.edit()
            .putBoolean(KEY_HAS_CONFIG, true)
            .putBoolean(KEY_COLLECTION_ENABLED, config.collectionEnabled)
            .putString(KEY_CONSENT_VERSION, config.consentVersion)
            .putInt(KEY_WINDOW_MINUTES, config.windowMinutes)
            .putLong(KEY_UPDATED_AT, config.updatedAt)
            .apply()
        _currentConfig.value = config

        if (config.collectionEnabled) {
            Log.i(TAG, "Consent gate: collectionEnabled=true. Starting SensorSamplingService.")
            SensorSamplingService.start(context)
        } else {
            Log.i(TAG, "Consent gate: collectionEnabled=false. Halting SensorSamplingService.")
            SensorSamplingService.stop(context)
        }
    }

    fun updateLocalConfig(config: ConfigSync) {
        persistConfig(config)
    }

    fun isCollectionEnabled(): Boolean = _currentConfig.value?.collectionEnabled == true

    fun getWindowMinutes(): Int = _currentConfig.value?.windowMinutes ?: 5

    override fun onDataChanged(dataEvents: DataEventBuffer) {
        for (event in dataEvents) {
            if (event.type == DataEvent.TYPE_CHANGED && event.dataItem.uri.path == DataLayerContracts.CONFIG_PATH) {
                val dataMapItem = DataMapItem.fromDataItem(event.dataItem)
                val payloadBytes = dataMapItem.dataMap.getByteArray(DataLayerContracts.CONFIG_KEY)
                if (payloadBytes != null) {
                    try {
                        val config = ConfigSync.fromByteArray(payloadBytes)
                        Log.i(TAG, "Received updated ConfigSync from DataClient: enabled=${config.collectionEnabled}, version=${config.consentVersion}")
                        persistConfig(config)
                    } catch (e: Exception) {
                        Log.e(TAG, "Error deserializing ConfigSync item", e)
                    }
                }
            }
        }
    }

    override fun onMessageReceived(messageEvent: MessageEvent) {
        if (messageEvent.path == DataLayerContracts.SAMPLES_ACK_PATH) {
            try {
                val ack = SamplesAck.fromByteArray(messageEvent.data)
                Log.i(TAG, "Received SamplesAck from phone: batchId=${ack.batchId}, accepted=${ack.accepted}")
                scope.launch {
                    val deleted = database.windowDao().deleteWindowsByBatchId(ack.batchId)
                    Log.i(TAG, "Deleted $deleted synced windows from watch local Room buffer for batch ${ack.batchId}")
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse SamplesAck message", e)
            }
        } else if (messageEvent.path == DataLayerContracts.COMMANDS_PATH) {
            try {
                val cmd = com.pulso.core.model.WatchCommand.fromByteArray(messageEvent.data)
                Log.i(TAG, "Received command from phone: ${cmd.command}")
                if (cmd.command == com.pulso.core.model.WatchCommand.CMD_RESET_BASELINE) {
                    scope.launch {
                        com.pulso.wear.data.baseline.WearBaselineManager.getInstance(context).resetBaseline()
                        sendStatusUpdate(serviceRunning = isCollectionEnabled())
                    }
                } else if (cmd.command == com.pulso.core.model.WatchCommand.CMD_START_BREATHING) {
                    scope.launch {
                        val openIntent = Intent(context, com.pulso.wear.presentation.MainActivity::class.java).apply {
                            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                            putExtra("route", "breathing")
                        }
                        context.startActivity(openIntent)
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse WatchCommand message", e)
            }
        } else if (messageEvent.path == DataLayerContracts.EVENTS_ACK_PATH) {
            try {
                val ack = com.pulso.core.stress.StressEventsAck.fromByteArray(messageEvent.data)
                Log.i(TAG, "Received StressEventsAck: acceptedCount=${ack.acceptedCount}, lastEventId=${ack.lastEventId}")
                scope.launch {
                    val unsynced = database.stressEventDao().getUnsyncedEvents()
                    val toMark = unsynced.take(ack.acceptedCount).map { it.id }
                    if (toMark.isNotEmpty()) {
                        database.stressEventDao().markEventsAsSynced(toMark)
                        Log.i(TAG, "Marked ${toMark.size} stress events as synced on watch")
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse StressEventsAck message", e)
            }
        } else if (messageEvent.path == DataLayerContracts.FEEDBACK_ACK_PATH) {
            try {
                val ack = com.pulso.core.intervention.FeedbackAck.fromByteArray(messageEvent.data)
                Log.i(TAG, "Received FeedbackAck: eventId=${ack.eventId}, action=${ack.action}")
                scope.launch {
                    database.interventionFeedbackDao().markSynced(ack.eventId, ack.action.name)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse FeedbackAck message", e)
            }
        } else if (messageEvent.path == DataLayerContracts.BREATHING_ACK_PATH) {
            try {
                val ack = com.pulso.core.breathing.BreathingAck.fromByteArray(messageEvent.data)
                Log.i(TAG, "Received BreathingAck: sessionId=${ack.sessionId}")
                scope.launch {
                    database.breathingSessionDao().markSessionSynced(ack.sessionId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse BreathingAck message", e)
            }
        }
    }

    suspend fun sendInterventionFeedback(feedback: com.pulso.core.intervention.InterventionFeedback): Boolean = withContext(Dispatchers.IO) {
        val connectedNodes = try {
            Tasks.await(nodeClient.connectedNodes)
        } catch (e: Exception) {
            emptyList()
        }
        val phoneNode = connectedNodes.firstOrNull() ?: return@withContext false

        try {
            Tasks.await(
                messageClient.sendMessage(
                    phoneNode.id,
                    DataLayerContracts.FEEDBACK_PATH,
                    feedback.toByteArray()
                )
            )
            Log.i(TAG, "Sent InterventionFeedback to ${phoneNode.displayName}: action=${feedback.action}, eventId=${feedback.eventId}")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send InterventionFeedback to phone: ${e.message}")
            false
        }
    }

    suspend fun sendWatchCommand(command: com.pulso.core.intervention.WatchCommand): Boolean = withContext(Dispatchers.IO) {
        val connectedNodes = try {
            Tasks.await(nodeClient.connectedNodes)
        } catch (e: Exception) {
            emptyList()
        }
        val phoneNode = connectedNodes.firstOrNull() ?: return@withContext false

        try {
            Tasks.await(
                messageClient.sendMessage(
                    phoneNode.id,
                    DataLayerContracts.COMMANDS_PATH,
                    command.toByteArray()
                )
            )
            Log.i(TAG, "Sent WatchCommand to ${phoneNode.displayName}: type=${command.type}, eventId=${command.eventId}")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send WatchCommand to phone: ${e.message}")
            false
        }
    }

    suspend fun sendBreathingSession(record: com.pulso.core.breathing.BreathingSessionRecord): Boolean = withContext(Dispatchers.IO) {
        val connectedNodes = try {
            Tasks.await(nodeClient.connectedNodes)
        } catch (e: Exception) {
            emptyList()
        }
        val phoneNode = connectedNodes.firstOrNull() ?: return@withContext false

        try {
            Tasks.await(
                messageClient.sendMessage(
                    phoneNode.id,
                    DataLayerContracts.BREATHING_PATH,
                    record.toByteArray()
                )
            )
            Log.i(TAG, "Sent BreathingSessionRecord to ${phoneNode.displayName}: sessionId=${record.sessionId}")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send BreathingSessionRecord to phone: ${e.message}")
            false
        }
    }

    suspend fun syncStressEventsNow(): Int = withContext(Dispatchers.IO) {
        val unsynced = database.stressEventDao().getUnsyncedEvents()
        if (unsynced.isEmpty()) return@withContext 0

        val connectedNodes = try {
            Tasks.await(nodeClient.connectedNodes)
        } catch (e: Exception) {
            Log.w(TAG, "Failed to query connected nodes for stress events: ${e.message}")
            return@withContext 0
        }

        val phoneNode = connectedNodes.firstOrNull() ?: return@withContext 0
        val domainEvents = unsynced.map { it.toDomain() }
        val batch = com.pulso.core.stress.StressEventsBatch(
            watchId = "${Build.MANUFACTURER}_${Build.MODEL}",
            events = domainEvents
        )

        try {
            Tasks.await(
                messageClient.sendMessage(
                    phoneNode.id,
                    DataLayerContracts.EVENTS_PATH,
                    batch.toByteArray()
                )
            )
            Log.i(TAG, "Sent StressEventsBatch with ${domainEvents.size} events to ${phoneNode.displayName}")
            domainEvents.size
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send stress events batch: ${e.message}")
            0
        }
    }

    fun schedulePeriodicSync() {
        val syncRequest = PeriodicWorkRequestBuilder<SyncWorker>(15, TimeUnit.MINUTES)
            .setConstraints(Constraints.Builder().setRequiredNetworkType(NetworkType.CONNECTED).build())
            .build()

        workManager.enqueueUniquePeriodicWork(
            SYNC_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            syncRequest
        )
    }

    suspend fun syncUnsyncedWindowsNow(): Int = withContext(Dispatchers.IO) {
        // Enforce 7-day retention cap
        val sevenDaysAgo = System.currentTimeMillis() - (7L * 24 * 60 * 60 * 1000)
        val purgedCount = database.windowDao().deleteExpiredWindows(sevenDaysAgo)
        if (purgedCount > 0) {
            Log.w(TAG, "Purged $purgedCount expired measurement windows older than 7 days from watch buffer")
        }

        val unsynced = database.windowDao().getUnsyncedWindows(100)
        if (unsynced.isEmpty()) {
            sendStatusUpdate(serviceRunning = isCollectionEnabled())
            return@withContext 0
        }

        val connectedNodes = try {
            Tasks.await(nodeClient.connectedNodes)
        } catch (e: Exception) {
            Log.e(TAG, "Error querying connected nodes for sync", e)
            emptyList()
        }

        val phoneNode = connectedNodes.firstOrNull()
        if (phoneNode == null) {
            Log.w(TAG, "No connected phone node available for sync. Will retry next cycle.")
            return@withContext 0
        }

        val domainWindows = unsynced.map { it.toDomain() }
        val batches = SamplesBatch.createBatches(
            watchId = Build.SERIAL.ifBlank { Build.MODEL },
            windows = domainWindows
        )

        var totalSent = 0
        for (batch in batches) {
            val batchWindowIds = batch.windows.map { it.id }
            database.windowDao().markWindowsWithBatchId(batchWindowIds, batch.batchId)

            val payload = batch.toByteArray()
            try {
                Tasks.await(
                    messageClient.sendMessage(
                        phoneNode.id,
                        DataLayerContracts.SAMPLES_PATH,
                        payload
                    )
                )
                totalSent += batch.windows.size
                Log.i(TAG, "Dispatched batch ${batch.batchId} with ${batch.windows.size} windows to ${phoneNode.displayName}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to send batch ${batch.batchId} to phone", e)
            }
        }

        sendStatusUpdate(serviceRunning = isCollectionEnabled())
        totalSent
    }

    fun sendStatusUpdate(serviceRunning: Boolean, lastWindowAt: Long? = null) {
        scope.launch {
            try {
                val nodes = Tasks.await(nodeClient.connectedNodes)
                val phoneNode = nodes.firstOrNull() ?: return@launch

                val batteryStatus: Intent? = IntentFilter(Intent.ACTION_BATTERY_CHANGED).let { filter ->
                    context.registerReceiver(null, filter)
                }
                val level = batteryStatus?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
                val scale = batteryStatus?.getIntExtra(BatteryManager.EXTRA_SCALE, -1) ?: -1
                val batteryPct = if (level >= 0 && scale > 0) (level * 100 / scale) else 100

                val baselineManager = com.pulso.wear.data.baseline.WearBaselineManager.getInstance(context)
                val baseline = baselineManager.currentBaseline.value
                val baselineStatusMap = baseline?.buckets?.mapKeys { it.key.name }

                val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    val vm = context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? android.os.VibratorManager
                    vm?.defaultVibrator ?: (context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator)
                } else {
                    @Suppress("DEPRECATION")
                    context.getSystemService(Context.VIBRATOR_SERVICE) as? android.os.Vibrator
                }
                val hasAmplitudeControl = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    vibrator?.hasAmplitudeControl() ?: false
                } else false

                val status = WatchStatus(
                    batteryPct = batteryPct,
                    serviceRunning = serviceRunning,
                    lastWindowAt = lastWindowAt,
                    watchModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                    permissions = mapOf("BODY_SENSORS" to true),
                    baselineStatus = baselineStatusMap,
                    hasAmplitudeControl = hasAmplitudeControl,
                    sentAt = System.currentTimeMillis()
                )

                Tasks.await(
                    messageClient.sendMessage(
                        phoneNode.id,
                        DataLayerContracts.STATUS_PATH,
                        status.toByteArray()
                    )
                )
                Log.d(TAG, "Sent WatchStatus to ${phoneNode.displayName}: battery=$batteryPct%, service=$serviceRunning, buckets=${baselineStatusMap?.keys}")
            } catch (e: Exception) {
                Log.w(TAG, "Failed to send status update: ${e.message}")
            }
        }
    }

    suspend fun openRemotePhoneChat(eventId: String?): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = Tasks.await(nodeClient.connectedNodes)
            val phoneNode = nodes.firstOrNull() ?: return@withContext false
            val payload = (eventId ?: "").toByteArray(Charsets.UTF_8)
            Tasks.await(
                messageClient.sendMessage(
                    phoneNode.id,
                    "/pulso/v1/open-chat",
                    payload
                )
            )
            Log.d(TAG, "Sent /pulso/v1/open-chat to ${phoneNode.displayName} (eventId=$eventId)")
            true
        } catch (e: Exception) {
            Log.w(TAG, "Failed to send open-chat message: ${e.message}")
            false
        }
    }

    companion object {
        private const val TAG = "PulsoSyncManager"
        private const val SYNC_WORK_NAME = "pulso_periodic_sync_work"
        private const val KEY_HAS_CONFIG = "has_config"
        private const val KEY_COLLECTION_ENABLED = "collection_enabled"
        private const val KEY_CONSENT_VERSION = "consent_version"
        private const val KEY_WINDOW_MINUTES = "window_minutes"
        private const val KEY_UPDATED_AT = "updated_at"

        @Volatile
        private var INSTANCE: SyncManager? = null

        fun getInstance(context: Context): SyncManager {
            return INSTANCE ?: synchronized(this) {
                val instance = SyncManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
