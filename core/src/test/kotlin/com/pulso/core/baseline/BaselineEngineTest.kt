package com.pulso.core.baseline

import com.pulso.core.model.MeasurementWindow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId

class BaselineEngineTest {

    private val utcZone = ZoneId.of("UTC")
    // 2026-04-12 00:00:00 UTC = 1776038400000L
    private val baseTimestamp = 1776038400000L

    @Test
    fun `resolveBucket assigns correct time bucket by hour of day`() {
        // Hour 03:00 UTC -> NIGHT
        val nightTs = 3L * 3600 * 1000
        assertEquals(TimeBucket.NIGHT, BaselineEngine.resolveBucket(nightTs, "PASSIVE", utcZone))

        // Hour 09:00 UTC -> MORNING
        val morningTs = 9L * 3600 * 1000
        assertEquals(TimeBucket.MORNING, BaselineEngine.resolveBucket(morningTs, "PASSIVE", utcZone))

        // Hour 14:00 UTC -> AFTERNOON
        val afternoonTs = 14L * 3600 * 1000
        assertEquals(TimeBucket.AFTERNOON, BaselineEngine.resolveBucket(afternoonTs, "PASSIVE", utcZone))

        // Hour 20:00 UTC -> EVENING
        val eveningTs = 20L * 3600 * 1000
        assertEquals(TimeBucket.EVENING, BaselineEngine.resolveBucket(eveningTs, "PASSIVE", utcZone))

        // ASLEEP activity state always maps to SLEEP regardless of time
        assertEquals(TimeBucket.SLEEP, BaselineEngine.resolveBucket(afternoonTs, "ASLEEP", utcZone))
    }

    @Test
    fun `isRestingWindow excludes non-resting, exercise, off-body, or invalid HRV`() {
        val validResting = MeasurementWindow(
            id = 1,
            windowStart = 1000L,
            windowEnd = 61000L,
            meanHr = 65.0,
            minHr = 60.0,
            maxHr = 72.0,
            rmssd = 45.0,
            validIbiCount = 40,
            ibiValidRatio = 0.95,
            activityState = "PASSIVE",
            onBody = true,
            source = "WEAR"
        )
        assertTrue(BaselineEngine.isRestingWindow(validResting))

        // Off-body window rejected
        assertFalse(BaselineEngine.isRestingWindow(validResting.copy(onBody = false)))

        // Exercise window rejected
        assertFalse(BaselineEngine.isRestingWindow(validResting.copy(activityState = "EXERCISE")))

        // Extreme HR rejected (<35 or >180)
        assertFalse(BaselineEngine.isRestingWindow(validResting.copy(meanHr = 195.0)))

        // Poor HRV validity rejected (<15 samples or <0.5 ratio)
        assertFalse(BaselineEngine.isRestingWindow(validResting.copy(validIbiCount = 8)))
        assertFalse(BaselineEngine.isRestingWindow(validResting.copy(ibiValidRatio = 0.3)))
    }

    @Test
    fun `insufficient windows remains in LEARNING state`() {
        val windows = (0 until 15).map { i ->
            MeasurementWindow(
                id = i.toLong(),
                windowStart = baseTimestamp + (i * 3600 * 1000), // spread over 15 hours
                windowEnd = baseTimestamp + (i * 3600 * 1000) + 60000,
                meanHr = 70.0,
                minHr = 65.0,
                maxHr = 75.0,
                activityState = "PASSIVE",
                onBody = true,
                source = "WEAR"
            )
        }

        val baseline = BaselineEngine.update(
            windows = windows,
            currentTimeMs = baseTimestamp + (16 * 3600 * 1000),
            requiredWindows = 30,
            requiredDays = 3,
            zoneId = utcZone
        )

        for ((_, stats) in baseline.buckets) {
            assertEquals(BaselineState.LEARNING, stats.state)
            assertTrue(stats.progressPct < 100.0)
        }
    }

    @Test
    fun `sufficient windows over 3 days transitions to READY state with valid zScores`() {
        // Generate 35 morning windows distributed across 4 consecutive days
        val windows = mutableListOf<MeasurementWindow>()
        val oneDayMs = 24L * 3600 * 1000

        var idCounter = 1L
        for (day in 0..3) {
            val dayStart = baseTimestamp + (day * oneDayMs)
            for (w in 0 until 10) {
                val windowStart = dayStart + ((7 + (w % 4)) * 3600 * 1000) // between 07:00 and 11:00 (MORNING)
                windows.add(
                    MeasurementWindow(
                        id = idCounter++,
                        windowStart = windowStart,
                        windowEnd = windowStart + 60000,
                        meanHr = 70.0 + (w % 5), // 70..74 bpm, median ~72
                        minHr = 65.0,
                        maxHr = 78.0,
                        rmssd = 40.0 + (w % 6), // 40..45 ms
                        validIbiCount = 40,
                        ibiValidRatio = 0.9,
                        activityState = "PASSIVE",
                        onBody = true,
                        source = "WEAR"
                    )
                )
            }
        }

        val currentEpoch = baseTimestamp + (4 * oneDayMs)
        val baseline = BaselineEngine.update(
            windows = windows,
            currentTimeMs = currentEpoch,
            requiredWindows = 30,
            requiredDays = 3,
            zoneId = utcZone
        )

        val morningStats = baseline.buckets[TimeBucket.MORNING]
        assertNotNull(morningStats)
        assertEquals(BaselineState.READY, morningStats!!.state)
        assertEquals(100.0, morningStats.progressPct, 0.01)
        assertNotNull(morningStats.hrMedian)
        assertNotNull(morningStats.hrMad)
        assertNotNull(morningStats.rmssdMedian)
        assertNotNull(morningStats.rmssdMad)

        // Test robust z-score computation
        val testWindow = windows.first()
        val zResult = baseline.calculateZScores(
            hr = 72.0,
            rmssd = 42.0,
            bucket = TimeBucket.MORNING
        )

        assertNotNull(zResult.hrZ)
        assertNotNull(zResult.hrvZ)
        assertTrue(kotlin.math.abs(zResult.hrZ!!) < 2.0)
    }

    @Test
    fun `Path B HR-only mode functions without HRV`() {
        val windows = mutableListOf<MeasurementWindow>()
        val oneDayMs = 24L * 3600 * 1000

        for (day in 0..3) {
            val dayStart = baseTimestamp + (day * oneDayMs)
            for (w in 0 until 10) {
                val windowStart = dayStart + (8 * 3600 * 1000)
                windows.add(
                    MeasurementWindow(
                        id = (day * 10 + w).toLong(),
                        windowStart = windowStart,
                        windowEnd = windowStart + 60000,
                        meanHr = 68.0,
                        minHr = 65.0,
                        maxHr = 72.0,
                        rmssd = null, // Path B (no HRV)
                        validIbiCount = 0,
                        ibiValidRatio = 0.0,
                        activityState = "PASSIVE",
                        onBody = true,
                        source = "HEALTH_SERVICES"
                    )
                )
            }
        }

        val baseline = BaselineEngine.update(
            windows = windows,
            currentTimeMs = baseTimestamp + (4 * oneDayMs),
            requiredWindows = 30,
            requiredDays = 3,
            zoneId = utcZone
        )

        val morningStats = baseline.buckets[TimeBucket.MORNING]!!
        assertEquals(BaselineState.READY, morningStats.state)
        assertNotNull(morningStats.hrMedian)
        assertNull(morningStats.rmssdMedian)

        val zResult = baseline.calculateZScores(
            hr = 75.0,
            rmssd = null,
            bucket = TimeBucket.MORNING
        )
        assertNotNull(zResult.hrZ)
        assertNull(zResult.hrvZ)
    }
}
