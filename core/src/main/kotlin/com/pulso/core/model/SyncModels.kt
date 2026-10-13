package com.pulso.core.model

import com.pulso.core.contract.DataLayerContracts
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
data class QuietHours(
    val startHour: Int,
    val startMinute: Int,
    val endHour: Int,
    val endMinute: Int
) {
    init {
        require(startHour in 0..23) { "startHour must be 0..23" }
        require(startMinute in 0..59) { "startMinute must be 0..59" }
        require(endHour in 0..23) { "endHour must be 0..23" }
        require(endMinute in 0..59) { "endMinute must be 0..59" }
    }
}

@Serializable
data class ConfigSync(
    val schemaVersion: Int = DataLayerContracts.CONFIG_SCHEMA_VERSION,
    val collectionEnabled: Boolean,
    val consentVersion: String? = null,
    val windowMinutes: Int = 5,
    val quietHours: QuietHours? = null,
    val updatedAt: Long
) {
    init {
        require(schemaVersion == DataLayerContracts.CONFIG_SCHEMA_VERSION) {
            "Unsupported schemaVersion: $schemaVersion (expected ${DataLayerContracts.CONFIG_SCHEMA_VERSION})"
        }
        require(windowMinutes > 0) {
            "windowMinutes must be positive: $windowMinutes"
        }
    }

    fun toByteArray(): ByteArray = jsonSerializer.encodeToString(this).encodeToByteArray()

    companion object {
        private val jsonSerializer = Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

        fun fromByteArray(bytes: ByteArray): ConfigSync {
            return jsonSerializer.decodeFromString(bytes.decodeToString())
        }
    }
}

@Serializable
data class SamplesBatch(
    val schemaVersion: Int = DataLayerContracts.SAMPLES_SCHEMA_VERSION,
    val watchId: String,
    val batchId: String,
    val windows: List<MeasurementWindow>
) {
    init {
        require(schemaVersion == DataLayerContracts.SAMPLES_SCHEMA_VERSION) {
            "Unsupported schemaVersion: $schemaVersion"
        }
        require(watchId.isNotBlank()) { "watchId must not be blank" }
        require(batchId.isNotBlank()) { "batchId must not be blank" }
    }

    fun toByteArray(): ByteArray = jsonSerializer.encodeToString(this).encodeToByteArray()

    companion object {
        private val jsonSerializer = Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

        fun fromByteArray(bytes: ByteArray): SamplesBatch {
            return jsonSerializer.decodeFromString(bytes.decodeToString())
        }

        /**
         * Partitions a list of measurement windows into batches that respect
         * max sample count (MAX_SAMPLES_PER_BATCH) and max byte payload size (MAX_BATCH_PAYLOAD_BYTES).
         */
        fun createBatches(
            watchId: String,
            windows: List<MeasurementWindow>,
            maxCount: Int = DataLayerContracts.MAX_SAMPLES_PER_BATCH,
            maxBytes: Int = DataLayerContracts.MAX_BATCH_PAYLOAD_BYTES
        ): List<SamplesBatch> {
            if (windows.isEmpty()) return emptyList()

            val batches = mutableListOf<SamplesBatch>()
            var currentBatchWindows = mutableListOf<MeasurementWindow>()

            for (window in windows) {
                currentBatchWindows.add(window)
                val testBatch = SamplesBatch(
                    watchId = watchId,
                    batchId = java.util.UUID.randomUUID().toString(),
                    windows = currentBatchWindows
                )
                if (currentBatchWindows.size > maxCount || testBatch.toByteArray().size > maxBytes) {
                    currentBatchWindows.removeAt(currentBatchWindows.lastIndex)
                    if (currentBatchWindows.isNotEmpty()) {
                        batches.add(
                            SamplesBatch(
                                watchId = watchId,
                                batchId = java.util.UUID.randomUUID().toString(),
                                windows = currentBatchWindows.toList()
                            )
                        )
                    }
                    currentBatchWindows = mutableListOf(window)
                }
            }

            if (currentBatchWindows.isNotEmpty()) {
                batches.add(
                    SamplesBatch(
                        watchId = watchId,
                        batchId = java.util.UUID.randomUUID().toString(),
                        windows = currentBatchWindows.toList()
                    )
                )
            }

            return batches
        }
    }
}

@Serializable
data class SamplesAck(
    val schemaVersion: Int = DataLayerContracts.SAMPLES_ACK_SCHEMA_VERSION,
    val batchId: String,
    val accepted: Int
) {
    init {
        require(schemaVersion == DataLayerContracts.SAMPLES_ACK_SCHEMA_VERSION) {
            "Unsupported schemaVersion: $schemaVersion"
        }
        require(accepted >= 0) { "accepted must be non-negative" }
    }

    fun toByteArray(): ByteArray = jsonSerializer.encodeToString(this).encodeToByteArray()

    companion object {
        private val jsonSerializer = Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

        fun fromByteArray(bytes: ByteArray): SamplesAck {
            return jsonSerializer.decodeFromString(bytes.decodeToString())
        }
    }
}

@Serializable
data class WatchStatus(
    val schemaVersion: Int = DataLayerContracts.STATUS_SCHEMA_VERSION,
    val batteryPct: Int,
    val serviceRunning: Boolean,
    val lastWindowAt: Long? = null,
    val watchModel: String,
    val permissions: Map<String, Boolean>,
    val baselineStatus: Map<String, com.pulso.core.baseline.BucketStats>? = null,
    val sentAt: Long
) {
    init {
        require(schemaVersion == DataLayerContracts.STATUS_SCHEMA_VERSION) {
            "Unsupported schemaVersion: $schemaVersion"
        }
        require(batteryPct in 0..100) { "batteryPct must be 0..100" }
    }

    fun toByteArray(): ByteArray = jsonSerializer.encodeToString(this).encodeToByteArray()

    companion object {
        private val jsonSerializer = Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

        fun fromByteArray(bytes: ByteArray): WatchStatus {
            return jsonSerializer.decodeFromString(bytes.decodeToString())
        }
    }
}

@Serializable
data class WatchCommand(
    val schemaVersion: Int = DataLayerContracts.COMMANDS_SCHEMA_VERSION,
    val command: String,
    val sentAt: Long = System.currentTimeMillis()
) {
    init {
        require(schemaVersion == DataLayerContracts.COMMANDS_SCHEMA_VERSION) {
            "Unsupported schemaVersion: $schemaVersion"
        }
    }

    fun toByteArray(): ByteArray = jsonSerializer.encodeToString(this).encodeToByteArray()

    companion object {
        const val CMD_RESET_BASELINE = "RESET_BASELINE"

        private val jsonSerializer = Json {
            ignoreUnknownKeys = false
            encodeDefaults = true
        }

        fun fromByteArray(bytes: ByteArray): WatchCommand {
            return jsonSerializer.decodeFromString(bytes.decodeToString())
        }
    }
}
