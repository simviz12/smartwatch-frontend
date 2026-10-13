package com.pulso.mobile.domain.breathing

import com.pulso.mobile.data.local.PhoneBreathingSessionEntity
import org.junit.Assert.assertEquals
import org.junit.Test

class BreathingHistoryAggregatorTest {

    @Test
    fun `empty list produces zero metrics`() {
        val summary = BreathingHistoryAggregator.computeWeeklySummary(emptyList(), now = 1000000L)
        assertEquals(0, summary.totalSessionsThisWeek)
        assertEquals(0, summary.completedSessionsThisWeek)
        assertEquals(0, summary.totalMinutesBreathed)
        assertEquals(0, summary.averageFeelingBetterPct)
    }

    @Test
    fun `sessions older than 7 days are ignored in weekly summary`() {
        val now = 1_000_000_000L
        val eightDaysAgo = now - (8L * 24 * 3600 * 1000)
        val threeDaysAgo = now - (3L * 24 * 3600 * 1000)

        val oldSession = PhoneBreathingSessionEntity(
            sessionId = "old",
            startedAt = eightDaysAgo - 60000L,
            completedAt = eightDaysAgo,
            planName = "Calma 4-6",
            targetCycles = 6,
            completedCycles = 6,
            endedBy = "COMPLETED",
            hrBefore = 80.0,
            hrAfter = 72.0,
            feeling = "BETTER"
        )

        val recentSession = PhoneBreathingSessionEntity(
            sessionId = "recent",
            startedAt = threeDaysAgo - 120000L,
            completedAt = threeDaysAgo,
            planName = "Calma 4-6",
            targetCycles = 12,
            completedCycles = 12,
            endedBy = "COMPLETED",
            hrBefore = 85.0,
            hrAfter = 75.0,
            feeling = "BETTER"
        )

        val summary = BreathingHistoryAggregator.computeWeeklySummary(listOf(oldSession, recentSession), now = now)

        assertEquals(1, summary.totalSessionsThisWeek)
        assertEquals(1, summary.completedSessionsThisWeek)
        assertEquals(2, summary.totalMinutesBreathed) // 120,000ms = 2 min
        assertEquals(100, summary.averageFeelingBetterPct)
    }

    @Test
    fun `correctly computes percentage of sessions where user felt better`() {
        val now = 1_000_000_000L
        val oneDayAgo = now - (1L * 24 * 3600 * 1000)
        val twoDaysAgo = now - (2L * 24 * 3600 * 1000)
        val threeDaysAgo = now - (3L * 24 * 3600 * 1000)
        val fourDaysAgo = now - (4L * 24 * 3600 * 1000)

        val session1 = PhoneBreathingSessionEntity(
            sessionId = "1",
            startedAt = oneDayAgo - 60000L,
            completedAt = oneDayAgo,
            planName = "Calma 4-6",
            targetCycles = 6,
            completedCycles = 6,
            endedBy = "COMPLETED",
            feeling = "BETTER"
        )

        val session2 = PhoneBreathingSessionEntity(
            sessionId = "2",
            startedAt = twoDaysAgo - 96000L,
            completedAt = twoDaysAgo,
            planName = "Cuadrada 4-4-4-4",
            targetCycles = 6,
            completedCycles = 6,
            endedBy = "COMPLETED",
            feeling = "SAME"
        )

        val session3 = PhoneBreathingSessionEntity(
            sessionId = "3",
            startedAt = threeDaysAgo - 76000L,
            completedAt = threeDaysAgo,
            planName = "Relajante 4-7-8",
            targetCycles = 4,
            completedCycles = 3,
            endedBy = "CANCELLED",
            feeling = "WORSE"
        )

        val session4 = PhoneBreathingSessionEntity(
            sessionId = "4",
            startedAt = fourDaysAgo - 60000L,
            completedAt = fourDaysAgo,
            planName = "Calma 4-6",
            targetCycles = 6,
            completedCycles = 6,
            endedBy = "COMPLETED",
            feeling = null // No rating given
        )

        val summary = BreathingHistoryAggregator.computeWeeklySummary(listOf(session1, session2, session3, session4), now = now)

        assertEquals(4, summary.totalSessionsThisWeek)
        assertEquals(3, summary.completedSessionsThisWeek)
        // Duration: (60 + 96 + 76 + 60)s = 292s = 4 minutes
        assertEquals(4, summary.totalMinutesBreathed)
        // Rated sessions: 3 (BETTER, SAME, WORSE). BETTER count: 1 -> (1 * 100) / 3 = 33%
        assertEquals(33, summary.averageFeelingBetterPct)
    }
}
