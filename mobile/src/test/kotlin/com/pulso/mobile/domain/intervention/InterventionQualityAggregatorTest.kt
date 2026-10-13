package com.pulso.mobile.domain.intervention

import com.pulso.mobile.data.local.PhoneStressEventEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class InterventionQualityAggregatorTest {

    @Test
    fun computeMetrics_emptyList_returnsZeroes() {
        val metrics = InterventionQualityAggregator.computeMetrics(emptyList())
        assertEquals(0, metrics.totalInterventions)
        assertEquals(0, metrics.accurateCount)
        assertEquals(0, metrics.falseAlarmCount)
        assertEquals(0, metrics.ignoredCount)
        assertEquals(0, metrics.pendingFeedbackCount)
        assertEquals(0.0, metrics.accuratePct, 0.01)
        assertEquals(0.0, metrics.falseAlarmPct, 0.01)
        assertEquals(0.0, metrics.ignoredPct, 0.01)
    }

    @Test
    fun computeMetrics_mixedFeedbacks_computesCorrectPercentages() {
        // 10 events:
        // 4 Accurate (TALK, BREATHE, REVISED_STRESSED)
        // 2 False alarm (NOT_STRESSED)
        // 3 Ignored (IGNORED, DISMISS)
        // 1 Unanswered (null)
        val events = listOf(
            createEntity(id = "1", feedback = "TALK"),
            createEntity(id = "2", feedback = "BREATHE"),
            createEntity(id = "3", feedback = "REVISED_STRESSED"),
            createEntity(id = "4", feedback = "TALK"),
            createEntity(id = "5", feedback = "NOT_STRESSED"),
            createEntity(id = "6", feedback = "NOT_STRESSED"),
            createEntity(id = "7", feedback = "IGNORED"),
            createEntity(id = "8", feedback = "DISMISS"),
            createEntity(id = "9", feedback = "IGNORED"),
            createEntity(id = "10", feedback = null)
        )

        val metrics = InterventionQualityAggregator.computeMetrics(events)
        assertEquals(10, metrics.totalInterventions)
        assertEquals(4, metrics.accurateCount)
        assertEquals(2, metrics.falseAlarmCount)
        assertEquals(3, metrics.ignoredCount)
        assertEquals(1, metrics.pendingFeedbackCount)

        // 4/10 = 40.0%
        assertEquals(40.0, metrics.accuratePct, 0.01)
        // 2/10 = 20.0%
        assertEquals(20.0, metrics.falseAlarmPct, 0.01)
        // 3/10 = 30.0%
        assertEquals(30.0, metrics.ignoredPct, 0.01)
    }

    @Test
    fun computeMetrics_allAccurate_returns100Percent() {
        val events = listOf(
            createEntity(id = "1", feedback = "TALK"),
            createEntity(id = "2", feedback = "BREATHE")
        )
        val metrics = InterventionQualityAggregator.computeMetrics(events)
        assertEquals(2, metrics.totalInterventions)
        assertEquals(100.0, metrics.accuratePct, 0.01)
        assertEquals(0.0, metrics.falseAlarmPct, 0.01)
        assertEquals(0.0, metrics.ignoredPct, 0.01)
    }

    private fun createEntity(id: String, feedback: String?): PhoneStressEventEntity {
        return PhoneStressEventEntity(
            id = id,
            time = System.currentTimeMillis(),
            confidence = 0.85,
            reasonsJson = "[]",
            windowsUsedJson = "[]",
            configVersion = 1,
            userFeedback = feedback
        )
    }
}
