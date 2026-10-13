package com.pulso.wear.presentation

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.wear.compose.material.Button
import androidx.wear.compose.material.ButtonDefaults
import androidx.wear.compose.material.CircularProgressIndicator
import androidx.wear.compose.material.MaterialTheme
import androidx.wear.compose.material.Scaffold
import androidx.wear.compose.material.Text
import androidx.wear.compose.material.TimeText
import com.pulso.wear.data.repository.AndroidWearDataLayerRepository
import com.pulso.wear.domain.usecase.GetConnectedPhoneUseCase
import com.pulso.wear.domain.usecase.SendPingUseCase
import com.pulso.wear.presentation.theme.PulsoWearTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels {
        val repository = AndroidWearDataLayerRepository(this)
        MainViewModel.provideFactory(
            getConnectedPhoneUseCase = GetConnectedPhoneUseCase(repository),
            sendPingUseCase = SendPingUseCase(repository)
        )
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            PulsoWearTheme {
                MainScreen(viewModel = viewModel)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        viewModel.checkConnectedNode()
    }
}

@Composable
fun MainScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsState()

    Scaffold(
        timeText = { TimeText() }
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colors.background)
                .padding(horizontal = 16.dp, vertical = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "PULSO",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colors.primary,
                    letterSpacing = 2.sp
                )

                Spacer(modifier = Modifier.height(4.dp))

                val nodeStatus = when {
                    uiState.isCheckingNode -> "Buscando celular..."
                    uiState.connectedNode != null -> "Conectado: ${uiState.connectedNode?.displayName}"
                    else -> "Sin celular conectado"
                }
                Text(
                    text = nodeStatus,
                    fontSize = 11.sp,
                    color = if (uiState.connectedNode != null) Color(0xFF81C784) else Color(0xFFFFB74D),
                    textAlign = TextAlign.Center,
                    maxLines = 2
                )

                Spacer(modifier = Modifier.height(10.dp))

                Button(
                    onClick = { viewModel.sendPing() },
                    enabled = !uiState.isSending,
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .height(44.dp),
                    colors = ButtonDefaults.primaryButtonColors()
                ) {
                    if (uiState.isSending) {
                        CircularProgressIndicator(
                            strokeWidth = 2.dp,
                            modifier = Modifier.fillMaxSize(0.6f)
                        )
                    } else {
                        Text(
                            text = "Enviar al celular",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Medium,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                uiState.lastResultText?.let { result ->
                    val isSuccess = result.startsWith("Enviado")
                    Text(
                        text = result,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSuccess) Color(0xFF00E676) else Color(0xFFFF5252),
                        textAlign = TextAlign.Center,
                        maxLines = 3
                    )
                }
            }
        }
    }
}
