package com.pulso.mobile.data.debug

import com.pulso.core.model.MeasurementWindow
import java.io.BufferedReader
import java.io.InputStream
import java.io.InputStreamReader

object DebugCsvImporter {

    /**
     * Parses CSV files exported by the watch or test fixtures.
     * Expected CSV headers (or flexible mapping):
     * windowStart,windowEnd,meanHr,minHr,maxHr,sampleCount,coverageRatio,rmssd,validIbiRatio,activityState,onBody
     */
    fun parseCsv(inputStream: InputStream, defaultWatchId: String = "debug-watch"): List<MeasurementWindow> {
        val reader = BufferedReader(InputStreamReader(inputStream))
        val windows = mutableListOf<MeasurementWindow>()

        val headerLine = reader.readLine() ?: return emptyList()
        val headers = headerLine.split(",").map { it.trim().lowercase() }

        val startIdx = headers.indexOfFirst { it.contains("start") }
        val endIdx = headers.indexOfFirst { it.contains("end") }
        val meanHrIdx = headers.indexOfFirst { it.contains("meanhr") || it == "hr" || it.contains("bpm") }
        val minHrIdx = headers.indexOfFirst { it.contains("minhr") }
        val maxHrIdx = headers.indexOfFirst { it.contains("maxhr") }
        val countIdx = headers.indexOfFirst { it.contains("count") || it.contains("samples") }
        val covIdx = headers.indexOfFirst { it.contains("coverage") }
        val rmssdIdx = headers.indexOfFirst { it.contains("rmssd") || it.contains("hrv") }
        val ibiRatioIdx = headers.indexOfFirst { it.contains("ibi") }
        val stateIdx = headers.indexOfFirst { it.contains("state") || it.contains("activity") }
        val onBodyIdx = headers.indexOfFirst { it.contains("onbody") || it.contains("body") }

        var line = reader.readLine()
        while (line != null) {
            val trimmed = line.trim()
            if (trimmed.isNotEmpty()) {
                val tokens = trimmed.split(",").map { it.trim() }
                try {
                    val start = if (startIdx in tokens.indices) tokens[startIdx].toLongOrNull() ?: 0L else 0L
                    val end = if (endIdx in tokens.indices) tokens[endIdx].toLongOrNull() ?: (start + 300_000L) else (start + 300_000L)
                    val meanHr = if (meanHrIdx in tokens.indices) tokens[meanHrIdx].toDoubleOrNull() ?: 70.0 else 70.0
                    val minHr = if (minHrIdx in tokens.indices) tokens[minHrIdx].toDoubleOrNull() ?: meanHr else meanHr
                    val maxHr = if (maxHrIdx in tokens.indices) tokens[maxHrIdx].toDoubleOrNull() ?: meanHr else meanHr
                    val sampleCount = if (countIdx in tokens.indices) tokens[countIdx].toIntOrNull() ?: 30 else 30
                    val coverage = if (covIdx in tokens.indices) tokens[covIdx].toDoubleOrNull() ?: 1.0 else 1.0
                    val rmssd = if (rmssdIdx in tokens.indices) tokens[rmssdIdx].toDoubleOrNull() else null
                    val validIbiRatio = if (ibiRatioIdx in tokens.indices) tokens[ibiRatioIdx].toDoubleOrNull() ?: 1.0 else 1.0
                    val state = if (stateIdx in tokens.indices) tokens[stateIdx].uppercase() else "PASSIVE"
                    val onBody = if (onBodyIdx in tokens.indices) tokens[onBodyIdx].toBooleanStrictOrNull() ?: true else true

                    if (start > 0) {
                        windows.add(
                            MeasurementWindow(
                                windowStart = start,
                                windowEnd = end,
                                meanHr = meanHr,
                                minHr = minHr,
                                maxHr = maxHr,
                                rmssd = rmssd,
                                validIbiCount = sampleCount,
                                ibiValidRatio = validIbiRatio,
                                activityState = state,
                                onBody = onBody,
                                source = defaultWatchId
                            )
                        )
                    }
                } catch (_: Exception) {
                    // Skip malformed row
                }
            }
            line = reader.readLine()
        }

        return windows
    }
}
