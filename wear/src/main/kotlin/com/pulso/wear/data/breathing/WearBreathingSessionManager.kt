package com.pulso.wear.data.breathing

import android.content.Context
import android.os.BatteryManager
import android.os.Build
import android.os.PowerManager
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import com.pulso.core.breathing.BreathingConfig
import com.pulso.core.breathing.BreathingPlan
import com.pulso.core.breathing.BreathingSessionRecord
import com.pulso.core.breathing.BreathingSessionSnapshot
import com.pulso.core.breathing.BreathingSessionStateMachine
import com.pulso.core.breathing.BreathingTimelineGenerator
import com.pulso.core.breathing.HapticIntensity
import com.pulso.core.breathing.HapticTimelineSegment
import com.pulso.core.breathing.SessionEndReason
import com.pulso.core.breathing.TimelineSegmentType
import com.pulso.wear.data.local.BreathingSessionEntity
import com.pulso.wear.data.local.PulsoWearDatabase
import com.pulso.wear.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.UUID

enum class BreathingDisplayMode {
    VIBRATION_ONLY, // Screen stays dark/ambient
    VISUAL_GUIDE    // Circle expansion animation synced with timeline
}

class WearBreathingSessionManager private constructor(private val context: Context) {

    private val vibrator: Vibrator? = getDeviceVibrator(context)
    private val powerManager = context.getSystemService(Context.POWER_SERVICE) as? PowerManager
    private val database = PulsoWearDatabase.getInstance(context)
    private val syncManager by lazy { SyncManager.getInstance(context) }
    private val scope = CoroutineScope(Dispatchers.Default)

    private var sessionJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var stateMachine: BreathingSessionStateMachine? = null

    private val _currentSnapshot = MutableStateFlow<BreathingSessionSnapshot?>(null)
    val currentSnapshot: StateFlow<BreathingSessionSnapshot?> = _currentSnapshot.asStateFlow()

    private val _displayMode = MutableStateFlow(BreathingDisplayMode.VISUAL_GUIDE)
    val displayMode: StateFlow<BreathingDisplayMode> = _displayMode.asStateFlow()

    private var activeSessionId: String? = null
    private var sessionStartTime: Long = 0L
    private var activePlan: BreathingPlan? = null
    private var hrBefore: Double? = null
    private var hrAfter: Double? = null

    fun setDisplayMode(mode: BreathingDisplayMode) {
        _displayMode.value = mode
    }

    fun startSession(
        plan: BreathingPlan? = null,
        intensity: HapticIntensity? = null,
        mode: BreathingDisplayMode = _displayMode.value,
        currentHr: Double? = null
    ) {
        stopSession(SessionEndReason.INTERRUPTED)

        _displayMode.value = mode
        hrBefore = currentHr
        hrAfter = null

        val config = syncManager.currentConfig.value?.breathingConfig ?: BreathingConfig()
        val effectivePlan = plan ?: config.toPlan()
        val effectiveIntensity = intensity ?: config.toHapticIntensity()

        activePlan = effectivePlan
        val sessionId = UUID.randomUUID().toString()
        activeSessionId = sessionId
        sessionStartTime = System.currentTimeMillis()

        val sm = BreathingSessionStateMachine(
            plan = effectivePlan,
            intensity = effectiveIntensity,
            stepMs = 100L
        )
        stateMachine = sm
        sm.start()
        _currentSnapshot.value = sm.getCurrentSnapshot()

        // Acquire partial wake lock so session runs smoothly even with dark/ambient screen
        try {
            wakeLock = powerManager?.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "pulso:breathing_session_wakelock"
            )?.apply {
                acquire(effectivePlan.totalDurationMs + 10_000L)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to acquire wake lock: ${e.message}")
        }

        sessionJob = scope.launch {
            runSessionLoop(sm)
        }
    }

    private suspend fun runSessionLoop(sm: BreathingSessionStateMachine) {
        val tickIntervalMs = 100L
        var lastSegment: HapticTimelineSegment? = null

        try {
            while (coroutineScopeActive() && sm.state == com.pulso.core.breathing.BreathingSessionState.RUNNING) {
                val currentSegment = sm.tick(tickIntervalMs)
                _currentSnapshot.value = sm.getCurrentSnapshot()

                if (currentSegment != null) {
                    if (currentSegment != lastSegment) {
                        lastSegment = currentSegment
                        playSegmentHaptics(currentSegment)
                    }
                } else {
                    // Session naturally completed
                    break
                }
                delay(tickIntervalMs)
            }
        } finally {
            vibrator?.cancel()
            val snap = sm.getCurrentSnapshot()
            _currentSnapshot.value = snap
            val endReason = snap.endReason ?: SessionEndReason.COMPLETED
            persistAndSyncSessionRecord(endReason)
            releaseWakeLock()
        }
    }

    private fun coroutineScopeActive(): Boolean = scope.isActive

    private fun playSegmentHaptics(segment: HapticTimelineSegment) {
        if (vibrator == null || !vibrator.hasVibrator()) return

        try {
            val hasAmplitudeControl = Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && vibrator.hasAmplitudeControl()
            if (hasAmplitudeControl && segment.amplitudes.isNotEmpty()) {
                val timings = LongArray(segment.amplitudes.size) { segment.stepMs }
                // Compensate first timing if needed
                val effect = VibrationEffect.createWaveform(timings, segment.amplitudes, -1)
                vibrator.vibrate(effect)
            } else {
                // Binary pulse density fallback
                val (timings, amps) = BreathingTimelineGenerator.generateBinaryPulseTimings(segment)
                if (timings.isNotEmpty()) {
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                        val effect = VibrationEffect.createWaveform(timings, amps, -1)
                        vibrator.vibrate(effect)
                    } else {
                        @Suppress("DEPRECATION")
                        vibrator.vibrate(timings, -1)
                    }
                }
            }
        } catch (e: Exception) {
            Log.w(TAG, "Failed to vibrate for segment ${segment.type}: ${e.message}")
        }
    }

    fun pauseSession() {
        vibrator?.cancel()
        stateMachine?.pause()
        _currentSnapshot.value = stateMachine?.getCurrentSnapshot()
    }

    fun resumeSession() {
        stateMachine?.resume()
        _currentSnapshot.value = stateMachine?.getCurrentSnapshot()
    }

    fun stopSession(reason: SessionEndReason = SessionEndReason.USER_STOPPED) {
        vibrator?.cancel()
        val sm = stateMachine
        if (sm != null) {
            when (reason) {
                SessionEndReason.USER_STOPPED -> sm.stop()
                SessionEndReason.INTERRUPTED -> sm.interrupt()
                else -> {}
            }
            _currentSnapshot.value = sm.getCurrentSnapshot()
        }
        sessionJob?.cancel()
        sessionJob = null
        releaseWakeLock()
    }

    fun updatePostSessionHr(hr: Double?) {
        hrAfter = hr
    }

    fun saveFeeling(feeling: String) {
        val sessionId = activeSessionId ?: return
        scope.launch(Dispatchers.IO) {
            database.breathingSessionDao().updateFeeling(sessionId, feeling)
            Log.i(TAG, "Updated breathing session $sessionId feeling to $feeling")
        }
    }

    private fun persistAndSyncSessionRecord(endedBy: SessionEndReason) {
        val sessionId = activeSessionId ?: return
        val plan = activePlan ?: return
        val completedCycles = stateMachine?.getCompletedCyclesCount() ?: 0

        val record = BreathingSessionRecord(
            sessionId = sessionId,
            startedAt = sessionStartTime,
            completedAt = System.currentTimeMillis(),
            planName = plan.name,
            targetCycles = plan.cycles,
            completedCycles = completedCycles,
            endedBy = endedBy.name,
            hrBefore = hrBefore,
            hrAfter = hrAfter,
            feeling = null
        )

        scope.launch(Dispatchers.IO) {
            try {
                // Save locally in Room
                val entity = BreathingSessionEntity.fromDomain(record, isSynced = false)
                database.breathingSessionDao().insertSession(entity)
                Log.i(TAG, "Saved BreathingSessionRecord $sessionId to Room (completedCycles=$completedCycles, endedBy=$endedBy)")

                // Send to phone via Wearable DataLayer
                syncManager.sendBreathingSession(record)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to persist/sync breathing session", e)
            }
        }
    }

    private fun releaseWakeLock() {
        try {
            if (wakeLock?.isHeld == true) {
                wakeLock?.release()
            }
            wakeLock = null
        } catch (e: Exception) {
            Log.w(TAG, "Error releasing wake lock: ${e.message}")
        }
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

    companion object {
        private const val TAG = "WearBreathingManager"

        @Volatile
        private var INSTANCE: WearBreathingSessionManager? = null

        fun getInstance(context: Context): WearBreathingSessionManager {
            return INSTANCE ?: synchronized(this) {
                val instance = WearBreathingSessionManager(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
