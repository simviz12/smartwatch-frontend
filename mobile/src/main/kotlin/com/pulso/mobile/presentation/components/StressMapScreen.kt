package com.pulso.mobile.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulso.core.aggregation.HeatmapCell
import com.pulso.core.aggregation.HourlyLoad
import com.pulso.core.aggregation.StressMapReport

@Composable
fun StressMapScreen(
    report: StressMapReport,
    isImportingSample: Boolean = false,
    sampleImportFeedback: String? = null,
    onGenerateSampleData: () -> Unit,
    onClearSampleData: () -> Unit
) {
    var selectedCell by remember { mutableStateOf<HeatmapCell?>(null) }
    var selectedTimeframeDays by remember { mutableStateOf(7) } // 7, 30, 90

    val scrollState = rememberScrollState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color(0xFF020617))
            .padding(16.dp)
            .verticalScroll(scrollState),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Header
        Text(
            text = "Mapa de Activación Corporal",
            color = Color.White,
            fontSize = 20.sp,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "Visualiza tus patrones semanales de activación y recuperación.",
            color = Color(0xFF94A3B8),
            fontSize = 13.sp
        )

        // Timeframe selector
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            listOf(7 to "7 días", 30 to "30 días", 90 to "90 días").forEach { (days, label) ->
                val isSelected = selectedTimeframeDays == days
                Card(
                    modifier = Modifier
                        .weight(1f)
                        .clickable { selectedTimeframeDays = days },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color(0xFF38BDF8) else Color(0xFF1E293B)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            color = if (isSelected) Color(0xFF0F172A) else Color.White,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }

        if (!report.hasSufficientData) {
            // Insufficient Data View
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Aún no hay suficientes datos",
                        color = Color.White,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = report.neutralInsight,
                        color = Color(0xFF94A3B8),
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        } else {
            // Insight Card (Neutral Tone)
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = "OBSERVACIÓN DE PATRONES",
                            color = Color(0xFF38BDF8),
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }
                    Text(
                        text = report.neutralInsight,
                        color = Color.White,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            }

            // 24x7 Heatmap Section
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Patrón 24×7 de activación",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Text(
                        text = "Filas: Lunes a Domingo • Columnas: 00h a 23h",
                        color = Color(0xFF64748B),
                        fontSize = 11.sp
                    )

                    HeatmapGrid(
                        cells = report.heatmap,
                        selectedCell = selectedCell,
                        onCellSelected = { selectedCell = it }
                    )

                    selectedCell?.let { cell ->
                        val dayName = when (cell.dayOfWeek) {
                            1 -> "Lunes"
                            2 -> "Martes"
                            3 -> "Miércoles"
                            4 -> "Jueves"
                            5 -> "Viernes"
                            6 -> "Sábado"
                            else -> "Domingo"
                        }
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(Color(0xFF1E293B), RoundedCornerShape(8.dp))
                                .padding(12.dp)
                        ) {
                            Text(
                                text = "$dayName ${cell.hourOfDay}:00 — Eventos: ${cell.stressEventCount} • Ventanas registradas: ${cell.windowCount}",
                                color = Color(0xFF38BDF8),
                                fontSize = 12.sp
                            )
                        }
                    }
                }
            }

            // Hourly Distribution Bar
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                shape = RoundedCornerShape(12.dp)
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = "Carga por hora del día",
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold
                    )
                    HourlyLoadRow(hourlyLoads = report.hourlyLoads)
                }
            }

            // Breathing Effectiveness Card
            report.breathingEffectiveness?.let { breathing ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Efectividad de pausas de respiración",
                            color = Color.White,
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold
                        )
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Sesiones realizadas:",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${breathing.totalSessions}",
                                color = Color.White,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Te sentiste mejor tras la pausa:",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${String.format("%.1f", breathing.feltBetterPercentage)}%",
                                color = Color(0xFF10B981),
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp
                            )
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Duración promedio:",
                                color = Color(0xFF94A3B8),
                                fontSize = 13.sp
                            )
                            Text(
                                text = "${breathing.averageDurationSeconds} segundos",
                                color = Color.White,
                                fontSize = 13.sp
                            )
                        }
                    }
                }
            }
        }

        // DEBUG-ONLY Sample Importer (Hidden in release, clearly tagged)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B)),
            shape = RoundedCornerShape(12.dp)
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = "HERRAMIENTA DEBUG — Simulador de Densidad",
                        color = Color(0xFFF59E0B),
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Text(
                    text = "Genera 90 días de datos densos para validar fluidez de scroll y cálculo sin inventar mediciones en producción.",
                    color = Color(0xFF94A3B8),
                    fontSize = 11.sp
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = onGenerateSampleData,
                        enabled = !isImportingSample,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF38BDF8)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        if (isImportingSample) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(16.dp),
                                color = Color.White,
                                strokeWidth = 2.dp
                            )
                        } else {
                            Text("Generar 90 días", color = Color(0xFF0F172A), fontSize = 12.sp)
                        }
                    }
                    Button(
                        onClick = onClearSampleData,
                        enabled = !isImportingSample,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444)),
                        shape = RoundedCornerShape(6.dp)
                    ) {
                        Text("Limpiar", color = Color.White, fontSize = 12.sp)
                    }
                }
                sampleImportFeedback?.let { feedback ->
                    Text(
                        text = feedback,
                        color = Color(0xFF38BDF8),
                        fontSize = 11.sp
                    )
                }
            }
        }
        Spacer(modifier = Modifier.height(16.dp))
    }
}

@Composable
private fun HeatmapGrid(
    cells: List<HeatmapCell>,
    selectedCell: HeatmapCell?,
    onCellSelected: (HeatmapCell) -> Unit
) {
    val dayLabels = listOf("L", "M", "X", "J", "V", "S", "D")

    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        (1..7).forEach { day ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    text = dayLabels[day - 1],
                    color = Color(0xFF94A3B8),
                    fontSize = 10.sp,
                    modifier = Modifier.width(14.dp),
                    textAlign = TextAlign.Center
                )

                val dayCells = cells.filter { it.dayOfWeek == day }.sortedBy { it.hourOfDay }
                dayCells.forEach { cell ->
                    val color = when {
                        cell.windowCount == 0 -> Color(0xFF1E293B)
                        cell.stressEventCount == 0 -> Color(0xFF0E7490) // calm
                        cell.stressLoadRatio < 0.25 -> Color(0xFFF59E0B) // light
                        else -> Color(0xFFEF4444) // peak
                    }
                    val isSelected = selectedCell == cell
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(14.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(color)
                            .clickable { onCellSelected(cell) }
                    )
                }
            }
        }
    }
}

@Composable
private fun HourlyLoadRow(hourlyLoads: List<HourlyLoad>) {
    LazyRow(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        items(hourlyLoads) { item ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .width(16.dp)
                        .height(48.dp)
                        .background(Color(0xFF1E293B), RoundedCornerShape(4.dp)),
                    contentAlignment = Alignment.BottomCenter
                ) {
                    val fillHeight = (item.averageLoad * 48).coerceIn(4.0, 48.0).dp
                    Box(
                        modifier = Modifier
                            .width(16.dp)
                            .height(fillHeight)
                            .background(
                                if (item.averageLoad > 0.2) Color(0xFFEF4444) else Color(0xFF38BDF8),
                                RoundedCornerShape(4.dp)
                            )
                    )
                }
                Text(
                    text = "${item.hourOfDay}h",
                    color = Color(0xFF64748B),
                    fontSize = 9.sp
                )
            }
        }
    }
}
