package com.pulso.core.model

import com.pulso.core.contract.DataLayerContracts
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class PingMessage(
    val schemaVersion: Int = DataLayerContracts.PING_SCHEMA_VERSION,
    val from: String,
    val sentAt: Long,
    val counter: Int
) {
    init {
        require(schemaVersion == DataLayerContracts.PING_SCHEMA_VERSION) {
            "Unsupported schemaVersion: $schemaVersion (expected ${DataLayerContracts.PING_SCHEMA_VERSION})"
        }
        require(from == DataLayerContracts.Source.WATCH || from == DataLayerContracts.Source.PHONE) {
            "Invalid source '$from': must be '${DataLayerContracts.Source.WATCH}' or '${DataLayerContracts.Source.PHONE}'"
        }
        require(counter >= 0) {
            "Counter must be non-negative: $counter"
        }
    }

    fun toByteArray(): ByteArray {
        val jsonString = jsonSerializer.encodeToString(this)
        return jsonString.encodeToByteArray()
    }

    companion object {
        private val jsonSerializer = Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

        fun fromByteArray(bytes: ByteArray): PingMessage {
            val jsonString = bytes.decodeToString()
            return jsonSerializer.decodeFromString(jsonString)
        }
    }
}
