package com.pulso.wear.presentation

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.pulso.wear.domain.model.ConnectedPhoneNode
import com.pulso.wear.domain.model.PingResult
import com.pulso.wear.domain.usecase.GetConnectedPhoneUseCase
import com.pulso.wear.domain.usecase.SendPingUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val connectedNode: ConnectedPhoneNode? = null,
    val isCheckingNode: Boolean = false,
    val isSending: Boolean = false,
    val lastResultText: String? = null,
    val pingCounter: Int = 0
)

class MainViewModel(
    private val getConnectedPhoneUseCase: GetConnectedPhoneUseCase,
    private val sendPingUseCase: SendPingUseCase
) : ViewModel() {

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    init {
        checkConnectedNode()
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
        if (_uiState.value.isSending) return
        val nextCounter = _uiState.value.pingCounter + 1

        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSending = true)
            when (val result = sendPingUseCase(nextCounter)) {
                is PingResult.Success -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        pingCounter = nextCounter,
                        lastResultText = "Enviado (#$nextCounter a ${result.nodeDisplayName})"
                    )
                }
                is PingResult.Failure -> {
                    _uiState.value = _uiState.value.copy(
                        isSending = false,
                        lastResultText = result.reason
                    )
                }
            }
        }
    }

    companion object {
        fun provideFactory(
            getConnectedPhoneUseCase: GetConnectedPhoneUseCase,
            sendPingUseCase: SendPingUseCase
        ): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                return MainViewModel(getConnectedPhoneUseCase, sendPingUseCase) as T
            }
        }
    }
}
