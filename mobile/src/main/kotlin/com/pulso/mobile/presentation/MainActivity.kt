package com.pulso.mobile.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
            stressEventDao = database.phoneStressEventDao(),
            breathingSessionDao = database.phoneBreathingSessionDao()
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        handleChatIntent(intent)
        setContent {
            MainPhoneScreen(
                viewModel = viewModel,
                chatManager = com.pulso.mobile.data.chat.PulsoGeminiChatManager.getInstance(this)
            )
        }
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleChatIntent(intent)
    }

    private fun handleChatIntent(intent: android.content.Intent?) {
        if (intent?.getStringExtra("route") == "chat_placeholder" || intent?.getStringExtra("route") == "chat") {
            viewModel.selectTab(6)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainPhoneScreen(
    viewModel: MobileViewModel,
    chatManager: com.pulso.mobile.data.chat.PulsoGeminiChatManager
) {
    val uiState by viewModel.uiState.collectAsState()
    val diagnostics by viewModel.diagnosticsState.collectAsState()
    val todayWindows by viewModel.todayWindows.collectAsState()
    val latestWindow by viewModel.latestWindow.collectAsState()
    val phoneBaseline by viewModel.phoneBaseline.collectAsState()
    val baselineMismatch by viewModel.baselineMismatch.collectAsState()
    val breathingSessions by viewModel.breathingSessions.collectAsState()
    val weeklyBreathingSummary by viewModel.weeklyBreathingSummary.collectAsState()

    var isLightMode by remember { mutableStateOf(false) }
    var currentLanguage by remember { mutableStateOf(com.pulso.mobile.presentation.i18n.AppLanguage.ES) }
    val theme = remember(isLightMode) { com.pulso.mobile.presentation.theme.getPulsoTheme(isLightMode) }

    val showOnboarding = !diagnostics.consentGranted

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "PULSO",
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp,
                            color = theme.primary
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(6.dp))
                                .background(theme.cardBackground)
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "IA",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = theme.primary
                            )
                        }
                    }
                },
                actions = {
                    // English / Spanish Switch Button
                    Button(
                        onClick = {
                            currentLanguage = if (currentLanguage == com.pulso.mobile.presentation.i18n.AppLanguage.ES) {
                                com.pulso.mobile.presentation.i18n.AppLanguage.EN
                            } else {
                                com.pulso.mobile.presentation.i18n.AppLanguage.ES
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.cardBackground),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(32.dp)
                    ) {
                        Text(
                            text = if (currentLanguage == com.pulso.mobile.presentation.i18n.AppLanguage.ES) "ES" else "EN",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primary
                        )
                    }

                    Spacer(modifier = Modifier.width(6.dp))

                    // Light / Dark Theme Switch Button
                    Button(
                        onClick = { isLightMode = !isLightMode },
                        colors = ButtonDefaults.buttonColors(containerColor = theme.cardBackground),
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.height(32.dp).padding(end = 8.dp)
                    ) {
                        Text(
                            text = if (isLightMode) com.pulso.mobile.presentation.i18n.PulsoStrings.themeLight(currentLanguage) else com.pulso.mobile.presentation.i18n.PulsoStrings.themeDark(currentLanguage),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = theme.primary
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = theme.topBarBackground,
                    titleContentColor = theme.primary
                )
            )
        },
        bottomBar = {
            if (!showOnboarding) {
                NavigationBar(
                    containerColor = theme.topBarBackground
                ) {
                    NavigationBarItem(
                        selected = uiState.selectedTab == 0,
                        onClick = { viewModel.selectTab(0) },
                        label = { Text(com.pulso.mobile.presentation.i18n.PulsoStrings.tabToday(currentLanguage)) },
                        icon = { Text("HOY", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (uiState.selectedTab == 0) theme.primary else theme.textSecondary) }
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 6,
                        onClick = { viewModel.selectTab(6) },
                        label = { Text(com.pulso.mobile.presentation.i18n.PulsoStrings.tabChat(currentLanguage)) },
                        icon = { Text("IA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (uiState.selectedTab == 6) theme.primary else theme.textSecondary) }
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 4,
                        onClick = { viewModel.selectTab(4) },
                        label = { Text(com.pulso.mobile.presentation.i18n.PulsoStrings.tabEvents(currentLanguage)) },
                        icon = { Text("EVT", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (uiState.selectedTab == 4) theme.primary else theme.textSecondary) }
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 7,
                        onClick = { viewModel.selectTab(7) },
                        label = { Text(com.pulso.mobile.presentation.i18n.PulsoStrings.tabBreathe(currentLanguage)) },
                        icon = { Text("RESP", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (uiState.selectedTab == 7) theme.primary else theme.textSecondary) }
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 8,
                        onClick = { viewModel.selectTab(8) },
                        label = { Text(com.pulso.mobile.presentation.i18n.PulsoStrings.tabMap(currentLanguage)) },
                        icon = { Text("MAPA", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (uiState.selectedTab == 8) theme.primary else theme.textSecondary) }
                    )
                    NavigationBarItem(
                        selected = uiState.selectedTab == 2,
                        onClick = { viewModel.selectTab(2) },
                        label = { Text(com.pulso.mobile.presentation.i18n.PulsoStrings.tabSettings(currentLanguage)) },
                        icon = { Text("AJUST", fontSize = 10.sp, fontWeight = FontWeight.Bold, color = if (uiState.selectedTab == 2) theme.primary else theme.textSecondary) }
                    )
                }
            }
        }
    ) { innerPadding ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
            color = theme.background
        ) {
            when {
                showOnboarding -> {
                    OnboardingScreen(
                        theme = theme,
                        language = currentLanguage,
                        onAcceptConsent = { viewModel.grantConsent() }
                    )
                }
                uiState.selectedTab == 0 -> {
                    HoyScreen(
                        windows = todayWindows,
                        latestWindow = latestWindow,
                        baseline = phoneBaseline,
                        breathingSessionTimes = breathingSessions.map { it.completedAt },
                        theme = theme,
                        language = currentLanguage,
                        onNavigateToBaseline = { viewModel.selectTab(3) }
                    )
                }
                uiState.selectedTab == 1 -> {
                    DiagnosticoScreen(
                        diagnostics = diagnostics,
                        baselineMismatch = baselineMismatch,
                        isTestingPing = uiState.isTestingPing,
                        pingFeedback = uiState.pingFeedback,
                        theme = theme,
                        language = currentLanguage,
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
                        theme = theme,
                        language = currentLanguage,
                        onPresetSelected = { viewModel.onPresetSelected(it) },
                        onUpdateThresholds = { hrZ, hrvZ, hrOnlyZ, a, b, cd, cap ->
                            viewModel.onUpdateThresholds(hrZ, hrvZ, hrOnlyZ, a, b, cd, cap)
                        },
                        onUpdateQuietHours = { sh, sm, eh, em -> viewModel.onUpdateQuietHours(sh, sm, eh, em) },
                        onResetDefaults = { viewModel.resetToDefaultConfig() },
                        onSaveConfig = { viewModel.saveConfig() },
                        onOpenLab = { viewModel.selectTab(5) },
                        onNavigateToBaseline = { viewModel.selectTab(3) },
                        onOpenBreathing = { viewModel.selectTab(7) },
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
                    com.pulso.mobile.presentation.components.GeminiVoiceChatScreen(
                        chatManager = chatManager,
                        language = currentLanguage,
                        theme = theme,
                        onBack = { viewModel.selectTab(0) }
                    )
                }
                uiState.selectedTab == 7 -> {
                    com.pulso.mobile.presentation.components.BreathingScreen(
                        currentConfig = uiState.breathingConfig,
                        sessions = breathingSessions,
                        weeklySummary = weeklyBreathingSummary,
                        isTestingOnWatch = uiState.isTestingBreathingOnWatch,
                        testingFeedback = uiState.breathingTestFeedback,
                        onUpdatePlan = { viewModel.updateBreathingPlan(it) },
                        onUpdateDuration = { viewModel.updateBreathingDuration(it) },
                        onUpdateIntensity = { viewModel.updateBreathingIntensity(it) },
                        onTestOnWatch = { viewModel.testBreathingOnWatch() }
                    )
                }
                uiState.selectedTab == 8 -> {
                    val mapReport by viewModel.stressMapReport.collectAsState()
                    com.pulso.mobile.presentation.components.StressMapScreen(
                        report = mapReport,
                        isImportingSample = false,
                        sampleImportFeedback = uiState.csvImportFeedback,
                        onGenerateSampleData = { viewModel.generateDenseSampleData(90) },
                        onClearSampleData = { viewModel.clearSampleData() }
                    )
                }
            }
        }
    }
}

@Composable
fun OnboardingScreen(
    theme: com.pulso.mobile.presentation.theme.PulsoThemeColors,
    language: com.pulso.mobile.presentation.i18n.AppLanguage,
    onAcceptConsent: () -> Unit
) {
    var isChecked by remember { mutableStateOf(true) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(24.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = if (language == com.pulso.mobile.presentation.i18n.AppLanguage.ES) "Bienvenido a Pulso" else "Welcome to Pulso",
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            color = theme.primary
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = if (language == com.pulso.mobile.presentation.i18n.AppLanguage.ES)
                "Tu acompañante de bienestar y autorregulación emocional."
                else "Your companion for emotional regulation and wellness.",
            fontSize = 14.sp,
            color = theme.textPrimary,
            textAlign = TextAlign.Center
        )

        Spacer(modifier = Modifier.height(24.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.border, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Text(
                    text = if (language == com.pulso.mobile.presentation.i18n.AppLanguage.ES) "Aviso de Bienestar y Privacidad" else "Wellness & Privacy Notice",
                    fontWeight = FontWeight.Bold,
                    color = theme.warning,
                    fontSize = 15.sp
                )
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = if (language == com.pulso.mobile.presentation.i18n.AppLanguage.ES)
                        "• Pulso es una herramienta de bienestar, NO un dispositivo médico.\n" +
                        "• Mide periódicamente tu pulso cardíaco para detectar patrones de tensión.\n" +
                        "• Tus datos biométricos permanecen exclusivamente en tus dispositivos (reloj y celular).\n" +
                        "• No vendemos ni compartimos tus datos de salud con terceros.\n" +
                        "• Asistente conversacional con IA para acompañarte en momentos de estrés."
                        else
                        "• Pulso is a wellness tool, NOT a medical device.\n" +
                        "• Periodically analyzes pulse to detect patterns of tension.\n" +
                        "• Biometric data remains strictly on your local devices.\n" +
                        "• We never sell or share your data with third parties.\n" +
                        "• AI emotional companion ready to support you when stressed.",
                    fontSize = 13.sp,
                    color = theme.textSecondary,
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
                text = if (language == com.pulso.mobile.presentation.i18n.AppLanguage.ES)
                    "He leído y acepto la recolección y análisis local de mis datos de pulso."
                    else "I have read and accept local analysis of my pulse data.",
                fontSize = 12.sp,
                color = theme.textPrimary
            )
        }

        Spacer(modifier = Modifier.height(20.dp))

        Button(
            onClick = onAcceptConsent,
            enabled = isChecked,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
        ) {
            Text(
                text = if (language == com.pulso.mobile.presentation.i18n.AppLanguage.ES) "Empezar a usar Pulso" else "Start using Pulso",
                fontSize = 15.sp,
                fontWeight = FontWeight.Bold,
                color = theme.onPrimary
            )
        }
    }
}

@Composable
fun HoyScreen(
    windows: List<MeasurementWindow>,
    latestWindow: MeasurementWindow?,
    baseline: com.pulso.core.baseline.Baseline?,
    breathingSessionTimes: List<Long> = emptyList(),
    theme: com.pulso.mobile.presentation.theme.PulsoThemeColors,
    language: com.pulso.mobile.presentation.i18n.AppLanguage,
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
            .background(theme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = com.pulso.mobile.presentation.i18n.PulsoStrings.todayTitle(language),
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = theme.textPrimary
            )
            Button(
                onClick = onNavigateToBaseline,
                colors = ButtonDefaults.buttonColors(containerColor = theme.cardBackground),
                modifier = Modifier
                    .height(34.dp)
                    .border(1.dp, theme.border, RoundedCornerShape(12.dp))
            ) {
                Text(
                    text = com.pulso.mobile.presentation.i18n.PulsoStrings.yourBaseline(language),
                    fontSize = 12.sp,
                    color = theme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(14.dp))

        // Cards Row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(10.dp)
        ) {
            // HR Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, theme.border, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = theme.cardBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(com.pulso.mobile.presentation.i18n.PulsoStrings.latestPulse(language), fontSize = 12.sp, color = theme.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    val hrText = latestWindow?.let { "${it.meanHr.toInt()} BPM" } ?: com.pulso.mobile.presentation.i18n.PulsoStrings.noData(language)
                    Text(hrText, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = theme.success)
                }
            }

            // HRV Card
            Card(
                modifier = Modifier
                    .weight(1f)
                    .border(1.dp, theme.border, RoundedCornerShape(12.dp)),
                colors = CardDefaults.cardColors(containerColor = theme.cardBackground)
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(com.pulso.mobile.presentation.i18n.PulsoStrings.hrvRmssd(language), fontSize = 12.sp, color = theme.textSecondary)
                    Spacer(modifier = Modifier.height(6.dp))
                    val hrvText = latestWindow?.rmssd?.let { "${it.toInt()} ms" } ?: com.pulso.mobile.presentation.i18n.PulsoStrings.noData(language)
                    Text(hrvText, fontSize = 22.sp, fontWeight = FontWeight.Bold, color = theme.primary)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        // Windows recorded card
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.border, RoundedCornerShape(12.dp)),
            colors = CardDefaults.cardColors(containerColor = theme.cardBackground)
        ) {
            Row(
                modifier = Modifier
                    .padding(14.dp)
                    .fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(com.pulso.mobile.presentation.i18n.PulsoStrings.measurementsToday(language), fontSize = 12.sp, color = theme.textSecondary)
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(com.pulso.mobile.presentation.i18n.PulsoStrings.windowsCount(language, windows.size), fontSize = 16.sp, fontWeight = FontWeight.SemiBold, color = theme.textPrimary)
                }
                latestWindow?.let {
                    val minutesAgo = ((System.currentTimeMillis() - it.windowEnd) / 60000).coerceAtLeast(0)
                    Text(com.pulso.mobile.presentation.i18n.PulsoStrings.minutesAgo(language, minutesAgo), fontSize = 12.sp, color = theme.warning)
                }
            }
        }

        Spacer(modifier = Modifier.height(20.dp))

        // HR Chart with baseline overlay and breathing markers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(com.pulso.mobile.presentation.i18n.PulsoStrings.hrTrendTitle(language), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = theme.textPrimary)
            if (hrBaselineRange != null) {
                Text(
                    com.pulso.mobile.presentation.i18n.PulsoStrings.rangeLabel(language, hrBaselineRange.first.toInt(), hrBaselineRange.second.toInt(), "BPM"),
                    fontSize = 11.sp,
                    color = theme.success
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        SimpleMetricLineChart(
            windows = windows,
            valueExtractor = { it.meanHr },
            lineColor = theme.success,
            emptyMessage = com.pulso.mobile.presentation.i18n.PulsoStrings.emptyHrMessage(language),
            baselineRange = hrBaselineRange,
            breathingSessionTimes = breathingSessionTimes,
            textColor = theme.textSecondary,
            gridColor = theme.border,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(theme.cardBackground)
                .border(1.dp, theme.border, RoundedCornerShape(10.dp))
        )

        Spacer(modifier = Modifier.height(20.dp))

        // HRV Chart with baseline overlay and breathing markers
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(com.pulso.mobile.presentation.i18n.PulsoStrings.hrvTrendTitle(language), fontSize = 14.sp, fontWeight = FontWeight.SemiBold, color = theme.textPrimary)
            if (hrvBaselineRange != null) {
                Text(
                    com.pulso.mobile.presentation.i18n.PulsoStrings.rangeLabel(language, hrvBaselineRange.first.toInt(), hrvBaselineRange.second.toInt(), "ms"),
                    fontSize = 11.sp,
                    color = theme.primary
                )
            }
        }
        Spacer(modifier = Modifier.height(6.dp))
        SimpleMetricLineChart(
            windows = windows,
            valueExtractor = { it.rmssd },
            lineColor = theme.primary,
            emptyMessage = com.pulso.mobile.presentation.i18n.PulsoStrings.emptyHrvMessage(language),
            baselineRange = hrvBaselineRange,
            breathingSessionTimes = breathingSessionTimes,
            textColor = theme.textSecondary,
            gridColor = theme.border,
            modifier = Modifier
                .clip(RoundedCornerShape(10.dp))
                .background(theme.cardBackground)
                .border(1.dp, theme.border, RoundedCornerShape(10.dp))
        )
    }
}

@Composable
fun DiagnosticoScreen(
    diagnostics: MobileDiagnosticsState,
    baselineMismatch: String?,
    isTestingPing: Boolean,
    pingFeedback: String?,
    theme: com.pulso.mobile.presentation.theme.PulsoThemeColors,
    language: com.pulso.mobile.presentation.i18n.AppLanguage,
    onTestConnection: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(com.pulso.mobile.presentation.i18n.PulsoStrings.diagnosticsTitle(language), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
        Spacer(modifier = Modifier.height(16.dp))

        // Mismatch Alert if detected
        baselineMismatch?.let { mismatchText ->
            Card(
                colors = CardDefaults.cardColors(containerColor = theme.error.copy(alpha = 0.15f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .border(1.dp, theme.error, RoundedCornerShape(12.dp))
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    Text(
                        if (language == com.pulso.mobile.presentation.i18n.AppLanguage.ES) "ALERTA: Discrepancia de Línea Base" else "ALERT: Baseline Discrepancy",
                        fontWeight = FontWeight.Bold,
                        color = theme.error,
                        fontSize = 14.sp
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(mismatchText, color = theme.textPrimary, fontSize = 12.sp)
                }
            }
            Spacer(modifier = Modifier.height(10.dp))
        }

        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.border, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(com.pulso.mobile.presentation.i18n.PulsoStrings.consentCardTitle(language), fontWeight = FontWeight.Bold, color = theme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                val statusText = if (diagnostics.consentGranted) {
                    if (language == com.pulso.mobile.presentation.i18n.AppLanguage.ES) "Aceptado" else "Granted"
                } else {
                    if (language == com.pulso.mobile.presentation.i18n.AppLanguage.ES) "No otorgado" else "Revoked"
                }
                Text("• Estado: $statusText", color = theme.textPrimary, fontSize = 13.sp)
                Text("• Versión: ${diagnostics.consentVersion ?: "N/A"}", color = theme.textSecondary, fontSize = 12.sp)
                diagnostics.consentTimestamp?.let {
                    Text("• Fecha: ${Date(it)}", color = theme.textSecondary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.border, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(com.pulso.mobile.presentation.i18n.PulsoStrings.watchConnectionTitle(language), fontWeight = FontWeight.Bold, color = theme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                val connStatus = if (diagnostics.watchConnected) {
                    if (language == com.pulso.mobile.presentation.i18n.AppLanguage.ES) "SÍ (${diagnostics.connectedWatchNodeName})" else "YES (${diagnostics.connectedWatchNodeName})"
                } else {
                    if (language == com.pulso.mobile.presentation.i18n.AppLanguage.ES) "NO" else "NO"
                }
                Text("• Conectado: $connStatus", color = theme.textPrimary, fontSize = 13.sp)
                diagnostics.lastWatchStatus?.let { status ->
                    Text("• Batería: ${status.batteryPct}%", color = theme.textPrimary, fontSize = 13.sp)
                    Text("• Servicio: ${if (status.serviceRunning) "Activo" else "Detenido"}", color = theme.textPrimary, fontSize = 13.sp)
                    Text("• Modelo: ${status.watchModel}", color = theme.textSecondary, fontSize = 12.sp)
                }
            }
        }

        Spacer(modifier = Modifier.height(10.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.border, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(com.pulso.mobile.presentation.i18n.PulsoStrings.storageSyncTitle(language), fontWeight = FontWeight.Bold, color = theme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text("• Ventanas: ${diagnostics.totalWindowsStored}", color = theme.textPrimary, fontSize = 13.sp)
                Text("• Lote: ${diagnostics.lastSyncBatchId ?: "Ninguno"}", color = theme.textSecondary, fontSize = 12.sp)
                diagnostics.lastError?.let { err ->
                    Text("• Error: $err", color = theme.error, fontSize = 12.sp, fontWeight = FontWeight.SemiBold)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Button(
            onClick = onTestConnection,
            enabled = !isTestingPing,
            modifier = Modifier
                .fillMaxWidth()
                .height(44.dp),
            colors = ButtonDefaults.buttonColors(containerColor = theme.primary)
        ) {
            if (isTestingPing) {
                CircularProgressIndicator(strokeWidth = 2.dp, color = theme.onPrimary)
            } else {
                Text(com.pulso.mobile.presentation.i18n.PulsoStrings.testWatchConnection(language), fontSize = 13.sp, color = theme.onPrimary)
            }
        }

        pingFeedback?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                it,
                color = if (it.contains("exitoso") || it.contains("success")) theme.success else theme.error,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
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
    theme: com.pulso.mobile.presentation.theme.PulsoThemeColors,
    language: com.pulso.mobile.presentation.i18n.AppLanguage,
    onPresetSelected: (com.pulso.mobile.domain.stress.SensitivityPreset) -> Unit,
    onUpdateThresholds: (hrZ: Double, hrvZ: Double, hrOnlyZ: Double, consecA: Int, consecB: Int, cooldown: Int, dailyCap: Int) -> Unit,
    onUpdateQuietHours: (startH: Int, startM: Int, endH: Int, endM: Int) -> Unit,
    onResetDefaults: () -> Unit,
    onSaveConfig: () -> Unit,
    onOpenLab: () -> Unit,
    onNavigateToBaseline: () -> Unit,
    onOpenBreathing: () -> Unit,
    onCancelSnooze: () -> Unit,
    onApplyAdaptiveSuggestion: () -> Unit,
    onDismissAdaptiveSuggestion: () -> Unit,
    onRevokeConsent: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(theme.background)
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(com.pulso.mobile.presentation.i18n.PulsoStrings.settingsTitle(language), fontSize = 20.sp, fontWeight = FontWeight.Bold, color = theme.textPrimary)
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
            colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.border, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(com.pulso.mobile.presentation.i18n.PulsoStrings.breathingHapticTitle(language), fontWeight = FontWeight.Bold, color = theme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    com.pulso.mobile.presentation.i18n.PulsoStrings.breathingHapticDesc(language),
                    fontSize = 12.sp,
                    color = theme.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onOpenBreathing,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.accent),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text(com.pulso.mobile.presentation.i18n.PulsoStrings.configureBreathing(language), fontSize = 13.sp, color = Color.White)
                }
            }
        }

        Spacer(modifier = Modifier.height(14.dp))

        Card(
            colors = CardDefaults.cardColors(containerColor = theme.cardBackground),
            modifier = Modifier
                .fillMaxWidth()
                .border(1.dp, theme.border, RoundedCornerShape(12.dp))
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(com.pulso.mobile.presentation.i18n.PulsoStrings.baselineCardTitle(language), fontWeight = FontWeight.Bold, color = theme.primary, fontSize = 14.sp)
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    com.pulso.mobile.presentation.i18n.PulsoStrings.baselineCardDesc(language),
                    fontSize = 12.sp,
                    color = theme.textSecondary
                )
                Spacer(modifier = Modifier.height(10.dp))
                Button(
                    onClick = onNavigateToBaseline,
                    colors = ButtonDefaults.buttonColors(containerColor = theme.primary),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(40.dp)
                ) {
                    Text(com.pulso.mobile.presentation.i18n.PulsoStrings.viewMyBaseline(language), fontSize = 13.sp, color = theme.onPrimary)
                }
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            com.pulso.mobile.presentation.i18n.PulsoStrings.revokeConsentWarning(language),
            fontSize = 13.sp,
            color = theme.textSecondary
        )

        Spacer(modifier = Modifier.height(14.dp))

        Button(
            onClick = onRevokeConsent,
            colors = ButtonDefaults.buttonColors(containerColor = theme.error),
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
        ) {
            Text(com.pulso.mobile.presentation.i18n.PulsoStrings.revokeConsent(language), fontWeight = FontWeight.Bold, color = Color.White)
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
        Surface(
            color = Color(0xFF1E293B),
            shape = RoundedCornerShape(16.dp),
            modifier = Modifier.padding(bottom = 16.dp)
        ) {
            Text(
                "PULSO ASISTENTE",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF38BDF8),
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
            )
        }
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
