package com.pulso.wear.presentation

import com.pulso.wear.domain.model.ConnectedPhoneNode
import com.pulso.wear.domain.model.PingResult
import com.pulso.wear.domain.repository.WearDataLayerRepository
import com.pulso.wear.domain.usecase.GetConnectedPhoneUseCase
import com.pulso.wear.domain.usecase.SendPingUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `sendPing success updates state with counter and recipient`() = runTest {
        val fakeRepo = object : WearDataLayerRepository {
            override suspend fun getConnectedPhoneNode() = ConnectedPhoneNode("phone_1", "Pixel 8 Pro")
            override suspend fun sendPing(counter: Int) = PingResult.Success("Pixel 8 Pro")
        }

        val viewModel = MainViewModel(
            getConnectedPhoneUseCase = GetConnectedPhoneUseCase(fakeRepo),
            sendPingUseCase = SendPingUseCase(fakeRepo)
        )
        advanceUntilIdle()

        assertEquals("Pixel 8 Pro", viewModel.uiState.value.connectedNode?.displayName)

        viewModel.sendPing()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertEquals(1, state.pingCounter)
        assertFalse(state.isSending)
        assertTrue(state.lastResultText?.contains("Enviado (#1 a Pixel 8 Pro)") == true)
    }

    @Test
    fun `sendPing failure updates state with error reason`() = runTest {
        val fakeRepo = object : WearDataLayerRepository {
            override suspend fun getConnectedPhoneNode() = null
            override suspend fun sendPing(counter: Int) = PingResult.Failure("Celular no conectado")
        }

        val viewModel = MainViewModel(
            getConnectedPhoneUseCase = GetConnectedPhoneUseCase(fakeRepo),
            sendPingUseCase = SendPingUseCase(fakeRepo)
        )
        advanceUntilIdle()

        viewModel.sendPing()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertFalse(state.isSending)
        assertEquals("Celular no conectado", state.lastResultText)
    }
}
