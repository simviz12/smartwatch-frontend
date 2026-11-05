package com.pulso.wear.presentation

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.pulso.wear.data.repository.AndroidWearDataLayerRepository
import com.pulso.wear.data.sensor.AndroidSensorRepository
import com.pulso.wear.data.sync.AndroidSyncRepository
import com.pulso.wear.domain.usecase.ExportWindowsCsvUseCase
import com.pulso.wear.domain.usecase.GetCapabilityReportUseCase
import com.pulso.wear.domain.usecase.GetConnectedPhoneUseCase
import com.pulso.wear.domain.usecase.ObserveConfigUseCase
import com.pulso.wear.domain.usecase.ObserveLiveHeartRateUseCase
import com.pulso.wear.domain.usecase.ObserveWindowsUseCase
import com.pulso.wear.domain.usecase.SendPingUseCase
import com.pulso.wear.domain.usecase.TriggerSyncUseCase
import com.pulso.wear.presentation.theme.PulsoWearTheme
import androidx.compose.ui.platform.LocalContext
import androidx.wear.compose.navigation.SwipeDismissableNavHost
import androidx.wear.compose.navigation.composable
import androidx.wear.compose.navigation.rememberSwipeDismissableNavController

class MainActivity : ComponentActivity() {

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val bodySensorsGranted = permissions[Manifest.permission.BODY_SENSORS] ?: false
        if (bodySensorsGranted) {
            viewModel.loadCapabilityReport()
            viewModel.startLiveMeasurement()
        } else {
            Toast.makeText(this, "Permiso de sensores corporales necesario", Toast.LENGTH_SHORT).show()
        }
    }

    private val viewModel: MainViewModel by viewModels {
        val wearRepo = AndroidWearDataLayerRepository(this)
        val sensorRepo = AndroidSensorRepository(this)
        val syncRepo = AndroidSyncRepository(this)
        MainViewModel.provideFactory(
            getConnectedPhoneUseCase = GetConnectedPhoneUseCase(wearRepo),
            sendPingUseCase = SendPingUseCase(wearRepo),
            getCapabilityReportUseCase = GetCapabilityReportUseCase(sensorRepo),
            observeLiveHeartRateUseCase = ObserveLiveHeartRateUseCase(sensorRepo),
            observeWindowsUseCase = ObserveWindowsUseCase(sensorRepo),
            exportWindowsCsvUseCase = ExportWindowsCsvUseCase(sensorRepo),
            observeConfigUseCase = ObserveConfigUseCase(syncRepo),
            triggerSyncUseCase = TriggerSyncUseCase(syncRepo)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()
        // Eagerly initialize speech engine on Wear OS so it is ready immediately
        try {
            com.pulso.wear.data.speech.WearSpeechManager.getInstance(applicationContext)
        } catch (_: Exception) {}

        val route = intent.getStringExtra("route")
        val launchedForBreathing = route == "breathing"
        val launchedForAlert = route == "alert"
        val alertEventId = intent.getStringExtra(com.pulso.wear.data.intervention.WearInterventionManager.EXTRA_EVENT_ID)
        val alertBucket = intent.getStringExtra(com.pulso.wear.data.intervention.WearInterventionManager.EXTRA_BUCKET)
        val alertSubtitle = intent.getStringExtra(com.pulso.wear.data.intervention.WearInterventionManager.EXTRA_ALERT_SUBTITLE)
            ?: "Tu pulso está sobre tu base sin actividad física"

        setContent {
            PulsoWearTheme {
                val uiState by viewModel.uiState.collectAsState()
                val breathingManager = remember { com.pulso.wear.data.breathing.WearBreathingSessionManager.getInstance(this@MainActivity) }
                val breathingSnapshot by breathingManager.currentSnapshot.collectAsState()
                val displayMode by breathingManager.displayMode.collectAsState()
                val liveHr = uiState.liveHeartRate.bpm
                val navController = rememberSwipeDismissableNavController()
                val context = LocalContext.current
                val breathingConfig = uiState.config?.breathingConfig ?: com.pulso.core.breathing.BreathingConfig()

                SwipeDismissableNavHost(
                    navController = navController,
                    startDestination = when {
                        launchedForAlert && alertEventId != null -> Routes.ALERT
                        launchedForBreathing -> Routes.BREATHING
                        else -> Routes.HOME
                    }
                ) {
                    composable(Routes.HOME) {
                        HomeScreen(
                            uiState = uiState,
                            onOpenBreathing = { navController.navigate(Routes.BREATHING) },
                            onOpenLivePulse = { navController.navigate(Routes.LIVE) },
                            onTalk = { navController.navigate(Routes.VOICE) },
                            onOpenPause = { navController.navigate(Routes.PAUSE) },
                            onOpenSensors = { navController.navigate(Routes.SENSORS) }
                        )
                    }
                    composable(Routes.LIVE) { LivePulseScreen(uiState) }
                    composable(Routes.VOICE) {
                        val assistant = remember { com.pulso.wear.data.assistant.WearAssistantManager.getInstance(context) }
                        androidx.compose.runtime.DisposableEffect(Unit) { onDispose { assistant.reset() } }
                        VoiceAssistantScreen(manager = assistant, onBreathe = {
                            assistant.reset()
                            navController.navigate(Routes.BREATHING) { popUpTo(Routes.VOICE) { inclusive = true } }
                        })
                    }
                    composable(Routes.ALERT) {
                        val intervention = remember { com.pulso.wear.data.intervention.WearInterventionManager.getInstance(context) }
                        val eventId = alertEventId ?: ""
                        val close: () -> Unit = {
                            if (navController.previousBackStackEntry == null) finish() else navController.popBackStack()
                        }
                        StressAlertScreen(
                            subtitle = alertSubtitle,
                            onBreathe = {
                                intervention.recordAlertAction(eventId, com.pulso.core.intervention.InterventionAction.BREATHE, alertBucket)
                                val base = breathingConfig.toPlan()
                                val plan = com.pulso.core.breathing.BreathingPlan.withTargetDuration(base, 1)
                                breathingManager.startSession(plan = plan, currentHr = liveHr)
                                navController.navigate(Routes.BREATHING) { popUpTo(Routes.ALERT) { inclusive = true } }
                            },
                            onNotNow = {
                                intervention.recordAlertAction(eventId, com.pulso.core.intervention.InterventionAction.DISMISS, alertBucket)
                                close()
                            },
                            onNotStressed = {
                                intervention.recordAlertAction(eventId, com.pulso.core.intervention.InterventionAction.NOT_STRESSED, alertBucket)
                                close()
                            },
                            onTimeout = {
                                intervention.recordAlertAction(eventId, com.pulso.core.intervention.InterventionAction.IGNORED, alertBucket)
                                close()
                            }
                        )
                    }
                    composable(Routes.PAUSE) {
                        PauseAlertsScreen(
                            snoozeUntil = uiState.config?.stressConfig?.snoozeUntil,
                            onPause = { minutes -> viewModel.snoozeAlerts(minutes, context) }
                        )
                    }
                    composable(Routes.SENSORS) {
                        SensorsScreen(
                            uiState = uiState,
                            onSync = { viewModel.triggerSync() },
                            onSaveReport = {
                                val path = viewModel.saveReportToFile(this@MainActivity)
                                Log.i("PulsoReport", "Capability report saved: $path")
                                Toast.makeText(this@MainActivity, "Guardado en $path", Toast.LENGTH_LONG).show()
                            }
                        )
                    }
                    composable(Routes.BREATHING) {
                        com.pulso.wear.presentation.components.BreathingSessionScreen(
                            snapshot = breathingSnapshot,
                            displayMode = displayMode,
                            liveHr = liveHr,
                            plan = breathingManager.currentPlan,
                            hrBefore = breathingManager.sessionHrBefore,
                            defaultPlanName = breathingConfig.preferredPlanName,
                            defaultMinutes = breathingConfig.durationMinutes,
                            onToggleDisplayMode = {
                                val nextMode = if (displayMode == com.pulso.wear.data.breathing.BreathingDisplayMode.VIBRATION_ONLY) {
                                    com.pulso.wear.data.breathing.BreathingDisplayMode.VISUAL_GUIDE
                                } else {
                                    com.pulso.wear.data.breathing.BreathingDisplayMode.VIBRATION_ONLY
                                }
                                breathingManager.setDisplayMode(nextMode)
                            },
                            onStartSession = { plan, _, mode ->
                                breathingManager.startSession(plan = plan, mode = mode, currentHr = liveHr)
                            },
                            onPauseResume = {
                                if (breathingSnapshot?.state == com.pulso.core.breathing.BreathingSessionState.PAUSED) {
                                    breathingManager.resumeSession()
                                } else {
                                    breathingManager.pauseSession()
                                }
                            },
                            onStopSession = {
                                breathingManager.stopSession(com.pulso.core.breathing.SessionEndReason.USER_STOPPED)
                            },
                            onSaveFeeling = { feeling ->
                                breathingManager.updatePostSessionHr(liveHr)
                                breathingManager.saveFeeling(feeling)
                            },
                            onFinishScreen = {
                                if (navController.previousBackStackEntry == null) {
                                    finish()
                                } else {
                                    navController.popBackStack()
                                }
                            }
                        )
                    }
                }
            }
        }
    }

    private fun checkAndRequestPermissions() {
        val permissionsToRequest = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.BODY_SENSORS) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.BODY_SENSORS)
        }
        if (ContextCompat.checkSelfPermission(this, Manifest.permission.ACTIVITY_RECOGNITION) != PackageManager.PERMISSION_GRANTED) {
            permissionsToRequest.add(Manifest.permission.ACTIVITY_RECOGNITION)
        }
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
                permissionsToRequest.add(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkConnectedNode()
    }
}

private object Routes {
    const val HOME = "home"
    const val LIVE = "live"
    const val PAUSE = "pause"
    const val SENSORS = "sensors"
    const val BREATHING = "breathing"
    const val VOICE = "voice"
    const val ALERT = "alert"
}
