package com.pulso.core.stress

import com.pulso.core.baseline.Baseline
import com.pulso.core.baseline.BaselineEngine
import com.pulso.core.baseline.BaselineState
import com.pulso.core.baseline.TimeBucket
import com.pulso.core.model.MeasurementWindow
import com.pulso.core.model.QuietHours
import java.time.Instant
import java.time.ZoneId
import java.time.ZonedDateTime

object StressEngine {

    /**
     * Evaluates recent measurement windows against the personal baseline and stress config.
     *
     * @param recentWindows Ordered ascending by windowStart, the most recent windows (e.g. last 6-8 windows).
     * @param baseline The personal baseline computed for the user.
     * @param config The stress detection configuration.
     * @param quietHours Optional quiet hours.
     * @param lastTriggeredAt Timestamp of the most recent triggered StressEvent (for cooldown check).
     * @param triggersTodayCount Number of triggers already occurred on the current calendar day (for daily cap).
     * @param lastExerciseEndAt Optional timestamp when exercise or high-motion state was last observed.
     * @param now Current epoch timestamp in milliseconds.
     * @param zoneId Time zone for quiet hours and bucket resolution.
     */
    fun evaluate(
        recentWindows: List<MeasurementWindow>,
        baseline: Baseline?,
        config: StressConfig = StressConfig(),
        quietHours: QuietHours? = null,
        lastTriggeredAt: Long? = null,
        triggersTodayCount: Int = 0,
        lastExerciseEndAt: Long? = null,
        now: Long = System.currentTimeMillis(),
        zoneId: ZoneId = ZoneId.systemDefault()
    ): StressDecision {
        val suppressions = mutableListOf<StressSuppression>()

        if (recentWindows.isEmpty()) {
            return StressDecision(
                triggered = false,
                confidence = 0.0,
                suppressedBy = listOf(StressSuppression(SuppressionCode.INSUFFICIENT_CONSECUTIVE_WINDOWS, "Sin ventanas recientes")),
                evaluatedAt = now
            )
        }

        val latestWindow = recentWindows.last()

        // 1. Gate: On-body
        if (!latestWindow.onBody) {
            suppressions.add(StressSuppression(SuppressionCode.OFF_BODY, "Reloj no colocado en el cuerpo"))
        }

        // 2. Gate: Sleep
        if (latestWindow.activityState.equals("ASLEEP", ignoreCase = true)) {
            suppressions.add(StressSuppression(SuppressionCode.ASLEEP, "Usuario dormido"))
        }

        // 3. Gate: Exercise / high motion active right now
        val isExercisingNow = latestWindow.activityState.equals("EXERCISING", ignoreCase = true) ||
                latestWindow.activityState.equals("ACTIVE", ignoreCase = true)
        if (isExercisingNow) {
            suppressions.add(StressSuppression(SuppressionCode.EXERCISE_ACTIVE, "Actividad física o ejercicio en curso"))
        }

        // 4. Gate: Post-exercise grace period (HR stays naturally elevated)
        val exerciseEndTime = lastExerciseEndAt ?: if (isExercisingNow) latestWindow.windowEnd else null
        if (exerciseEndTime != null) {
            val minutesSinceExercise = (now - exerciseEndTime) / (60 * 1000)
            if (minutesSinceExercise in 0..config.postExerciseGraceMinutes) {
                suppressions.add(
                    StressSuppression(
                        SuppressionCode.RECENT_EXERCISE_GRACE,
                        "Recuperación post-ejercicio ($minutesSinceExercise de ${config.postExerciseGraceMinutes} min de gracia)"
                    )
                )
            }
        }

        // 5. Gate: User snooze
        if (config.snoozeUntil != null && now < config.snoozeUntil) {
            suppressions.add(StressSuppression(SuppressionCode.USER_SNOOZE, "Avisos pausados por el usuario hasta ${config.snoozeUntil}"))
        }

        // 6. Gate: Quiet hours
        if (quietHours != null && isWithinQuietHours(now, quietHours, zoneId)) {
            suppressions.add(
                StressSuppression(
                    SuppressionCode.QUIET_HOURS,
                    "Horario de descanso activo (${quietHours.startHour}:${quietHours.startMinute} - ${quietHours.endHour}:${quietHours.endMinute})"
                )
            )
        }

        // 7. Gate: Cooldown after last trigger
        if (lastTriggeredAt != null) {
            val minutesSinceLastTrigger = (now - lastTriggeredAt) / (60 * 1000)
            if (minutesSinceLastTrigger in 0..config.cooldownMinutes) {
                suppressions.add(
                    StressSuppression(
                        SuppressionCode.COOLDOWN_ACTIVE,
                        "Periodo de enfriamiento activo ($minutesSinceLastTrigger de ${config.cooldownMinutes} min)"
                    )
                )
            }
        }

        // 8. Gate: Daily cap
        if (triggersTodayCount >= config.dailyCap) {
            suppressions.add(
                StressSuppression(
                    SuppressionCode.DAILY_CAP_REACHED,
                    "Límite diario alcanzado ($triggersTodayCount de ${config.dailyCap} avisos hoy)"
                )
            )
        }

        // 9. Gate: Baseline readiness for current bucket
        val currentBucket = BaselineEngine.resolveBucket(now, latestWindow.activityState, zoneId)
        val bucketStats = baseline?.buckets?.get(currentBucket)
        if (baseline == null || bucketStats == null || bucketStats.state != BaselineState.READY) {
            suppressions.add(
                StressSuppression(
                    SuppressionCode.BASELINE_NOT_READY,
                    "Línea base no lista para el horario $currentBucket (estado: ${bucketStats?.state ?: "SIN DATOS"})"
                )
            )
        }

        // If hard gates blocked and baseline isn't ready, return suppressed decision
        if (suppressions.isNotEmpty() && (bucketStats == null || bucketStats.state != BaselineState.READY)) {
            return StressDecision(
                triggered = false,
                confidence = 0.0,
                suppressedBy = suppressions,
                evaluatedAt = now
            )
        }

        // Evaluate Rules on consecutive resting windows
        // Try Rule A first (HRV + HR)
        val ruleAResult = evaluateRuleA(recentWindows, baseline!!, config, zoneId)
        if (ruleAResult != null) {
            val (confidence, reasons) = ruleAResult
            val isTriggered = suppressions.isEmpty()
            return StressDecision(
                triggered = isTriggered,
                confidence = confidence,
                reasons = reasons,
                suppressedBy = suppressions,
                evaluatedAt = now
            )
        }

        // Try Rule B (HR-only fallback)
        val ruleBResult = evaluateRuleB(recentWindows, baseline, config, zoneId)
        if (ruleBResult != null) {
            val (confidence, reasons) = ruleBResult
            val isTriggered = suppressions.isEmpty()
            return StressDecision(
                triggered = isTriggered,
                confidence = confidence,
                reasons = reasons,
                suppressedBy = suppressions,
                evaluatedAt = now
            )
        }

        // Neither rule matched
        if (suppressions.isEmpty()) {
            suppressions.add(
                StressSuppression(
                    SuppressionCode.INSUFFICIENT_CONSECUTIVE_WINDOWS,
                    "No se cumplen los umbrales de elevación de pulso o reducción de HRV"
                )
            )
        }

        return StressDecision(
            triggered = false,
            confidence = 0.0,
            suppressedBy = suppressions,
            evaluatedAt = now
        )
    }

    private fun evaluateRuleA(
        recentWindows: List<MeasurementWindow>,
        baseline: Baseline,
        config: StressConfig,
        zoneId: ZoneId
    ): Pair<Double, List<StressReason>>? {
        val n = config.consecutiveWindowsRuleA
        if (recentWindows.size < n) return null

        val tail = recentWindows.takeLast(n)

        // Check resting and signal quality for Rule A
        for (w in tail) {
            if (!BaselineEngine.isRestingWindow(w)) return null
            if (w.rmssd == null || w.ibiValidRatio < config.minValidIbiRatio) return null
        }

        val zScores = tail.map { w ->
            val bucket = BaselineEngine.resolveBucket(w.windowStart, w.activityState, zoneId)
            baseline.calculateZScores(w.meanHr, w.rmssd, bucket)
        }

        // Verify all N windows satisfy hrZ >= hrZThreshold AND hrvZ <= hrvZThreshold
        val allSatisfy = zScores.all { z ->
            (z.hrZ != null && z.hrZ >= config.hrZThreshold) &&
                    (z.hrvZ != null && z.hrvZ <= config.hrvZThreshold)
        }

        if (!allSatisfy) return null

        val lastZ = zScores.last()
        val avgHrZ = zScores.mapNotNull { it.hrZ }.average()
        val avgHrvZ = zScores.mapNotNull { it.hrvZ }.average()

        // Confidence calculation (explainable formula):
        // Base confidence = 0.5
        // + how far hrZ exceeded threshold: min(0.25, (avgHrZ - config.hrZThreshold) * 0.15)
        // + how far hrvZ plunged below threshold: min(0.25, (config.hrvZThreshold - avgHrvZ) * 0.15)
        val hrExcess = (avgHrZ - config.hrZThreshold).coerceAtLeast(0.0)
        val hrvExcess = (config.hrvZThreshold - avgHrvZ).coerceAtLeast(0.0)
        val confidence = (0.50 + (hrExcess * 0.15).coerceAtMost(0.25) + (hrvExcess * 0.15).coerceAtMost(0.25)).coerceIn(0.5, 0.98)

        val reasons = listOf(
            StressReason(
                code = StressReasonCode.ELEVATED_HR_LOW_HRV,
                description = "Pulso elevado y variabilidad (HRV) baja de manera sostenida (hrZ=${String.format(java.util.Locale.US, "%.1f", avgHrZ)}, hrvZ=${String.format(java.util.Locale.US, "%.1f", avgHrvZ)})",
                hrZ = lastZ.hrZ,
                hrvZ = lastZ.hrvZ
            ),
            StressReason(
                code = StressReasonCode.PERSISTENT_DURATION,
                description = "Patrón constante a lo largo de $n ventanas consecutivas (~${n * 5} min en reposo)"
            )
        )

        return Pair(confidence, reasons)
    }

    private fun evaluateRuleB(
        recentWindows: List<MeasurementWindow>,
        baseline: Baseline,
        config: StressConfig,
        zoneId: ZoneId
    ): Pair<Double, List<StressReason>>? {
        val n = config.consecutiveWindowsRuleB
        if (recentWindows.size < n) return null

        val tail = recentWindows.takeLast(n)

        for (w in tail) {
            if (!BaselineEngine.isRestingWindow(w)) return null
        }

        val zScores = tail.map { w ->
            val bucket = BaselineEngine.resolveBucket(w.windowStart, w.activityState, zoneId)
            baseline.calculateZScores(w.meanHr, null, bucket)
        }

        val allSatisfy = zScores.all { z ->
            z.hrZ != null && z.hrZ >= config.hrOnlyZThreshold
        }

        if (!allSatisfy) return null

        val lastZ = zScores.last()
        val avgHrZ = zScores.mapNotNull { it.hrZ }.average()

        // Rule B confidence (lower ceiling since no HRV confirmation):
        // Base 0.40 + hrExcess (up to 0.40)
        val hrExcess = (avgHrZ - config.hrOnlyZThreshold).coerceAtLeast(0.0)
        val confidence = (0.40 + (hrExcess * 0.15).coerceAtMost(0.40)).coerceIn(0.4, 0.85)

        val reasons = listOf(
            StressReason(
                code = StressReasonCode.ELEVATED_HR_ONLY,
                description = "Pulso elevado sostenido sin datos de HRV (hrZ=${String.format("%.1f", avgHrZ)})",
                hrZ = lastZ.hrZ
            ),
            StressReason(
                code = StressReasonCode.PERSISTENT_DURATION,
                description = "Elevación constante durante $n ventanas consecutivas (~${n * 5} min en reposo)"
            )
        )

        return Pair(confidence, reasons)
    }

    fun isWithinQuietHours(timestamp: Long, quietHours: QuietHours, zoneId: ZoneId): Boolean {
        val zdt = ZonedDateTime.ofInstant(Instant.ofEpochMilli(timestamp), zoneId)
        val currentMinutesOfDay = zdt.hour * 60 + zdt.minute

        val startMinutes = quietHours.startHour * 60 + quietHours.startMinute
        val endMinutes = quietHours.endHour * 60 + quietHours.endMinute

        return if (startMinutes <= endMinutes) {
            currentMinutesOfDay in startMinutes..endMinutes
        } else {
            // Wraps over midnight (e.g. 22:00 to 07:00)
            currentMinutesOfDay >= startMinutes || currentMinutesOfDay <= endMinutes
        }
    }
}
