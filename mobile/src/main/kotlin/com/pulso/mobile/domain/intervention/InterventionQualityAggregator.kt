package com.pulso.mobile.domain.intervention

import com.pulso.mobile.data.local.PhoneStressEventEntity

data class QualityMetrics(
    val totalInterventions: Int,
    val accurateCount: Int,
    val accuratePct: Double,
    val falseAlarmCount: Int,
    val falseAlarmPct: Double,
    val ignoredCount: Int,
    val ignoredPct: Double,
    val pendingFeedbackCount: Int
)

object InterventionQualityAggregator {

    /**
     * Computes quality metrics over the provided list of events.
     * Categorization:
     * - Accurate: TALK, BREATHE, REVISED_STRESSED
     * - False alarm: NOT_STRESSED
     * - Ignored / dismissed: IGNORED, DISMISS
     * - Pending: null / no answer yet
     */
    fun computeMetrics(events: List<PhoneStressEventEntity>): QualityMetrics {
        val total = events.size
        if (total == 0) {
            return QualityMetrics(
                totalInterventions = 0,
                accurateCount = 0,
                accuratePct = 0.0,
                falseAlarmCount = 0,
                falseAlarmPct = 0.0,
                ignoredCount = 0,
                ignoredPct = 0.0,
                pendingFeedbackCount = 0
            )
        }

        var accurate = 0
        var falseAlarm = 0
        var ignored = 0
        var pending = 0

        for (event in events) {
            when (event.userFeedback) {
                "TALK", "BREATHE", "REVISED_STRESSED" -> accurate++
                "NOT_STRESSED" -> falseAlarm++
                "IGNORED", "DISMISS" -> ignored++
                else -> pending++
            }
        }

        return QualityMetrics(
            totalInterventions = total,
            accurateCount = accurate,
            accuratePct = (accurate * 100.0) / total,
            falseAlarmCount = falseAlarm,
            falseAlarmPct = (falseAlarm * 100.0) / total,
            ignoredCount = ignored,
            ignoredPct = (ignored * 100.0) / total,
            pendingFeedbackCount = pending
        )
    }
}
