package com.pulso.wear.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pulso.core.intervention.InterventionAction
import com.pulso.core.intervention.InterventionFeedback

@Entity(tableName = "intervention_feedback")
data class InterventionFeedbackEntity(
    @PrimaryKey val id: String, // composite eventId + "_" + action.name
    val eventId: String,
    val action: String,
    val timeBucket: String?,
    val timestamp: Long,
    val synced: Boolean = false
) {
    fun toDomain(): InterventionFeedback {
        return InterventionFeedback(
            eventId = eventId,
            action = try { InterventionAction.valueOf(action) } catch (_: Exception) { InterventionAction.DISMISS },
            timeBucket = timeBucket,
            timestamp = timestamp
        )
    }

    companion object {
        fun fromDomain(domain: InterventionFeedback, synced: Boolean = false): InterventionFeedbackEntity {
            return InterventionFeedbackEntity(
                id = "${domain.eventId}_${domain.action.name}",
                eventId = domain.eventId,
                action = domain.action.name,
                timeBucket = domain.timeBucket,
                timestamp = domain.timestamp,
                synced = synced
            )
        }
    }
}
