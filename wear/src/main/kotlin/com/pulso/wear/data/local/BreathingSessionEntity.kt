package com.pulso.wear.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pulso.core.breathing.BreathingSessionRecord

@Entity(tableName = "breathing_sessions")
data class BreathingSessionEntity(
    @PrimaryKey val sessionId: String,
    val startedAt: Long,
    val completedAt: Long,
    val planName: String,
    val targetCycles: Int,
    val completedCycles: Int,
    val endedBy: String,
    val hrBefore: Double? = null,
    val hrAfter: Double? = null,
    val feeling: String? = null,
    val isSynced: Boolean = false
) {
    fun toDomain(): BreathingSessionRecord {
        return BreathingSessionRecord(
            sessionId = sessionId,
            startedAt = startedAt,
            completedAt = completedAt,
            planName = planName,
            targetCycles = targetCycles,
            completedCycles = completedCycles,
            endedBy = endedBy,
            hrBefore = hrBefore,
            hrAfter = hrAfter,
            feeling = feeling
        )
    }

    companion object {
        fun fromDomain(record: BreathingSessionRecord, isSynced: Boolean = false): BreathingSessionEntity {
            return BreathingSessionEntity(
                sessionId = record.sessionId,
                startedAt = record.startedAt,
                completedAt = record.completedAt,
                planName = record.planName,
                targetCycles = record.targetCycles,
                completedCycles = record.completedCycles,
                endedBy = record.endedBy,
                hrBefore = record.hrBefore,
                hrAfter = record.hrAfter,
                feeling = record.feeling,
                isSynced = isSynced
            )
        }
    }
}
