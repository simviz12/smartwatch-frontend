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
import kotlinx.serialization.encodeToString
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.json.Json
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
    val baselineMismatch: String? = null,
    val lastEventReceived: com.pulso.core.stress.StressEvent? = null,
    val totalEventsStored: Int = 0,
    val phoneConfigVersion: Int = 1,
    val watchConfigVersion: Int = 1,
    val hasPendingConfig: Boolean = false,
    val lastFeedbackReceived: com.pulso.core.intervention.InterventionFeedback? = null,
    val activeSnoozeUntil: Long? = null,
    val adaptiveSuggestion: com.pulso.core.intervention.AdaptiveSuggestion? = null,
    val lastBreathingSession: com.pulso.core.breathing.BreathingSessionRecord? = null,
    val totalBreathingSessionsStored: Int = 0,
    val watchHapticCapability: String? = null // "Control de amplitud analógico" or "Modulación por pulsos (sin amplitud)"
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
        val granted = sharedPrefs.getBoolean(KEY_CONSENT_GRANTED, true)
        val version = sharedPrefs.getString(KEY_CONSENT_VERSION, "1.0-es")
        val timestamp = if (sharedPrefs.contains(KEY_CONSENT_TIMESTAMP)) sharedPrefs.getLong(KEY_CONSENT_TIMESTAMP, System.currentTimeMillis()) else System.currentTimeMillis()
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

        val config = loadPersistedConfig().copy(
            collectionEnabled = granted,
            consentVersion = if (granted) version else null,
            updatedAt = now
        )
        persistAndSendConfig(config)
    }

    fun loadPersistedConfig(): ConfigSync {
        val granted = sharedPrefs.getBoolean(KEY_CONSENT_GRANTED, false)
        val consentVer = sharedPrefs.getString(KEY_CONSENT_VERSION, null)
        val stressJson = sharedPrefs.getString(KEY_STRESS_CONFIG, null)
        val stressConfig = if (stressJson != null) {
            try {
                kotlinx.serialization.json.Json.decodeFromString<com.pulso.core.stress.StressConfig>(stressJson)
            } catch (e: Exception) {
                com.pulso.core.stress.StressConfig()
            }
        } else com.pulso.core.stress.StressConfig()

        val qStartH = sharedPrefs.getInt(KEY_QUIET_START_H, 22)
        val qStartM = sharedPrefs.getInt(KEY_QUIET_START_M, 0)
        val qEndH = sharedPrefs.getInt(KEY_QUIET_END_H, 7)
        val qEndM = sharedPrefs.getInt(KEY_QUIET_END_M, 0)
        val quiet = com.pulso.core.model.QuietHours(qStartH, qStartM, qEndH, qEndM)

        val breathingPlanName = sharedPrefs.getString(KEY_BREATHING_PLAN, "Calma 4-6") ?: "Calma 4-6"
        val breathingDuration = sharedPrefs.getInt(KEY_BREATHING_DURATION, 2)
        val breathingIntensity = sharedPrefs.getString(KEY_BREATHING_INTENSITY, "MEDIUM") ?: "MEDIUM"
        val breathing = com.pulso.core.breathing.BreathingConfig(
            preferredPlanName = breathingPlanName,
            durationMinutes = breathingDuration,
            hapticIntensity = breathingIntensity
        )

        return ConfigSync(
            collectionEnabled = granted,
            consentVersion = consentVer,
            windowMinutes = 5,
            quietHours = quiet,
            stressConfig = stressConfig,
            breathingConfig = breathing,
            updatedAt = sharedPrefs.getLong(KEY_CONFIG_UPDATED_AT, System.currentTimeMillis())
        )
    }

    suspend fun updateBreathingConfig(
        breathingConfig: com.pulso.core.breathing.BreathingConfig
    ): Boolean = withContext(Dispatchers.IO) {
        val current = loadPersistedConfig()
        val newConfig = current.copy(
            breathingConfig = breathingConfig,
            updatedAt = System.currentTimeMillis()
        )
        persistAndSendConfig(newConfig)
    }

    suspend fun updateStressAndQuietConfig(
        stressConfig: com.pulso.core.stress.StressConfig,
        quietHours: com.pulso.core.model.QuietHours?
    ): Boolean = withContext(Dispatchers.IO) {
        val current = loadPersistedConfig()
        val newConfig = current.copy(
            stressConfig = stressConfig.copy(version = stressConfig.version + 1),
            quietHours = quietHours,
            updatedAt = System.currentTimeMillis()
        )
        persistAndSendConfig(newConfig)
    }

    private suspend fun persistAndSendConfig(config: ConfigSync): Boolean {
        sharedPrefs.edit()
            .putString(KEY_STRESS_CONFIG, kotlinx.serialization.json.Json.encodeToString(config.stressConfig))
            .putString(KEY_BREATHING_PLAN, config.breathingConfig.preferredPlanName)
            .putInt(KEY_BREATHING_DURATION, config.breathingConfig.durationMinutes)
            .putString(KEY_BREATHING_INTENSITY, config.breathingConfig.hapticIntensity)
            .putLong(KEY_CONFIG_UPDATED_AT, config.updatedAt)
            .apply()

        config.quietHours?.let {
            sharedPrefs.edit()
                .putInt(KEY_QUIET_START_H, it.startHour)
                .putInt(KEY_QUIET_START_M, it.startMinute)
                .putInt(KEY_QUIET_END_H, it.endHour)
                .putInt(KEY_QUIET_END_M, it.endMinute)
                .apply()
        }

        _diagnosticsState.value = _diagnosticsState.value.copy(
            phoneConfigVersion = config.stressConfig.version,
            hasPendingConfig = true
        )

        return sendConfigToWatch(config)
    }

    suspend fun sendConfigToWatch(config: ConfigSync): Boolean = withContext(Dispatchers.IO) {
        try {
            val putDataMapReq = PutDataMapRequest.create(DataLayerContracts.CONFIG_PATH).apply {
                dataMap.putByteArray(DataLayerContracts.CONFIG_KEY, config.toByteArray())
            }
            val putDataReq = putDataMapReq.asPutDataRequest().setUrgent()
            Tasks.await(dataClient.putDataItem(putDataReq))
            Log.i(TAG, "Successfully synchronized /pulso/v1/config to Wearable DataLayer: enabled=${config.collectionEnabled}, stressVer=${config.stressConfig.version}")
            _diagnosticsState.value = _diagnosticsState.value.copy(hasPendingConfig = false)
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
            DataLayerContracts.EVENTS_PATH -> handleEventsMessage(messageEvent)
            DataLayerContracts.FEEDBACK_PATH -> handleFeedbackMessage(messageEvent)
            DataLayerContracts.COMMANDS_PATH -> handleWatchCommandsMessage(messageEvent)
            "/pulso/v1/open-chat" -> {
                val eventId = String(messageEvent.data, Charsets.UTF_8).ifBlank { null }
                Log.i(TAG, "Direct /pulso/v1/open-chat requested from watch (eventId=$eventId)")
                postOpenChatNotification(eventId)
            }
            DataLayerContracts.BREATHING_PATH -> handleBreathingSessionMessage(messageEvent)
        }
    }

    suspend fun sendStartBreathingCommand(): Boolean = withContext(Dispatchers.IO) {
        try {
            val nodes = Tasks.await(nodeClient.connectedNodes)
            val watchNode = nodes.firstOrNull()
            if (watchNode == null) {
                _diagnosticsState.value = _diagnosticsState.value.copy(lastError = "No se puede iniciar respiración: reloj no conectado")
                return@withContext false
            }

            val cmd = com.pulso.core.model.WatchCommand(
                command = com.pulso.core.model.WatchCommand.CMD_START_BREATHING,
                sentAt = System.currentTimeMillis()
            )

            Tasks.await(
                messageClient.sendMessage(
                    watchNode.id,
                    DataLayerContracts.COMMANDS_PATH,
                    cmd.toByteArray()
                )
            )
            Log.i(TAG, "Dispatched CMD_START_BREATHING to watch ${watchNode.id}")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to send start breathing command", e)
            _diagnosticsState.value = _diagnosticsState.value.copy(lastError = "Fallo enviando comando de respiración: ${e.message}")
            false
        }
    }

    private fun handleBreathingSessionMessage(messageEvent: MessageEvent) {
        scope.launch {
            try {
                val record = com.pulso.core.breathing.BreathingSessionRecord.fromByteArray(messageEvent.data)
                Log.i(TAG, "Received BreathingSessionRecord from watch: id=${record.sessionId}, plan=${record.planName}, cycles=${record.completedCycles}/${record.targetCycles}")

                val entity = com.pulso.mobile.data.local.PhoneBreathingSessionEntity.fromDomain(record)
                database.phoneBreathingSessionDao().insertOrUpdate(entity)
                val count = database.phoneBreathingSessionDao().getCount()

                _diagnosticsState.value = _diagnosticsState.value.copy(
                    lastBreathingSession = record,
                    totalBreathingSessionsStored = count,
                    lastError = null
                )

                // Send BreathingAck back to watch
                val ack = com.pulso.core.breathing.BreathingAck(
                    sessionId = record.sessionId,
                    status = "OK"
                )
                Tasks.await(
                    messageClient.sendMessage(
                        messageEvent.sourceNodeId,
                        DataLayerContracts.BREATHING_ACK_PATH,
                        ack.toByteArray()
                    )
                )
                Log.i(TAG, "Dispatched BreathingAck to watch for session ${record.sessionId}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle BreathingSessionRecord", e)
            }
        }
    }

    private val interventionStateMachine = com.pulso.core.intervention.InterventionStateMachine(
        adaptiveThresholdK = 3,
        adaptiveHelpEnabled = true
    )

    private fun handleFeedbackMessage(messageEvent: MessageEvent) {
        scope.launch {
            try {
                val feedback = com.pulso.core.intervention.InterventionFeedback.fromByteArray(messageEvent.data)
                Log.i(TAG, "Received InterventionFeedback from watch: eventId=${feedback.eventId}, action=${feedback.action}")

                // Update event feedback in Room
                database.phoneStressEventDao().updateFeedback(feedback.eventId, feedback.action.name)

                // Process in adaptive state machine
                val suggestion = interventionStateMachine.processFeedback(feedback)

                _diagnosticsState.value = _diagnosticsState.value.copy(
                    lastFeedbackReceived = feedback,
                    adaptiveSuggestion = suggestion ?: _diagnosticsState.value.adaptiveSuggestion
                )

                // Acknowledge back to watch
                val ack = com.pulso.core.intervention.FeedbackAck(
                    eventId = feedback.eventId,
                    action = feedback.action,
                    receivedAt = System.currentTimeMillis()
                )
                Tasks.await(
                    messageClient.sendMessage(
                        messageEvent.sourceNodeId,
                        DataLayerContracts.FEEDBACK_ACK_PATH,
                        ack.toByteArray()
                    )
                )
                Log.i(TAG, "Dispatched FeedbackAck to watch for event ${feedback.eventId}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle InterventionFeedback", e)
            }
        }
    }

    private fun handleWatchCommandsMessage(messageEvent: MessageEvent) {
        scope.launch {
            try {
                val cmd = com.pulso.core.intervention.WatchCommand.fromByteArray(messageEvent.data)
                Log.i(TAG, "Received WatchCommand from watch: type=${cmd.type}, eventId=${cmd.eventId}")
                if (cmd.type == com.pulso.core.intervention.WatchCommand.TYPE_OPEN_CHAT) {
                    Log.i(TAG, "Command OPEN_CHAT triggered from gentle intervention on watch for event ${cmd.eventId}")
                    postOpenChatNotification(cmd.eventId)
                }
            } catch (e: Exception) {
                Log.e(TAG, "Failed to handle WatchCommand", e)
            }
        }
    }

    private fun postOpenChatNotification(eventId: String?) {
        try {
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as android.app.NotificationManager
            val channelId = "pulso_mobile_chat_channel"
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {
                val channel = android.app.NotificationChannel(
                    channelId,
                    "Conversación de apoyo",
                    android.app.NotificationManager.IMPORTANCE_HIGH
                ).apply {
                    description = "Avisos para iniciar conversación de apoyo desde el reloj"
                }
                notificationManager.createNotificationChannel(channel)
            }

            val intent = android.content.Intent(context, com.pulso.mobile.presentation.MainActivity::class.java).apply {
                flags = android.content.Intent.FLAG_ACTIVITY_NEW_TASK or android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("route", "chat_placeholder")
                putExtra("eventId", eventId)
            }
            val pendingIntent = android.app.PendingIntent.getActivity(
                context,
                100,
                intent,
                android.app.PendingIntent.FLAG_UPDATE_CURRENT or android.app.PendingIntent.FLAG_IMMUTABLE
            )

            val notification = androidx.core.app.NotificationCompat.Builder(context, channelId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle("Pulso")
                .setContentText("Abre Pulso para hablar")
                .setStyle(androidx.core.app.NotificationCompat.BigTextStyle().bigText("Tu reloj solicitó abrir el espacio de conversación."))
                .setPriority(androidx.core.app.NotificationCompat.PRIORITY_HIGH)
                .setAutoCancel(true)
                .setContentIntent(pendingIntent)
                .build()

            notificationManager.notify(7001, notification)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to post open chat notification on mobile", e)
        }
    }

    private fun handleEventsMessage(messageEvent: MessageEvent) {
        scope.launch {
            try {
                val batch = com.pulso.core.stress.StressEventsBatch.fromByteArray(messageEvent.data)
                Log.i(TAG, "Received StressEventsBatch with ${batch.events.size} events from watch ${batch.watchId}")

                val entities = batch.events.map { com.pulso.mobile.data.local.PhoneStressEventEntity.fromDomain(it) }
                database.phoneStressEventDao().insertOrUpdateAll(entities)

                val count = database.phoneStressEventDao().countEvents()
                _diagnosticsState.value = _diagnosticsState.value.copy(
                    lastEventReceived = batch.events.lastOrNull(),
                    totalEventsStored = count,
                    lastError = null
                )

                // Dispatch ACK back to watch
                val ack = com.pulso.core.stress.StressEventsAck(
                    acceptedCount = entities.size,
                    lastEventId = batch.events.lastOrNull()?.id
                )
                Tasks.await(
                    messageClient.sendMessage(
                        messageEvent.sourceNodeId,
                        DataLayerContracts.EVENTS_ACK_PATH,
                        ack.toByteArray()
                    )
                )
                Log.i(TAG, "Sent StressEventsAck to watch accepted=${entities.size}")
            } catch (e: Exception) {
                Log.e(TAG, "Failed to parse or persist StressEventsBatch", e)
                _diagnosticsState.value = _diagnosticsState.value.copy(lastError = "Error procesando eventos de estrés: ${e.message}")
            }
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
            Log.d(TAG, "Received WatchStatus: battery=${status.batteryPct}%, model=${status.watchModel}, serviceRunning=${status.serviceRunning}, stressVer=${status.stressConfigVersion}")
            val pending = status.stressConfigVersion != null && status.stressConfigVersion != _diagnosticsState.value.phoneConfigVersion
            val hapticDesc = when (status.hasAmplitudeControl) {
                true -> "Control de amplitud analógico (onda suave)"
                false -> "Modulación por pulsos (sin amplitud analógica)"
                null -> "Desconocido (sin reporte del reloj)"
            }
            _diagnosticsState.value = _diagnosticsState.value.copy(
                lastWatchStatus = status,
                watchConfigVersion = status.stressConfigVersion ?: _diagnosticsState.value.watchConfigVersion,
                hasPendingConfig = pending,
                watchConnected = true,
                watchHapticCapability = hapticDesc
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
        private const val KEY_STRESS_CONFIG = "stress_config_json"
        private const val KEY_CONFIG_UPDATED_AT = "config_updated_at"
        private const val KEY_QUIET_START_H = "quiet_start_h"
        private const val KEY_QUIET_START_M = "quiet_start_m"
        private const val KEY_QUIET_END_H = "quiet_end_h"
        private const val KEY_QUIET_END_M = "quiet_end_m"
        private const val KEY_BREATHING_PLAN = "breathing_plan"
        private const val KEY_BREATHING_DURATION = "breathing_duration"
        private const val KEY_BREATHING_INTENSITY = "breathing_intensity"

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
