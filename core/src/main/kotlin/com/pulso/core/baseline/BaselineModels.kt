package com.pulso.core.baseline

import kotlinx.serialization.Serializable

enum class TimeBucket {
    NIGHT,     // 00:00 - 06:00
    MORNING,   // 06:00 - 12:00
    AFTERNOON, // 12:00 - 18:00
    EVENING,   // 18:00 - 24:00
    SLEEP      // Window specifically flagged as ASLEEP
}

enum class BaselineState {
    LEARNING,
    READY
}

@Serializable
data class BucketStats(
    val state: BaselineState,
    val windowCount: Int,
    val requiredWindows: Int,
    val daysCollected: Int,
    val requiredDays: Int,
    val progressPct: Double,
    val hrMedian: Double? = null,
    val hrMad: Double? = null,
    val rmssdMedian: Double? = null,
    val rmssdMad: Double? = null,
    val updatedAt: Long
)

@Serializable
data class Baseline(
    val version: Int = 1,
    val buckets: Map<TimeBucket, BucketStats>,
    val updatedAt: Long
) {
    /**
     * Computes robust z-scores:
     * z = (x - median) / (1.4826 * MAD)
     * Uses a minimum MAD floor (default 1.0) to avoid division by zero or excessive magnification.
     */
    fun calculateZScores(
        hr: Double,
        rmssd: Double?,
        bucket: TimeBucket,
        madFloor: Double = 1.0
    ): ZScoreResult {
        val stats = buckets[bucket]
        if (stats == null || stats.state != BaselineState.READY || stats.hrMedian == null || stats.hrMad == null) {
            return ZScoreResult(hrZ = null, hrvZ = null, status = stats ?: defaultLearningStats(bucket))
        }

        val effectiveHrMad = stats.hrMad.coerceAtLeast(madFloor)
        val hrZ = (hr - stats.hrMedian) / (1.4826 * effectiveHrMad)

        val hrvZ = if (rmssd != null && stats.rmssdMedian != null && stats.rmssdMad != null) {
            val effectiveHrvMad = stats.rmssdMad.coerceAtLeast(madFloor)
            (rmssd - stats.rmssdMedian) / (1.4826 * effectiveHrvMad)
        } else {
            null
        }

        return ZScoreResult(hrZ = hrZ, hrvZ = hrvZ, status = stats)
    }

    companion object {
        fun defaultLearningStats(bucket: TimeBucket): BucketStats = BucketStats(
            state = BaselineState.LEARNING,
            windowCount = 0,
            requiredWindows = 30,
            daysCollected = 0,
            requiredDays = 3,
            progressPct = 0.0,
            updatedAt = System.currentTimeMillis()
        )
    }
}

@Serializable
data class ZScoreResult(
    val hrZ: Double?,
    val hrvZ: Double?,
    val status: BucketStats
)
