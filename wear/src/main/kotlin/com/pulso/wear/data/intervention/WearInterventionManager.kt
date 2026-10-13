package com.pulso.wear.data.intervention

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.provider.Settings
import android.util.Log
import androidx.core.app.NotificationCompat
import com.pulso.core.contract.DataLayerContracts
import com.pulso.core.intervention.HapticPattern
import com.pulso.core.intervention.InterventionAction
import com.pulso.core.intervention.InterventionFeedback
import com.pulso.core.intervention.WatchCommand
import com.pulso.core.stress.StressEvent
import com.pulso.wear.data.baseline.WearBaselineManager
import com.pulso.wear.data.local.InterventionFeedbackEntity
import com.pulso.wear.data.local.PulsoWearDatabase
import com.pulso.wear.data.sync.SyncManager
import com.pulso.wear.presentation.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class WearInterventionManager private constructor(private val context: Context) {

    private val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    private val vibrator: Vibrator? = getDeviceVibrator(context)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val database = PulsoWearDatabase.getInstance(context)
    private val syncManager by lazy { SyncManager.getInstance(context) }
    private val scope = CoroutineScope(Dispatchers.IO)

    init {
        createNotificationChannel()
    }

    private fun getDeviceVibrator(ctx: Context): Vibrator? {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = ctx.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager
            vibratorManager?.defaultVibrator ?: (ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator)
        } else {
            @Suppress("DEPRECATION")
            ctx.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Intervenciones de bienestar",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos suaves de autorregulación emocional y bienestar"
                enableVibration(false) // We handle custom haptics directly with HapticPattern
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    /**
     * Checks environmental restrictions:
     * - Do Not Disturb (DND) / Zen mode
     * - Battery Saver mode
     * - System haptic settings
     */
    fun isDoNotDisturbActive(): Boolean {
        return try {
            val filter = notificationManager.currentInterruptionFilter
            filter != NotificationManager.INTERRUPTION_FILTER_ALL && filter != NotificationManager.INTERRUPTION_FILTER_UNKNOWN
        } catch (_: Exception) {
            false
        }
    }

    fun isBatterySaverActive(): Boolean {
        return try {
            powerManager?.isPowerSaveMode == true
        } catch (_: Exception) {
            false
        }
    }

    fun areHapticsEnabledInSystem(): Boolean {
        return try {
            Settings.System.getInt(context.contentResolver, Settings.System.HAPTIC_FEEDBACK_ENABLED, 1) != 0
        } catch (_: Exception) {
            true
        }
    }

    /**
     * Triggers the gentle intervention for a StressEvent.
     */
    fun triggerIntervention(event: StressEvent, currentBucket: String? = null) {
        val dndActive = isDoNotDisturbActive()
        val batterySaver = isBatterySaverActive()
        val hapticsEnabled = areHapticsEnabledInSystem()

        Log.i(
            TAG,
            "Triggering gentle intervention for event ${event.id}: DND=$dndActive, BatterySaver=$batterySaver, HapticsEnabled=$hapticsEnabled"
        )

        // 1. Vibration: ONLY if DND is inactive, Battery Saver is inactive, and haptics enabled
        if (!dndActive && !batterySaver && hapticsEnabled) {
            playGentleVibration()
        } else {
            Log.i(TAG, "Vibration silenced due to DND=$dndActive, BatterySaver=$batterySaver, or HapticsDisabled=${!hapticsEnabled}")
        }

        // 2. Ongoing-free Notification with actions
        postGentleNotification(event, currentBucket)
    }

    /**
     * Executes the gentle vibration pattern defined in :core HapticPattern.
     * Respects amplitude control if supported; falls back to binary timings if unsupported.
     */
    fun playGentleVibration() {
        val vib = vibrator ?: return
        if (!vib.hasVibrator()) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                if (vib.hasAmplitudeControl()) {
                    val effect = VibrationEffect.createWaveform(
                        HapticPattern.GENTLE_PULSE_TIMINGS,
                        HapticPattern.GENTLE_PULSE_AMPLITUDES,
                        HapticPattern.REPEAT_INDEX_NONE
                    )
                    vib.vibrate(effect)
                    Log.d(TAG, "Played gentle waveform vibration with amplitude control")
                } else {
                    val effect = VibrationEffect.createWaveform(
                        HapticPattern.BINARY_FALLBACK_TIMINGS,
                        HapticPattern.REPEAT_INDEX_NONE
                    )
                    vib.vibrate(effect)
                    Log.d(TAG, "Played gentle binary fallback vibration (no amplitude control)")
                }
            } else {
                @Suppress("DEPRECATION")
                vib.vibrate(HapticPattern.BINARY_FALLBACK_TIMINGS, HapticPattern.REPEAT_INDEX_NONE)
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to vibrate", e)
        }
    }

    private fun postGentleNotification(event: StressEvent, currentBucket: String?) {
        val eventId = event.id
        val bucket = currentBucket ?: "UNKNOWN"

        // Build Action Intents
        val talkIntent = createActionPendingIntent(eventId, InterventionAction.TALK, bucket, 1)
        val breatheIntent = createActionPendingIntent(eventId, InterventionAction.BREATHE, bucket, 2)
        val dismissIntent = createActionPendingIntent(eventId, InterventionAction.DISMISS, bucket, 3)
        val notStressedIntent = createActionPendingIntent(eventId, InterventionAction.NOT_STRESSED, bucket, 4)

        // Delete intent (when notification times out or is swiped away) -> IGNORED
        val deleteIntent = createActionPendingIntent(eventId, InterventionAction.IGNORED, bucket, 5)

        // Content intent: Opens main activity to breathing route or status
        val contentIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                putExtra("route", "breathing")
                putExtra("eventId", eventId)
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentTitle("Pulso")
            .setContentText("¿Quieres hablar un momento?")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Notamos una elevación inusual en tu pulso sin actividad física. ¿Quieres hablar un momento?"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setOngoing(false) // Ongoing-free
            .setTimeoutAfter(NOTIFICATION_TIMEOUT_MS) // Auto-dismiss after 60 seconds
            .setContentIntent(contentIntent)
            .setDeleteIntent(deleteIntent)
            .addAction(0, "Hablar", talkIntent)
            .addAction(0, "Respirar", breatheIntent)
            .addAction(0, "Ahora no", dismissIntent)
            .addAction(0, "No estoy estresado", notStressedIntent)
            .build()

        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun createActionPendingIntent(
        eventId: String,
        action: InterventionAction,
        bucket: String,
        requestCode: Int
    ): PendingIntent {
        val intent = Intent(context, InterventionActionReceiver::class.java).apply {
            this.action = ACTION_INTERVENTION_CLICK
            putExtra(EXTRA_EVENT_ID, eventId)
            putExtra(EXTRA_ACTION_NAME, action.name)
            putExtra(EXTRA_BUCKET, bucket)
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    fun dismissNotification() {
        notificationManager.cancel(NOTIFICATION_ID)
    }

    fun recordFeedback(feedback: InterventionFeedback) {
        scope.launch {
            try {
                // Save locally in Room
                val entity = InterventionFeedbackEntity.fromDomain(feedback, synced = false)
                database.interventionFeedbackDao().insertOrUpdate(entity)
                Log.i(TAG, "Recorded intervention feedback locally: ${feedback.action} for event ${feedback.eventId}")

                // Send to phone via Wearable DataLayer
                syncManager.sendInterventionFeedback(feedback)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to record intervention feedback", e)
            }
        }
    }

    companion object {
        const val TAG = "WearIntervention"
        const val CHANNEL_ID = "pulso_gentle_intervention_channel"
        const val NOTIFICATION_ID = 4001
        const val NOTIFICATION_TIMEOUT_MS = 60_000L // 60 seconds timeout (triggers IGNORED)

        const val ACTION_INTERVENTION_CLICK = "com.pulso.wear.ACTION_INTERVENTION_CLICK"
        const val EXTRA_EVENT_ID = "extra_event_id"
        const val EXTRA_ACTION_NAME = "extra_action_name"
        const val EXTRA_BUCKET = "extra_bucket"

        @Volatile
        private var INSTANCE: WearInterventionManager? = null

        fun getInstance(context: Context): WearInterventionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = WearInterventionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}

class InterventionActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val eventId = intent.getStringExtra(WearInterventionManager.EXTRA_EVENT_ID) ?: return
        val actionName = intent.getStringExtra(WearInterventionManager.EXTRA_ACTION_NAME) ?: return
        val bucket = intent.getStringExtra(WearInterventionManager.EXTRA_BUCKET)

        val action = try {
            InterventionAction.valueOf(actionName)
        } catch (_: Exception) {
            InterventionAction.DISMISS
        }

        Log.i(WearInterventionManager.TAG, "User clicked action $action for event $eventId (bucket=$bucket)")

        val manager = WearInterventionManager.getInstance(context)
        manager.dismissNotification()

        val feedback = InterventionFeedback(
            eventId = eventId,
            action = action,
            timeBucket = bucket,
            timestamp = System.currentTimeMillis()
        )
        manager.recordFeedback(feedback)

        // Handle specific actions
        when (action) {
            InterventionAction.TALK -> {
                // Send command /pulso/v1/commands {type:"OPEN_CHAT", eventId} to phone
                CoroutineScope(Dispatchers.IO).launch {
                    SyncManager.getInstance(context).sendWatchCommand(
                        WatchCommand(
                            type = WatchCommand.TYPE_OPEN_CHAT,
                            eventId = eventId,
                            timestamp = System.currentTimeMillis()
                        )
                    )
                }
            }
            InterventionAction.BREATHE -> {
                // Open Breathing screen on watch
                val openIntent = Intent(context, MainActivity::class.java).apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
                    putExtra("route", "breathing")
                    putExtra("eventId", eventId)
                }
                context.startActivity(openIntent)
            }
            else -> {
                // DISMISS, NOT_STRESSED, IGNORED already recorded in feedback
            }
        }
    }
}
