package com.pulso.mobile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pulso.core.baseline.Baseline
import com.pulso.core.baseline.BaselineEngine
import com.pulso.core.model.MeasurementWindow
import com.pulso.core.model.QuietHours
import com.pulso.core.stress.StressConfig
import com.pulso.core.stress.StressEvent
import com.pulso.mobile.data.local.PhoneStressEventDao
import com.pulso.mobile.data.local.PhoneWindowDao
import com.pulso.mobile.data.sync.MobileDiagnosticsState
import com.pulso.mobile.data.sync.MobileSyncManager
import com.pulso.mobile.domain.stress.ReplayLabRunner
import com.pulso.mobile.domain.stress.ReplaySimulationResult
import com.pulso.mobile.domain.stress.SensitivityPreset
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class MobileUiState(
    val selectedTab: Int = 0, // 0 = Hoy, 1 = Diagnóstico, 2 = Ajustes, 3 = Línea Base, 4 = Eventos, 5 = Laboratorio
    val isTestingPing: Boolean = false,
    val pingFeedback: String? = null,
    val isResettingBaseline: Boolean = false,
    val resetFeedback: String? = null,
    // Settings state
    val stressConfig: StressConfig = StressConfig(),
    val quietHours: QuietHours = QuietHours(22, 0, 7, 0),
    val sensitivityPreset: SensitivityPreset = SensitivityPreset.MEDIA,
    val configSaveFeedback: String? = null,
    val isSavingConfig: Boolean = false,
    // Breathing state
    val breathingConfig: com.pulso.core.breathing.BreathingConfig = com.pulso.core.breathing.BreathingConfig(),
    val isTestingBreathingOnWatch: Boolean = false,
    val breathingTestFeedback: String? = null,
    // Replay Lab state
    val isRunningReplay: Boolean = false,
    val replayResult: ReplaySimulationResult? = null,
    val replayDaysRange: Int = 3, // Default replay last 3 days
    val replayFeedback: String? = null,
    // Debug CSV import state
    val csvImportFeedback: String? = null
)

class MobileViewModel(
    private val syncManager: MobileSyncManager,
    private val windowDao: PhoneWindowDao,
    private val stressEventDao: PhoneStressEventDao,
    private val breathingSessionDao: com.pulso.mobile.data.local.PhoneBreathingSessionDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadInitialUiState())
    val uiState: StateFlow<MobileUiState> = _uiState.asStateFlow()

    val diagnosticsState: StateFlow<MobileDiagnosticsState> = syncManager.diagnosticsState

    val breathingSessions: StateFlow<List<com.pulso.mobile.data.local.PhoneBreathingSessionEntity>> = breathingSessionDao
        .getAllSessionsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val weeklyBreathingSummary: StateFlow<com.pulso.mobile.domain.breathing.BreathingWeeklySummary> = breathingSessions
        .map { list -> com.pulso.mobile.domain.breathing.BreathingHistoryAggregator.computeWeeklySummary(list) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.pulso.mobile.domain.breathing.BreathingWeeklySummary(0, 0, 0, 0))

    private val startOfToday: Long = Calendar.getInstance().apply {
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

    val todayWindows: StateFlow<List<MeasurementWindow>> = windowDao
        .getWindowsForTodayFlow(startOfToday)
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val latestWindow: StateFlow<MeasurementWindow?> = windowDao
        .getLatestWindowFlow()
        .map { it?.toDomain() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val stressEventEntities: StateFlow<List<com.pulso.mobile.data.local.PhoneStressEventEntity>> = stressEventDao
        .getAllEventsFlow()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stressEvents: StateFlow<List<StressEvent>> = stressEventEntities
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    // 30-day intervention quality metrics
    private val thirtyDaysAgo: Long
        get() = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000)

    val qualityMetrics: StateFlow<com.pulso.mobile.domain.intervention.QualityMetrics> = stressEventEntities
        .map { list ->
            val recentEvents = list.filter { it.time >= thirtyDaysAgo }
            com.pulso.mobile.domain.intervention.InterventionQualityAggregator.computeMetrics(recentEvents)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), com.pulso.mobile.domain.intervention.InterventionQualityAggregator.computeMetrics(emptyList()))

    // Compute rolling 14-day baseline from phone database
    private val fourteenDaysAgo: Long
        get() = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000)

    val phoneBaseline: StateFlow<Baseline?> = windowDao
        .getWindowsSinceFlow(fourteenDaysAgo)
        .map { list ->
            val domainWindows = list.map { it.toDomain() }
            BaselineEngine.update(domainWindows)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Compare phone baseline with watch reported baselineStatus
    val baselineMismatch: StateFlow<String?> = combine(
        phoneBaseline,
        diagnosticsState
    ) { baseline, diag ->
        com.pulso.mobile.domain.baseline.BaselineMismatchDetector.checkMismatch(
            phoneBaseline = baseline,
            watchStatusMap = diag.lastWatchStatus?.baselineStatus
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    val allWindows: StateFlow<List<MeasurementWindow>> = windowDao
        .getAllWindowsFlow()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val stressMapReport: StateFlow<com.pulso.core.aggregation.StressMapReport> = combine(
        allWindows,
        stressEvents,
        breathingSessions
    ) { windows: List<MeasurementWindow>, events: List<StressEvent>, breathings: List<com.pulso.mobile.data.local.PhoneBreathingSessionEntity> ->
        val feelings = breathings.mapNotNull { it.feeling }
        val durations = breathings.map { ((it.completedAt - it.startedAt) / 1000).toInt() }
        com.pulso.core.aggregation.StressMapAggregator.aggregate(
            windows = windows,
            stressEvents = events,
            breathingFeelings = feelings,
            breathingDurations = durations
        )
    }.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5000),
        com.pulso.core.aggregation.StressMapAggregator.aggregate(emptyList(), emptyList())
    )

    private fun loadInitialUiState(): MobileUiState {
        val configSync = syncManager.loadPersistedConfig()
        val preset = SensitivityPreset.fromConfig(configSync.stressConfig)
        return MobileUiState(
            stressConfig = configSync.stressConfig,
            quietHours = configSync.quietHours ?: QuietHours(22, 0, 7, 0),
            sensitivityPreset = preset,
            breathingConfig = configSync.breathingConfig
        )
    }

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

    // --- Settings & Sensitivity Actions ---

    fun onPresetSelected(preset: SensitivityPreset) {
        if (preset == SensitivityPreset.PERSONALIZADO) {
            _uiState.value = _uiState.value.copy(sensitivityPreset = preset)
        } else {
            val newConfig = SensitivityPreset.toConfig(preset, _uiState.value.stressConfig)
            _uiState.value = _uiState.value.copy(
                sensitivityPreset = preset,
                stressConfig = newConfig
            )
        }
    }

    fun onUpdateThresholds(
        hrZ: Double = _uiState.value.stressConfig.hrZThreshold,
        hrvZ: Double = _uiState.value.stressConfig.hrvZThreshold,
        hrOnlyZ: Double = _uiState.value.stressConfig.hrOnlyZThreshold,
        consecA: Int = _uiState.value.stressConfig.consecutiveWindowsRuleA,
        consecB: Int = _uiState.value.stressConfig.consecutiveWindowsRuleB,
        cooldown: Int = _uiState.value.stressConfig.cooldownMinutes,
        dailyCap: Int = _uiState.value.stressConfig.dailyCap
    ) {
        // Enforce validations from :core
        val validatedHrZ = hrZ.coerceIn(0.5, 3.0)
        val validatedHrvZ = hrvZ.coerceIn(-3.0, -0.5)
        val validatedHrOnlyZ = hrOnlyZ.coerceIn(0.8, 4.0)
        val validatedConsecA = consecA.coerceIn(1, 10)
        val validatedConsecB = consecB.coerceIn(1, 10)
        val validatedCooldown = cooldown.coerceIn(10, 240)
        val validatedDailyCap = dailyCap.coerceIn(1, 20)

        val updated = _uiState.value.stressConfig.copy(
            hrZThreshold = validatedHrZ,
            hrvZThreshold = validatedHrvZ,
            hrOnlyZThreshold = validatedHrOnlyZ,
            consecutiveWindowsRuleA = validatedConsecA,
            consecutiveWindowsRuleB = validatedConsecB,
            cooldownMinutes = validatedCooldown,
            dailyCap = validatedDailyCap
        )
        val preset = SensitivityPreset.fromConfig(updated)
        _uiState.value = _uiState.value.copy(
            stressConfig = updated,
            sensitivityPreset = preset
        )
    }

    fun onUpdateQuietHours(startH: Int, startM: Int, endH: Int, endM: Int) {
        _uiState.value = _uiState.value.copy(
            quietHours = QuietHours(startH, startM, endH, endM)
        )
    }

    fun resetToDefaultConfig() {
        val defaultConfig = StressConfig()
        val defaultQuiet = QuietHours(22, 0, 7, 0)
        _uiState.value = _uiState.value.copy(
            stressConfig = defaultConfig,
            quietHours = defaultQuiet,
            sensitivityPreset = SensitivityPreset.MEDIA,
            configSaveFeedback = null
        )
    }

    fun saveConfig() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSavingConfig = true, configSaveFeedback = null)
            val success = syncManager.updateStressAndQuietConfig(
                stressConfig = _uiState.value.stressConfig,
                quietHours = _uiState.value.quietHours
            )
            _uiState.value = _uiState.value.copy(
                isSavingConfig = false,
                configSaveFeedback = if (success) "Configuración sincronizada exitosamente con el reloj" else "Guardado localmente. Se enviará al conectar el reloj."
            )
        }
    }

    fun updateEventFeedback(eventId: String, feedback: String) {
        viewModelScope.launch {
            stressEventDao.updateFeedback(eventId, feedback)
        }
    }

    fun cancelSnooze() {
        viewModelScope.launch {
            val current = _uiState.value.stressConfig
            val cleared = current.copy(snoozeUntil = null)
            _uiState.value = _uiState.value.copy(stressConfig = cleared)
            saveConfig()
        }
    }

    fun applyAdaptiveSuggestion() {
        viewModelScope.launch {
            // Suggestion: raise thresholds by switching to BAJA sensitivity preset or adjusting z-scores
            onPresetSelected(SensitivityPreset.BAJA)
            saveConfig()
        }
    }

    fun dismissAdaptiveSuggestion() {
        // Clear suggestion in diagnostics
    }

    // --- Breathing Actions ---

    fun updateBreathingPlan(planName: String) {
        val current = _uiState.value.breathingConfig
        val updated = current.copy(preferredPlanName = planName)
        _uiState.value = _uiState.value.copy(breathingConfig = updated)
        saveBreathingConfig(updated)
    }

    fun updateBreathingDuration(durationMinutes: Int) {
        val current = _uiState.value.breathingConfig
        val updated = current.copy(durationMinutes = durationMinutes)
        _uiState.value = _uiState.value.copy(breathingConfig = updated)
        saveBreathingConfig(updated)
    }

    fun updateBreathingIntensity(intensity: String) {
        val current = _uiState.value.breathingConfig
        val updated = current.copy(hapticIntensity = intensity)
        _uiState.value = _uiState.value.copy(breathingConfig = updated)
        saveBreathingConfig(updated)
    }

    private fun saveBreathingConfig(config: com.pulso.core.breathing.BreathingConfig) {
        viewModelScope.launch {
            syncManager.updateBreathingConfig(config)
        }
    }

    fun testBreathingOnWatch() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingBreathingOnWatch = true, breathingTestFeedback = null)
            val success = syncManager.sendStartBreathingCommand()
            _uiState.value = _uiState.value.copy(
                isTestingBreathingOnWatch = false,
                breathingTestFeedback = if (success) "Sesión iniciada en el reloj" else "No se pudo conectar al reloj"
            )
        }
    }

    // --- Replay Lab Actions ---

    fun setReplayDaysRange(days: Int) {
        _uiState.value = _uiState.value.copy(replayDaysRange = days.coerceIn(1, 14))
    }

    fun runReplaySimulation() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRunningReplay = true, replayFeedback = null)
            val since = System.currentTimeMillis() - (_uiState.value.replayDaysRange * 24L * 60 * 60 * 1000)
            val entities = windowDao.getWindowsSince(since)
            val windows = entities.map { it.toDomain() }

            if (windows.isEmpty()) {
                _uiState.value = _uiState.value.copy(
                    isRunningReplay = false,
                    replayResult = null,
                    replayFeedback = "No hay mediciones registradas en el período seleccionado (${_uiState.value.replayDaysRange} días)."
                )
                return@launch
            }

            val result = ReplayLabRunner.runReplay(
                windows = windows,
                baseline = phoneBaseline.value,
                config = _uiState.value.stressConfig,
                quietHours = _uiState.value.quietHours
            )

            _uiState.value = _uiState.value.copy(
                isRunningReplay = false,
                replayResult = result,
                replayFeedback = null
            )
        }
    }

    fun applyLabConfigToSettings(config: StressConfig) {
        _uiState.value = _uiState.value.copy(
            stressConfig = config,
            sensitivityPreset = SensitivityPreset.fromConfig(config)
        )
        saveConfig()
    }

    // --- CSV Importer (Debug) ---

    fun importDebugCsv(inputStream: java.io.InputStream) {
        viewModelScope.launch {
            try {
                val parsedWindows = com.pulso.mobile.data.debug.DebugCsvImporter.parseCsv(inputStream)
                if (parsedWindows.isNotEmpty()) {
                    val entities = parsedWindows.map { com.pulso.mobile.data.local.PhoneWindowEntity.fromDomain(it, "csv-imported") }
                    windowDao.insertOrUpdateAll(entities)
                    val newCount = windowDao.countWindows()
                    _uiState.value = _uiState.value.copy(
                        csvImportFeedback = "Se importaron ${entities.size} mediciones exitosamente ($newCount en total)."
                    )
                } else {
                    _uiState.value = _uiState.value.copy(csvImportFeedback = "El archivo CSV no contiene mediciones válidas.")
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(csvImportFeedback = "Error al importar CSV: ${e.message}")
            }
        }
    }

    fun generateDenseSampleData(days: Int = 90) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(csvImportFeedback = "Generando $days días de datos densos...")
            val now = System.currentTimeMillis()
            val windowDurationMs = 5 * 60 * 1000L // 5 min
            val totalWindows = (days * 24 * 12) // 12 windows per hour

            val generatedWindows = mutableListOf<com.pulso.mobile.data.local.PhoneWindowEntity>()
            val generatedEvents = mutableListOf<com.pulso.mobile.data.local.PhoneStressEventEntity>()

            var curTime = now - (days.toLong() * 24 * 60 * 60 * 1000L)
            var count = 0
            while (curTime < now) {
                val hour = (curTime / (1000 * 60 * 60) % 24).toInt()
                val isPeakHour = hour in 14..17
                val baseHr = if (isPeakHour) 85.0 else 68.0
                val rmssd = if (isPeakHour) 24.0 else 55.0

                generatedWindows.add(
                    com.pulso.mobile.data.local.PhoneWindowEntity(
                        watchId = "debug-watch-01",
                        windowStart = curTime,
                        windowEnd = curTime + windowDurationMs,
                        meanHr = baseHr + (count % 7),
                        minHr = baseHr - 5,
                        maxHr = baseHr + 12,
                        rmssd = rmssd,
                        validIbiCount = 280,
                        ibiValidRatio = 0.95,
                        activityState = "STILL",
                        steps = 10,
                        onBody = true,
                        source = "DEBUG_GENERATOR"
                    )
                )

                if (isPeakHour && count % 24 == 0) {
                    generatedEvents.add(
                        com.pulso.mobile.data.local.PhoneStressEventEntity(
                            id = "debug-ev-$curTime",
                            time = curTime,
                            confidence = 0.88,
                            reasonsJson = "[]",
                            windowsUsedJson = "[$curTime]",
                            configVersion = 1,
                            userFeedback = if (count % 48 == 0) "HELPFUL" else null
                        )
                    )
                }

                curTime += windowDurationMs
                count++
            }

            windowDao.insertOrUpdateAll(generatedWindows)
            stressEventDao.insertOrUpdateAll(generatedEvents)
            _uiState.value = _uiState.value.copy(csvImportFeedback = "Datos de $days días generados (${generatedWindows.size} ventanas, ${generatedEvents.eventsCountOrNull() ?: generatedEvents.size} eventos).")
        }
    }

    private fun List<com.pulso.mobile.data.local.PhoneStressEventEntity>.eventsCountOrNull(): Int? = this.size

    fun clearSampleData() {
        viewModelScope.launch {
            windowDao.clearAll()
            stressEventDao.clearAll()
            _uiState.value = _uiState.value.copy(csvImportFeedback = "Base de datos local limpiada.")
        }
    }

    // --- Baseline & Consent Actions ---

    fun resetBaseline() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isResettingBaseline = true, resetFeedback = null)
            val sent = syncManager.sendResetBaselineCommand()
            _uiState.value = _uiState.value.copy(
                isResettingBaseline = false,
                resetFeedback = if (sent) "Comando de reinicio enviado al reloj exitosamente" else "No se pudo comunicar con el reloj"
            )
        }
    }

    fun grantConsent() {
        viewModelScope.launch {
            syncManager.setConsent(true, "1.0-es")
            _uiState.value = _uiState.value.copy(selectedTab = 0)
        }
    }

    fun revokeConsent() {
        viewModelScope.launch {
            syncManager.setConsent(false)
        }
    }

    fun testWatchConnection() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isTestingPing = true, pingFeedback = null)
            val roundtrip = syncManager.testPing()
            _uiState.value = _uiState.value.copy(
                isTestingPing = false,
                pingFeedback = if (roundtrip != null) "Ping exitoso: ${roundtrip}ms" else "No se pudo conectar al reloj"
            )
        }
    }

    companion object {
        fun provideFactory(
            syncManager: MobileSyncManager,
            windowDao: PhoneWindowDao,
            stressEventDao: PhoneStressEventDao,
            breathingSessionDao: com.pulso.mobile.data.local.PhoneBreathingSessionDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MobileViewModel(syncManager, windowDao, stressEventDao, breathingSessionDao) as T
            }
        }
    }
}
