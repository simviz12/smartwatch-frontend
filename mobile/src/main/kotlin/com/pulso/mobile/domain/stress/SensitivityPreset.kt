package com.pulso.mobile.domain.stress

import com.pulso.core.stress.StressConfig

enum class SensitivityPreset(val displayName: String, val description: String) {
    BAJA(
        displayName = "Baja",
        description = "Menos avisos. Requiere desviaciones más marcadas y sostenidas para activarse."
    ),
    MEDIA(
        displayName = "Media",
        description = "Equilibrio estándar recomendado. 3 ventanas consecutivas con pulso elevado y HRV bajo."
    ),
    ALTA(
        displayName = "Alta",
        description = "Mayor sensibilidad ante aumentos moderados de pulso o menor tolerancia a tensión."
    ),
    PERSONALIZADO(
        displayName = "Personalizado",
        description = "Valores ajustados manualmente en la sección avanzada."
    );

    companion object {
        fun toConfig(preset: SensitivityPreset, current: StressConfig = StressConfig()): StressConfig {
            return when (preset) {
                BAJA -> current.copy(
                    hrZThreshold = 1.4,
                    hrvZThreshold = -1.4,
                    hrOnlyZThreshold = 2.0,
                    consecutiveWindowsRuleA = 4,
                    consecutiveWindowsRuleB = 5
                )
                MEDIA -> current.copy(
                    hrZThreshold = 1.0,
                    hrvZThreshold = -1.0,
                    hrOnlyZThreshold = 1.5,
                    consecutiveWindowsRuleA = 3,
                    consecutiveWindowsRuleB = 4
                )
                ALTA -> current.copy(
                    hrZThreshold = 0.8,
                    hrvZThreshold = -0.8,
                    hrOnlyZThreshold = 1.2,
                    consecutiveWindowsRuleA = 2,
                    consecutiveWindowsRuleB = 3
                )
                PERSONALIZADO -> current
            }
        }

        fun fromConfig(config: StressConfig): SensitivityPreset {
            return when {
                config.hrZThreshold == 1.4 && config.hrvZThreshold == -1.4 && config.hrOnlyZThreshold == 2.0 &&
                        config.consecutiveWindowsRuleA == 4 && config.consecutiveWindowsRuleB == 5 -> BAJA
                config.hrZThreshold == 1.0 && config.hrvZThreshold == -1.0 && config.hrOnlyZThreshold == 1.5 &&
                        config.consecutiveWindowsRuleA == 3 && config.consecutiveWindowsRuleB == 4 -> MEDIA
                config.hrZThreshold == 0.8 && config.hrvZThreshold == -0.8 && config.hrOnlyZThreshold == 1.2 &&
                        config.consecutiveWindowsRuleA == 2 && config.consecutiveWindowsRuleB == 3 -> ALTA
                else -> PERSONALIZADO
            }
        }
    }
}
