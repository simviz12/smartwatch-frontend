package com.pulso.core.aggregation

import kotlinx.serialization.Serializable

@Serializable
data class HeatmapCell(
    val dayOfWeek: Int, // 1 = Lunes, ..., 7 = Domingo (ISO-8601 standard)
    val hourOfDay: Int, // 0 .. 23
    val stressEventCount: Int,
    val windowCount: Int,
    val stressLoadRatio: Double // stressEventCount / windowCount or normalized index (0.0 .. 1.0)
)

@Serializable
data class HourlyLoad(
    val hourOfDay: Int,
    val totalStressEvents: Int,
    val totalWindows: Int,
    val averageLoad: Double
)

@Serializable
data class TimelineEvent(
    val timestamp: Long,
    val type: String, // "STRESS", "BREATHING"
    val description: String,
    val intensity: Double
)

@Serializable
data class WeeklyTrend(
    val weekStartTimestamp: Long,
    val totalEvents: Int,
    val averageDailyLoad: Double,
    val topStressSlots: List<TopSlot>
)

@Serializable
data class TopSlot(
    val dayOfWeek: Int,
    val hourOfDay: Int,
    val eventCount: Int,
    val message: String
)

@Serializable
data class BreathingEffectiveness(
    val totalSessions: Int,
    val feltBetterCount: Int,
    val feltBetterPercentage: Double,
    val averageDurationSeconds: Int
)

@Serializable
data class StressMapReport(
    val hasSufficientData: Boolean,
    val totalWindowsAnalyzed: Int,
    val totalEventsAnalyzed: Int,
    val heatmap: List<HeatmapCell>,
    val hourlyLoads: List<HourlyLoad>,
    val weeklyTrend: WeeklyTrend?,
    val neutralInsight: String,
    val breathingEffectiveness: BreathingEffectiveness?
)
