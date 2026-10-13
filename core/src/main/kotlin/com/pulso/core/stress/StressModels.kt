package com.pulso.core.stress

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class StressConfig(
    val version: Int = 1,
    // Rule A (HRV + HR)
    val consecutiveWindowsRuleA: Int = 3, // ~15 min (3 * 5min)
    val hrZThreshold: Double = 1.0,
    val hrvZThreshold: Double = -1.0, // hrZ >= hrZThreshold AND hrvZ <= hrvZThreshold

    // Rule B (HR-only fallback)
    val consecutiveWindowsRuleB: Int = 4, // ~20 min
    val hrOnlyZThreshold: Double = 1.5,

    // Signal Quality Gate
    val minValidIbiRatio: Double = 0.8,

    // Operational Gates
    val cooldownMinutes: Int = 60,
    val dailyCap: Int = 4,
    val postExerciseGraceMinutes: Int = 20,

    // User snooze until epoch ms
    val snoozeUntil: Long? = null
) {
    init {
        require(version > 0) { "version must be positive" }
        require(consecutiveWindowsRuleA in 1..12) { "consecutiveWindowsRuleA must be 1..12" }
        require(consecutiveWindowsRuleB in 1..12) { "consecutiveWindowsRuleB must be 1..12" }
        require(hrZThreshold in 0.5..4.0) { "hrZThreshold must be 0.5..4.0" }
        require(hrvZThreshold in -4.0..-0.5) { "hrvZThreshold must be -4.0..-0.5" }
        require(hrOnlyZThreshold in 0.5..4.0) { "hrOnlyZThreshold must be 0.5..4.0" }
        require(minValidIbiRatio in 0.5..1.0) { "minValidIbiRatio must be 0.5..1.0" }
        require(cooldownMinutes in 5..240) { "cooldownMinutes must be 5..240" }
        require(dailyCap in 1..20) { "dailyCap must be 1..20" }
        require(postExerciseGraceMinutes in 0..60) { "postExerciseGraceMinutes must be 0..60" }
    }
}

enum class StressReasonCode {
    ELEVATED_HR_LOW_HRV,     // Rule A
    ELEVATED_HR_ONLY,        // Rule B
    PERSISTENT_DURATION      // Sustained across N windows
}

@Serializable
data class StressReason(
    val code: StressReasonCode,
    val description: String,
    val hrZ: Double? = null,
    val hrvZ: Double? = null
)

enum class SuppressionCode {
    EXERCISE_ACTIVE,
    RECENT_EXERCISE_GRACE,
    ASLEEP,
    OFF_BODY,
    POOR_SIGNAL_QUALITY,
    BASELINE_NOT_READY,
    COOLDOWN_ACTIVE,
    DAILY_CAP_REACHED,
    QUIET_HOURS,
    USER_SNOOZE,
    INSUFFICIENT_CONSECUTIVE_WINDOWS
}

@Serializable
data class StressSuppression(
    val code: SuppressionCode,
    val detail: String
)

@Serializable
data class StressDecision(
    val triggered: Boolean,
    val confidence: Double, // 0.0 .. 1.0
    val reasons: List<StressReason> = emptyList(),
    val suppressedBy: List<StressSuppression> = emptyList(),
    val evaluatedAt: Long
)

@Serializable
data class StressEvent(
    val id: String,
    val time: Long,
    val confidence: Double,
    val reasons: List<StressReason>,
    val windowsUsed: List<Long>, // windowStart timestamps
    val configVersion: Int
)

@Serializable
data class StressEventsBatch(
    val schemaVersion: Int = 1,
    val watchId: String,
    val events: List<StressEvent>
) {
    fun toByteArray(): ByteArray = jsonSerializer.encodeToString(this).encodeToByteArray()

    companion object {
        private val jsonSerializer = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

        fun fromByteArray(bytes: ByteArray): StressEventsBatch {
            return jsonSerializer.decodeFromString(bytes.decodeToString())
        }
    }
}

@Serializable
data class StressEventsAck(
    val schemaVersion: Int = 1,
    val acceptedCount: Int,
    val lastEventId: String? = null
) {
    fun toByteArray(): ByteArray = jsonSerializer.encodeToString(this).encodeToByteArray()

    companion object {
        private val jsonSerializer = kotlinx.serialization.json.Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

        fun fromByteArray(bytes: ByteArray): StressEventsAck {
            return jsonSerializer.decodeFromString(bytes.decodeToString())
        }
    }
}
