package com.pulso.mobile.domain.stress

import com.pulso.core.stress.StressConfig
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class SensitivityPresetTest {

    @Test
    fun testBajaPresetMapping() {
        val config = SensitivityPreset.toConfig(SensitivityPreset.BAJA)
        assertEquals(1.4, config.hrZThreshold, 0.001)
        assertEquals(-1.4, config.hrvZThreshold, 0.001)
        assertEquals(2.0, config.hrOnlyZThreshold, 0.001)
        assertEquals(4, config.consecutiveWindowsRuleA)
        assertEquals(5, config.consecutiveWindowsRuleB)

        val resolved = SensitivityPreset.fromConfig(config)
        assertEquals(SensitivityPreset.BAJA, resolved)
    }

    @Test
    fun testMediaPresetMapping() {
        val config = SensitivityPreset.toConfig(SensitivityPreset.MEDIA)
        assertEquals(1.0, config.hrZThreshold, 0.001)
        assertEquals(-1.0, config.hrvZThreshold, 0.001)
        assertEquals(1.5, config.hrOnlyZThreshold, 0.001)
        assertEquals(3, config.consecutiveWindowsRuleA)
        assertEquals(4, config.consecutiveWindowsRuleB)

        val resolved = SensitivityPreset.fromConfig(config)
        assertEquals(SensitivityPreset.MEDIA, resolved)
    }

    @Test
    fun testAltaPresetMapping() {
        val config = SensitivityPreset.toConfig(SensitivityPreset.ALTA)
        assertEquals(0.8, config.hrZThreshold, 0.001)
        assertEquals(-0.8, config.hrvZThreshold, 0.001)
        assertEquals(1.2, config.hrOnlyZThreshold, 0.001)
        assertEquals(2, config.consecutiveWindowsRuleA)
        assertEquals(3, config.consecutiveWindowsRuleB)

        val resolved = SensitivityPreset.fromConfig(config)
        assertEquals(SensitivityPreset.ALTA, resolved)
    }

    @Test
    fun testPersonalizadoPresetMapping() {
        val customConfig = StressConfig(
            hrZThreshold = 1.25,
            hrvZThreshold = -1.15,
            hrOnlyZThreshold = 1.8,
            consecutiveWindowsRuleA = 3,
            consecutiveWindowsRuleB = 4
        )
        val resolved = SensitivityPreset.fromConfig(customConfig)
        assertEquals(SensitivityPreset.PERSONALIZADO, resolved)
    }
}
