package com.pulso.mobile.domain.stress

import com.pulso.core.baseline.Baseline
import com.pulso.core.baseline.BaselineState
import com.pulso.core.baseline.BucketStats
import com.pulso.core.baseline.TimeBucket
import com.pulso.core.model.MeasurementWindow
import com.pulso.core.stress.StressConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ReplayLabRunnerTest {

    private fun createReadyBaseline(): Baseline {
        val readyStat = BucketStats(
            state = BaselineState.READY,
            windowCount = 40,
            requiredWindows = 30,
            daysCollected = 7,
            requiredDays = 3,
            progressPct = 1.0,
            hrMedian = 68.0,
            hrMad = 4.0,
            rmssdMedian = 55.0,
            rmssdMad = 6.0,
            updatedAt = System.currentTimeMillis()
        )
        val buckets = TimeBucket.entries.associateWith { readyStat }
        return Baseline(buckets = buckets, updatedAt = System.currentTimeMillis())
    }

    private fun createSampleWindows(count: Int, highStress: Boolean): List<MeasurementWindow> {
        val list = mutableListOf<MeasurementWindow>()
        val baseStart = 1718028000000L // An afternoon time: ~14:00
        for (i in 0 until count) {
            val start = baseStart + (i * 300_000L)
            list.add(
                MeasurementWindow(
                    windowStart = start,
                    windowEnd = start + 300_000L,
                    meanHr = if (highStress) 88.0 else 69.0, // High stress is +20 BPM above median 68
                    minHr = 65.0,
                    maxHr = 95.0,
                    rmssd = if (highStress) 28.0 else 54.0, // High stress is drop in HRV below median 55
                    validIbiCount = 30,
                    ibiValidRatio = 0.95,
                    activityState = "PASSIVE",
                    onBody = true,
                    source = "watch-test"
                )
            )
        }
        return list
    }

    @Test
    fun testReplayDetectsStressWhenPatternPresent() {
        val baseline = createReadyBaseline()
        val config = StressConfig(
            hrZThreshold = 1.0,
            hrvZThreshold = -1.0,
            consecutiveWindowsRuleA = 3,
            cooldownMinutes = 60
        )
        val windows = createSampleWindows(count = 5, highStress = true)

        val result = ReplayLabRunner.runReplay(
            windows = windows,
            baseline = baseline,
            config = config
        )

        assertEquals(5, result.totalWindowsAnalyzed)
        assertTrue("Expected at least 1 trigger in high stress windows", result.totalTriggers >= 1)
        assertEquals(5, result.timeline.size)
    }

    @Test
    fun testReplayNoTriggersInNormalRestingWindows() {
        val baseline = createReadyBaseline()
        val config = StressConfig()
        val windows = createSampleWindows(count = 6, highStress = false)

        val result = ReplayLabRunner.runReplay(
            windows = windows,
            baseline = baseline,
            config = config
        )

        assertEquals(6, result.totalWindowsAnalyzed)
        assertEquals(0, result.totalTriggers)
    }
}
