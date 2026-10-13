package com.pulso.mobile.data.local

import androidx.room.Entity
import androidx.room.PrimaryKey
import com.pulso.core.stress.StressEvent
import com.pulso.core.stress.StressReason
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Entity(tableName = "phone_stress_events")
data class PhoneStressEventEntity(
    @PrimaryKey val id: String,
    val time: Long,
    val confidence: Double,
    val reasonsJson: String,
    val windowsUsedJson: String,
    val configVersion: Int
) {
    fun toDomain(): StressEvent {
        val reasons = try {
            Json.decodeFromString<List<StressReason>>(reasonsJson)
        } catch (e: Exception) {
            emptyList()
        }
        val windowsUsed = try {
            Json.decodeFromString<List<Long>>(windowsUsedJson)
        } catch (e: Exception) {
            emptyList()
        }
        return StressEvent(
            id = id,
            time = time,
            confidence = confidence,
            reasons = reasons,
            windowsUsed = windowsUsed,
            configVersion = configVersion
        )
    }

    companion object {
        fun fromDomain(event: StressEvent): PhoneStressEventEntity {
            return PhoneStressEventEntity(
                id = event.id,
                time = event.time,
                confidence = event.confidence,
                reasonsJson = Json.encodeToString(event.reasons),
                windowsUsedJson = Json.encodeToString(event.windowsUsed),
                configVersion = event.configVersion
            )
        }
    }
}
