package com.pulso.wear.data.sensor

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import com.google.common.util.concurrent.Futures
import com.google.common.util.concurrent.ListenableFuture
import com.pulso.core.model.MeasurementWindow
import com.pulso.wear.data.local.PulsoWearDatabase
import com.pulso.wear.data.local.WindowEntity
import com.pulso.wear.domain.model.LiveHeartRateState
import com.pulso.wear.domain.model.SensorCapabilityReport
import com.pulso.wear.domain.repository.SensorRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.guava.await
import java.io.File

class AndroidSensorRepository(
    private val context: Context
) : SensorRepository {

    private val healthServicesClient = HealthServices.getClient(context)
    private val measureClient = healthServicesClient.measureClient
    private val passiveMonitoringClient = healthServicesClient.passiveMonitoringClient
    private val database = PulsoWearDatabase.getInstance(context)
    private val windowDao = database.windowDao()

    override suspend fun getCapabilityReport(): SensorCapabilityReport {
        val permissions = mapOf(
            android.Manifest.permission.BODY_SENSORS to (ContextCompat.checkSelfPermission(context, android.Manifest.permission.BODY_SENSORS) == PackageManager.PERMISSION_GRANTED),
            android.Manifest.permission.BODY_SENSORS_BACKGROUND to (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                ContextCompat.checkSelfPermission(context, android.Manifest.permission.BODY_SENSORS_BACKGROUND) == PackageManager.PERMISSION_GRANTED
            } else true),
            android.Manifest.permission.ACTIVITY_RECOGNITION to (ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACTIVITY_RECOGNITION) == PackageManager.PERMISSION_GRANTED)
        )

        val supportedTypes = mutableListOf<String>()
        try {
            val capabilitiesFuture: com.google.common.util.concurrent.ListenableFuture<androidx.health.services.client.data.PassiveMonitoringCapabilities> = passiveMonitoringClient.getCapabilitiesAsync()
            val capabilities = capabilitiesFuture.await()
            for (type in capabilities.supportedDataTypesPassiveMonitoring) {
                supportedTypes.add(type.name)
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error querying passive capabilities: ${e.message}")
            supportedTypes.add("DataType.HEART_RATE_BPM (por defecto)")
        }

        val samsungSdkPresent = try {
            Class.forName("com.samsung.android.service.health.tracking.HealthTrackingService")
            true
        } catch (e: ClassNotFoundException) {
            false
        }

        val samsungTrackers = if (samsungSdkPresent) {
            listOf("HEART_RATE_CONTINUOUS (con soporte de IBI si se activa Developer Mode)")
        } else {
            listOf("AAR no empaquetado en runtime; requiere SDK privado de Samsung")
        }

        return SensorCapabilityReport(
            deviceModel = Build.MODEL,
            manufacturer = Build.MANUFACTURER,
            wearOsVersion = Build.VERSION.RELEASE,
            androidApiLevel = Build.VERSION.SDK_INT,
            healthServicesSupportedDataTypes = supportedTypes,
            samsungSdkAvailable = samsungSdkPresent,
            samsungSdkSupportedTrackers = samsungTrackers,
            permissionsGranted = permissions
        )
    }

    override fun observeLiveHeartRate(): Flow<LiveHeartRateState> = callbackFlow {
        val callback = object : MeasureCallback {
            override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
                val availStr = when (availability) {
                    DataTypeAvailability.AVAILABLE -> "DISPONIBLE"
                    DataTypeAvailability.ACQUIRING -> "ADQUIRIENDO..."
                    DataTypeAvailability.UNAVAILABLE -> "NO DISPONIBLE"
                    DataTypeAvailability.UNAVAILABLE_DEVICE_OFF_BODY -> "RELOJ FUERA DEL CUERPO"
                    else -> availability.toString()
                }
                val onBody = availability != DataTypeAvailability.UNAVAILABLE_DEVICE_OFF_BODY
                trySend(LiveHeartRateState(availability = availStr, onBody = onBody))
            }

            override fun onDataReceived(data: DataPointContainer) {
                val heartRatePoints = data.getData(DataType.HEART_RATE_BPM)
                if (heartRatePoints.isNotEmpty()) {
                    val latest = heartRatePoints.last()
                    val bpm = latest.value
                    trySend(
                        LiveHeartRateState(
                            bpm = bpm,
                            availability = "DISPONIBLE",
                            onBody = true
                        )
                    )
                }
            }
        }

        Log.d(TAG, "Registering MeasureClient callback for HEART_RATE_BPM")
        measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, callback)

        awaitClose {
            Log.d(TAG, "Unregistering MeasureClient callback for HEART_RATE_BPM")
            Futures.getUnchecked(measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, callback))
        }
    }

    override fun observeSavedWindows(): Flow<List<MeasurementWindow>> {
        return windowDao.getAllWindowsFlow().map { list -> list.map { it.toDomain() } }
    }

    override suspend fun saveWindow(window: MeasurementWindow): Long {
        return windowDao.insertWindow(WindowEntity.fromDomain(window))
    }

    override suspend fun exportWindowsToCsv(): File {
        val windows = windowDao.getAllWindows()
        val exportDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "exports")
        if (!exportDir.exists()) exportDir.mkdirs()

        val csvFile = File(exportDir, "pulso_windows_${System.currentTimeMillis()}.csv")
        csvFile.bufferedWriter().use { writer ->
            writer.write(MeasurementWindow.CSV_HEADER)
            writer.newLine()
            for (w in windows) {
                writer.write(w.toDomain().toCsvRow())
                writer.newLine()
            }
        }
        Log.i(TAG, "Exported ${windows.size} windows to ${csvFile.absolutePath}")
        return csvFile
    }

    override suspend fun startBackgroundMeasurementService() {
        SensorSamplingService.start(context)
    }

    override suspend fun stopBackgroundMeasurementService() {
        SensorSamplingService.stop(context)
    }

    companion object {
        private const val TAG = "PulsoSensorRepo"
    }
}
