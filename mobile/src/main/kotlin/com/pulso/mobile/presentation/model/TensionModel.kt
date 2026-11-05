package com.pulso.mobile.presentation.model

import com.pulso.core.baseline.Baseline
import com.pulso.core.baseline.BaselineEngine
import com.pulso.core.baseline.BaselineState
import com.pulso.core.baseline.BucketStats
import com.pulso.core.baseline.TimeBucket
import com.pulso.core.model.MeasurementWindow

/** Tension scale from the design system: always shown with text and icon, never color alone. */
enum class TensionLevel { CALM, ALERT, TENSION, LEARNING, NO_DATA }

data class Range(val low: Double, val high: Double)

private const val MAD_SCALE = 1.4826

fun BucketStats.hrRange(): Range? {
    if (state != BaselineState.READY) return null
    val median = hrMedian ?: return null
    val mad = (hrMad ?: return null).coerceAtLeast(1.0)
    return Range(median - MAD_SCALE * mad, median + MAD_SCALE * mad)
}

fun BucketStats.hrvRange(): Range? {
    if (state != BaselineState.READY) return null
    val median = rmssdMedian ?: return null
    val mad = (rmssdMad ?: return null).coerceAtLeast(1.0)
    return Range(median - MAD_SCALE * mad, median + MAD_SCALE * mad)
}

fun Baseline?.currentBucketStats(now: Long, activityState: String?): BucketStats? {
    if (this == null) return null
    val bucket = BaselineEngine.resolveBucket(timestamp = now, activityState = activityState ?: "PASSIVE")
    return buckets[bucket]
}

/**
 * Classifies the latest reading against the user's own baseline.
 * A detected stress event in the last 30 minutes always wins.
 */
fun computeTensionLevel(
    latest: MeasurementWindow?,
    baseline: Baseline?,
    lastEventTime: Long?,
    now: Long = System.currentTimeMillis()
): TensionLevel {
    if (lastEventTime != null && now - lastEventTime < 30 * 60_000L) return TensionLevel.TENSION
    if (latest == null) return TensionLevel.NO_DATA
    val stats = baseline.currentBucketStats(now, latest.activityState) ?: return TensionLevel.LEARNING
    if (stats.state != BaselineState.READY || stats.hrMedian == null || stats.hrMad == null) return TensionLevel.LEARNING
    val z = (latest.meanHr - stats.hrMedian!!) / (MAD_SCALE * stats.hrMad!!.coerceAtLeast(1.0))
    return when {
        z >= 2.0 -> TensionLevel.TENSION
        z >= 1.0 -> TensionLevel.ALERT
        else -> TensionLevel.CALM
    }
}

/** Overall baseline learning progress across the four day buckets (0..1). */
fun Baseline?.learningProgress(): Float {
    if (this == null || buckets.isEmpty()) return 0f
    val relevant = DAY_SLOTS.mapNotNull { slotStats(it) }
    if (relevant.isEmpty()) return 0f
    return (relevant.map { if (it.state == BaselineState.READY) 1.0 else (it.progressPct / 100.0).coerceIn(0.0, 1.0) }.average()).toFloat()
}

/** The four day slots shown to the user (00–06, 06–12, 12–18, 18–24). */
val DAY_SLOTS = listOf(TimeBucket.NIGHT, TimeBucket.MORNING, TimeBucket.AFTERNOON, TimeBucket.EVENING)

/**
 * Stats for a day slot. Readings taken while asleep are learned in the SLEEP bucket, so the
 * overnight slot uses whichever of NIGHT / SLEEP has more readings.
 */
fun Baseline.slotStats(slot: TimeBucket): BucketStats? {
    if (slot != TimeBucket.NIGHT) return buckets[slot]
    val night = buckets[TimeBucket.NIGHT]
    val sleep = buckets[TimeBucket.SLEEP]
    return listOfNotNull(night, sleep).maxByOrNull { (if (it.state == BaselineState.READY) 1_000_000 else 0) + it.windowCount }
}

/** Maps the engine bucket of "now" to the visible day slot. */
fun TimeBucket.toDaySlot(): TimeBucket = if (this == TimeBucket.SLEEP) TimeBucket.NIGHT else this
