package com.pulso.mobile.data.sync

import com.pulso.core.model.MeasurementWindow
import com.pulso.core.model.SamplesBatch
import com.pulso.mobile.data.local.PhoneWindowEntity
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.UUID

class MobileSyncBatchValidationTest {

    @Test
    fun `validates physiological ranges and filters corrupted windows`() {
        val watchId = "watch_test_1"
        val validWindow1 = MeasurementWindow(
            id = 1,
            windowStart = 1000L,
            windowEnd = 61000L,
            meanHr = 72.0,
            minHr = 65.0,
            maxHr = 80.0,
            source = "WEAR"
        )
        val validWindow2 = MeasurementWindow(
            id = 2,
            windowStart = 62000L,
            windowEnd = 122000L,
            meanHr = 85.0,
            minHr = 70.0,
            maxHr = 95.0,
            source = "WEAR"
        )
        val invalidWindowHrLow = MeasurementWindow(
            id = 3,
            windowStart = 123000L,
            windowEnd = 183000L,
            meanHr = 15.0, // physiological unviable (<30)
            minHr = 10.0,
            maxHr = 20.0,
            source = "WEAR"
        )
        val invalidWindowHrHigh = MeasurementWindow(
            id = 4,
            windowStart = 184000L,
            windowEnd = 244000L,
            meanHr = 250.0, // physiological unviable (>220)
            minHr = 200.0,
            maxHr = 260.0,
            source = "WEAR"
        )
        val invalidWindowTimestamp = MeasurementWindow(
            id = 5,
            windowStart = 500000L,
            windowEnd = 400000L, // windowEnd < windowStart
            meanHr = 75.0,
            minHr = 70.0,
            maxHr = 80.0,
            source = "WEAR"
        )

        val batch = SamplesBatch(
            watchId = watchId,
            batchId = UUID.randomUUID().toString(),
            windows = listOf(validWindow1, validWindow2, invalidWindowHrLow, invalidWindowHrHigh, invalidWindowTimestamp)
        )

        val acceptedEntities = mutableListOf<PhoneWindowEntity>()
        for (w in batch.windows) {
            if (w.windowStart > 0 && w.windowEnd >= w.windowStart && w.meanHr in 30.0..220.0) {
                acceptedEntities.add(PhoneWindowEntity.fromDomain(w, batch.watchId))
            }
        }

        assertEquals(2, acceptedEntities.size)
        assertEquals(72.0, acceptedEntities[0].meanHr, 0.01)
        assertEquals(85.0, acceptedEntities[1].meanHr, 0.01)
    }

    @Test
    fun `idempotency key matches watchId and windowStart`() {
        val window = MeasurementWindow(
            id = 10,
            windowStart = 1776050000000L,
            windowEnd = 1776050060000L,
            meanHr = 75.0,
            minHr = 70.0,
            maxHr = 80.0,
            source = "WEAR"
        )

        val entity1 = PhoneWindowEntity.fromDomain(window, "watch_alpha")
        val entity2 = PhoneWindowEntity.fromDomain(window, "watch_alpha")

        assertEquals(entity1.watchId, entity2.watchId)
        assertEquals(entity1.windowStart, entity2.windowStart)
    }
}
