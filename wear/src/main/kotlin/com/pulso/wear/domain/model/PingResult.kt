package com.pulso.wear.domain.model

sealed interface PingResult {
    data class Success(val nodeDisplayName: String) : PingResult
    data class Failure(val reason: String) : PingResult
}

data class ConnectedPhoneNode(
    val id: String,
    val displayName: String
)
