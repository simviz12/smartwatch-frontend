package com.pulso.mobile.presentation

import android.content.Context
import android.graphics.Color as AndroidColor
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.isImeVisible
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.pulso.mobile.data.chat.PulsoGeminiChatManager
import com.pulso.mobile.data.local.PulsoMobileDatabase
import com.pulso.mobile.data.sync.MobileSyncManager
import com.pulso.mobile.domain.stress.SensitivityPreset
import com.pulso.mobile.presentation.components.NavDest
import com.pulso.mobile.presentation.components.PulsoBottomBar
import com.pulso.mobile.presentation.i18n.AppLanguage
import com.pulso.mobile.presentation.screens.AssistantScreen
import com.pulso.mobile.presentation.screens.BaselineScreen
import com.pulso.mobile.presentation.screens.BreatheScreen
import com.pulso.mobile.presentation.screens.DiagnosticsScreen
import com.pulso.mobile.presentation.screens.HistoryScreen
import com.pulso.mobile.presentation.screens.LabScreen
import com.pulso.mobile.presentation.screens.OnboardingScreen
import com.pulso.mobile.presentation.screens.SettingsScreen
import com.pulso.mobile.presentation.screens.TodayScreen
import com.pulso.mobile.presentation.theme.DarkPulsoColors
import com.pulso.mobile.presentation.theme.LightPulsoColors
import com.pulso.mobile.presentation.theme.PulsoTheme
import com.pulso.mobile.presentation.theme.ThemeMode
import java.util.Calendar

/** Tab indices kept stable because notifications/intents route by number (6 = assistant). */
object Tabs {
    const val TODAY = 0
    const val DIAGNOSTICS = 1
    const val SETTINGS = 2
    const val BASELINE = 3
    const val EVENTS = 4
    const val LAB = 5
    const val ASSISTANT = 6
    const val BREATHE = 7
    const val MAP = 8
}

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
            PulsoApp(
                viewModel = viewModel,
                chatManager = PulsoGeminiChatManager.getInstance(this),
                onSystemBars = { light -> applySystemBars(light) }
            )
        }
    }

    private fun applySystemBars(light: Boolean) {
        val transparent = AndroidColor.TRANSPARENT
        enableEdgeToEdge(
            statusBarStyle = if (light) SystemBarStyle.light(transparent, transparent) else SystemBarStyle.dark(transparent),
            navigationBarStyle = if (light) SystemBarStyle.light(transparent, transparent) else SystemBarStyle.dark(transparent)
        )
    }

    override fun onNewIntent(intent: android.content.Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleChatIntent(intent)
    }

    private fun handleChatIntent(intent: android.content.Intent?) {
        val route = intent?.getStringExtra("route")
        if (route == "chat_placeholder" || route == "chat") {
            viewModel.selectTab(Tabs.ASSISTANT)
        }
    }
}

private fun screenOrder(tab: Int): Int = when (tab) {
    Tabs.TODAY -> 0
    Tabs.EVENTS, Tabs.MAP -> 1
    Tabs.BREATHE -> 2
    Tabs.ASSISTANT -> 3
    Tabs.SETTINGS -> 4
    Tabs.BASELINE -> 5
    Tabs.LAB -> 6
    else -> 7
}

private const val PREFS = "pulso_ui"
private const val KEY_THEME = "theme_mode"
private const val KEY_LANG = "language"

@Composable
fun PulsoApp(
    viewModel: MobileViewModel,
    chatManager: PulsoGeminiChatManager,
    onSystemBars: (Boolean) -> Unit
) {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
    var themeMode by remember {
        mutableStateOf(runCatching { ThemeMode.valueOf(prefs.getString(KEY_THEME, ThemeMode.DARK.name)!!) }.getOrDefault(ThemeMode.DARK))
    }
    var language by remember {
        mutableStateOf(runCatching { AppLanguage.valueOf(prefs.getString(KEY_LANG, AppLanguage.ES.name)!!) }.getOrDefault(AppLanguage.ES))
    }
    val systemDark = isSystemInDarkTheme()
    val light = when (themeMode) {
        ThemeMode.SYSTEM -> !systemDark
        ThemeMode.LIGHT -> true
        ThemeMode.DARK -> false
    }
    LaunchedEffect(light) { onSystemBars(light) }

    val setTheme: (ThemeMode) -> Unit = { themeMode = it; prefs.edit().putString(KEY_THEME, it.name).apply() }
    val setLanguage: (AppLanguage) -> Unit = { language = it; prefs.edit().putString(KEY_LANG, it.name).apply() }

    PulsoTheme(colors = if (light) LightPulsoColors else DarkPulsoColors, language = language) {
        PulsoNavigation(
            viewModel = viewModel,
            chatManager = chatManager,
            themeMode = themeMode,
            language = language,
            isLight = light,
            onTheme = setTheme,
            onLanguage = setLanguage
        )
    }
}

@OptIn(androidx.compose.foundation.layout.ExperimentalLayoutApi::class)
@Composable
private fun PulsoNavigation(
    viewModel: MobileViewModel,
    chatManager: PulsoGeminiChatManager,
    themeMode: ThemeMode,
    language: AppLanguage,
    isLight: Boolean,
    onTheme: (ThemeMode) -> Unit,
    onLanguage: (AppLanguage) -> Unit
) {
    val c = com.pulso.mobile.presentation.theme.Pulso.colors
    val uiState by viewModel.uiState.collectAsState()
    val diagnostics by viewModel.diagnosticsState.collectAsState()
    val todayWindows by viewModel.todayWindows.collectAsState()
    val latestWindow by viewModel.latestWindow.collectAsState()
    val baseline by viewModel.phoneBaseline.collectAsState()
    val baselineMismatch by viewModel.baselineMismatch.collectAsState()
    val breathingSessions by viewModel.breathingSessions.collectAsState()
    val weeklySummary by viewModel.weeklyBreathingSummary.collectAsState()
    val events by viewModel.stressEventEntities.collectAsState()
    val qualityMetrics by viewModel.qualityMetrics.collectAsState()
    val mapReport by viewModel.stressMapReport.collectAsState()

    var diagnosticsReturn by rememberSaveable { mutableIntStateOf(Tabs.SETTINGS) }
    val tab = uiState.selectedTab
    val imeVisible = WindowInsets.isImeVisible

    LaunchedEffect(language) { chatManager.initInitialGreeting(language.name) }

    Box(Modifier.fillMaxSize().background(c.bg)) {
        if (!diagnostics.consentGranted) {
            Box(Modifier.fillMaxSize().statusBarsPadding().navigationBarsPadding()) {
                OnboardingScreen(
                    language = language,
                    onLanguage = onLanguage,
                    onToggleTheme = { onTheme(if (isLight) ThemeMode.DARK else ThemeMode.LIGHT) },
                    onAccept = { viewModel.grantConsent() }
                )
            }
            return@Box
        }

        val subScreenParent = when (tab) {
            Tabs.BASELINE, Tabs.LAB -> Tabs.SETTINGS
            Tabs.DIAGNOSTICS -> diagnosticsReturn
            else -> null
        }
        BackHandler(enabled = subScreenParent != null) { viewModel.selectTab(subScreenParent ?: Tabs.TODAY) }

        val dest = when (tab) {
            Tabs.TODAY -> NavDest.TODAY
            Tabs.EVENTS, Tabs.MAP -> NavDest.HISTORY
            Tabs.BREATHE -> NavDest.BREATHE
            Tabs.ASSISTANT -> NavDest.ASSISTANT
            Tabs.DIAGNOSTICS -> if (diagnosticsReturn == Tabs.TODAY) NavDest.TODAY else NavDest.SETTINGS
            else -> NavDest.SETTINGS
        }

        Column(Modifier.fillMaxSize()) {
            Box(Modifier.weight(1f).fillMaxWidth().statusBarsPadding().imePadding()) {
                AnimatedContent(
                    targetState = if (tab == Tabs.MAP) Tabs.EVENTS else tab,
                    transitionSpec = {
                        // Slide in the direction of travel along the bottom bar; sub-screens come from the right.
                        val forward = screenOrder(targetState) >= screenOrder(initialState)
                        val dir = if (forward) 1 else -1
                        (fadeIn(tween(260, delayMillis = 60)) + slideInHorizontally(tween(340, easing = FastOutSlowInEasing)) { it / 7 * dir })
                            .togetherWith(fadeOut(tween(140)) + slideOutHorizontally(tween(280, easing = FastOutSlowInEasing)) { -it / 10 * dir })
                    },
                    label = "screen"
                ) { screen ->
                    when (screen) {
                        Tabs.TODAY -> {
                            val dayStart = remember {
                                Calendar.getInstance().apply {
                                    set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                                }.timeInMillis
                            }
                            TodayScreen(
                                windows = todayWindows,
                                latestWindow = latestWindow,
                                baseline = baseline,
                                todayEvents = events.filter { it.time >= dayStart },
                                breathingSessionTimes = breathingSessions.map { it.completedAt },
                                watchConnected = diagnostics.watchConnected,
                                breathingMinutes = uiState.breathingConfig.durationMinutes,
                                onOpenDiagnostics = { diagnosticsReturn = Tabs.TODAY; viewModel.selectTab(Tabs.DIAGNOSTICS) },
                                onBreathe = { viewModel.selectTab(Tabs.BREATHE) },
                                onOpenEvents = { viewModel.selectTab(Tabs.EVENTS) }
                            )
                        }
                        Tabs.EVENTS -> HistoryScreen(
                            initialTab = if (tab == Tabs.MAP) 1 else 0,
                            events = events,
                            report = mapReport,
                            sampleFeedback = uiState.csvImportFeedback,
                            onTabChange = { viewModel.selectTab(if (it == 1) Tabs.MAP else Tabs.EVENTS) },
                            onUpdateFeedback = { id, fb -> viewModel.updateEventFeedback(id, fb) },
                            onGenerateSampleData = { viewModel.generateDenseSampleData(30) },
                            onClearAllData = { viewModel.clearSampleData() }
                        )
                        Tabs.BREATHE -> BreatheScreen(
                            config = uiState.breathingConfig,
                            sessions = breathingSessions,
                            weeklySummary = weeklySummary,
                            isStartingOnWatch = uiState.isTestingBreathingOnWatch,
                            feedback = uiState.breathingTestFeedback,
                            onUpdatePlan = { viewModel.updateBreathingPlan(it) },
                            onUpdateDuration = { viewModel.updateBreathingDuration(it) },
                            onUpdateIntensity = { viewModel.updateBreathingIntensity(it) },
                            onStartOnWatch = { viewModel.testBreathingOnWatch() }
                        )
                        Tabs.ASSISTANT -> AssistantScreen(
                            chatManager = chatManager,
                            onStartBreathing = { viewModel.selectTab(Tabs.BREATHE) }
                        )
                        Tabs.SETTINGS -> SettingsScreen(
                            themeMode = themeMode,
                            language = language,
                            config = uiState.stressConfig,
                            preset = uiState.sensitivityPreset,
                            quietHours = uiState.quietHours,
                            qualityMetrics = qualityMetrics,
                            adaptiveSuggestion = diagnostics.adaptiveSuggestion.takeUnless { uiState.adaptiveSuggestionDismissed },
                            isSaving = uiState.isSavingConfig,
                            saveFeedback = uiState.configSaveFeedback,
                            onThemeMode = onTheme,
                            onLanguage = onLanguage,
                            onPreset = { viewModel.onPresetSelected(it) },
                            onUpdateThresholds = { hrZ, hrvZ, hrOnlyZ, a, b, cd, cap -> viewModel.onUpdateThresholds(hrZ, hrvZ, hrOnlyZ, a, b, cd, cap) },
                            onQuietHours = { viewModel.onUpdateQuietHours(it.startHour, it.startMinute, it.endHour, it.endMinute) },
                            onResetDefaults = { viewModel.resetToDefaultConfig() },
                            onSave = { viewModel.saveConfig() },
                            onCancelSnooze = { viewModel.cancelSnooze() },
                            onApplySuggestion = { viewModel.applyAdaptiveSuggestion() },
                            onDismissSuggestion = { viewModel.dismissAdaptiveSuggestion() },
                            onOpenBaseline = { viewModel.selectTab(Tabs.BASELINE) },
                            onOpenLab = { viewModel.selectTab(Tabs.LAB) },
                            onOpenDiagnostics = { diagnosticsReturn = Tabs.SETTINGS; viewModel.selectTab(Tabs.DIAGNOSTICS) },
                            onRevokeConsent = { viewModel.revokeConsent() },
                            onWipeData = { viewModel.wipeAllUserData() }
                        )
                        Tabs.BASELINE -> BaselineScreen(
                            baseline = baseline,
                            latestWindow = latestWindow,
                            isResetting = uiState.isResettingBaseline,
                            resetFeedback = uiState.resetFeedback,
                            onReset = { viewModel.resetBaseline() },
                            onBack = { viewModel.selectTab(Tabs.SETTINGS) }
                        )
                        Tabs.LAB -> LabScreen(
                            daysRange = uiState.replayDaysRange,
                            preset = uiState.replayPreset ?: uiState.sensitivityPreset,
                            isRunning = uiState.isRunningReplay,
                            result = uiState.replayResult,
                            comparison = uiState.replayComparison,
                            feedback = uiState.replayFeedback,
                            onDaysRange = { viewModel.setReplayDaysRange(it) },
                            onPreset = { viewModel.setReplayPreset(it) },
                            onRun = { viewModel.runReplaySimulation() },
                            onApply = { preset ->
                                viewModel.applyLabConfigToSettings(SensitivityPreset.toConfig(preset, uiState.stressConfig))
                                viewModel.selectTab(Tabs.SETTINGS)
                            },
                            onBack = { viewModel.selectTab(Tabs.SETTINGS) }
                        )
                        Tabs.DIAGNOSTICS -> DiagnosticsScreen(
                            diagnostics = diagnostics,
                            baselineMismatch = baselineMismatch,
                            isTesting = uiState.isTestingPing,
                            pingFeedback = uiState.pingFeedback,
                            onTestConnection = { viewModel.testWatchConnection() },
                            onBack = { viewModel.selectTab(diagnosticsReturn) }
                        )
                    }
                }
            }
            if (!imeVisible) PulsoBottomBar(selected = dest) { d ->
                viewModel.selectTab(
                    when (d) {
                        NavDest.TODAY -> Tabs.TODAY
                        NavDest.HISTORY -> Tabs.EVENTS
                        NavDest.BREATHE -> Tabs.BREATHE
                        NavDest.ASSISTANT -> Tabs.ASSISTANT
                        NavDest.SETTINGS -> Tabs.SETTINGS
                    }
                )
            }
        }
    }
}
