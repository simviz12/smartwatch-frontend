package com.pulso.wear.domain.model

data class SensorCapabilityReport(
    val deviceModel: String,
    val manufacturer: String,
    val wearOsVersion: String,
    val androidApiLevel: Int,
    val healthServicesSupportedDataTypes: List<String>,
    val samsungSdkAvailable: Boolean,
    val samsungSdkSupportedTrackers: List<String>,
    val permissionsGranted: Map<String, Boolean>,
    val timestamp: Long = System.currentTimeMillis()
) {
    fun toFormattedReport(): String {
        val permLines = permissionsGranted.entries.joinToString("\n") { (perm, granted) ->
            val shortName = perm.substringAfterLast(".")
            "  - $shortName: ${if (granted) "CONCEDIDO" else "DENEGADO"}"
        }
        val dataTypes = if (healthServicesSupportedDataTypes.isEmpty()) "  (Ninguno detectado)" else healthServicesSupportedDataTypes.joinToString("\n") { "  - $it" }
        val samsungTrackers = if (samsungSdkSupportedTrackers.isEmpty()) "  (No disponible o sin soporte)" else samsungSdkSupportedTrackers.joinToString("\n") { "  - $it" }

        return """
            ========================================
            INFORME DE CAPACIDADES PULSO (WEAR OS)
            Fecha: ${java.util.Date(timestamp)}
            ========================================
            Dispositivo: $manufacturer $deviceModel
            Wear OS / Android API: API $androidApiLevel (OS $wearOsVersion)
            
            [Permisos]:
            $permLines
            
            [Google Health Services - Tipos soportados]:
            $dataTypes
            
            [Samsung Health Sensor SDK]:
            - SDK AAR presente en runtime: ${if (samsungSdkAvailable) "SÍ" else "NO"}
            $samsungTrackers
            ========================================
        """.trimIndent()
    }
}
