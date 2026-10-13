package com.pulso.core.stress

import com.pulso.core.baseline.Baseline
import com.pulso.core.baseline.BaselineState
import com.pulso.core.baseline.BucketStats
import com.pulso.core.baseline.TimeBucket
import com.pulso.core.model.MeasurementWindow
import com.pulso.core.model.QuietHours
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class StressEngineTest {

    private val utcZone = ZoneId.of("UTC")
    // 2026-04-12 10:00:00 UTC (TimeBucket.MORNING)
    private val baseTimestamp = 1776074400000L

    private val readyMorningBaseline: Baseline
        get() {
            val stats = mapOf(
                TimeBucket.MORNING to BucketStats(
                    state = BaselineState.READY,
                    windowCount = 40,
                    requiredWindows = 30,
                    daysCollected = 5,
                    requiredDays = 3,
                    progressPct = 100.0,
                    hrMedian = 70.0,
                    hrMad = 3.0,
                    rmssdMedian = 50.0,
                    rmssdMad = 4.0,
                    updatedAt = baseTimestamp
                )
            )
            return Baseline(buckets = stats, updatedAt = baseTimestamp)
        }

    private fun createRestingWindow(
        offsetMinutes: Int,
        hr: Double = 70.0,
        rmssd: Double? = 50.0,
        activity: String = "PASSIVE",
        onBody: Boolean = true,
        ibiRatio: Double = 0.9
    ): MeasurementWindow {
        val start = baseTimestamp + (offsetMinutes * 60 * 1000L)
        return MeasurementWindow(
            id = offsetMinutes.toLong(),
            windowStart = start,
            windowEnd = start + 60000L,
            meanHr = hr,
            minHr = hr - 5,
            maxHr = hr + 5,
            rmssd = rmssd,
            validIbiCount = 35,
            ibiValidRatio = ibiRatio,
            activityState = activity,
            onBody = onBody,
            source = "WEAR"
        )
    }

    @Test
    fun `Rule A triggers when 3 consecutive windows have elevated HR and depressed HRV`() {
        // hrMedian = 70, hrMad = 3 -> 1.4826*3 = 4.45. hr=76 -> hrZ = (76-70)/4.45 = 1.35 >= 1.0
        // rmssdMedian = 50, rmssdMad = 4 -> 1.4826*4 = 5.93. rmssd=42 -> hrvZ = (42-50)/5.93 = -1.35 <= -1.0
        val windows = listOf(
            createRestingWindow(0, hr = 76.0, rmssd = 42.0),
            createRestingWindow(5, hr = 77.0, rmssd = 41.0),
            createRestingWindow(10, hr = 78.0, rmssd = 40.0)
        )

        val decision = StressEngine.evaluate(
            recentWindows = windows,
            baseline = readyMorningBaseline,
            config = StressConfig(),
            now = baseTimestamp + (10 * 60 * 1000L),
            zoneId = utcZone
        )

        assertTrue("Decision should trigger on Rule A match", decision.triggered)
        assertTrue("Confidence should be >= 0.5", decision.confidence >= 0.5)
        assertEquals(2, decision.reasons.size)
        assertEquals(StressReasonCode.ELEVATED_HR_LOW_HRV, decision.reasons[0].code)
        assertTrue(decision.suppressedBy.isEmpty())
    }

    @Test
    fun `Rule B triggers on path B with 4 consecutive windows of high HR without HRV`() {
        // hrOnlyZThreshold = 1.5 -> (hr - 70) / 4.45 >= 1.5 => hr >= 76.7
        val windows = listOf(
            createRestingWindow(0, hr = 78.0, rmssd = null),
            createRestingWindow(5, hr = 79.0, rmssd = null),
            createRestingWindow(10, hr = 78.5, rmssd = null),
            createRestingWindow(15, hr = 80.0, rmssd = null)
        )

        val decision = StressEngine.evaluate(
            recentWindows = windows,
            baseline = readyMorningBaseline,
            config = StressConfig(),
            now = baseTimestamp + (15 * 60 * 1000L),
            zoneId = utcZone
        )

        assertTrue("Rule B should trigger for HR-only path", decision.triggered)
        assertEquals(StressReasonCode.ELEVATED_HR_ONLY, decision.reasons[0].code)
    }

    @Test
    fun `Exercise and post-exercise grace gates suppress triggers`() {
        val stressedWindows = listOf(
            createRestingWindow(0, hr = 80.0, rmssd = 35.0),
            createRestingWindow(5, hr = 80.0, rmssd = 35.0),
            createRestingWindow(10, hr = 80.0, rmssd = 35.0)
        )

        // Exercising active
        val exercisingWindows = stressedWindows.dropLast(1) + createRestingWindow(10, hr = 80.0, rmssd = 35.0, activity = "EXERCISING")
        val exerciseDecision = StressEngine.evaluate(
            recentWindows = exercisingWindows,
            baseline = readyMorningBaseline,
            now = baseTimestamp + (10 * 60 * 1000L),
            zoneId = utcZone
        )
        assertFalse(exerciseDecision.triggered)
        assertTrue(exerciseDecision.suppressedBy.any { it.code == SuppressionCode.EXERCISE_ACTIVE })

        // Post exercise within grace period (15 min ago)
        val graceDecision = StressEngine.evaluate(
            recentWindows = stressedWindows,
            baseline = readyMorningBaseline,
            lastExerciseEndAt = baseTimestamp - (10 * 60 * 1000L), // 10 min ago (< 20 min grace)
            now = baseTimestamp + (10 * 60 * 1000L),
            zoneId = utcZone
        )
        assertFalse(graceDecision.triggered)
        assertTrue(graceDecision.suppressedBy.any { it.code == SuppressionCode.RECENT_EXERCISE_GRACE })
    }

    @Test
    fun `Cooldown and daily cap gates suppress triggers`() {
        val stressedWindows = listOf(
            createRestingWindow(0, hr = 80.0, rmssd = 35.0),
            createRestingWindow(5, hr = 80.0, rmssd = 35.0),
            createRestingWindow(10, hr = 80.0, rmssd = 35.0)
        )

        // Cooldown active (last trigger 20 min ago, cooldown 60 min)
        val cooldownDecision = StressEngine.evaluate(
            recentWindows = stressedWindows,
            baseline = readyMorningBaseline,
            lastTriggeredAt = baseTimestamp - (10 * 60 * 1000L),
            now = baseTimestamp + (10 * 60 * 1000L),
            zoneId = utcZone
        )
        assertFalse(cooldownDecision.triggered)
        assertTrue(cooldownDecision.suppressedBy.any { it.code == SuppressionCode.COOLDOWN_ACTIVE })

        // Daily cap reached
        val capDecision = StressEngine.evaluate(
            recentWindows = stressedWindows,
            baseline = readyMorningBaseline,
            triggersTodayCount = 4, // cap is 4
            now = baseTimestamp + (10 * 60 * 1000L),
            zoneId = utcZone
        )
        assertFalse(capDecision.triggered)
        assertTrue(capDecision.suppressedBy.any { it.code == SuppressionCode.DAILY_CAP_REACHED })
    }

    @Test
    fun `Quiet hours and user snooze suppress triggers`() {
        val stressedWindows = listOf(
            createRestingWindow(0, hr = 80.0, rmssd = 35.0),
            createRestingWindow(5, hr = 80.0, rmssd = 35.0),
            createRestingWindow(10, hr = 80.0, rmssd = 35.0)
        )

        // Quiet hours 09:00 - 11:00 UTC (baseTimestamp is 10:00 UTC)
        val quiet = QuietHours(startHour = 9, startMinute = 0, endHour = 11, endMinute = 0)
        val quietDecision = StressEngine.evaluate(
            recentWindows = stressedWindows,
            baseline = readyMorningBaseline,
            quietHours = quiet,
            now = baseTimestamp + (10 * 60 * 1000L),
            zoneId = utcZone
        )
        assertFalse(quietDecision.triggered)
        assertTrue(quietDecision.suppressedBy.any { it.code == SuppressionCode.QUIET_HOURS })

        // User snooze active
        val snoozeDecision = StressEngine.evaluate(
            recentWindows = stressedWindows,
            baseline = readyMorningBaseline,
            config = StressConfig(snoozeUntil = baseTimestamp + (3600 * 1000L)),
            now = baseTimestamp + (10 * 60 * 1000L),
            zoneId = utcZone
        )
        assertFalse(snoozeDecision.triggered)
        assertTrue(snoozeDecision.suppressedBy.any { it.code == SuppressionCode.USER_SNOOZE })
    }
}
