package com.pulso.core.model

import com.pulso.core.contract.DataLayerContracts
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

private val assistantJson = Json { ignoreUnknownKeys = true }

/** Watch → phone: what the user said to the assistant on the wrist. */
@Serializable
data class AssistantAsk(
    val schemaVersion: Int = DataLayerContracts.ASSISTANT_SCHEMA_VERSION,
    val requestId: String,
    val text: String,
    val language: String = "ES",
    val sentAt: Long
) {
    init {
        require(schemaVersion == DataLayerContracts.ASSISTANT_SCHEMA_VERSION) { "Unsupported schemaVersion: $schemaVersion" }
        require(requestId.isNotBlank()) { "requestId must not be blank" }
        require(text.isNotBlank()) { "text must not be blank" }
        require(text.length <= MAX_TEXT) { "text too long: ${text.length}" }
    }

    fun toByteArray(): ByteArray = assistantJson.encodeToString(this).toByteArray(Charsets.UTF_8)

    companion object {
        const val MAX_TEXT = 2000
        fun fromByteArray(bytes: ByteArray): AssistantAsk = assistantJson.decodeFromString(bytes.toString(Charsets.UTF_8))
    }
}

/** Phone → watch: the assistant's answer to an [AssistantAsk]. */
@Serializable
data class AssistantReply(
    val schemaVersion: Int = DataLayerContracts.ASSISTANT_SCHEMA_VERSION,
    val requestId: String,
    val text: String,
    val suggestsBreathing: Boolean = false,
    val sentAt: Long
) {
    init {
        require(schemaVersion == DataLayerContracts.ASSISTANT_SCHEMA_VERSION) { "Unsupported schemaVersion: $schemaVersion" }
        require(requestId.isNotBlank()) { "requestId must not be blank" }
    }

    fun toByteArray(): ByteArray = assistantJson.encodeToString(this).toByteArray(Charsets.UTF_8)

    companion object {
        fun fromByteArray(bytes: ByteArray): AssistantReply = assistantJson.decodeFromString(bytes.toString(Charsets.UTF_8))
    }
}
