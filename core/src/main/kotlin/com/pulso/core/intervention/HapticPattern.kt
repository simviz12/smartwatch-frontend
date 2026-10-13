package com.pulso.core.intervention

/**
 * Defines standard haptic vibration patterns for gentle interventions.
 *
 * For devices with amplitude control (VibrationEffect.createWaveform with amplitudes),
 * gentle pulse amplitude should be low (e.g. 50-80 out of 255) to prevent startle or anxiety.
 *
 * For legacy devices or Wear OS watches without fine-grained amplitude control:
 * Fallback to binary timing pulses (ON/OFF). The pattern uses very short durations
 * (e.g., 60ms pulse, 120ms pause, 60ms pulse) to create a gentle, unobtrusive tap feel
 * distinct from alarm, incoming call or standard aggressive notifications.
 */
object HapticPattern {

    /**
     * Named gentle pattern: Two short, soft pulses.
     * Timings:
     * 0 ms delay
     * 70 ms pulse 1
     * 140 ms silence
     * 70 ms pulse 2
     */
    val GENTLE_PULSE_TIMINGS = longArrayOf(0, 70, 140, 70)

    /**
     * Amplitude curve:
     * 0 amplitude at offset 0
     * 65 amplitude (out of 255) for pulse 1 (~25% intensity)
     * 0 amplitude for silence
     * 65 amplitude for pulse 2 (~25% intensity)
     */
    val GENTLE_PULSE_AMPLITUDES = intArrayOf(0, 65, 0, 65)

    /**
     * Index of repeat: -1 means do not loop/repeat.
     */
    const val REPEAT_INDEX_NONE = -1

    /**
     * Fallback binary timing for legacy vibration motors.
     */
    val BINARY_FALLBACK_TIMINGS = longArrayOf(0, 50, 120, 50)
}
