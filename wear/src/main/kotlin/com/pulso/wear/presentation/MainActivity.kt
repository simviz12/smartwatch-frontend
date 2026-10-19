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
                .background(Color(0xFF0A0F14))
                .padding(horizontal = 12.dp),
            state = listState,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header Minimalista
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 18.dp, bottom = 6.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "PULSO",
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Black,
                        color = Color(0xFF2DD4BF),
                        letterSpacing = 3.sp
                    )
                    Text(
                        text = "ACOMPAÑAMIENTO EN VIVO",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = Color(0xFF64748B),
                        letterSpacing = 1.sp
                    )
                }
            }

            // TARJETA DE ACCIÓN 1: Respiración Adaptativa (Clínica)
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF0F2332))
                        .clickable { onOpenBreathing() }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "RESPIRACIÓN",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF38BDF8),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Calma Guiada",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF1F5F9)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF0284C7))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "INICIAR",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Espaciador suave
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // TARJETA DE ACCIÓN 2: Asistente Psicológico por Voz
            item {
                val context = androidx.compose.ui.platform.LocalContext.current
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .background(Color(0xFF062D24))
                        .clickable {
                            try {
                                com.pulso.wear.data.speech.WearSpeechManager.getInstance(context).speak("Estoy aquí contigo. Respira profundo.")
                            } catch (_: Exception) {}
                            viewModel.openChatOnPhone(context)
                        }
                        .padding(horizontal = 14.dp, vertical = 10.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = "ASISTENCIA",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF34D399),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "Hablar con Terapeuta",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFF1F5F9)
                            )
                        }
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(Color(0xFF059669))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "VOZ",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Black,
                                color = Color.White
                            )
                        }
                    }
                }
            }

            // Espaciador suave
            item { Spacer(modifier = Modifier.height(4.dp)) }

            // Consent Gate o Monitor Biométrico en Tiempo Real
            val isConsentGranted = uiState.config?.collectionEnabled == true
            if (!isConsentGranted) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .background(Color(0xFF1E1B18))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "VINCULACIÓN PENDIENTE",
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFFFBBF24),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(3.dp))
                            Text(
                                text = "Activa los sensores en tu celular",
                                fontSize = 10.sp,
                                color = Color(0xFFE2E8F0),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            } else {
                // Tarjeta Biométrica Elegante
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(16.dp))
                            .background(Color(0xFF131C24))
                            .padding(12.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = "RITMO CARDÍACO",
                                fontSize = 8.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF64748B),
                                letterSpacing = 1.sp
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            val bpmText = uiState.liveHeartRate.bpm?.let { "${it.toInt()}" } ?: "--"
                            Row(verticalAlignment = Alignment.Bottom) {
                                Text(
                                    text = bpmText,
                                    fontSize = 28.sp,
                                    fontWeight = FontWeight.Black,
                                    color = if (uiState.liveHeartRate.bpm != null) Color(0xFF34D399) else Color(0xFF94A3B8)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "BPM",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF64748B),
                                    modifier = Modifier.padding(bottom = 4.dp)
                                )
                            }
                            Text(
                                text = "Sensor: ${uiState.liveHeartRate.availability}",
                                fontSize = 9.sp,
                                color = Color(0xFF475569)
                            )
                        }
                    }
                }

                // Sincronización biométrica rápida
                item {
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1B2733))
                            .clickable(enabled = !uiState.isSyncing) { viewModel.triggerSync() }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        if (uiState.isSyncing) {
                            CircularProgressIndicator(strokeWidth = 2.dp, modifier = Modifier.size(16.dp))
                        } else {
                            Text(
                                text = "Sincronizar datos (${uiState.totalWindowsLogged})",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }

            // Pausar Alertas (Snooze Clínico)
            item {
                Spacer(modifier = Modifier.height(4.dp))
                val context = androidx.compose.ui.platform.LocalContext.current
                var showSnoozeOptions by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(false) }
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(12.dp))
                            .background(Color(0xFF1B2733))
                            .clickable { showSnoozeOptions = !showSnoozeOptions }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (showSnoozeOptions) "Cerrar pausa" else "Pausar notificaciones",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = Color(0xFF94A3B8)
                        )
                    }

                    if (showSnoozeOptions) {
                        Spacer(modifier = Modifier.height(4.dp))
                        androidx.compose.foundation.layout.Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = androidx.compose.foundation.layout.Arrangement.spacedBy(4.dp)
                        ) {
                            listOf(60L to "1h", 240L to "4h", 720L to "12h").forEach { (min, label) ->
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(Color(0xFF263544))
                                        .clickable {
                                            viewModel.snoozeAlerts(min, context)
                                            showSnoozeOptions = false
                                        }
                                        .padding(vertical = 6.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(label, fontSize = 10.sp, fontWeight = FontWeight.Bold, color = Color.White)
                                }
                            }
                        }
                    }
                }
            }

            // Diagnósticos & Herramientas avanzadas
            item {
                Spacer(modifier = Modifier.height(4.dp))
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .background(Color(0xFF161F28))
                        .clickable { onCopyReport() }
                        .padding(vertical = 8.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Informe de sensores",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Medium,
                        color = Color(0xFF64748B)
                    )
                }
            }

            // Feedback / Mensajes de estado
            item {
                uiState.statusMessage?.let { msg ->
                    Text(
                        text = msg,
                        fontSize = 9.sp,
                        color = Color(0xFF2DD4BF),
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp)
                    )
                }
            }

            // Espaciador final para dar holgura al scroll redondo
            item {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}
