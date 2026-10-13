package com.pulso.wear.data.stress

import android.content.Context
import android.util.Log
import com.pulso.core.model.MeasurementWindow
import com.pulso.core.stress.StressDecision
import com.pulso.core.stress.StressEngine
import com.pulso.core.stress.StressEvent
import com.pulso.core.stress.SuppressionCode
import com.pulso.wear.data.baseline.WearBaselineManager
import com.pulso.wear.data.local.PulsoWearDatabase
import com.pulso.wear.data.local.StressEventEntity
import com.pulso.wear.data.sync.SyncManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.util.Calendar
import java.util.UUID

class WearStressDetector private constructor(private val context: Context) {

    private val database = PulsoWearDatabase.getInstance(context)
    private val baselineManager = WearBaselineManager.getInstance(context)
    private val syncManager by lazy { SyncManager.getInstance(context) }
    private val sharedPrefs = context.getSharedPreferences("pulso_stress_stats", Context.MODE_PRIVATE)
    private val mutex = Mutex()

    /**
     * Evaluates the latest windows and records a StressEvent if triggered.
     * OBSERVE-ONLY mode: logs and persists, without vibrating or triggering notifications yet.
     */
    suspend fun onNewWindowRecorded(newWindow: MeasurementWindow): StressDecision = withContext(Dispatchers.IO) {
        mutex.withLock {
            val configSync = syncManager.currentConfig.value
            val stressConfig = configSync?.stressConfig ?: com.pulso.core.stress.StressConfig()
            val quietHours = configSync?.quietHours

            // Retrieve recent resting / passive windows (up to last 10)
            val recentEntities = database.windowDao().getRecentWindows(10)
            val recentWindows = recentEntities.map { it.toDomain() }.reversed() // ascending order

            val baseline = baselineManager.getBaseline()

            // Count triggers today for daily cap
            val startOfDay = Calendar.getInstance().apply {
                set(Calendar.HOUR_OF_DAY, 0)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
            val triggersToday = database.stressEventDao().countTriggersToday(startOfDay)
            val lastTriggerTime = database.stressEventDao().getLastTriggerTime()

            val now = System.currentTimeMillis()
            val decision = StressEngine.evaluate(
                recentWindows = recentWindows,
                baseline = baseline,
                config = stressConfig,
                quietHours = quietHours,
                lastTriggeredAt = lastTriggerTime,
                triggersTodayCount = triggersToday,
                now = now
            )

            // Track suppressions counters in SharedPreferences for local debugging
            for (suppression in decision.suppressedBy) {
                recordSuppressionCount(suppression.code)
            }

            if (decision.triggered) {
                val event = StressEvent(
                    id = UUID.randomUUID().toString(),
                    time = now,
                    confidence = decision.confidence,
                    reasons = decision.reasons,
                    windowsUsed = recentWindows.takeLast(stressConfig.consecutiveWindowsRuleA).map { it.windowStart },
                    configVersion = stressConfig.version
                )
                database.stressEventDao().insertEvent(StressEventEntity.fromDomain(event, isSynced = false))
                Log.w(
                    TAG,
                    "[OBSERVE-ONLY] Stress detected! EventId=${event.id}, confidence=${event.confidence}, reasons=${event.reasons.size}"
                )
                // Schedule or trigger event sync
                syncManager.syncStressEventsNow()
            } else {
                Log.d(TAG, "StressEngine evaluated: triggered=false, suppressions=${decision.suppressedBy.map { it.code }}")
            }

            decision
        }
    }

    private fun recordSuppressionCount(code: SuppressionCode) {
        val key = "suppression_${code.name}"
        val current = sharedPrefs.getInt(key, 0)
        sharedPrefs.edit().putInt(key, current + 1).apply()
    }

    fun getSuppressionCounts(): Map<String, Int> {
        val all = sharedPrefs.all
        val counts = mutableMapOf<String, Int>()
        for ((k, v) in all) {
            if (k.startsWith("suppression_") && v is Int) {
                counts[k.removePrefix("suppression_")] = v
            }
        }
        return counts
    }

    /**
     * DEBUG-only helper to exercise the pipeline end-to-end without polluting real history.
     */
    suspend fun simulateStressDebug(): StressEvent = withContext(Dispatchers.IO) {
        val now = System.currentTimeMillis()
        val event = StressEvent(
            id = "sim-${UUID.randomUUID()}",
            time = now,
            confidence = 0.88,
            reasons = listOf(
                com.pulso.core.stress.StressReason(
                    code = com.pulso.core.stress.StressReasonCode.ELEVATED_HR_LOW_HRV,
                    description = "Simulación DEBUG: Pulso +22% sobre la línea base y HRV deprimido",
                    hrZ = 1.8,
                    hrvZ = -1.6
                )
            ),
            windowsUsed = listOf(now - 120000, now - 60000, now),
            configVersion = 1
        )
        database.stressEventDao().insertEvent(StressEventEntity.fromDomain(event, isSynced = false))
        syncManager.syncStressEventsNow()
        event
    }

    companion object {
        private const val TAG = "WearStressDetector"

        @Volatile
        private var INSTANCE: WearStressDetector? = null

        fun getInstance(context: Context): WearStressDetector {
            return INSTANCE ?: synchronized(this) {
                val instance = WearStressDetector(context.applicationContext)
                INSTANCE = instance
                instance
            }
        }
    }
}
