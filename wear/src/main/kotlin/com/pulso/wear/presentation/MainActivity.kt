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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import androidx.wear.compose.material.Card
import androidx.wear.compose.material.CardDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.PositionIndicator
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.pulso.wear.data.repository.AndroidWearDataLayerRepository
import com.pulso.wear.data.sensor.AndroidSensorRepository
import com.pulso.wear.domain.usecase.ExportWindowsCsvUseCase
import com.pulso.wear.domain.usecase.GetCapabilityReportUseCase
import com.pulso.wear.domain.usecase.GetConnectedPhoneUseCase
import com.pulso.wear.domain.usecase.ManageBackgroundSamplingUseCase
import com.pulso.wear.domain.usecase.ObserveLiveHeartRateUseCase
import com.pulso.wear.domain.usecase.ObserveWindowsUseCase
import com.pulso.wear.domain.usecase.SendPingUseCase
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
        MainViewModel.provideFactory(
            getConnectedPhoneUseCase = GetConnectedPhoneUseCase(wearRepo),
            sendPingUseCase = SendPingUseCase(wearRepo),
            getCapabilityReportUseCase = GetCapabilityReportUseCase(sensorRepo),
            observeLiveHeartRateUseCase = ObserveLiveHeartRateUseCase(sensorRepo),
            observeWindowsUseCase = ObserveWindowsUseCase(sensorRepo),
            exportWindowsCsvUseCase = ExportWindowsCsvUseCase(sensorRepo),
            manageBackgroundSamplingUseCase = ManageBackgroundSamplingUseCase(sensorRepo)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        checkAndRequestPermissions()

        setContent {
            PulsoWearTheme {
                MainScreen(
                    viewModel = viewModel,
                    onRequestPermissions = { checkAndRequestPermissions() },
                    onCopyReport = {
                        val path = viewModel.saveReportToFile(this)
                        Log.i("PulsoReport", "Capability report saved: $path")
                        Toast.makeText(this, "Guardado en $path", Toast.LENGTH_LONG).show()
                    }
                )
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
        } else {
            viewModel.startLiveMeasurement()
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
    onRequestPermissions: () -> Unit,
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
                    Text(
                        text = "MODO DEPURACIÓN / SENSORES",
                        fontSize = 9.sp,
                        color = Color(0xFFFFB74D),
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            // Live HR Card
            item {
                Card(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(backgroundColor = Color(0xFF1A1A1A))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "FRECUENCIA CARDÍACA EN VIVO",
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

            // HRV Availability Card
            item {
                Card(
                    onClick = {},
                    enabled = false,
                    modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                    colors = CardDefaults.cardColors(backgroundColor = Color(0xFF1E2124))
                ) {
                    Column(
                        modifier = Modifier.padding(8.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            text = "VARIABILIDAD (HRV / IBI)",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFFB0BEC5)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        val report = uiState.capabilityReport
                        if (report?.samsungSdkAvailable == true) {
                            Text(
                                text = "Samsung Sensor SDK Activo",
                                fontSize = 11.sp,
                                color = Color(0xFF81C784),
                                fontWeight = FontWeight.SemiBold
                            )
                        } else {
                            Text(
                                text = "HRV no disponible en este reloj",
                                fontSize = 11.sp,
                                color = Color(0xFFFF8A80),
                                textAlign = TextAlign.Center
                            )
                            Text(
                                text = "Requiere Samsung Health Sensor SDK con Developer Mode activo",
                                fontSize = 9.sp,
                                color = Color(0xFF78909C),
                                textAlign = TextAlign.Center
                            )
                        }
                    }
                }
            }

            // Background Service Toggle Button
            item {
                val serviceActive = uiState.isMonitoringActive
                Button(
                    onClick = { viewModel.toggleBackgroundSampling() },
                    modifier = Modifier.fillMaxWidth(0.9f).height(40.dp).padding(vertical = 2.dp),
                    colors = ButtonDefaults.buttonColors(
                        backgroundColor = if (serviceActive) Color(0xFFD32F2F) else MaterialTheme.colors.primary
                    )
                ) {
                    Text(
                        text = if (serviceActive) "Detener Servicio" else "Iniciar Muestreo (5m)",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Export CSV Button
            item {
                Button(
                    onClick = { viewModel.exportCsv() },
                    modifier = Modifier.fillMaxWidth(0.9f).height(40.dp).padding(vertical = 2.dp),
                    colors = ButtonDefaults.secondaryButtonColors()
                ) {
                    Text(
                        text = "Exportar CSV (${uiState.totalWindowsLogged})",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Copy Capability Report Button
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

            // Send Ping to Companion Button
            item {
                Button(
                    onClick = { viewModel.sendPing() },
                    enabled = !uiState.isSendingPing,
                    modifier = Modifier.fillMaxWidth(0.9f).height(40.dp).padding(vertical = 2.dp),
                    colors = ButtonDefaults.primaryButtonColors()
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

            // Status feedback message
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
