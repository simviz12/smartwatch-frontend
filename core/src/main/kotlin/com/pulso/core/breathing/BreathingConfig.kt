package com.pulso.core.breathing

import kotlinx.serialization.Serializable

@Serializable
data class BreathingConfig(
    val preferredPlanName: String = "Calma 4-6",
    val durationMinutes: Int = 2,
    val hapticIntensity: String = "MEDIUM" // "LOW", "MEDIUM", "HIGH"
) {
    init {
        require(durationMinutes in 1..10) { "durationMinutes must be between 1 and 10: $durationMinutes" }
        require(hapticIntensity in listOf("LOW", "MEDIUM", "HIGH")) { "Invalid haptic intensity: $hapticIntensity" }
    }

    fun toPlan(): BreathingPlan {
        val basePlan = when (preferredPlanName) {
            "Cuadrada 4-4-4-4" -> BreathingPlan.cuadrada4444()
            "Relajante 4-7-8" -> BreathingPlan.relajante478()
            else -> BreathingPlan.calma46()
        }
        return BreathingPlan.withTargetDuration(basePlan, durationMinutes)
    }

    fun toHapticIntensity(): HapticIntensity {
        return try {
            HapticIntensity.valueOf(hapticIntensity)
        } catch (_: Exception) {
            HapticIntensity.MEDIUM
        }
    }
}
