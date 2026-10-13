package com.pulso.wear.presentation

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pulso.core.model.ConfigSync
import com.pulso.wear.domain.model.ConnectedPhoneNode
import com.pulso.wear.domain.model.LiveHeartRateState
import com.pulso.wear.domain.model.PingResult
import com.pulso.wear.domain.model.SensorCapabilityReport
import com.pulso.wear.domain.usecase.ExportWindowsCsvUseCase
import com.pulso.wear.domain.usecase.GetCapabilityReportUseCase
import com.pulso.wear.domain.usecase.GetConnectedPhoneUseCase
import com.pulso.wear.domain.usecase.ObserveConfigUseCase
import com.pulso.wear.domain.usecase.ObserveLiveHeartRateUseCase
import com.pulso.wear.domain.usecase.ObserveWindowsUseCase
import com.pulso.wear.domain.usecase.SendPingUseCase
import com.pulso.wear.domain.usecase.TriggerSyncUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.io.File

data class MainUiState(
    val connectedNode: ConnectedPhoneNode? = null,
    val isCheckingNode: Boolean = false,
    val isSendingPing: Boolean = false,
    val lastPingResultText: String? = null,
    val pingCounter: Int = 0,
    val capabilityReport: SensorCapabilityReport? = null,
    val liveHeartRate: LiveHeartRateState = LiveHeartRateState(),
    val config: ConfigSync? = null,
    val exportedCsvPath: String? = null,
    val totalWindowsLogged: Int = 0,
    val isSyncing: Boolean = false,
    val statusMessage: String? = null
)

class MainViewModel(
    private val getConnectedPhoneUseCase: GetConnectedPhoneUseCase,
    private val sendPingUseCase: SendPingUseCase,
    private val getCapabilityReportUseCase: GetCapabilityReportUseCase,
    private val observeLiveHeartRateUseCase: ObserveLiveHeartRateUseCase,
    private val observeWindowsUseCase: ObserveWindowsUseCase,
    private val exportWindowsCsvUseCase: ExportWindowsCsvUseCase,
    private val observeConfigUseCase: ObserveConfigUseCase,
    private val triggerSyncUseCase: TriggerSyncUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkConnectedNode()
        loadCapabilityReport()
        observeWindows()
        observeConfig()
    }

    private fun observeConfig() {
        viewModelScope.launch {
            observeConfigUseCase().collect { conf ->
                _uiState.value = _uiState.value.copy(config = conf)
                if (conf?.collectionEnabled == true) {
                    startLiveMeasurement()
                }
            }
        }
    }

    fun loadCapabilityReport() {
        viewModelScope.launch {
            val report = getCapabilityReportUseCase()
            _uiState.value = _uiState.value.copy(capabilityReport = report)
        }
    }

    fun startLiveMeasurement() {
        if (_uiState.value.config?.collectionEnabled != true) return
        viewModelScope.launch {
            observeLiveHeartRateUseCase().collect { state ->
                _uiState.value = _uiState.value.copy(liveHeartRate = state)
            }
        }
    }

    private fun observeWindows() {
        viewModelScope.launch {
            observeWindowsUseCase().collect { list ->
                _uiState.value = _uiState.value.copy(totalWindowsLogged = list.size)
            }
        }
    }

    fun triggerSync() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSyncing = true)
            try {
                val sent = triggerSyncUseCase()
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    statusMessage = "Sincronizados $sent ventanas con el celular"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSyncing = false,
                    statusMessage = "Error de sincronización: ${e.message}"
                )
            }
        }
    }

    fun exportCsv() {
        viewModelScope.launch {
            try {
                val file = exportWindowsCsvUseCase()
                _uiState.value = _uiState.value.copy(
                    exportedCsvPath = file.absolutePath,
                    statusMessage = "CSV exportado: ${file.name}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Error al exportar CSV: ${e.message}"
                )
            }
        }
    }

    fun saveReportToFile(context: Context): String {
        val report = _uiState.value.capabilityReport ?: return "Informe no disponible"
        val reportDir = File(context.getExternalFilesDir(null) ?: context.filesDir, "reports")
        if (!reportDir.exists()) reportDir.mkdirs()
        val file = File(reportDir, "capability_report.txt")
        file.writeText(report.toFormattedReport())
        _uiState.value = _uiState.value.copy(
            statusMessage = "Informe guardado en: ${file.name}"
        )
        return file.absolutePath
    }

    fun checkConnectedNode() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isCheckingNode = true)
            val node = getConnectedPhoneUseCase()
            _uiState.value = _uiState.value.copy(
                connectedNode = node,
                isCheckingNode = false
            )
        }
    }

    fun sendPing() {
        if (_uiState.value.isSendingPing) return
        val nextCounter = _uiState.value.pingCounter + 1

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingPing = true)
            when (val result = sendPingUseCase(nextCounter)) {
                is PingResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSendingPing = false,
                        pingCounter = nextCounter,
                        lastPingResultText = "Enviado (#$nextCounter a ${result.nodeDisplayName})"
                    )
                }
                is PingResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isSendingPing = false,
                        lastPingResultText = result.reason
                    )
                }
            }
        }
    }

    fun simulateStress(context: Context) {
        viewModelScope.launch {
            try {
                val detector = com.pulso.wear.data.stress.WearStressDetector.getInstance(context)
                val event = detector.simulateStressDebug()
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Estrés simulado: ${event.id.take(8)}"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Fallo simulación: ${e.message}"
                )
            }
        }
    }

    fun snoozeAlerts(durationMinutes: Long, context: Context) {
        viewModelScope.launch {
            try {
                val until = System.currentTimeMillis() + (durationMinutes * 60 * 1000L)
                val syncManager = com.pulso.wear.data.sync.SyncManager.getInstance(context)
                val current = syncManager.currentConfig.value
                if (current != null) {
                    val updatedStress = current.stressConfig.copy(snoozeUntil = until)
                    syncManager.updateLocalConfig(current.copy(stressConfig = updatedStress))
                }
                val label = when (durationMinutes) {
                    60L -> "1 hora"
                    240L -> "4 horas"
                    else -> "mañana"
                }
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Avisos pausados por $label"
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    statusMessage = "Error al pausar: ${e.message}"
                )
            }
        }
    }

    companion object {
        fun provideFactory(
            getConnectedPhoneUseCase: GetConnectedPhoneUseCase,
            sendPingUseCase: SendPingUseCase,
            getCapabilityReportUseCase: GetCapabilityReportUseCase,
            observeLiveHeartRateUseCase: ObserveLiveHeartRateUseCase,
            observeWindowsUseCase: ObserveWindowsUseCase,
            exportWindowsCsvUseCase: ExportWindowsCsvUseCase,
            observeConfigUseCase: ObserveConfigUseCase,
            triggerSyncUseCase: TriggerSyncUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(
                    getConnectedPhoneUseCase,
                    sendPingUseCase,
                    getCapabilityReportUseCase,
                    observeLiveHeartRateUseCase,
                    observeWindowsUseCase,
                    exportWindowsCsvUseCase,
                    observeConfigUseCase,
                    triggerSyncUseCase
                ) as T
            }
        }
    }
}
