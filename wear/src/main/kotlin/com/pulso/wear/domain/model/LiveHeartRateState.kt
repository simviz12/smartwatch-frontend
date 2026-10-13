package com.pulso.wear.domain.model

data class LiveHeartRateState(
    val bpm: Double? = null,
    val availability: String = "NO DISPONIBLE",
    val activityState: String = "DESCONOCIDO",
    val onBody: Boolean = true,
    val timestamp: Long = System.currentTimeMillis()
)
