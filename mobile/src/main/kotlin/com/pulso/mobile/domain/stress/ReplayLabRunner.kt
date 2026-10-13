package com.pulso.mobile.domain.stress

import com.pulso.core.baseline.Baseline
import com.pulso.core.model.MeasurementWindow
import com.pulso.core.model.QuietHours
import com.pulso.core.stress.StressConfig
import com.pulso.core.stress.StressDecision
import com.pulso.core.stress.StressEngine

data class ReplayTimelinePoint(
    val window: MeasurementWindow,
    val decision: StressDecision,
    val hrBaselineRange: Pair<Double, Double>?,
    val hrvBaselineRange: Pair<Double, Double>?
)

data class ReplaySimulationResult(
    val timeline: List<ReplayTimelinePoint>,
    val totalWindowsAnalyzed: Int,
    val totalTriggers: Int,
    val totalSuppressedRuleMatches: Int,
    val topSuppressionReasons: Map<String, Int>
)

object ReplayLabRunner {

    /**
     * Replays a list of windows in chronological order, evaluating each window
     * with historical context up to that point.
     */
    fun runReplay(
        windows: List<MeasurementWindow>,
        baseline: Baseline?,
        config: StressConfig,
        quietHours: QuietHours? = null
    ): ReplaySimulationResult {
        val sortedWindows = windows.sortedBy { it.windowStart }
        if (sortedWindows.isEmpty()) {
            return ReplaySimulationResult(
                timeline = emptyList(),
                totalWindowsAnalyzed = 0,
                totalTriggers = 0,
                totalSuppressedRuleMatches = 0,
                topSuppressionReasons = emptyMap()
            )
        }

        val timeline = mutableListOf<ReplayTimelinePoint>()
        var lastTriggeredAt: Long? = null
        var triggersTodayCount = 0
        var currentDayKey = -1L
        var lastExerciseEndAt: Long? = null
        var totalSuppressedRuleMatches = 0
        val suppressionCounts = mutableMapOf<String, Int>()

        for (i in sortedWindows.indices) {
            val currentWindow = sortedWindows[i]
            val windowTime = currentWindow.windowEnd

            // Check day change for daily cap
            val dayKey = windowTime / (24 * 60 * 60 * 1000L)
            if (dayKey != currentDayKey) {
                currentDayKey = dayKey
                triggersTodayCount = 0
            }

            // Update last exercise end if recent window was exercise
            if (currentWindow.activityState.equals("EXERCISING", ignoreCase = true) ||
                currentWindow.activityState.equals("RUNNING", ignoreCase = true) ||
                currentWindow.activityState.equals("WALKING", ignoreCase = true)
            ) {
                lastExerciseEndAt = currentWindow.windowEnd
            }

            // Window context: up to max(consecutiveWindowsRuleA, consecutiveWindowsRuleB) + 1 prior windows
            val maxContext = maxOf(config.consecutiveWindowsRuleA, config.consecutiveWindowsRuleB) + 2
            val startIndex = (i - maxContext + 1).coerceAtLeast(0)
            val windowContext = sortedWindows.subList(startIndex, i + 1)

            val decision = StressEngine.evaluate(
                recentWindows = windowContext,
                baseline = baseline,
                config = config,
                quietHours = quietHours,
                lastTriggeredAt = lastTriggeredAt,
                triggersTodayCount = triggersTodayCount,
                lastExerciseEndAt = lastExerciseEndAt,
                now = windowTime
            )

            if (decision.triggered) {
                lastTriggeredAt = windowTime
                triggersTodayCount++
            } else if (decision.reasons.isNotEmpty() && decision.suppressedBy.isNotEmpty()) {
                totalSuppressedRuleMatches++
                for (suppression in decision.suppressedBy) {
                    suppressionCounts[suppression.detail] = (suppressionCounts[suppression.detail] ?: 0) + 1
                }
            }

            // Baseline ranges for this bucket
            val bucket = com.pulso.core.baseline.BaselineEngine.resolveBucket(
                timestamp = currentWindow.windowStart,
                activityState = currentWindow.activityState
            )
            val bucketStats = baseline?.buckets?.get(bucket)
            val hrRange = if (bucketStats?.state == com.pulso.core.baseline.BaselineState.READY) {
                val med = bucketStats.hrMedian ?: 70.0
                val mad = bucketStats.hrMad ?: 5.0
                Pair(med - 1.4826 * mad, med + 1.4826 * mad)
            } else null

            val hrvRange = if (bucketStats?.state == com.pulso.core.baseline.BaselineState.READY &&
                bucketStats.rmssdMedian != null && bucketStats.rmssdMad != null
            ) {
                val med = bucketStats.rmssdMedian!!
                val mad = bucketStats.rmssdMad!!
                Pair(med - 1.4826 * mad, med + 1.4826 * mad)
            } else null

            timeline.add(
                ReplayTimelinePoint(
                    window = currentWindow,
                    decision = decision,
                    hrBaselineRange = hrRange,
                    hrvBaselineRange = hrvRange
                )
            )
        }

        return ReplaySimulationResult(
            timeline = timeline,
            totalWindowsAnalyzed = sortedWindows.size,
            totalTriggers = timeline.count { it.decision.triggered },
            totalSuppressedRuleMatches = totalSuppressedRuleMatches,
            topSuppressionReasons = suppressionCounts
        )
    }
}
