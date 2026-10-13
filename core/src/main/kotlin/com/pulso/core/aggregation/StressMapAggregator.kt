package com.pulso.core.aggregation

import com.pulso.core.model.MeasurementWindow
import com.pulso.core.stress.StressEvent
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object StressMapAggregator {

    private const val MIN_WINDOWS_FOR_MAP = 24 // At least 2 hours of data
    private const val MIN_EVENTS_FOR_TOP_SLOTS = 2

    fun aggregate(
        windows: List<MeasurementWindow>,
        stressEvents: List<StressEvent>,
        breathingFeelings: List<String> = emptyList(),
        breathingDurations: List<Int> = emptyList(),
        zoneId: ZoneId = ZoneId.systemDefault(),
        excludeNotStressed: Boolean = true
    ): StressMapReport {
        if (windows.size < MIN_WINDOWS_FOR_MAP) {
            return StressMapReport(
                hasSufficientData = false,
                totalWindowsAnalyzed = windows.size,
                totalEventsAnalyzed = stressEvents.size,
                heatmap = emptyList(),
                hourlyLoads = emptyList(),
                weeklyTrend = null,
                neutralInsight = "Aún no hay suficientes datos para generar tu mapa de patrones.",
                breathingEffectiveness = computeBreathingEffectiveness(breathingFeelings, breathingDurations)
            )
        }

        // 24x7 grid: Day of week (1=Mon..7=Sun), Hour of day (0..23)
        val windowGrid = Array(8) { IntArray(24) }
        val eventGrid = Array(8) { IntArray(24) }

        for (window in windows) {
            val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(window.windowStart), zoneId)
            val day = zdt.dayOfWeek.value // 1..7
            val hour = zdt.hour // 0..23
            windowGrid[day][hour]++
        }

        for (event in stressEvents) {
            val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(event.time), zoneId)
            val day = zdt.dayOfWeek.value
            val hour = zdt.hour
            eventGrid[day][hour]++
        }

        val heatmapCells = mutableListOf<HeatmapCell>()
        for (day in 1..7) {
            for (hour in 0..23) {
                val winCount = windowGrid[day][hour]
                val evCount = eventGrid[day][hour]
                val ratio = if (winCount > 0) {
                    (evCount.toDouble() / winCount.toDouble()).coerceIn(0.0, 1.0)
                } else 0.0

                heatmapCells.add(
                    HeatmapCell(
                        dayOfWeek = day,
                        hourOfDay = hour,
                        stressEventCount = evCount,
                        windowCount = winCount,
                        stressLoadRatio = ratio
                    )
                )
            }
        }

        // Hourly load (aggregated over all days)
        val hourlyLoads = (0..23).map { hour ->
            var totalWins = 0
            var totalEvs = 0
            for (day in 1..7) {
                totalWins += windowGrid[day][hour]
                totalEvs += eventGrid[day][hour]
            }
            val avgLoad = if (totalWins > 0) (totalEvs.toDouble() / totalWins.toDouble()).coerceIn(0.0, 1.0) else 0.0
            HourlyLoad(
                hourOfDay = hour,
                totalStressEvents = totalEvs,
                totalWindows = totalWins,
                averageLoad = avgLoad
            )
        }

        // Top slots
        val topSlots = mutableListOf<TopSlot>()
        for (day in 1..7) {
            for (hour in 0..23) {
                val evCount = eventGrid[day][hour]
                if (evCount >= MIN_EVENTS_FOR_TOP_SLOTS) {
                    val dayName = when (day) {
                        1 -> "Lunes"
                        2 -> "Martes"
                        3 -> "Miércoles"
                        4 -> "Jueves"
                        5 -> "Viernes"
                        6 -> "Sábado"
                        else -> "Domingo"
                    }
                    topSlots.add(
                        TopSlot(
                            dayOfWeek = day,
                            hourOfDay = hour,
                            eventCount = evCount,
                            message = "$dayName alrededor de las ${hour}:00"
                        )
                    )
                }
            }
        }
        val sortedTopSlots = topSlots.sortedByDescending { it.eventCount }.take(3)

        // Weekly trend
        val nowZdt = ZonedDateTime.now(zoneId)
        val weekStart = nowZdt.minusDays((nowZdt.dayOfWeek.value - 1).toLong())
            .toLocalDate().atStartOfDay(zoneId).toInstant().toEpochMilli()

        val weeklyTrend = WeeklyTrend(
            weekStartTimestamp = weekStart,
            totalEvents = stressEvents.size,
            averageDailyLoad = stressEvents.size / 7.0,
            topStressSlots = sortedTopSlots
        )

        val neutralInsight = if (sortedTopSlots.isNotEmpty()) {
            val top = sortedTopSlots.first()
            "Tus momentos de mayor activación corporal suelen presentarse los ${top.message.lowercase()}."
        } else {
            "Tus patrones de activación se distribuyen de forma equilibrada a lo largo del día."
        }

        return StressMapReport(
            hasSufficientData = true,
            totalWindowsAnalyzed = windows.size,
            totalEventsAnalyzed = stressEvents.size,
            heatmap = heatmapCells,
            hourlyLoads = hourlyLoads,
            weeklyTrend = weeklyTrend,
            neutralInsight = neutralInsight,
            breathingEffectiveness = computeBreathingEffectiveness(breathingFeelings, breathingDurations)
        )
    }

    private fun computeBreathingEffectiveness(
        feelings: List<String>,
        durations: List<Int>
    ): BreathingEffectiveness? {
        if (feelings.isEmpty()) return null
        val betterCount = feelings.count { it.equals("BETTER", ignoreCase = true) || it.contains("mejor", ignoreCase = true) }
        val percentage = (betterCount.toDouble() / feelings.size.toDouble()) * 100.0
        val avgDuration = if (durations.isNotEmpty()) durations.average().toInt() else 0

        return BreathingEffectiveness(
            totalSessions = feelings.size,
            feltBetterCount = betterCount,
            feltBetterPercentage = percentage,
            averageDurationSeconds = avgDuration
        )
    }
}
