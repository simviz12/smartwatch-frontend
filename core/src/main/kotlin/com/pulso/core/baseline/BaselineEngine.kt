package com.pulso.core.baseline

import com.pulso.core.model.MeasurementWindow
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime
import kotlin.math.abs
import kotlin.math.exp

object BaselineEngine {

    const val DEFAULT_REQUIRED_WINDOWS = 30
    const val DEFAULT_REQUIRED_DAYS = 3
    const val ROLLING_WINDOW_DAYS = 14
    private const val HALF_LIFE_DAYS = 5.0 // exponential weight half-life

    /**
     * Identifies the applicable TimeBucket for a given timestamp and activity state.
     */
    fun resolveBucket(
        timestamp: Long,
        activityState: String,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): TimeBucket {
        if (activityState.equals("ASLEEP", ignoreCase = true)) {
            return TimeBucket.SLEEP
        }

        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId)
        val hour = zdt.hour

        return when (hour) {
            in 0..5 -> TimeBucket.NIGHT
            in 6..11 -> TimeBucket.MORNING
            in 12..17 -> TimeBucket.AFTERNOON
            else -> TimeBucket.EVENING
        }
    }

    /**
     * Determines whether a MeasurementWindow qualifies as a valid resting observation.
     * Rule:
     * - onBody == true
     * - activityState == "PASSIVE" or "ASLEEP" (never EXERCISE or HIGH_MOTION)
     * - meanHr between 35 and 180 bpm
     * - If HRV is present, validIbiCount >= 15 and ibiValidRatio >= 0.5
     */
    fun isRestingWindow(window: MeasurementWindow): Boolean {
        if (!window.onBody) return false

        val isPassiveOrSleep = window.activityState.equals("PASSIVE", ignoreCase = true) ||
                window.activityState.equals("ASLEEP", ignoreCase = true)
        if (!isPassiveOrSleep) return false

        if (window.meanHr < 35.0 || window.meanHr > 180.0) return false

        if (window.rmssd != null) {
            if (window.validIbiCount < 15 || window.ibiValidRatio < 0.5) {
                return false
            }
        }

        return true
    }

    /**
     * Updates the Baseline using a rolling 14-day history of windows.
     */
    fun update(
        windows: List<MeasurementWindow>,
        currentTimeMs: Long = System.currentTimeMillis(),
        requiredWindows: Int = DEFAULT_REQUIRED_WINDOWS,
        requiredDays: Int = DEFAULT_REQUIRED_DAYS,
        zoneId: ZoneId = ZoneId.systemDefault()
    ): Baseline {
        val cutoffMs = currentTimeMs - (ROLLING_WINDOW_DAYS.toLong() * 24 * 60 * 60 * 1000)

        // Filter valid resting windows within the rolling window
        val validWindows = windows.filter { w ->
            w.windowStart in cutoffMs..currentTimeMs && isRestingWindow(w)
        }

        // Group by bucket
        val grouped = validWindows.groupBy { w ->
            resolveBucket(w.windowStart, w.activityState, zoneId)
        }

        val bucketsMap = mutableMapOf<TimeBucket, BucketStats>()

        for (bucket in TimeBucket.values()) {
            val bucketWindows = grouped[bucket] ?: emptyList()

            // Count unique days
            val uniqueDays = bucketWindows.map { w ->
                val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(w.windowStart), zoneId)
                "${zdt.year}-${zdt.dayOfYear}"
            }.distinct().size

            val windowCount = bucketWindows.size
            val countProgress = (windowCount.toDouble() / requiredWindows).coerceAtMost(1.0)
            val daysProgress = (uniqueDays.toDouble() / requiredDays).coerceAtMost(1.0)
            val progressPct = ((countProgress * 0.6) + (daysProgress * 0.4)) * 100.0

            val isReady = windowCount >= requiredWindows && uniqueDays >= requiredDays

            if (!isReady || bucketWindows.isEmpty()) {
                bucketsMap[bucket] = BucketStats(
                    state = BaselineState.LEARNING,
                    windowCount = windowCount,
                    requiredWindows = requiredWindows,
                    daysCollected = uniqueDays,
                    requiredDays = requiredDays,
                    progressPct = progressPct,
                    updatedAt = currentTimeMs
                )
            } else {
                // Compute robust median and MAD with exponential recency weighting
                val hrValuesWithWeights = bucketWindows.map { w ->
                    val ageDays = (currentTimeMs - w.windowStart).toDouble() / (24.0 * 60 * 60 * 1000)
                    val weight = exp(-ageDays / HALF_LIFE_DAYS)
                    Pair(w.meanHr, weight)
                }

                val hrMedian = computeWeightedMedian(hrValuesWithWeights)
                val hrMad = computeWeightedMad(hrValuesWithWeights, hrMedian)

                val rmssdWindows = bucketWindows.filter { it.rmssd != null }
                val (rmssdMedian, rmssdMad) = if (rmssdWindows.size >= 10) {
                    val rmssdValuesWithWeights = rmssdWindows.map { w ->
                        val ageDays = (currentTimeMs - w.windowStart).toDouble() / (24.0 * 60 * 60 * 1000)
                        val weight = exp(-ageDays / HALF_LIFE_DAYS)
                        Pair(w.rmssd!!, weight)
                    }
                    val median = computeWeightedMedian(rmssdValuesWithWeights)
                    val mad = computeWeightedMad(rmssdValuesWithWeights, median)
                    Pair(median, mad)
                } else {
                    Pair(null, null)
                }

                bucketsMap[bucket] = BucketStats(
                    state = BaselineState.READY,
                    windowCount = windowCount,
                    requiredWindows = requiredWindows,
                    daysCollected = uniqueDays,
                    requiredDays = requiredDays,
                    progressPct = 100.0,
                    hrMedian = hrMedian,
                    hrMad = hrMad,
                    rmssdMedian = rmssdMedian,
                    rmssdMad = rmssdMad,
                    updatedAt = currentTimeMs
                )
            }
        }

        return Baseline(
            version = 1,
            buckets = bucketsMap,
            updatedAt = currentTimeMs
        )
    }

    /**
     * Computes the weighted median of (value, weight) pairs.
     */
    fun computeWeightedMedian(samples: List<Pair<Double, Double>>): Double {
        if (samples.isEmpty()) return 0.0
        val sorted = samples.sortedBy { it.first }
        val totalWeight = sorted.sumOf { it.second }
        val halfWeight = totalWeight / 2.0

        var cumulative = 0.0
        for ((value, weight) in sorted) {
            cumulative += weight
            if (cumulative >= halfWeight) {
                return value
            }
        }
        return sorted.last().first
    }

    /**
     * Computes Median Absolute Deviation (MAD): median(|x_i - median|).
     */
    fun computeWeightedMad(samples: List<Pair<Double, Double>>, median: Double): Double {
        if (samples.isEmpty()) return 0.0
        val deviations = samples.map { Pair(abs(it.first - median), it.second) }
        return computeWeightedMedian(deviations)
    }
}
