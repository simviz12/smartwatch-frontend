package com.pulso.mobile.domain.baseline

import com.pulso.core.baseline.Baseline
import com.pulso.core.baseline.BaselineState
import com.pulso.core.baseline.BucketStats
import com.pulso.core.baseline.TimeBucket
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class BaselineMismatchDetectorTest {

    @Test
    fun `returns null when inputs are empty or both match`() {
        assertNull(BaselineMismatchDetector.checkMismatch(null, null))

        val phoneStats = mapOf(
            TimeBucket.MORNING to BucketStats(
                state = BaselineState.READY,
                windowCount = 35,
                requiredWindows = 30,
                daysCollected = 4,
                requiredDays = 3,
                progressPct = 100.0,
                hrMedian = 72.0,
                hrMad = 4.0,
                rmssdMedian = 45.0,
                rmssdMad = 5.0,
                updatedAt = 1000L
            )
        )
        val phoneBaseline = Baseline(buckets = phoneStats, updatedAt = 1000L)

        val watchStatusMap = mapOf(
            TimeBucket.MORNING.name to BucketStats(
                state = BaselineState.READY,
                windowCount = 34,
                requiredWindows = 30,
                daysCollected = 4,
                requiredDays = 3,
                progressPct = 100.0,
                hrMedian = 72.0,
                hrMad = 4.0,
                rmssdMedian = 45.0,
                rmssdMad = 5.0,
                updatedAt = 1000L
            )
        )

        val result = BaselineMismatchDetector.checkMismatch(phoneBaseline, watchStatusMap)
        assertNull(result)
    }

    @Test
    fun `detects state mismatch when phone is READY but watch is LEARNING with sample divergence`() {
        val phoneStats = mapOf(
            TimeBucket.MORNING to BucketStats(
                state = BaselineState.READY,
                windowCount = 35,
                requiredWindows = 30,
                daysCollected = 4,
                requiredDays = 3,
                progressPct = 100.0,
                hrMedian = 70.0,
                hrMad = 3.0,
                updatedAt = 1000L
            )
        )
        val phoneBaseline = Baseline(buckets = phoneStats, updatedAt = 1000L)

        val watchStatusMap = mapOf(
            TimeBucket.MORNING.name to BucketStats(
                state = BaselineState.LEARNING,
                windowCount = 10,
                requiredWindows = 30,
                daysCollected = 1,
                requiredDays = 3,
                progressPct = 33.3,
                updatedAt = 1000L
            )
        )

        val result = BaselineMismatchDetector.checkMismatch(phoneBaseline, watchStatusMap)
        assertNotNull(result)
        assertEquals(
            "Línea base del reloj y del celular no coinciden: bucket MORNING está LEARNING en reloj y READY en celular",
            result
        )
    }

    @Test
    fun `detects sample count discrepancy above tolerance in same state`() {
        val phoneStats = mapOf(
            TimeBucket.EVENING to BucketStats(
                state = BaselineState.LEARNING,
                windowCount = 28,
                requiredWindows = 30,
                daysCollected = 2,
                requiredDays = 3,
                progressPct = 90.0,
                updatedAt = 1000L
            )
        )
        val phoneBaseline = Baseline(buckets = phoneStats, updatedAt = 1000L)

        val watchStatusMap = mapOf(
            TimeBucket.EVENING.name to BucketStats(
                state = BaselineState.LEARNING,
                windowCount = 5,
                requiredWindows = 30,
                daysCollected = 1,
                requiredDays = 3,
                progressPct = 16.6,
                updatedAt = 1000L
            )
        )

        val result = BaselineMismatchDetector.checkMismatch(phoneBaseline, watchStatusMap)
        assertNotNull(result)
        assertEquals(
            "Línea base del reloj y del celular no coinciden: discrepancia de 23 mediciones en EVENING",
            result
        )
    }
}
