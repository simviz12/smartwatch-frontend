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
    private val stressEventDao: PhoneStressEventDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(loadInitialUiState())
    val uiState: StateFlow<MobileUiState> = _uiState.asStateFlow()

    val diagnosticsState: StateFlow<MobileDiagnosticsState> = syncManager.diagnosticsState

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

    val stressEvents: StateFlow<List<StressEvent>> = stressEventDao
        .getAllEventsFlow()
        .map { list -> list.map { it.toDomain() } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

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

    private fun loadInitialUiState(): MobileUiState {
        val configSync = syncManager.loadPersistedConfig()
        val preset = SensitivityPreset.fromConfig(configSync.stressConfig)
        return MobileUiState(
            stressConfig = configSync.stressConfig,
            quietHours = configSync.quietHours ?: QuietHours(22, 0, 7, 0),
            sensitivityPreset = preset
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
            stressEventDao: PhoneStressEventDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MobileViewModel(syncManager, windowDao, stressEventDao) as T
            }
        }
    }
}
