package com.pulso.wear.data.local

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey
import com.pulso.core.model.MeasurementWindow

@Entity(
    tableName = "measurement_windows",
    indices = [
        Index(value = ["windowStart"], unique = true),
        Index(value = ["synced"])
    ]
)
data class WindowEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val windowStart: Long,
    val windowEnd: Long,
    val meanHr: Double,
    val minHr: Double,
    val maxHr: Double,
    val rmssd: Double?,
    val validIbiCount: Int,
    val ibiValidRatio: Double,
    val activityState: String,
    val steps: Long?,
    val onBody: Boolean,
    val source: String,
    val synced: Boolean = false,
    val batchId: String? = null
) {
    fun toDomain(): MeasurementWindow = MeasurementWindow(
        id = id,
        windowStart = windowStart,
        windowEnd = windowEnd,
        meanHr = meanHr,
        minHr = minHr,
        maxHr = maxHr,
        rmssd = rmssd,
        validIbiCount = validIbiCount,
        ibiValidRatio = ibiValidRatio,
        activityState = activityState,
        steps = steps,
        onBody = onBody,
        source = source
    )

    companion object {
        fun fromDomain(domain: MeasurementWindow, synced: Boolean = false): WindowEntity = WindowEntity(
            id = domain.id,
            windowStart = domain.windowStart,
            windowEnd = domain.windowEnd,
            meanHr = domain.meanHr,
            minHr = domain.minHr,
            maxHr = domain.maxHr,
            rmssd = domain.rmssd,
            validIbiCount = domain.validIbiCount,
            ibiValidRatio = domain.ibiValidRatio,
            activityState = domain.activityState,
            steps = domain.steps,
            onBody = domain.onBody,
            source = domain.source,
            synced = synced
        )
    }
}
