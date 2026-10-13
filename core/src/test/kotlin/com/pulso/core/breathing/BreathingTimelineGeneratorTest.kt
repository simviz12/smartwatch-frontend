package com.pulso.core.breathing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class BreathingTimelineGeneratorTest {

    @Test
    fun generateTimeline_calma46_continuityAndTiming() {
        val plan = BreathingPlan.calma46(cycles = 3)
        val timeline = BreathingTimelineGenerator.generateTimeline(plan, HapticIntensity.MEDIUM)

        assertTrue("Timeline must contain segments", timeline.isNotEmpty())

        // Verify segment order and continuity: no gaps, no overlaps
        var expectedStart = 0L
        for (i in timeline.indices) {
            val seg = timeline[i]
            assertEquals("Segment $i start time mismatch", expectedStart, seg.startTimeMs)
            assertTrue("Segment duration must be positive", seg.durationMs > 0)
            expectedStart += seg.durationMs
        }

        // Verify start tap
        assertEquals(TimelineSegmentType.START_TAP, timeline.first().type)

        // Verify end tap
        assertEquals(TimelineSegmentType.END_DOUBLE_TAP, timeline.last().type)

        // Cycles breakdown: For Calma 4-6 (no hold): START, PAUSE, (INHALE, EXHALE) * 3, END = 1 + 1 + 6 + 1 = 9 segments
        assertEquals(9, timeline.size)

        val inhaleSegments = timeline.filter { it.type == TimelineSegmentType.INHALE }
        assertEquals(3, inhaleSegments.size)
        inhaleSegments.forEach { seg ->
            assertEquals(4000L, seg.durationMs)
            // Check that amplitudes are rising
            assertTrue("First amplitude should be less than last amplitude for inhale",
                seg.amplitudes.first() < seg.amplitudes.last())
        }

        val exhaleSegments = timeline.filter { it.type == TimelineSegmentType.EXHALE }
        assertEquals(3, exhaleSegments.size)
        exhaleSegments.forEach { seg ->
            assertEquals(6000L, seg.durationMs)
            // Check that amplitudes are falling
            assertTrue("First amplitude should be greater than last amplitude for exhale",
                seg.amplitudes.first() > seg.amplitudes.last())
        }
    }

    @Test
    fun generateTimeline_cuadrada4444_includesHoldSegmentsWithZeroAmplitude() {
        val plan = BreathingPlan.cuadrada4444(cycles = 2)
        val timeline = BreathingTimelineGenerator.generateTimeline(plan, HapticIntensity.HIGH)

        val holdInhale = timeline.filter { it.type == TimelineSegmentType.HOLD_AFTER_INHALE }
        val holdExhale = timeline.filter { it.type == TimelineSegmentType.HOLD_AFTER_EXHALE && it.cycleIndex > 0 }

        assertEquals(2, holdInhale.size)
        assertEquals(2, holdExhale.size)

        holdInhale.forEach { seg ->
            assertEquals(4000L, seg.durationMs)
            assertTrue(seg.amplitudes.all { it == 0 })
        }
        holdExhale.forEach { seg ->
            assertEquals(4000L, seg.durationMs)
            assertTrue(seg.amplitudes.all { it == 0 })
        }
    }

    @Test
    fun generateTimeline_intensityScaling() {
        val plan = BreathingPlan.calma46(cycles = 1)
        val lowTimeline = BreathingTimelineGenerator.generateTimeline(plan, HapticIntensity.LOW)
        val highTimeline = BreathingTimelineGenerator.generateTimeline(plan, HapticIntensity.HIGH)

        val lowMax = lowTimeline.first { it.type == TimelineSegmentType.INHALE }.amplitudes.maxOrNull() ?: 0
        val highMax = highTimeline.first { it.type == TimelineSegmentType.INHALE }.amplitudes.maxOrNull() ?: 0

        assertTrue("High intensity max amplitude ($highMax) must exceed low intensity ($lowMax)", highMax > lowMax)
    }

    @Test
    fun binaryPulseTimings_generatesValidPatterns() {
        val plan = BreathingPlan.calma46(cycles = 1)
        val timeline = BreathingTimelineGenerator.generateTimeline(plan)
        val inhaleSeg = timeline.first { it.type == TimelineSegmentType.INHALE }
        val (timings, amps) = BreathingTimelineGenerator.generateBinaryPulseTimings(inhaleSeg)

        assertTrue(timings.isNotEmpty())
        assertEquals(timings.size, amps.size)
        val totalTime = timings.sum()
        assertEquals(inhaleSeg.durationMs, totalTime)
    }
}
