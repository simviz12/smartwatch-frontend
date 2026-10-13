package com.pulso.core.breathing

import com.pulso.core.contract.DataLayerContracts
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class HapticIntensity {
    LOW,
    MEDIUM,
    HIGH
}

enum class BreathingFeeling {
    BETTER,  // "Mejor"
    SAME,    // "Igual"
    WORSE    // "Peor"
}

enum class SessionEndReason {
    COMPLETED,
    USER_STOPPED,
    INTERRUPTED
}

@Serializable
data class BreathingPlan(
    val name: String,
    val inhaleMs: Long,
    val holdAfterInhaleMs: Long = 0L,
    val exhaleMs: Long,
    val holdAfterExhaleMs: Long = 0L,
    val cycles: Int
) {
    init {
        require(name.isNotBlank()) { "Breathing plan name cannot be blank" }
        require(inhaleMs in 2000L..15000L) { "Inhale must be between 2 and 15 seconds: $inhaleMs" }
        require(holdAfterInhaleMs in 0L..15000L) { "Hold after inhale must be 0 to 15 seconds: $holdAfterInhaleMs" }
        require(exhaleMs in 2000L..15000L) { "Exhale must be between 2 and 15 seconds: $exhaleMs" }
        require(holdAfterExhaleMs in 0L..15000L) { "Hold after exhale must be 0 to 15 seconds: $holdAfterExhaleMs" }
        require(cycles in 1..60) { "Cycles must be between 1 and 60: $cycles" }
    }

    val singleCycleDurationMs: Long
        get() = inhaleMs + holdAfterInhaleMs + exhaleMs + holdAfterExhaleMs

    val totalDurationMs: Long
        get() = singleCycleDurationMs * cycles

    companion object {
        /**
         * "Calma 4-6": Inhale 4s, Exhale 6s.
         * Default: 12 cycles (~2 minutes).
         */
        fun calma46(cycles: Int = 12): BreathingPlan = BreathingPlan(
            name = "Calma 4-6",
            inhaleMs = 4000L,
            holdAfterInhaleMs = 0L,
            exhaleMs = 6000L,
            holdAfterExhaleMs = 0L,
            cycles = cycles
        )

        /**
         * "Cuadrada 4-4-4-4": Box breathing (4s inhale, 4s hold, 4s exhale, 4s hold).
         * Default: 8 cycles (~2 minutes 8s).
         */
        fun cuadrada4444(cycles: Int = 8): BreathingPlan = BreathingPlan(
            name = "Cuadrada 4-4-4-4",
            inhaleMs = 4000L,
            holdAfterInhaleMs = 4000L,
            exhaleMs = 4000L,
            holdAfterExhaleMs = 4000L,
            cycles = cycles
        )

        /**
         * "Relajante 4-7-8": Relaxing 4-7-8 (4s inhale, 7s hold, 8s exhale).
         * Default: 6 cycles (~1 minute 54s). Advanced.
         */
        fun relajante478(cycles: Int = 6): BreathingPlan = BreathingPlan(
            name = "Relajante 4-7-8",
            inhaleMs = 4000L,
            holdAfterInhaleMs = 7000L,
            exhaleMs = 8000L,
            holdAfterExhaleMs = 0L,
            cycles = cycles
        )

        /**
         * Helper to compute cycles for a given target duration in minutes (e.g. 1, 2, 5 min)
         */
        fun withTargetDuration(basePlan: BreathingPlan, targetMinutes: Int): BreathingPlan {
            require(targetMinutes in 1..10) { "Target duration must be 1 to 10 minutes" }
            val targetMs = targetMinutes * 60 * 1000L
            val cycles = (targetMs / basePlan.singleCycleDurationMs).toInt().coerceAtLeast(1)
            return basePlan.copy(cycles = cycles)
        }
    }
}

enum class TimelineSegmentType {
    START_TAP,
    INHALE,
    HOLD_AFTER_INHALE,
    EXHALE,
    HOLD_AFTER_EXHALE,
    END_DOUBLE_TAP
}

/**
 * Timed segment in the haptic timeline.
 * [startTimeMs] & [durationMs] represent timing within the overall session.
 * [amplitudes] envelope from 0..255 (if available).
 * [stepMs] is the resolution interval for amplitude envelope points (e.g. 100ms or 200ms).
 */
data class HapticTimelineSegment(
    val type: TimelineSegmentType,
    val cycleIndex: Int, // 0 for start/end signals, 1..N for breathing cycles
    val startTimeMs: Long,
    val durationMs: Long,
    val amplitudes: IntArray = intArrayOf(),
    val stepMs: Long = 100L
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is HapticTimelineSegment) return false
        return type == other.type &&
                cycleIndex == other.cycleIndex &&
                startTimeMs == other.startTimeMs &&
                durationMs == other.durationMs &&
                amplitudes.contentEquals(other.amplitudes) &&
                stepMs == other.stepMs
    }

    override fun hashCode(): Int {
        var result = type.hashCode()
        result = 31 * result + cycleIndex
        result = 31 * result + startTimeMs.hashCode()
        result = 31 * result + durationMs.hashCode()
        result = 31 * result + amplitudes.contentHashCode()
        result = 31 * result + stepMs.hashCode()
        return result
    }
}

/**
 * Record of a completed or stopped breathing session.
 */
@Serializable
data class BreathingSessionRecord(
    val schemaVersion: Int = DataLayerContracts.BREATHING_SCHEMA_VERSION,
    val sessionId: String,
    val startedAt: Long,
    val completedAt: Long,
    val planName: String,
    val targetCycles: Int,
    val completedCycles: Int,
    val endedBy: String, // COMPLETED, USER_STOPPED, INTERRUPTED
    val hrBefore: Double? = null,
    val hrAfter: Double? = null,
    val feeling: String? = null // BETTER, SAME, WORSE, or null if skipped
) {
    fun toByteArray(): ByteArray = jsonSerializer.encodeToString(this).encodeToByteArray()

    companion object {
        private val jsonSerializer = Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

        fun fromByteArray(bytes: ByteArray): BreathingSessionRecord {
            return jsonSerializer.decodeFromString(bytes.decodeToString())
        }
    }
}

@Serializable
data class BreathingAck(
    val schemaVersion: Int = DataLayerContracts.BREATHING_ACK_SCHEMA_VERSION,
    val sessionId: String,
    val status: String = "OK"
) {
    fun toByteArray(): ByteArray = jsonSerializer.encodeToString(this).encodeToByteArray()

    companion object {
        private val jsonSerializer = Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

        fun fromByteArray(bytes: ByteArray): BreathingAck {
            return jsonSerializer.decodeFromString(bytes.decodeToString())
        }
    }
}
