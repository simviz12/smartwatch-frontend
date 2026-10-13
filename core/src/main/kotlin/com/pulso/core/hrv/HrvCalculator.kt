package com.pulso.core.hrv

sealed interface RmssdResult {
    data class Success(
        val rmssd: Double,
        val validCount: Int,
        val totalCount: Int,
        val validRatio: Double
    ) : RmssdResult

    data class InsufficientData(
        val validCount: Int,
        val requiredCount: Int,
        val reason: String
    ) : RmssdResult
}

object HrvCalculator {

    const val MIN_IBI_MS = 300
    const val MAX_IBI_MS = 2000
    const val DEFAULT_MIN_SAMPLE_COUNT = 30
    const val MAX_SUCCESSIVE_DIFF_MS = 300

    /**
     * Cleans an array of raw IBI values in milliseconds:
     * 1. Drops values outside physiological boundaries [300ms, 2000ms] (corresponding to 30 to 200 BPM).
     * 2. Drops successive-difference outliers (> 300ms sudden jump between consecutive beats,
     *    which indicates ectopic beats or optical sensor motion artifacts).
     *
     * Returns the filtered list of physiological intervals.
     */
    fun cleanIbis(rawIbisMs: List<Int>): List<Int> {
        if (rawIbisMs.isEmpty()) return emptyList()

        val bounded = rawIbisMs.filter { it in MIN_IBI_MS..MAX_IBI_MS }
        if (bounded.size < 2) return bounded

        val cleaned = mutableListOf<Int>()
        cleaned.add(bounded[0])

        for (i in 1 until bounded.size) {
            val prev = cleaned.last()
            val curr = bounded[i]
            val diff = kotlin.math.abs(curr - prev)
            if (diff <= MAX_SUCCESSIVE_DIFF_MS) {
                cleaned.add(curr)
            }
        }

        return cleaned
    }

    /**
     * Computes the Root Mean Square of Successive Differences (RMSSD) from raw IBI intervals.
     * Formula: sqrt( 1 / (N - 1) * sum( (RR_{i+1} - RR_i)^2 ) )
     * Requires at least [minCount] valid intervals.
     */
    fun calculateRmssd(
        rawIbisMs: List<Int>,
        minCount: Int = DEFAULT_MIN_SAMPLE_COUNT
    ): RmssdResult {
        if (rawIbisMs.isEmpty()) {
            return RmssdResult.InsufficientData(
                validCount = 0,
                requiredCount = minCount,
                reason = "No se recibieron intervalos IBI."
            )
        }

        val cleaned = cleanIbis(rawIbisMs)
        val validCount = cleaned.size
        val totalCount = rawIbisMs.size
        val validRatio = if (totalCount > 0) validCount.toDouble() / totalCount.toDouble() else 0.0

        if (validCount < minCount) {
            return RmssdResult.InsufficientData(
                validCount = validCount,
                requiredCount = minCount,
                reason = "Muestras válidas insuficientes ($validCount de $minCount mínimas)."
            )
        }

        var sumSquaredDiffs = 0.0
        for (i in 0 until cleaned.size - 1) {
            val diff = (cleaned[i + 1] - cleaned[i]).toDouble()
            sumSquaredDiffs += diff * diff
        }

        val meanSquaredDiff = sumSquaredDiffs / (cleaned.size - 1)
        val rmssd = kotlin.math.sqrt(meanSquaredDiff)

        return RmssdResult.Success(
            rmssd = rmssd,
            validCount = validCount,
            totalCount = totalCount,
            validRatio = validRatio
        )
    }
}
