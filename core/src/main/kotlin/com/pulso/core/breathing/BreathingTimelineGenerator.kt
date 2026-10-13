package com.pulso.core.breathing

import kotlin.math.roundToInt

object BreathingTimelineGenerator {

    const val START_TAP_DURATION_MS = 250L
    const val START_PAUSE_MS = 500L
    const val END_DOUBLE_TAP_DURATION_MS = 350L

    /**
     * Converts a [BreathingPlan] and [HapticIntensity] into a continuous haptic timeline.
     * Guaranteed:
     * - No gaps between segments (segment[i+1].startTime == segment[i].startTime + segment[i].duration).
     * - Inhale envelope gently grows across inhale duration.
     * - Exhale envelope gently fades across exhale duration.
     * - Hold periods contain 0 amplitude (silence).
     * - Start is marked by one distinct tap.
     * - End is marked by a double tap.
     */
    fun generateTimeline(
        plan: BreathingPlan,
        intensity: HapticIntensity = HapticIntensity.MEDIUM,
        stepMs: Long = 100L
    ): List<HapticTimelineSegment> {
        val segments = mutableListOf<HapticTimelineSegment>()
        var currentTime = 0L

        val maxAmp = when (intensity) {
            HapticIntensity.LOW -> 120
            HapticIntensity.MEDIUM -> 180
            HapticIntensity.HIGH -> 240
        }
        val minInhaleAmp = (maxAmp * 0.20).roundToInt().coerceAtLeast(30)
        val minExhaleAmp = (maxAmp * 0.15).roundToInt().coerceAtLeast(20)

        // 1. Start Signal: 1 distinct tap (100ms vibration + 150ms decay/silence)
        val startAmpSteps = (START_TAP_DURATION_MS / stepMs).toInt().coerceAtLeast(2)
        val startAmps = IntArray(startAmpSteps) { idx ->
            if (idx == 0) maxAmp else (maxAmp * 0.5).roundToInt()
        }
        segments.add(
            HapticTimelineSegment(
                type = TimelineSegmentType.START_TAP,
                cycleIndex = 0,
                startTimeMs = currentTime,
                durationMs = START_TAP_DURATION_MS,
                amplitudes = startAmps,
                stepMs = stepMs
            )
        )
        currentTime += START_TAP_DURATION_MS

        // Optional short settle pause before cycle 1
        if (START_PAUSE_MS > 0) {
            val pauseSteps = (START_PAUSE_MS / stepMs).toInt().coerceAtLeast(1)
            segments.add(
                HapticTimelineSegment(
                    type = TimelineSegmentType.HOLD_AFTER_EXHALE,
                    cycleIndex = 0,
                    startTimeMs = currentTime,
                    durationMs = START_PAUSE_MS,
                    amplitudes = IntArray(pauseSteps) { 0 },
                    stepMs = stepMs
                )
            )
            currentTime += START_PAUSE_MS
        }

        // 2. Breathing Cycles
        for (cycle in 1..plan.cycles) {
            // INHALE: rising amplitude (minInhaleAmp -> maxAmp)
            val inhaleSteps = (plan.inhaleMs / stepMs).toInt().coerceAtLeast(1)
            val inhaleAmps = IntArray(inhaleSteps) { stepIdx ->
                val progress = stepIdx.toDouble() / (inhaleSteps - 1).coerceAtLeast(1)
                (minInhaleAmp + progress * (maxAmp - minInhaleAmp)).roundToInt().coerceIn(0, 255)
            }
            segments.add(
                HapticTimelineSegment(
                    type = TimelineSegmentType.INHALE,
                    cycleIndex = cycle,
                    startTimeMs = currentTime,
                    durationMs = plan.inhaleMs,
                    amplitudes = inhaleAmps,
                    stepMs = stepMs
                )
            )
            currentTime += plan.inhaleMs

            // HOLD AFTER INHALE (silence)
            if (plan.holdAfterInhaleMs > 0) {
                val holdSteps = (plan.holdAfterInhaleMs / stepMs).toInt().coerceAtLeast(1)
                segments.add(
                    HapticTimelineSegment(
                        type = TimelineSegmentType.HOLD_AFTER_INHALE,
                        cycleIndex = cycle,
                        startTimeMs = currentTime,
                        durationMs = plan.holdAfterInhaleMs,
                        amplitudes = IntArray(holdSteps) { 0 },
                        stepMs = stepMs
                    )
                )
                currentTime += plan.holdAfterInhaleMs
            }

            // EXHALE: descending amplitude (maxAmp -> minExhaleAmp)
            val exhaleSteps = (plan.exhaleMs / stepMs).toInt().coerceAtLeast(1)
            val exhaleAmps = IntArray(exhaleSteps) { stepIdx ->
                val progress = stepIdx.toDouble() / (exhaleSteps - 1).coerceAtLeast(1)
                (maxAmp - progress * (maxAmp - minExhaleAmp)).roundToInt().coerceIn(0, 255)
            }
            segments.add(
                HapticTimelineSegment(
                    type = TimelineSegmentType.EXHALE,
                    cycleIndex = cycle,
                    startTimeMs = currentTime,
                    durationMs = plan.exhaleMs,
                    amplitudes = exhaleAmps,
                    stepMs = stepMs
                )
            )
            currentTime += plan.exhaleMs

            // HOLD AFTER EXHALE (silence)
            if (plan.holdAfterExhaleMs > 0) {
                val holdSteps = (plan.holdAfterExhaleMs / stepMs).toInt().coerceAtLeast(1)
                segments.add(
                    HapticTimelineSegment(
                        type = TimelineSegmentType.HOLD_AFTER_EXHALE,
                        cycleIndex = cycle,
                        startTimeMs = currentTime,
                        durationMs = plan.holdAfterExhaleMs,
                        amplitudes = IntArray(holdSteps) { 0 },
                        stepMs = stepMs
                    )
                )
                currentTime += plan.holdAfterExhaleMs
            }
        }

        // 3. End Signal: Double Tap (tap 100ms + pause 100ms + tap 150ms)
        val endSteps = (END_DOUBLE_TAP_DURATION_MS / stepMs).toInt().coerceAtLeast(3)
        val endAmps = IntArray(endSteps) { idx ->
            when (idx) {
                0 -> maxAmp
                1 -> 0
                else -> maxAmp
            }
        }
        segments.add(
            HapticTimelineSegment(
                type = TimelineSegmentType.END_DOUBLE_TAP,
                cycleIndex = plan.cycles,
                startTimeMs = currentTime,
                durationMs = END_DOUBLE_TAP_DURATION_MS,
                amplitudes = endAmps,
                stepMs = stepMs
            )
        )

        return segments
    }

    /**
     * Binary fallback waveform generator for devices without amplitude control.
     * Uses pulse density modulation:
     * - INHALE: dense pulses (e.g., [on=60ms, off=40ms])
     * - EXHALE: sparser pulses (e.g., [on=80ms, off=180ms])
     * - HOLD: silence
     * Returns pairs of timings (ms) and on/off states (true=vibrate, false=rest).
     */
    fun generateBinaryPulseTimings(
        segment: HapticTimelineSegment
    ): Pair<LongArray, IntArray> {
        when (segment.type) {
            TimelineSegmentType.START_TAP -> {
                // 1 distinct pulse: 120ms on, rest of duration off
                val onMs = 120L.coerceAtMost(segment.durationMs)
                val offMs = segment.durationMs - onMs
                return Pair(longArrayOf(0, onMs, offMs), intArrayOf(0, 255, 0))
            }
            TimelineSegmentType.END_DOUBLE_TAP -> {
                // Double pulse: 100ms on, 80ms off, 120ms on
                return Pair(longArrayOf(0, 100, 80, 120), intArrayOf(0, 255, 0, 255))
            }
            TimelineSegmentType.HOLD_AFTER_INHALE, TimelineSegmentType.HOLD_AFTER_EXHALE -> {
                // Silence
                return Pair(longArrayOf(0, segment.durationMs), intArrayOf(0, 0))
            }
            TimelineSegmentType.INHALE -> {
                // Dense pulses: on 60ms, off 50ms
                val timings = mutableListOf<Long>(0L)
                val amps = mutableListOf<Int>(0)
                var rem = segment.durationMs
                var state = true
                while (rem > 0) {
                    val step = if (state) 60L.coerceAtMost(rem) else 50L.coerceAtMost(rem)
                    timings.add(step)
                    amps.add(if (state) 255 else 0)
                    rem -= step
                    state = !state
                }
                return Pair(timings.toLongArray(), amps.toIntArray())
            }
            TimelineSegmentType.EXHALE -> {
                // Sparse pulses: on 80ms, off 160ms
                val timings = mutableListOf<Long>(0L)
                val amps = mutableListOf<Int>(0)
                var rem = segment.durationMs
                var state = true
                while (rem > 0) {
                    val step = if (state) 80L.coerceAtMost(rem) else 160L.coerceAtMost(rem)
                    timings.add(step)
                    amps.add(if (state) 255 else 0)
                    rem -= step
                    state = !state
                }
                return Pair(timings.toLongArray(), amps.toIntArray())
            }
        }
    }
}
