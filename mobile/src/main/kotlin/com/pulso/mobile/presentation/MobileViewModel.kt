package com.pulso.mobile.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pulso.core.model.MeasurementWindow
import com.pulso.mobile.data.local.PhoneWindowDao
import com.pulso.mobile.data.sync.MobileDiagnosticsState
import com.pulso.mobile.data.sync.MobileSyncManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.Calendar

data class MobileUiState(
    val selectedTab: Int = 0, // 0 = Hoy, 1 = Diagnóstico, 2 = Ajustes, 3 = Línea Base
    val isTestingPing: Boolean = false,
    val pingFeedback: String? = null,
    val isResettingBaseline: Boolean = false,
    val resetFeedback: String? = null
)

class MobileViewModel(
    private val syncManager: MobileSyncManager,
    private val windowDao: PhoneWindowDao
) : ViewModel() {

    private val _uiState = MutableStateFlow(MobileUiState())
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

    // Compute rolling 14-day baseline from phone database
    private val fourteenDaysAgo: Long
        get() = System.currentTimeMillis() - (14L * 24 * 60 * 60 * 1000)

    val phoneBaseline: StateFlow<com.pulso.core.baseline.Baseline?> = windowDao
        .getWindowsSinceFlow(fourteenDaysAgo)
        .map { list ->
            val domainWindows = list.map { it.toDomain() }
            com.pulso.core.baseline.BaselineEngine.update(domainWindows)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    // Compare phone baseline with watch reported baselineStatus
    val baselineMismatch: StateFlow<String?> = kotlinx.coroutines.flow.combine(
        phoneBaseline,
        diagnosticsState
    ) { baseline, diag ->
        com.pulso.mobile.domain.baseline.BaselineMismatchDetector.checkMismatch(
            phoneBaseline = baseline,
            watchStatusMap = diag.lastWatchStatus?.baselineStatus
        )
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), null)

    fun selectTab(index: Int) {
        _uiState.value = _uiState.value.copy(selectedTab = index)
    }

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
            windowDao: PhoneWindowDao
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MobileViewModel(syncManager, windowDao) as T
            }
        }
    }
}
