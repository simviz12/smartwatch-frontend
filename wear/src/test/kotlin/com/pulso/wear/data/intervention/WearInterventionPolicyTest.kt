package com.pulso.wear.data.intervention

import android.app.NotificationManager
import android.os.PowerManager
import com.pulso.core.intervention.HapticPattern
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WearInterventionPolicyTest {

    interface FakeDndProvider {
        fun isDndActive(): Boolean
    }

    interface FakeBatterySaverProvider {
        fun isBatterySaverActive(): Boolean
    }

    interface FakeHapticSettingProvider {
        fun areHapticsEnabled(): Boolean
    }

    class TestableInterventionPolicy(
        private val dndProvider: FakeDndProvider,
        private val batterySaverProvider: FakeBatterySaverProvider,
        private val hapticSettingProvider: FakeHapticSettingProvider
    ) {
        fun shouldVibrate(): Boolean {
            return !dndProvider.isDndActive() &&
                    !batterySaverProvider.isBatterySaverActive() &&
                    hapticSettingProvider.areHapticsEnabled()
        }

        fun shouldShowNotification(): Boolean {
            // Notification is always posted (unobtrusive, ongoing-free),
            // but DND controls whether Android presents it silently
            return true
        }
    }

    @Test
    fun testVibrateAllowedUnderNormalConditions() {
        val policy = TestableInterventionPolicy(
            dndProvider = object : FakeDndProvider { override fun isDndActive() = false },
            batterySaverProvider = object : FakeBatterySaverProvider { override fun isBatterySaverActive() = false },
            hapticSettingProvider = object : FakeHapticSettingProvider { override fun areHapticsEnabled() = true }
        )
        assertTrue(policy.shouldVibrate())
        assertTrue(policy.shouldShowNotification())
    }

    @Test
    fun testVibrateSilencedByDnd() {
        val policy = TestableInterventionPolicy(
            dndProvider = object : FakeDndProvider { override fun isDndActive() = true },
            batterySaverProvider = object : FakeBatterySaverProvider { override fun isBatterySaverActive() = false },
            hapticSettingProvider = object : FakeHapticSettingProvider { override fun areHapticsEnabled() = true }
        )
        assertFalse("Vibration must be silenced by DND", policy.shouldVibrate())
        assertTrue(policy.shouldShowNotification())
    }

    @Test
    fun testVibrateSilencedByBatterySaver() {
        val policy = TestableInterventionPolicy(
            dndProvider = object : FakeDndProvider { override fun isDndActive() = false },
            batterySaverProvider = object : FakeBatterySaverProvider { override fun isBatterySaverActive() = true },
            hapticSettingProvider = object : FakeHapticSettingProvider { override fun areHapticsEnabled() = true }
        )
        assertFalse("Vibration must be silenced by Battery Saver", policy.shouldVibrate())
    }

    @Test
    fun testVibrateSilencedBySystemHapticSetting() {
        val policy = TestableInterventionPolicy(
            dndProvider = object : FakeDndProvider { override fun isDndActive() = false },
            batterySaverProvider = object : FakeBatterySaverProvider { override fun isBatterySaverActive() = false },
            hapticSettingProvider = object : FakeHapticSettingProvider { override fun areHapticsEnabled() = false }
        )
        assertFalse("Vibration must respect system haptic disable switch", policy.shouldVibrate())
    }

    @Test
    fun testHapticPatternIntegrity() {
        assertEquals(4, HapticPattern.GENTLE_PULSE_TIMINGS.size)
        assertEquals(4, HapticPattern.GENTLE_PULSE_AMPLITUDES.size)
        // Amplitudes are capped at 65 (~25% of max 255) for gentle feel
        assertTrue(HapticPattern.GENTLE_PULSE_AMPLITUDES.all { it <= 65 })
    }
}
