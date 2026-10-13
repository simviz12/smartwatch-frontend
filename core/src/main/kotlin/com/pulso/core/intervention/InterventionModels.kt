package com.pulso.core.intervention

import kotlinx.serialization.Serializable
import kotlinx.serialization.decodeFromString
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

enum class InterventionAction {
    TALK,
    BREATHE,
    DISMISS,
    NOT_STRESSED,
    IGNORED
}

@Serializable
data class InterventionFeedback(
    val schemaVersion: Int = 1,
    val eventId: String,
    val action: InterventionAction,
    val timeBucket: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toByteArray(): ByteArray = jsonSerializer.encodeToString(this).encodeToByteArray()

    companion object {
        private val jsonSerializer = Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

        fun fromByteArray(bytes: ByteArray): InterventionFeedback {
            return jsonSerializer.decodeFromString(bytes.decodeToString())
        }
    }
}

@Serializable
data class FeedbackAck(
    val schemaVersion: Int = 1,
    val eventId: String,
    val action: InterventionAction,
    val receivedAt: Long = System.currentTimeMillis()
) {
    fun toByteArray(): ByteArray = jsonSerializer.encodeToString(this).encodeToByteArray()

    companion object {
        private val jsonSerializer = Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

        fun fromByteArray(bytes: ByteArray): FeedbackAck {
            return jsonSerializer.decodeFromString(bytes.decodeToString())
        }
    }
}

@Serializable
data class WatchCommand(
    val schemaVersion: Int = 1,
    val type: String, // "OPEN_CHAT"
    val eventId: String? = null,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toByteArray(): ByteArray = jsonSerializer.encodeToString(this).encodeToByteArray()

    companion object {
        const val TYPE_OPEN_CHAT = "OPEN_CHAT"

        private val jsonSerializer = Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

        fun fromByteArray(bytes: ByteArray): WatchCommand {
            return jsonSerializer.decodeFromString(bytes.decodeToString())
        }
    }
}
