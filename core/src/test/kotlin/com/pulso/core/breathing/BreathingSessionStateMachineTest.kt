package com.pulso.core.breathing

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BreathingSessionStateMachineTest {

    @Test
    fun startAndCompleteLifecycle() {
        val plan = BreathingPlan.calma46(cycles = 2)
        val sm = BreathingSessionStateMachine(plan)

        assertEquals(BreathingSessionState.IDLE, sm.state)
        sm.start()
        assertEquals(BreathingSessionState.RUNNING, sm.state)

        // Tick through the duration
        var current = sm.tick(100L)
        assertNotNull(current)
        assertEquals(TimelineSegmentType.START_TAP, current?.type)

        // Jump through remaining time
        val totalMs = sm.totalDurationMs
        sm.tick(totalMs + 1000L)

        assertEquals(BreathingSessionState.COMPLETED, sm.state)
        assertEquals(2, sm.getCompletedCyclesCount())
        val snap = sm.getCurrentSnapshot()
        assertEquals(SessionEndReason.COMPLETED, snap.endReason)
    }

    @Test
    fun pauseAndResume() {
        val plan = BreathingPlan.calma46(cycles = 2)
        val sm = BreathingSessionStateMachine(plan)
        sm.start()
        sm.tick(500L)

        sm.pause()
        assertEquals(BreathingSessionState.PAUSED, sm.state)

        // Tick while paused should not advance
        val seg = sm.tick(1000L)
        assertNull(seg)
        assertEquals(BreathingSessionState.PAUSED, sm.state)

        sm.resume()
        assertEquals(BreathingSessionState.RUNNING, sm.state)
    }

    @Test
    fun earlyStopByUser() {
        val plan = BreathingPlan.calma46(cycles = 3)
        val sm = BreathingSessionStateMachine(plan)
        sm.start()

        // Tick 1 full cycle (~10.75s)
        sm.tick(11000L)
        sm.stop()

        assertEquals(BreathingSessionState.STOPPED, sm.state)
        val snap = sm.getCurrentSnapshot()
        assertEquals(SessionEndReason.USER_STOPPED, snap.endReason)
    }

    @Test
    fun interruptionByIncomingAlert() {
        val plan = BreathingPlan.calma46(cycles = 2)
        val sm = BreathingSessionStateMachine(plan)
        sm.start()
        sm.tick(2000L)

        sm.interrupt()
        assertEquals(BreathingSessionState.INTERRUPTED, sm.state)
        val snap = sm.getCurrentSnapshot()
        assertEquals(SessionEndReason.INTERRUPTED, snap.endReason)
    }
}
