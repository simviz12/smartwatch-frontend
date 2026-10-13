package com.pulso.core.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SyncModelsTest {

    @Test
    fun `ConfigSync serializes and deserializes accurately`() {
        val config = ConfigSync(
            collectionEnabled = true,
            consentVersion = "1.0-es",
            windowMinutes = 5,
            quietHours = QuietHours(startHour = 22, startMinute = 0, endHour = 7, endMinute = 0),
            updatedAt = 1776050000000L
        )

        val bytes = config.toByteArray()
        val deserialized = ConfigSync.fromByteArray(bytes)

        assertEquals(config, deserialized)
        assertTrue(deserialized.collectionEnabled)
        assertEquals(5, deserialized.windowMinutes)
        assertEquals(22, deserialized.quietHours?.startHour)
    }

    @Test
    fun `SamplesBatch batching splits correctly according to count and payload size`() {
        val windows = (1..250).map { i ->
            MeasurementWindow(
                id = i.toLong(),
                windowStart = 1000L * i,
                windowEnd = 1000L * i + 60000,
                meanHr = 70.0 + (i % 20),
                minHr = 65.0,
                maxHr = 85.0,
                source = "TEST_SOURCE"
            )
        }

        val batches = SamplesBatch.createBatches(
            watchId = "watch_abc_123",
            windows = windows,
            maxCount = 100,
            maxBytes = 90 * 1024
        )

        // 250 windows with max 100 per batch should produce 3 batches (100, 100, 50)
        assertEquals(3, batches.size)
        assertEquals(100, batches[0].windows.size)
        assertEquals(100, batches[1].windows.size)
        assertEquals(50, batches[2].windows.size)

        for (batch in batches) {
            assertTrue(batch.toByteArray().size <= 90 * 1024)
            val decoded = SamplesBatch.fromByteArray(batch.toByteArray())
            assertEquals(batch.batchId, decoded.batchId)
            assertEquals("watch_abc_123", decoded.watchId)
        }
    }

    @Test
    fun `SamplesAck serializes and deserializes accurately`() {
        val ack = SamplesAck(
            batchId = "550e8400-e29b-41d4-a716-446655440000",
            accepted = 50
        )
        val decoded = SamplesAck.fromByteArray(ack.toByteArray())
        assertEquals(ack, decoded)
        assertEquals(50, decoded.accepted)
    }

    @Test
    fun `WatchStatus serializes and deserializes accurately`() {
        val status = WatchStatus(
            batteryPct = 85,
            serviceRunning = true,
            lastWindowAt = 1776050000000L,
            watchModel = "Galaxy Watch 6",
            permissions = mapOf("BODY_SENSORS" to true),
            sentAt = 1776050005000L
        )
        val decoded = WatchStatus.fromByteArray(status.toByteArray())
        assertEquals(status, decoded)
        assertEquals(85, decoded.batteryPct)
        assertEquals(true, decoded.serviceRunning)
    }
}
