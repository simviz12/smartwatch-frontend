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
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
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
        var currentRoute by mutableStateOf(route)

        setContent {
            PulsoWearTheme {
                val uiState by viewModel.uiState.collectAsState()
                val breathingManager = remember { com.pulso.wear.data.breathing.WearBreathingSessionManager.getInstance(this@MainActivity) }
                val breathingSnapshot by breathingManager.currentSnapshot.collectAsState()
                val displayMode by breathingManager.displayMode.collectAsState()
                val liveHr = uiState.liveHeartRate.bpm

                if (currentRoute == "breathing") {
                    com.pulso.wear.presentation.components.BreathingSessionScreen(
                        snapshot = breathingSnapshot,
                        displayMode = displayMode,
                        liveHr = liveHr,
                        onToggleDisplayMode = {
                            val nextMode = if (displayMode == com.pulso.wear.data.breathing.BreathingDisplayMode.VIBRATION_ONLY) {
                                com.pulso.wear.data.breathing.BreathingDisplayMode.VISUAL_GUIDE
                            } else {
                                com.pulso.wear.data.breathing.BreathingDisplayMode.VIBRATION_ONLY
                            }
                            breathingManager.setDisplayMode(nextMode)
                        },
                        onStartSession = { plan, minutes, mode ->
                            breathingManager.startSession(plan = plan, mode = mode, currentHr = liveHr)
                        },
                        onStopSession = {
                            breathingManager.stopSession(com.pulso.core.breathing.SessionEndReason.USER_STOPPED)
                        },
                        onSaveFeeling = { feeling ->
                            breathingManager.saveFeeling(feeling)
                        },
                        onFinishScreen = {
                            if (route == "breathing") {
                                finish()
                            } else {
                                currentRoute = null
                            }
                        }
                    )
                } else {
                    MainScreen(
                        viewModel = viewModel,
                        onOpenBreathing = { currentRoute = "breathing" },
                        onCopyReport = {
                            val path = viewModel.saveReportToFile(this@MainActivity)
                            Log.i("PulsoReport", "Capability report saved: $path")
                            Toast.makeText(this@MainActivity, "Guardado en $path", Toast.LENGTH_LONG).show()
                        }
                    )
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

@Composable
fun MainScreen(
    viewModel: MainViewModel,
    onOpenBreathing: () -> Unit,
    onCopyReport: () -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val listState = rememberScalingLazyListState()

    Scaffold(
        timeText = { TimeText() },
        positionIndicator = { PositionIndicator(scalingLazyListState = listState) }
    ) {
        ScalingLazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .padding(horizontal = 8.dp),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            item {
                Column(
                    modifier = Modifier.fillMaxWidth().padding(top = 16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PULSO",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colors.primary,
                        letterSpacing = 2.sp
                    )
                }
            }

            // Ejercicio de Respiración
            item {
                Button(
                    onClick = onOpenBreathing,
                    modifier = Modifier.fillMaxWidth(0.9f).height(40.dp).padding(vertical = 2.dp),
                    colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xFF0284C7))
                ) {
                    Text(
                        text = "Respiración Guiada",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Hablar con IA (Acompañamiento emocional y conexión con teléfono)
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                Button(
                    onClick = {
                        try {
                            com.pulso.wear.data.speech.WearSpeechManager.getInstance(context).speak("Estoy aquí contigo. Respira profundo.")
                        } catch (_: Exception) {}
                        viewModel.openChatOnPhone(context)
                    },
                    modifier = Modifier.fillMaxWidth(0.9f).height(40.dp).padding(vertical = 2.dp),
                    colors = ButtonDefaults.primaryButtonColors(backgroundColor = Color(0xFF10B981))
                ) {
                    Text(
                        text = "Hablar con Asistente",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }

            // Consent Gate Display
            val isConsentGranted = uiState.config?.collectionEnabled == true
            if (!isConsentGranted) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF263238))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "Consentimiento requerido",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFFB74D),
                                textAlign = TextAlign.Center
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "Abre Pulso en tu celular para empezar",
                                fontSize = 11.sp,
                                color = Color.White,
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Live HR Box (Only when consent is granted)
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1A1A1A))
                            .padding(10.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "FRECUENCIA CARDÍACA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFB0BEC5)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            val bpmText = uiState.liveHeartRate.bpm?.let { "${it.toInt()} BPM" } ?: "-- BPM"
                            Text(
                                text = bpmText,
                                fontSize = 24.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (uiState.liveHeartRate.bpm != null) Color(0xFF00E676) else Color.White
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Estado: ${uiState.liveHeartRate.availability}",
                                fontSize = 10.sp,
                                color = Color(0xFF90A4AE)
                            )
                        }
                    }
                }

                // Sync action button
                item {
                    Button(
                        onClick = { viewModel.triggerSync() },
                        enabled = !uiState.isSyncing,
                        modifier = Modifier.fillMaxWidth(0.9f).height(40.dp).padding(vertical = 2.dp),
                        colors = ButtonDefaults.primaryButtonColors()
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(strokeWidth = 2.dp)
                        } else {
                            Text(
                                text = "Sincronizar (${uiState.totalWindowsLogged})",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }

            // Diagnostics and Export Tools
            item {
                Button(
                    onClick = onCopyReport,
                    modifier = Modifier.fillMaxWidth(0.9f).height(40.dp).padding(vertical = 2.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text(
                        text = "Copiar Informe",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            item {
                Button(
                    onClick = { viewModel.exportCsv() },
                    modifier = Modifier.fillMaxWidth(0.9f).height(40.dp).padding(vertical = 2.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text(
                        text = "Exportar CSV (Debug)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            if (com.pulso.wear.BuildConfig.DEBUG) {
                item {
                    val context = androidx.compose.ui.platform.LocalContext.current
                    Button(
                        onClick = { viewModel.simulateStress(context) },
                        modifier = Modifier.fillMaxWidth(0.9f).height(40.dp).padding(vertical = 2.dp),
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text(
                            text = "Simular estrés (Debug)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Pausar avisos (Snooze)
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                var showSnoozeOptions by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = { showSnoozeOptions = !showSnoozeOptions },
                        modifier = Modifier.fillMaxWidth(0.9f).height(40.dp).padding(vertical = 2.dp),
                        colors = ButtonDefaults.secondaryButtonColors()
                    ) {
                        Text(
                            text = if (showSnoozeOptions) "Ocultar pausa" else "Pausar avisos",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                    if (showSnoozeOptions) {
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(0.9f),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                        ) {
                            Button(
                                onClick = {
                                    viewModel.snoozeAlerts(60L, context)
                                    showSnoozeOptions = false
                                },
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.primaryButtonColors()
                            ) {
                                Text("1h", fontSize = 10.sp)
                            }
                            Button(
                                onClick = {
                                    viewModel.snoozeAlerts(240L, context)
                                    showSnoozeOptions = false
                                },
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.primaryButtonColors()
                            ) {
                                Text("4h", fontSize = 10.sp)
                            }
                            Button(
                                onClick = {
                                    // Hasta mañana (~12h)
                                    viewModel.snoozeAlerts(720L, context)
                                    showSnoozeOptions = false
                                },
                                modifier = Modifier.weight(1f).height(32.dp),
                                colors = ButtonDefaults.primaryButtonColors()
                            ) {
                                Text("Mañana", fontSize = 9.sp)
                            }
                        }
                    }
                }
            }

            // Send Ping to Companion Button
            item {
                Button(
                    onClick = { viewModel.sendPing() },
                    enabled = !uiState.isSendingPing,
                    modifier = Modifier.fillMaxWidth(0.9f).height(40.dp).padding(vertical = 2.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    if (uiState.isSendingPing) {
                        CircularProgressIndicator(strokeWidth = 2.dp)
                    } else {
                        Text(
                            text = "Enviar al celular",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
            }

            // Status message
            item {
                uiState.statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 10.sp,
                        color = Color(0xFF80CBC4),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                    )
                }
                uiState.lastPingResultText?.let { pingMsg ->
                    val isSuccess = pingMsg.startsWith("Enviado")
                    Text(
                        text = pingMsg,
                        fontSize = 10.sp,
                        color = if (isSuccess) Color(0xFF00E676) else Color(0xFFFF5252),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
