package com.pulso.wear.presentation

import com.pulso.core.model.MeasurementWindow
import com.pulso.wear.domain.model.ConnectedPhoneNode
import com.pulso.wear.domain.model.LiveHeartRateState
import com.pulso.wear.domain.model.PingResult
import com.pulso.wear.domain.model.SensorCapabilityReport
import com.pulso.wear.domain.repository.SensorRepository
import com.pulso.wear.domain.repository.WearDataLayerRepository
import com.pulso.wear.domain.usecase.ExportWindowsCsvUseCase
import com.pulso.wear.domain.usecase.GetCapabilityReportUseCase
import com.pulso.wear.domain.usecase.GetConnectedPhoneUseCase
import com.pulso.wear.domain.usecase.ManageBackgroundSamplingUseCase
import com.pulso.wear.domain.usecase.ObserveLiveHeartRateUseCase
import com.pulso.wear.domain.usecase.ObserveWindowsUseCase
import com.pulso.wear.domain.usecase.SendPingUseCase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.io.File

@OptIn(ExperimentalCoroutinesApi::class)
class MainViewModelTest {

    private val testDispatcher = StandardTestDispatcher()

    private val fakeWearRepo = object : WearDataLayerRepository {
        override suspend fun getConnectedPhoneNode() = ConnectedPhoneNode("phone_1", "Pixel 8 Pro")
        override suspend fun sendPing(counter: Int) = PingResult.Success("Pixel 8 Pro")
    }

    private val fakeSensorRepo = object : SensorRepository {
        override suspend fun getCapabilityReport(): SensorCapabilityReport = SensorCapabilityReport(
            deviceModel = "Pixel Watch 3",
            manufacturer = "Google",
            wearOsVersion = "5.0",
            androidApiLevel = 35,
            healthServicesSupportedDataTypes = listOf("DataType.HEART_RATE_BPM"),
            samsungSdkAvailable = false,
            samsungSdkSupportedTrackers = emptyList(),
            permissionsGranted = mapOf("BODY_SENSORS" to true)
        )

        override fun observeLiveHeartRate() = flowOf(
            LiveHeartRateState(bpm = 72.0, availability = "DISPONIBLE", onBody = true)
        )

        override fun observeSavedWindows() = flowOf(
            listOf(
                MeasurementWindow(
                    id = 1,
                    windowStart = 1000L,
                    windowEnd = 2000L,
                    meanHr = 72.0,
                    minHr = 70.0,
                    maxHr = 75.0,
                    source = "TEST"
                )
            )
        )

        override suspend fun saveWindow(window: MeasurementWindow) = 1L

        override suspend fun exportWindowsToCsv(): File {
            val tempFile = File.createTempFile("pulso_test", ".csv")
            tempFile.writeText("test_csv")
            return tempFile
        }

        override suspend fun startBackgroundMeasurementService() {}
        override suspend fun stopBackgroundMeasurementService() {}
    }

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun createViewModel(): MainViewModel {
        return MainViewModel(
            getConnectedPhoneUseCase = GetConnectedPhoneUseCase(fakeWearRepo),
            sendPingUseCase = SendPingUseCase(fakeWearRepo),
            getCapabilityReportUseCase = GetCapabilityReportUseCase(fakeSensorRepo),
            observeLiveHeartRateUseCase = ObserveLiveHeartRateUseCase(fakeSensorRepo),
            observeWindowsUseCase = ObserveWindowsUseCase(fakeSensorRepo),
            exportWindowsCsvUseCase = ExportWindowsCsvUseCase(fakeSensorRepo),
            manageBackgroundSamplingUseCase = ManageBackgroundSamplingUseCase(fakeSensorRepo)
        )
    }

    @Test
    fun `init loads capability report and window count`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.capabilityReport)
        assertEquals("Pixel Watch 3", state.capabilityReport?.deviceModel)
        assertEquals(1, state.totalWindowsLogged)
    }

    @Test
    fun `exportCsv updates state with file path`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.exportCsv()
        advanceUntilIdle()

        val state = viewModel.uiState.value
        assertNotNull(state.exportedCsvPath)
        assertTrue(state.statusMessage?.contains("CSV exportado") == true)
    }

    @Test
    fun `toggleBackgroundSampling alternates monitoring status`() = runTest {
        val viewModel = createViewModel()
        advanceUntilIdle()

        viewModel.toggleBackgroundSampling()
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value.isMonitoringActive)

        viewModel.toggleBackgroundSampling()
        advanceUntilIdle()
        assertTrue(!viewModel.uiState.value.isMonitoringActive)
    }
}
