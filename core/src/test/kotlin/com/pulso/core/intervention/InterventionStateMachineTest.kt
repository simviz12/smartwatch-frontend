package com.pulso.core.intervention

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class InterventionStateMachineTest {

    @Test
    fun testHapticPatternProperties() {
        assertEquals(4, HapticPattern.GENTLE_PULSE_TIMINGS.size)
        assertEquals(4, HapticPattern.GENTLE_PULSE_AMPLITUDES.size)
        assertEquals(0L, HapticPattern.GENTLE_PULSE_TIMINGS[0])
        assertEquals(70L, HapticPattern.GENTLE_PULSE_TIMINGS[1])
        assertEquals(140L, HapticPattern.GENTLE_PULSE_TIMINGS[2])
        assertEquals(70L, HapticPattern.GENTLE_PULSE_TIMINGS[3])

        assertEquals(0, HapticPattern.GENTLE_PULSE_AMPLITUDES[0])
        assertEquals(65, HapticPattern.GENTLE_PULSE_AMPLITUDES[1])
        assertEquals(0, HapticPattern.GENTLE_PULSE_AMPLITUDES[2])
        assertEquals(65, HapticPattern.GENTLE_PULSE_AMPLITUDES[3])
        assertEquals(-1, HapticPattern.REPEAT_INDEX_NONE)
    }

    @Test
    fun testAdaptiveHelpDisabledByDefault() {
        val machine = InterventionStateMachine(adaptiveThresholdK = 3, adaptiveHelpEnabled = false)
        val feedback = InterventionFeedback(
            eventId = "ev-1",
            action = InterventionAction.NOT_STRESSED,
            timeBucket = "AFTERNOON"
        )

        // Submit 3 times
        assertNull(machine.processFeedback(feedback))
        assertNull(machine.processFeedback(feedback))
        assertNull(machine.processFeedback(feedback))
        assertEquals(3, machine.getStreak("AFTERNOON"))
    }

    @Test
    fun testAdaptiveHelpTriggersWhenEnabledAndStreakReached() {
        val machine = InterventionStateMachine(adaptiveThresholdK = 3, adaptiveHelpEnabled = true)
        val feedback = InterventionFeedback(
            eventId = "ev-1",
            action = InterventionAction.NOT_STRESSED,
            timeBucket = "AFTERNOON"
        )

        assertNull(machine.processFeedback(feedback))
        assertNull(machine.processFeedback(feedback))
        val suggestion = machine.processFeedback(feedback)

        assertNotNull(suggestion)
        assertEquals("AFTERNOON", suggestion?.bucket)
        assertEquals(3, suggestion?.consecutiveNotStressedCount)
    }

    @Test
    fun testStreakResetsOnPositiveEngagement() {
        val machine = InterventionStateMachine(adaptiveThresholdK = 3, adaptiveHelpEnabled = true)
        val notStressed = InterventionFeedback(
            eventId = "ev-1",
            action = InterventionAction.NOT_STRESSED,
            timeBucket = "AFTERNOON"
        )
        val talk = InterventionFeedback(
            eventId = "ev-2",
            action = InterventionAction.TALK,
            timeBucket = "AFTERNOON"
        )

        machine.processFeedback(notStressed)
        machine.processFeedback(notStressed)
        assertEquals(2, machine.getStreak("AFTERNOON"))

        machine.processFeedback(talk)
        assertEquals(0, machine.getStreak("AFTERNOON"))
    }

    @Test
    fun testSerializationRoundtrip() {
        val originalFeedback = InterventionFeedback(
            eventId = "evt-123",
            action = InterventionAction.BREATHE,
            timeBucket = "MORNING",
            timestamp = 1718000000000L
        )
        val bytes = originalFeedback.toByteArray()
        val decoded = InterventionFeedback.fromByteArray(bytes)

        assertEquals(originalFeedback.eventId, decoded.eventId)
        assertEquals(originalFeedback.action, decoded.action)
        assertEquals(originalFeedback.timeBucket, decoded.timeBucket)
        assertEquals(originalFeedback.timestamp, decoded.timestamp)
    }
}
