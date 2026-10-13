package com.pulso.wear.data.sensor

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.health.services.client.HealthServices
import androidx.health.services.client.MeasureCallback
import androidx.health.services.client.data.Availability
import androidx.health.services.client.data.DataPointContainer
import androidx.health.services.client.data.DataType
import androidx.health.services.client.data.DataTypeAvailability
import androidx.health.services.client.data.DeltaDataType
import com.google.common.util.concurrent.Futures
import com.pulso.core.model.MeasurementWindow
import com.pulso.wear.data.local.PulsoWearDatabase
import com.pulso.wear.data.local.WindowEntity
import com.pulso.wear.data.sync.SyncManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.CopyOnWriteArrayList

class SensorSamplingService : Service() {

    private val serviceScope = CoroutineScope(Dispatchers.Default + SupervisorJob())
    private var wakeLock: PowerManager.WakeLock? = null
    private val windowSamples = CopyOnWriteArrayList<Double>()
    private var isMeasuring = false
    private var currentOnBody = true

    private val healthServicesClient by lazy { HealthServices.getClient(this) }
    private val measureClient by lazy { healthServicesClient.measureClient }
    private val database by lazy { PulsoWearDatabase.getInstance(this) }
    private val syncManager by lazy { SyncManager.getInstance(this) }

    private val measureCallback = object : MeasureCallback {
        override fun onAvailabilityChanged(dataType: DeltaDataType<*, *>, availability: Availability) {
            currentOnBody = availability != DataTypeAvailability.UNAVAILABLE_DEVICE_OFF_BODY
            Log.d(TAG, "Service onAvailabilityChanged: $availability, onBody=$currentOnBody")
        }

        override fun onDataReceived(data: DataPointContainer) {
            val points = data.getData(DataType.HEART_RATE_BPM)
            for (p in points) {
                windowSamples.add(p.value)
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        if (!syncManager.isCollectionEnabled()) {
            Log.w(TAG, "Consent gate blocked: collectionEnabled is false. Stopping service immediately.")
            stopSelf()
            return
        }

        createNotificationChannel()
        acquireWakeLock()
        startAsForeground()
        startSamplingLoop()
    }

    private fun acquireWakeLock() {
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "Pulso:SensorSamplingWakeLock").apply {
            setReferenceCounted(false)
            acquire(24 * 60 * 60 * 1000L) // 24 hours max safeguard
        }
    }

    private fun startAsForeground() {
        val notification = createNotification("Monitoreando pulso periódicamente...")
        val serviceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
        } else {
            0
        }
        ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, serviceType)
    }

    private fun startSamplingLoop() {
        serviceScope.launch {
            Log.i(TAG, "Starting periodic sensor sampling loop with consent gate active")
            while (isActive) {
                if (!syncManager.isCollectionEnabled()) {
                    Log.i(TAG, "Consent revoked dynamically. Halting sampling service.")
                    stopSelf()
                    break
                }

                val windowStart = System.currentTimeMillis()
                windowSamples.clear()

                try {
                    isMeasuring = true
                    measureClient.registerMeasureCallback(DataType.HEART_RATE_BPM, measureCallback)
                    Log.d(TAG, "Window measurement started at $windowStart")
                    delay(60_000L) // 60 seconds window
                } catch (e: Exception) {
                    Log.e(TAG, "Error during active measure window", e)
                } finally {
                    try {
                        Futures.getUnchecked(measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, measureCallback))
                    } catch (e: Exception) {
                        Log.w(TAG, "Error unregistering measure callback", e)
                    }
                    isMeasuring = false
                }

                val windowEnd = System.currentTimeMillis()
                if (windowSamples.isNotEmpty() && currentOnBody) {
                    val meanHr = windowSamples.average()
                    val minHr = windowSamples.minOrNull() ?: meanHr
                    val maxHr = windowSamples.maxOrNull() ?: meanHr

                    val window = MeasurementWindow(
                        windowStart = windowStart,
                        windowEnd = windowEnd,
                        meanHr = meanHr,
                        minHr = minHr,
                        maxHr = maxHr,
                        rmssd = null,
                        validIbiCount = 0,
                        ibiValidRatio = 0.0,
                        activityState = "PASSIVE",
                        steps = null,
                        onBody = true,
                        source = "HEALTH_SERVICES_PASSIVE_WINDOW"
                    )

                    database.windowDao().insertWindow(WindowEntity.fromDomain(window, synced = false))
                    Log.i(TAG, "Saved window: meanHr=$meanHr, minHr=$minHr, maxHr=$maxHr, samples=${windowSamples.size}")

                    // Send status update
                    syncManager.sendStatusUpdate(serviceRunning = true, lastWindowAt = windowEnd)
                } else {
                    Log.w(TAG, "Window completed without valid on-body samples (watch off-body or acquiring)")
                }

                val windowIntervalMinutes = syncManager.getWindowMinutes()
                val waitMs = (windowIntervalMinutes * 60_000L) - 60_000L
                delay(if (waitMs > 0) waitMs else 60_000L)
            }
        }
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Servicio de Monitoreo Pulso",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Monitoreo continuo de salud en segundo plano"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    private fun createNotification(text: String): Notification {
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Pulso Monitoreo")
            .setContentText(text)
            .setSmallIcon(android.R.drawable.stat_notify_sync)
            .setOngoing(true)
            .build()
    }

    override fun onDestroy() {
        serviceScope.cancel()
        try {
            Futures.getUnchecked(measureClient.unregisterMeasureCallbackAsync(DataType.HEART_RATE_BPM, measureCallback))
        } catch (e: Exception) {
            // ignore
        }
        wakeLock?.let {
            if (it.isHeld) it.release()
        }
        syncManager.sendStatusUpdate(serviceRunning = false)
        super.onDestroy()
        Log.i(TAG, "SensorSamplingService stopped")
    }

    override fun onBind(intent: Intent?): IBinder? = null

    companion object {
        private const val TAG = "PulsoSamplingService"
        private const val CHANNEL_ID = "pulso_sensor_sampling_channel"
        private const val NOTIFICATION_ID = 2001

        fun start(context: Context) {
            val intent = Intent(context, SensorSamplingService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
            }
        }

        fun stop(context: Context) {
            val intent = Intent(context, SensorSamplingService::class.java)
            context.stopService(intent)
        }
    }
}
