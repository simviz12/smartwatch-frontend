package com.pulso.core.hrv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class HrvCalculatorTest {

    @Test
    fun `cleanIbis removes unphysiological boundaries below 300ms and above 2000ms`() {
        val raw = listOf(150, 800, 850, 2500, 900, 100)
        val cleaned = HrvCalculator.cleanIbis(raw)

        assertEquals(listOf(800, 850, 900), cleaned)
    }

    @Test
    fun `cleanIbis removes sudden excessive jumps above 300ms`() {
        // 800 -> 820 (+20 OK) -> 1300 (+480 NOT OK, motion/ectopic) -> 830 (+10 from 820 OK)
        val raw = listOf(800, 820, 1300, 830)
        val cleaned = HrvCalculator.cleanIbis(raw)

        assertEquals(listOf(800, 820, 830), cleaned)
    }

    @Test
    fun `calculateRmssd on constant sequence yields 0 ms`() {
        val constantIbis = List(35) { 1000 }
        val result = HrvCalculator.calculateRmssd(constantIbis, minCount = 30)

        assertTrue(result is RmssdResult.Success)
        val success = result as RmssdResult.Success
        assertEquals(0.0, success.rmssd, 0.001)
        assertEquals(35, success.validCount)
        assertEquals(1.0, success.validRatio, 0.001)
    }

    @Test
    fun `calculateRmssd hand computed vector matches mathematical formula`() {
        // Alternate differences: 800, 850 (+50), 800 (-50), 850 (+50)...
        // diffs squared: 50^2 = 2500. Mean = 2500. sqrt(2500) = 50.0
        val alternating = (0 until 35).map { if (it % 2 == 0) 800 else 850 }
        val result = HrvCalculator.calculateRmssd(alternating, minCount = 30)

        assertTrue(result is RmssdResult.Success)
        val success = result as RmssdResult.Success
        assertEquals(50.0, success.rmssd, 0.001)
    }

    @Test
    fun `calculateRmssd with empty list returns InsufficientData with 0 valid count`() {
        val result = HrvCalculator.calculateRmssd(emptyList())

        assertTrue(result is RmssdResult.InsufficientData)
        val failure = result as RmssdResult.InsufficientData
        assertEquals(0, failure.validCount)
    }

    @Test
    fun `calculateRmssd with fewer than minCount samples returns InsufficientData`() {
        val fewSamples = List(20) { 800 }
        val result = HrvCalculator.calculateRmssd(fewSamples, minCount = 30)

        assertTrue(result is RmssdResult.InsufficientData)
        val failure = result as RmssdResult.InsufficientData
        assertEquals(20, failure.validCount)
        assertEquals(30, failure.requiredCount)
    }
}
