package com.pulso.mobile.presentation

import androidx.compose.foundation.background
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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulso.core.baseline.Baseline
import com.pulso.core.baseline.BaselineState
import com.pulso.core.baseline.BucketStats
import com.pulso.core.baseline.TimeBucket

@Composable
fun BaselineScreen(
    baseline: Baseline?,
    onResetBaseline: () -> Unit,
    resetFeedback: String?
) {
    var showConfirmDialog by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Tu Línea Base Personal",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(6.dp))
        Text(
            text = "Calculada con tus últimos 14 días de reposo",
            fontSize = 13.sp,
            color = Color(0xFF38BDF8)
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Educational explanation card
        Card(
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(14.dp)) {
                Text(
                    text = "¿Qué es tu línea base y por qué importa?",
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFBBF24),
                    fontSize = 14.sp
                )
                Spacer(modifier = Modifier.height(6.dp))
                Text(
                    text = "Tu cuerpo tiene ritmos naturales distintos según la hora del día. Tu línea base aprende tu rango normal de pulso y variabilidad (HRV) en calma para cada momento. Así, cuando detectamos una subida sostenida, sabemos si es una reacción atípica o parte de tu reposo habitual.",
                    fontSize = 12.sp,
                    color = Color(0xFFCBD5E1),
                    lineHeight = 18.sp
                )
            }
        }

        Spacer(modifier = Modifier.height(16.dp))

        Text(
            text = "Rangos por momento del día",
            fontSize = 16.sp,
            fontWeight = FontWeight.SemiBold,
            color = Color.White
        )
        Spacer(modifier = Modifier.height(10.dp))

        val bucketsOrdered = listOf(
            TimeBucket.MORNING to "Mañana (06:00 - 12:00)",
            TimeBucket.AFTERNOON to "Tarde (12:00 - 18:00)",
            TimeBucket.EVENING to "Noche (18:00 - 24:00)",
            TimeBucket.NIGHT to "Madrugada (00:00 - 06:00)",
            TimeBucket.SLEEP to "Sueño (detectado en descanso)"
        )

        for ((bucket, title) in bucketsOrdered) {
            val stats = baseline?.buckets?.get(bucket)
            BucketCard(title = title, stats = stats)
            Spacer(modifier = Modifier.height(10.dp))
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Reset Button
        Button(
            onClick = { showConfirmDialog = true },
            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF991B1B)),
            modifier = Modifier.fillMaxWidth().height(46.dp)
        ) {
            Text("Reiniciar línea base", fontWeight = FontWeight.Bold, fontSize = 14.sp)
        }

        resetFeedback?.let {
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = it,
                color = if (it.contains("exitosamente")) Color(0xFF4ADE80) else Color(0xFFEF4444),
                fontSize = 12.sp
            )
        }

        Spacer(modifier = Modifier.height(24.dp))
    }

    if (showConfirmDialog) {
        AlertDialog(
            onDismissRequest = { showConfirmDialog = false },
            title = { Text("¿Reiniciar línea base?") },
            text = {
                Text(
                    "Esta acción borrará los perfiles aprendidos en el reloj y en el celular para que vuelvan a aprender desde cero. Tu historial de mediciones pasadas se conservará intacto."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showConfirmDialog = false
                        onResetBaseline()
                    }
                ) {
                    Text("Sí, reiniciar", color = Color(0xFFEF4444), fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showConfirmDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

@Composable
fun BucketCard(title: String, stats: BucketStats?) {
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(title, fontWeight = FontWeight.SemiBold, color = Color.White, fontSize = 14.sp)
                val stateTag = when (stats?.state) {
                    BaselineState.READY -> "LISTA" to Color(0xFF4ADE80)
                    BaselineState.LEARNING -> "APRENDIENDO" to Color(0xFFFBBF24)
                    null -> "SIN DATOS" to Color.Gray
                }
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(4.dp))
                        .background(stateTag.second.copy(alpha = 0.2f))
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(stateTag.first, color = stateTag.second, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            when {
                stats == null || stats.state == BaselineState.LEARNING -> {
                    val days = stats?.daysCollected ?: 0
                    val samples = stats?.windowCount ?: 0
                    val progress = ((stats?.progressPct ?: 0.0) / 100.0).toFloat().coerceIn(0f, 1f)

                    Text(
                        text = "Aprendiendo: día $days de 3 · $samples de 30 mediciones de reposo",
                        fontSize = 12.sp,
                        color = Color.LightGray
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    LinearProgressIndicator(
                        progress = { progress },
                        modifier = Modifier.fillMaxWidth().height(6.dp).clip(RoundedCornerShape(3.dp)),
                        color = Color(0xFFFBBF24),
                        trackColor = Color(0xFF334155)
                    )
                }
                stats.state == BaselineState.READY -> {
                    // Show typical resting HR range: median ± (1.4826 * MAD)
                    val hrMedian = stats.hrMedian ?: 0.0
                    val hrMad = stats.hrMad ?: 0.0
                    val hrLow = (hrMedian - (1.4826 * hrMad)).coerceAtLeast(35.0).toInt()
                    val hrHigh = (hrMedian + (1.4826 * hrMad)).toInt()

                    Text(
                        text = "Pulso habitual en reposo: $hrLow - $hrHigh BPM (mediana: ${hrMedian.toInt()})",
                        fontSize = 13.sp,
                        color = Color(0xFF4ADE80),
                        fontWeight = FontWeight.Medium
                    )

                    // Visual bar range indicator
                    Spacer(modifier = Modifier.height(4.dp))
                    RangeBarVisualizer(
                        low = hrLow.toFloat(),
                        high = hrHigh.toFloat(),
                        minScale = 40f,
                        maxScale = 120f,
                        barColor = Color(0xFF4ADE80)
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    if (stats.rmssdMedian != null && stats.rmssdMad != null) {
                        val hrvMedian = stats.rmssdMedian!!
                        val hrvMad = stats.rmssdMad!!
                        val hrvLow = (hrvMedian - (1.4826 * hrvMad)).coerceAtLeast(5.0).toInt()
                        val hrvHigh = (hrvMedian + (1.4826 * hrvMad)).toInt()
                        Text(
                            text = "Variabilidad (HRV) típica: $hrvLow - $hrvHigh ms (mediana: ${hrvMedian.toInt()})",
                            fontSize = 12.sp,
                            color = Color(0xFF38BDF8)
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        RangeBarVisualizer(
                            low = hrvLow.toFloat(),
                            high = hrvHigh.toFloat(),
                            minScale = 10f,
                            maxScale = 120f,
                            barColor = Color(0xFF38BDF8)
                        )
                    } else {
                        Text(
                            text = "Nota: HRV no disponible (Camino B: solo pulso cardíaco)",
                            fontSize = 11.sp,
                            color = Color.LightGray
                        )
                    }

                    Spacer(modifier = Modifier.height(6.dp))
                    Text(
                        text = "Basado en ${stats.windowCount} mediciones durante ${stats.daysCollected} días",
                        fontSize = 11.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
fun RangeBarVisualizer(
    low: Float,
    high: Float,
    minScale: Float,
    maxScale: Float,
    barColor: Color
) {
    val totalRange = (maxScale - minScale).coerceAtLeast(1f)
    val startFraction = ((low - minScale) / totalRange).coerceIn(0f, 1f)
    val endFraction = ((high - minScale) / totalRange).coerceIn(0f, 1f)
    val widthFraction = (endFraction - startFraction).coerceAtLeast(0.05f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(8.dp)
            .clip(RoundedCornerShape(4.dp))
            .background(Color(0xFF334155))
    ) {
        Row(modifier = Modifier.fillMaxSize()) {
            if (startFraction > 0f) {
                Spacer(modifier = Modifier.weight(startFraction))
            }
            Box(
                modifier = Modifier
                    .weight(widthFraction)
                    .fillMaxSize()
                    .clip(RoundedCornerShape(4.dp))
                    .background(barColor)
            )
            val remainingFraction = (1f - (startFraction + widthFraction)).coerceAtLeast(0f)
            if (remainingFraction > 0f) {
                Spacer(modifier = Modifier.weight(remainingFraction))
            }
        }
    }
}
