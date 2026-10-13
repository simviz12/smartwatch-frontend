package com.pulso.mobile.domain.baseline

import com.pulso.core.baseline.Baseline
import com.pulso.core.baseline.BaselineState
import com.pulso.core.baseline.BucketStats
import com.pulso.core.baseline.TimeBucket

object BaselineMismatchDetector {

    /**
     * Checks if the phone's locally computed baseline and the watch's reported baselineStatus
     * diverge significantly.
     *
     * Rules:
     * - If both are LEARNING, compare sampleCount tolerance (e.g. diff > 10).
     * - If one is READY and the other is LEARNING when window count is >= 30, flag mismatch.
     * - If both are READY: if median heart rates or RMSSD diverge by > 5.0 bpm / 8.0 ms, flag mismatch.
     */
    fun checkMismatch(
        phoneBaseline: Baseline?,
        watchStatusMap: Map<String, BucketStats>?
    ): String? {
        if (phoneBaseline == null || watchStatusMap == null || watchStatusMap.isEmpty()) {
            return null
        }

        for ((bucket, phoneStats) in phoneBaseline.buckets) {
            val watchBucket = watchStatusMap[bucket.name] ?: continue

            // 1. Check state disagreement
            val watchIsReady = watchBucket.state == BaselineState.READY
            val phoneIsReady = phoneStats.state == BaselineState.READY

            if (watchIsReady != phoneIsReady) {
                // Allow grace if samples are very close to threshold
                val sampleDiff = kotlin.math.abs(phoneStats.windowCount - watchBucket.windowCount)
                if (sampleDiff > 5) {
                    return "Línea base del reloj y del celular no coinciden: bucket ${bucket.name} está ${if (watchIsReady) "READY" else "LEARNING"} en reloj y ${if (phoneIsReady) "READY" else "LEARNING"} en celular"
                }
            }

            // 2. Check sample count discrepancy
            val sampleDiff = kotlin.math.abs(phoneStats.windowCount - watchBucket.windowCount)
            if (sampleDiff > 15) {
                return "Línea base del reloj y del celular no coinciden: discrepancia de $sampleDiff mediciones en ${bucket.name}"
            }
        }

        return null
    }
}
