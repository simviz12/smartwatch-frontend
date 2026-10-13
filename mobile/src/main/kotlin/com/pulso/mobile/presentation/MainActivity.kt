package com.pulso.mobile.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
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
import com.pulso.core.model.MeasurementWindow
import com.pulso.mobile.data.local.PulsoMobileDatabase
import com.pulso.mobile.data.sync.MobileDiagnosticsState
import com.pulso.mobile.data.sync.MobileSyncManager
import java.util.Date

class MainActivity : ComponentActivity() {

    private val viewModel: MobileViewModel by viewModels {
        val syncManager = MobileSyncManager.getInstance(this)
        val database = PulsoMobileDatabase.getInstance(this)
        MobileViewModel.provideFactory(
            syncManager = syncManager,
            windowDao = database.phoneWindowDao(),
            stressEventDao = database.phoneStressEventDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        if (intent?.getStringExtra("route") == "chat_placeholder") {
            viewModel.selectTab(6)
        }
        setContent {
            MaterialTheme {
                MainPhoneScreen(viewModel = viewModel)
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPhoneScreen(viewModel: MobileViewModel) {
    val uiState by viewModel.uiState.collectAsState()
    val diagnostics by viewModel.diagnosticsState.collectAsState()
    val todayWindows by viewModel.todayWindows.collectAsState()
    val latestWindow by viewModel.latestWindow.collectAsState()
    val phoneBaseline by viewModel.phoneBaseline.collectAsState()
    val baselineMismatch by viewModel.baselineMismatch.collectAsState()

    val showOnboarding = !diagnostics.consentGranted

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        text = "PULSO",
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 2.sp
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF0F172A),
                    titleContentColor = Color(0xFF38BDF8)
                )
            )
        },
        bottomBar = {
            if (!showOnboarding) {
                NavigationBar(
                    containerColor = Color(0xFF0F172A)
                ) {
                    NavigationBarItem(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        label = { Text("Hoy") },
                        icon = { Text("📊") }
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 4,
                        onClick = { viewModel.selectTab(4) },
                        label = { Text("Eventos") },
                        icon = { Text("⚡") }
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 3,
                        onClick = { viewModel.selectTab(3) },
                        label = { Text("Línea Base") },
                        icon = { Text("📈") }
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 1,
                        onClick = { viewModel.selectTab(1) },
                        label = { Text("Diagnóstico") },
                        icon = { Text("🩺") }
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 2,
                        onClick = { viewModel.selectTab(2) },
                        label = { Text("Ajustes") },
                        icon = { Text("⚙️") }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = Color(0xFF020617)
        ) {
            when {
                showOnboarding -> {
                    OnboardingScreen(onAcceptConsent = { viewModel.grantConsent() })
                }
                uiState.selectedTab == 0 -> {
                    HoyScreen(
                        windows = todayWindows,
                        latestWindow = latestWindow,
                        baseline = phoneBaseline,
                        onNavigateToBaseline = { viewModel.selectTab(3) }
                    )
                }
                uiState.selectedTab == 1 -> {
                    DiagnosticoScreen(
                        diagnostics = diagnostics,
                        baselineMismatch = baselineMismatch,
                        isTestingPing = uiState.isTestingPing,
                        pingFeedback = uiState.pingFeedback,
                        onTestConnection = { viewModel.testWatchConnection() }
                    )
                }
                uiState.selectedTab == 2 -> {
                    val qualityMetrics by viewModel.qualityMetrics.collectAsState()
                    AjustesScreen(
                        diagnostics = diagnostics,
                        currentConfig = uiState.stressConfig,
                        currentPreset = uiState.sensitivityPreset,
                        quietHours = uiState.quietHours,
                        isSavingConfig = uiState.isSavingConfig,
                        configSaveFeedback = uiState.configSaveFeedback,
                        qualityMetrics = qualityMetrics,
                        onPresetSelected = { viewModel.onPresetSelected(it) },
                        onUpdateThresholds = { hrZ, hrvZ, hrOnlyZ, a, b, cd, cap ->
                            viewModel.onUpdateThresholds(hrZ, hrvZ, hrOnlyZ, a, b, cd, cap)
                        },
                        onUpdateQuietHours = { sh, sm, eh, em -> viewModel.onUpdateQuietHours(sh, sm, eh, em) },
                        onResetDefaults = { viewModel.resetToDefaultConfig() },
                        onSaveConfig = { viewModel.saveConfig() },
                        onOpenLab = { viewModel.selectTab(5) },
                        onNavigateToBaseline = { viewModel.selectTab(3) },
                        onCancelSnooze = { viewModel.cancelSnooze() },
                        onApplyAdaptiveSuggestion = { viewModel.applyAdaptiveSuggestion() },
                        onDismissAdaptiveSuggestion = { viewModel.dismissAdaptiveSuggestion() },
                        onRevokeConsent = { viewModel.revokeConsent() }
                    )
                }
                uiState.selectedTab == 3 -> {
                    BaselineScreen(
                        baseline = phoneBaseline,
                        onResetBaseline = { viewModel.resetBaseline() },
                        resetFeedback = uiState.resetFeedback
                    )
                }
                uiState.selectedTab == 4 -> {
                    val stressEventEntities by viewModel.stressEventEntities.collectAsState()
                    com.pulso.mobile.presentation.components.StressEventListScreen(
                        entities = stressEventEntities,
                        onUpdateFeedback = { eventId, feedback -> viewModel.updateEventFeedback(eventId, feedback) },
                        onBack = { viewModel.selectTab(0) }
                    )
                }
                uiState.selectedTab == 5 -> {
                    com.pulso.mobile.presentation.components.ReplayLabScreen(
                        currentConfig = uiState.stressConfig,
                        daysRange = uiState.replayDaysRange,
                        isRunning = uiState.isRunningReplay,
                        result = uiState.replayResult,
                        feedback = uiState.replayFeedback,
                        onDaysRangeChange = { viewModel.setReplayDaysRange(it) },
                        onRunReplay = { viewModel.runReplaySimulation() },
                        onApplyConfig = {
                            viewModel.applyLabConfigToSettings(it)
                            viewModel.selectTab(2)
                        },
                        onBack = { viewModel.selectTab(2) }
                    )
                }
                uiState.selectedTab == 6 -> {
                    ChatPlaceholderScreen(
                        onBack = { viewModel.selectTab(0) }
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(onAcceptConsent: () -> Unit) {
    var isChecked by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Bienvenido a Pulso",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF38BDF8)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Tu acompañante de bienestar y autorregulación emocional.",
            fontSize = 14.sp,
            color = Color.White,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = "Aviso de Bienestar y Privacidad",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24),
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = "• Pulso es una herramienta de bienestar, NO un dispositivo médico.\n" +
                            "• Mide periódicamente tu pulso cardíaco para detectar patrones de tensión.\n" +
                            "• Tus datos biométricos permanecen exclusivamente en tus dispositivos (reloj y celular).\n" +
                            "• No vendemos ni compartimos tus datos de salud con terceros.\n" +
                            "• En versiones futuras se incorporará un chat de apoyo emocional con consentimiento específico.",
                    fontSize = 13.sp,
                    color = Color(0xFFE2E8F0),
                    lineHeight = 20.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Checkbox(
                checked = isChecked,
                onCheckedChange = { isChecked = it }
            )
            Spacer(modifier = Modifier.width(8.dp))
            Text(
                text = "He leído y acepto la recolección y análisis local de mis datos de pulso.",
                fontSize = 12.sp,
                color = Color.White
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onAcceptConsent,
            enabled = isChecked,
            modifier = Modifier.fillMaxWidth().height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
        ) {
            Text("Empezar a usar Pulso", fontSize = 15.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun HoyScreen(
    windows: List<MeasurementWindow>,
    latestWindow: MeasurementWindow?,
    baseline: com.pulso.core.baseline.Baseline?,
    onNavigateToBaseline: () -> Unit
) {
    // Resolve current bucket for baseline overlay
    val now = System.currentTimeMillis()
    val currentBucket = com.pulso.core.baseline.BaselineEngine.resolveBucket(
        timestamp = now,
        activityState = latestWindow?.activityState ?: "PASSIVE"
    )
    val currentBucketStats = baseline?.buckets?.get(currentBucket)
    val hrBaselineRange: Pair<Double, Double>? = if (currentBucketStats?.state == com.pulso.core.baseline.BaselineState.READY) {
        val median = currentBucketStats.hrMedian ?: 70.0
        val mad = currentBucketStats.hrMad ?: 5.0
        Pair(median - (1.4826 * mad), median + (1.4826 * mad))
    } else null

    val hrvBaselineRange: Pair<Double, Double>? = if (currentBucketStats?.state == com.pulso.core.baseline.BaselineState.READY && currentBucketStats.rmssdMedian != null && currentBucketStats.rmssdMad != null) {
        val median = currentBucketStats.rmssdMedian!!
        val mad = currentBucketStats.rmssdMad!!
        Pair(median - (1.4826 * mad), median + (1.4826 * mad))
    } else null

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Resumen de Hoy",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color.White
            )
            Button(
                onClick = onNavigateToBaseline,
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF1E293B)),
                modifier = Modifier.height(34.dp)
            ) {
                Text("Tu línea base ›", fontSize = 12.sp, color = Color(0xFF38BDF8))
            }
        }
        Spacer(modifier = Modifier.height(12.dp))

        // Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // HR Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Último Pulso", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    val hrText = latestWindow?.let { "${it.meanHr.toInt()} BPM" } ?: "Sin datos"
                    Text(hrText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF4ADE80))
                }
            }

            // HRV Card
            Card(
                modifier = Modifier.weight(1f),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text("Variabilidad (HRV)", fontSize = 11.sp, color = Color.Gray)
                    Spacer(modifier = Modifier.height(4.dp))
                    val hrvText = latestWindow?.rmssd?.let { "${it.toInt()} ms" } ?: "Sin datos"
                    Text(hrvText, fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8))
                }
            }
        }

        Spacer(modifier = Modifier.height(8.dp))

        // Windows recorded card
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
        ) {
            Row(
                modifier = Modifier.padding(12.dp).fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("Mediciones hoy", fontSize = 11.sp, color = Color.Gray)
                    Text("${windows.size} ventanas", fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
                }
                latestWindow?.let {
                    val minutesAgo = ((System.currentTimeMillis() - it.windowEnd) / 60000).coerceAtLeast(0)
                    Text("Hace $minutesAgo min", fontSize = 12.sp, color = Color(0xFFFBBF24))
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // HR Chart with baseline overlay
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Frecuencia Cardíaca (BPM)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            if (hrBaselineRange != null) {
                Text(
                    "Franja: ${hrBaselineRange.first.toInt()}-${hrBaselineRange.second.toInt()} BPM",
                    fontSize = 11.sp,
                    color = Color(0xFF4ADE80)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        SimpleMetricLineChart(
            windows = windows,
            valueExtractor = { it.meanHr },
            lineColor = Color(0xFF4ADE80),
            emptyMessage = "Sin datos suficientes para graficar hoy",
            baselineRange = hrBaselineRange,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF1E293B))
        )

        Spacer(modifier = Modifier.height(20.dp))

        // HRV Chart with baseline overlay
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text("Variabilidad RMSSD (ms)", fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = Color.White)
            if (hrvBaselineRange != null) {
                Text(
                    "Franja: ${hrvBaselineRange.first.toInt()}-${hrvBaselineRange.second.toInt()} ms",
                    fontSize = 11.sp,
                    color = Color(0xFF38BDF8)
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        SimpleMetricLineChart(
            windows = windows,
            valueExtractor = { it.rmssd },
            lineColor = Color(0xFF38BDF8),
            emptyMessage = "HRV no disponible en este reloj o sin muestras IBI",
            baselineRange = hrvBaselineRange,
            modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Color(0xFF1E293B))
        )
    }
}

@Composable
fun DiagnosticoScreen(
    diagnostics: MobileDiagnosticsState,
    baselineMismatch: String?,
    isTestingPing: Boolean,
    pingFeedback: String?,
    onTestConnection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Diagnóstico de Pulso", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(16.dp))

        // Mismatch Alert if detected
        baselineMismatch?.let { mismatchText ->
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF450A0A)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text("⚠️ Discrepancia de Línea Base", fontWeight = FontWeight.Bold, color = Color(0xFFEF4444), fontSize = 14.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(mismatchText, color = Color(0xFFFCA5A5), fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Consentimiento", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Estado: ${if (diagnostics.consentGranted) "Aceptado" else "No otorgado"}", color = Color.White, fontSize = 13.sp)
                Text("• Versión: ${diagnostics.consentVersion ?: "N/A"}", color = Color.LightGray, fontSize = 12.sp)
                diagnostics.consentTimestamp?.let {
                    Text("• Fecha: ${Date(it)}", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Conexión con el Reloj", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Conectado: ${if (diagnostics.watchConnected) "SÍ (${diagnostics.connectedWatchNodeName})" else "NO"}", color = Color.White, fontSize = 13.sp)
                diagnostics.lastWatchStatus?.let { status ->
                    Text("• Batería del reloj: ${status.batteryPct}%", color = Color.White, fontSize = 13.sp)
                    Text("• Servicio de medición: ${if (status.serviceRunning) "Activo" else "Detenido"}", color = Color.White, fontSize = 13.sp)
                    Text("• Modelo reloj: ${status.watchModel}", color = Color.LightGray, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Almacenamiento y Sincronización", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Ventanas almacenadas en celular: ${diagnostics.totalWindowsStored}", color = Color.White, fontSize = 13.sp)
                Text("• Último lote recibido: ${diagnostics.lastSyncBatchId ?: "Ninguno"}", color = Color.LightGray, fontSize = 12.sp)
                Text("• Muestras aceptadas último lote: ${diagnostics.lastSyncAcceptedCount}", color = Color.LightGray, fontSize = 12.sp)
                diagnostics.lastError?.let { err ->
                    Text("• Último error: $err", color = Color(0xFFEF4444), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)), modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Detección y Eventos", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Total eventos almacenados: ${diagnostics.totalEventsStored}", color = Color.White, fontSize = 13.sp)
                diagnostics.lastEventReceived?.let { ev ->
                    val timeStr = Date(ev.time).toString()
                    Text("• Último evento: $timeStr (Confianza: ${(ev.confidence * 100).toInt()}%)", color = Color(0xFFFBBF24), fontSize = 12.sp)
                } ?: Text("• Último evento: Ninguno registrado aún", color = Color.LightGray, fontSize = 12.sp)
                diagnostics.lastFeedbackReceived?.let { fb ->
                    val timeStr = Date(fb.timestamp).toString()
                    Text("• Última retroalimentación: ${fb.action.name} en ${fb.timeBucket} ($timeStr)", color = Color(0xFF38BDF8), fontSize = 12.sp)
                } ?: Text("• Última retroalimentación: Ninguna registrada aún", color = Color.LightGray, fontSize = 12.sp)
                diagnostics.activeSnoozeUntil?.let { snoozeUntil ->
                    val diffMin = ((snoozeUntil - System.currentTimeMillis()) / 60000).coerceAtLeast(0)
                    Text("• Pausa de avisos activa: $diffMin min restantes", color = Color(0xFFF59E0B), fontSize = 12.sp)
                }
                Text("• Versión config en celular: v${diagnostics.phoneConfigVersion}", color = Color.LightGray, fontSize = 12.sp)
                Text("• Versión config en reloj: v${diagnostics.watchConfigVersion}", color = Color.LightGray, fontSize = 12.sp)
                if (diagnostics.hasPendingConfig) {
                    Text("• Sincronización de config: PENDIENTE (esperando confirmación del reloj)", color = Color(0xFFFBBF24), fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                } else {
                    Text("• Sincronización de config: AL DÍA", color = Color(0xFF4ADE80), fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onTestConnection,
            enabled = !isTestingPing,
            modifier = Modifier.fillMaxWidth().height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7))
        ) {
            if (isTestingPing) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = Color.White)
            } else {
                Text("Probar conexión con el reloj", fontSize = 13.sp)
            }
        }

        pingFeedback?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(it, color = if (it.contains("exitoso")) Color(0xFF4ADE80) else Color(0xFFEF4444), textAlign = TextAlign.Center, modifier = Modifier.fillMaxWidth())
        }
    }
}

@Composable
fun AjustesScreen(
    diagnostics: MobileDiagnosticsState,
    currentConfig: com.pulso.core.stress.StressConfig,
    currentPreset: com.pulso.mobile.domain.stress.SensitivityPreset,
    quietHours: com.pulso.core.model.QuietHours,
    isSavingConfig: Boolean,
    configSaveFeedback: String?,
    qualityMetrics: com.pulso.mobile.domain.intervention.QualityMetrics,
    onPresetSelected: (com.pulso.mobile.domain.stress.SensitivityPreset) -> Unit,
    onUpdateThresholds: (hrZ: Double, hrvZ: Double, hrOnlyZ: Double, consecA: Int, consecB: Int, cooldown: Int, dailyCap: Int) -> Unit,
    onUpdateQuietHours: (startH: Int, startM: Int, endH: Int, endM: Int) -> Unit,
    onResetDefaults: () -> Unit,
    onSaveConfig: () -> Unit,
    onOpenLab: () -> Unit,
    onNavigateToBaseline: () -> Unit,
    onCancelSnooze: () -> Unit,
    onApplyAdaptiveSuggestion: () -> Unit,
    onDismissAdaptiveSuggestion: () -> Unit,
    onRevokeConsent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text("Ajustes y Parámetros", fontSize = 20.sp, fontWeight = FontWeight.Bold, color = Color.White)
        Spacer(modifier = Modifier.height(14.dp))

        // Sensitivity Section
        com.pulso.mobile.presentation.components.SensitivitySettingsSection(
            currentConfig = currentConfig,
            currentPreset = currentPreset,
            quietHours = quietHours,
            isSaving = isSavingConfig,
            saveFeedback = configSaveFeedback,
            qualityMetrics = qualityMetrics,
            adaptiveSuggestion = diagnostics.adaptiveSuggestion,
            onPresetSelected = onPresetSelected,
            onUpdateThresholds = onUpdateThresholds,
            onUpdateQuietHours = onUpdateQuietHours,
            onResetDefaults = onResetDefaults,
            onSaveConfig = onSaveConfig,
            onOpenLab = onOpenLab,
            onCancelSnooze = onCancelSnooze,
            onApplyAdaptiveSuggestion = onApplyAdaptiveSuggestion,
            onDismissAdaptiveSuggestion = onDismissAdaptiveSuggestion
        )

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text("Línea Base Personal", fontWeight = FontWeight.Bold, color = Color(0xFF38BDF8), fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    "Consulta el aprendizaje de tus rangos normales de pulso y variabilidad para cada horario del día.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onNavigateToBaseline,
                    colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
                    modifier = Modifier.fillMaxWidth().height(40.dp)
                ) {
                    Text("Ver mi línea base", fontSize = 13.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            "Al retirar tu consentimiento, el reloj inteligente detendrá inmediatamente cualquier monitoreo y recolección de pulso.",
            fontSize = 13.sp,
            color = Color.LightGray
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onRevokeConsent,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFDC2626)),
            modifier = Modifier.fillMaxWidth().height(48.dp)
        ) {
            Text("Retirar consentimiento de medición", fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
fun ChatPlaceholderScreen(onBack: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("💬", fontSize = 48.sp)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            "Espacio de Acompañamiento",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            "Elegiste 'Hablar' desde tu reloj. Queremos escucharte y brindarte un espacio de calma.",
            fontSize = 14.sp,
            color = Color(0xFF94A3B8),
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(20.dp))
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    "El asistente conversacional llega en la Parte 8.",
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color(0xFF38BDF8)
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    "Por ahora registramos tu intención de diálogo para calibrar las intervenciones. No generamos respuestas simuladas.",
                    fontSize = 12.sp,
                    color = Color.LightGray
                )
            }
        }
        Spacer(modifier = Modifier.height(24.dp))
        Button(
            onClick = onBack,
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF0284C7)),
            modifier = Modifier.fillMaxWidth().height(44.dp)
        ) {
            Text("Volver al inicio", fontSize = 14.sp)
        }
    }
}
