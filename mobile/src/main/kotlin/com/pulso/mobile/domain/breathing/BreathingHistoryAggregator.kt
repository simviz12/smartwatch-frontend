package com.pulso.mobile.domain.breathing

import com.pulso.mobile.data.local.PhoneBreathingSessionEntity
import java.util.Calendar

data class BreathingWeeklySummary(
    val totalSessionsThisWeek: Int,
    val completedSessionsThisWeek: Int,
    val totalMinutesBreathed: Int,
    val averageFeelingBetterPct: Int
)

object BreathingHistoryAggregator {

    /**
     * Aggregates weekly summary from a list of sessions.
     * Uses rolling 7-day window from [now].
     */
    fun computeWeeklySummary(
        sessions: List<PhoneBreathingSessionEntity>,
        now: Long = System.currentTimeMillis()
    ): BreathingWeeklySummary {
        val sevenDaysAgo = now - (7L * 24 * 60 * 60 * 1000)
        val weeklySessions = sessions.filter { it.completedAt >= sevenDaysAgo }

        if (weeklySessions.isEmpty()) {
            return BreathingWeeklySummary(
                totalSessionsThisWeek = 0,
                completedSessionsThisWeek = 0,
                totalMinutesBreathed = 0,
                averageFeelingBetterPct = 0
            )
        }

        val completed = weeklySessions.count { it.endedBy == "COMPLETED" }
        val totalDurationMs = weeklySessions.sumOf { (it.completedAt - it.startedAt).coerceAtLeast(0L) }
        val totalMinutes = (totalDurationMs / 60000L).toInt()

        val withFeeling = weeklySessions.filter { it.feeling != null }
        val betterCount = withFeeling.count { it.feeling == "BETTER" }
        val betterPct = if (withFeeling.isNotEmpty()) (betterCount * 100) / withFeeling.size else 0

        return BreathingWeeklySummary(
            totalSessionsThisWeek = weeklySessions.size,
            completedSessionsThisWeek = completed,
            totalMinutesBreathed = totalMinutes,
            averageFeelingBetterPct = betterPct
        )
    }
}
