package com.pulso.core.model

import kotlinx.serialization.Serializable

@Serializable
data class MeasurementWindow(
    val id: Long = 0,
    val windowStart: Long,
    val windowEnd: Long,
    val meanHr: Double,
    val minHr: Double,
    val maxHr: Double,
    val rmssd: Double? = null,
    val validIbiCount: Int = 0,
    val ibiValidRatio: Double = 0.0,
    val activityState: String = "UNKNOWN",
    val steps: Long? = null,
    val onBody: Boolean = true,
    val source: String
) {
    fun toCsvRow(): String {
        return "$id,$windowStart,$windowEnd,$meanHr,$minHr,$maxHr,${rmssd ?: ""},$validIbiCount,$ibiValidRatio,$activityState,${steps ?: ""},$onBody,$source"
    }

    companion object {
        const val CSV_HEADER = "id,windowStart,windowEnd,meanHr,minHr,maxHr,rmssd,validIbiCount,ibiValidRatio,activityState,steps,onBody,source"
    }
}
