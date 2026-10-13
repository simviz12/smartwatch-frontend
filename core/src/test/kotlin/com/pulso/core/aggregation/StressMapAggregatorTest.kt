package com.pulso.core.aggregation

import com.pulso.core.model.MeasurementWindow
import com.pulso.core.stress.StressEvent
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.ZoneId
import java.time.ZonedDateTime

class StressMapAggregatorTest {

    private val zoneId = ZoneId.of("America/Bogota")

    @Test
    fun testEmptyDataReturnsInsufficientDataReport() {
        val report = StressMapAggregator.aggregate(
            windows = emptyList(),
            stressEvents = emptyList(),
            zoneId = zoneId
        )

        assertFalse(report.hasSufficientData)
        assertEquals(0, report.totalWindowsAnalyzed)
        assertEquals(0, report.totalEventsAnalyzed)
        assertTrue(report.heatmap.isEmpty())
        assertTrue(report.neutralInsight.contains("Aún no hay suficientes datos"))
    }

    @Test
    fun testLessThanMinWindowsReturnsInsufficientData() {
        val baseTime = ZonedDateTime.of(2026, 10, 12, 10, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        val windows = (0 until 10).map { i ->
            MeasurementWindow(
                id = i.toLong(),
                windowStart = baseTime + (i * 300_000L),
                windowEnd = baseTime + ((i + 1) * 300_000L),
                meanHr = 75.0,
                minHr = 70.0,
                maxHr = 80.0,
                source = "HEALTH_SERVICES"
            )
        }

        val report = StressMapAggregator.aggregate(
            windows = windows,
            stressEvents = emptyList(),
            zoneId = zoneId
        )

        assertFalse(report.hasSufficientData)
    }

    @Test
    fun testSufficientDataBuilds24x7HeatmapAndNeutralInsight() {
        // Monday 10:00 AM (2026-10-12 was a Monday)
        val mondayStart = ZonedDateTime.of(2026, 10, 12, 10, 0, 0, 0, zoneId).toInstant().toEpochMilli()
        
        // Create 30 windows
        val windows = (0 until 30).map { i ->
            MeasurementWindow(
                id = i.toLong(),
                windowStart = mondayStart + (i * 300_000L),
                windowEnd = mondayStart + ((i + 1) * 300_000L),
                meanHr = 75.0,
                minHr = 70.0,
                maxHr = 80.0,
                source = "HEALTH_SERVICES"
            )
        }

        // Create 3 stress events at Monday 10:15
        val stressEvents = listOf(
            StressEvent("ev-1", mondayStart + 900_000L, 0.9, emptyList(), emptyList(), 1),
            StressEvent("ev-2", mondayStart + 1200_000L, 0.85, emptyList(), emptyList(), 1),
            StressEvent("ev-3", mondayStart + 1500_000L, 0.88, emptyList(), emptyList(), 1)
        )

        val report = StressMapAggregator.aggregate(
            windows = windows,
            stressEvents = stressEvents,
            breathingFeelings = listOf("BETTER", "BETTER", "SAME"),
            breathingDurations = listOf(180, 240, 180),
            zoneId = zoneId
        )

        assertTrue(report.hasSufficientData)
        assertEquals(30, report.totalWindowsAnalyzed)
        assertEquals(3, report.totalEventsAnalyzed)
        assertEquals(7 * 24, report.heatmap.size) // 168 cells
        assertEquals(24, report.hourlyLoads.size)

        // Check Monday (day 1) hour 10 cell
        val monday10Cell = report.heatmap.first { it.dayOfWeek == 1 && it.hourOfDay == 10 }
        assertTrue(monday10Cell.stressEventCount >= 3)
        assertTrue(monday10Cell.stressLoadRatio > 0.0)

        assertNotNull(report.weeklyTrend)
        assertTrue(report.weeklyTrend!!.topStressSlots.isNotEmpty())
        assertEquals(1, report.weeklyTrend!!.topStressSlots.first().dayOfWeek)

        // Neutral insight wording
        assertFalse(report.neutralInsight.contains("ansiedad", ignoreCase = true))
        assertFalse(report.neutralInsight.contains("trastorno", ignoreCase = true))
        assertTrue(report.neutralInsight.contains("activación", ignoreCase = true) || report.neutralInsight.contains("lunes", ignoreCase = true))

        // Breathing effectiveness
        assertNotNull(report.breathingEffectiveness)
        assertEquals(3, report.breathingEffectiveness!!.totalSessions)
        assertEquals(2, report.breathingEffectiveness!!.feltBetterCount)
        assertEquals(66.6, report.breathingEffectiveness!!.feltBetterPercentage, 1.0)
    }

    @Test
    fun testDSTAndBoundaryHandling() {
        // Test cross-midnight boundary
        val sundayNight = ZonedDateTime.of(2026, 10, 18, 23, 45, 0, 0, zoneId).toInstant().toEpochMilli()
        val mondayMorning = ZonedDateTime.of(2026, 10, 19, 0, 15, 0, 0, zoneId).toInstant().toEpochMilli()

        val windows = (0 until 30).map { i ->
            MeasurementWindow(
                id = i.toLong(),
                windowStart = sundayNight + (i * 300_000L),
                windowEnd = sundayNight + ((i + 1) * 300_000L),
                meanHr = 70.0,
                minHr = 65.0,
                maxHr = 75.0,
                source = "HEALTH_SERVICES"
            )
        }

        val events = listOf(
            StressEvent("ev-sun", sundayNight, 0.9, emptyList(), emptyList(), 1),
            StressEvent("ev-mon", mondayMorning, 0.9, emptyList(), emptyList(), 1)
        )

        val report = StressMapAggregator.aggregate(windows, events, zoneId = zoneId)
        assertTrue(report.hasSufficientData)

        val sunCell = report.heatmap.first { it.dayOfWeek == 7 && it.hourOfDay == 23 }
        val monCell = report.heatmap.first { it.dayOfWeek == 1 && it.hourOfDay == 0 }

        assertTrue(sunCell.stressEventCount >= 1)
        assertTrue(monCell.stressEventCount >= 1)
    }
}
