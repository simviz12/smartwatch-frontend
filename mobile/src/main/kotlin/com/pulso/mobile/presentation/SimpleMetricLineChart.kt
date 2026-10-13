package com.pulso.mobile.presentation

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.pulso.core.model.MeasurementWindow

@Composable
fun SimpleMetricLineChart(
    windows: List<MeasurementWindow>,
    valueExtractor: (MeasurementWindow) -> Double?,
    lineColor: Color,
    emptyMessage: String,
    baselineRange: Pair<Double, Double>? = null, // (min, max) typical resting range
    modifier: Modifier = Modifier
) {
    val validPoints = windows.mapNotNull { w ->
        val v = valueExtractor(w)
        if (v != null) Pair(w.windowStart, v) else null
    }

    if (validPoints.size < 2) {
        Box(
            modifier = modifier
                .fillMaxWidth()
                .height(140.dp)
                .padding(16.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = emptyMessage,
                fontSize = 12.sp,
                color = Color.Gray
            )
        }
        return
    }

    val minTime = validPoints.first().first
    val maxTime = validPoints.last().first
    val timeSpan = (maxTime - minTime).coerceAtLeast(1L).toFloat()

    val values = validPoints.map { it.second }
    val minValue = (values.minOrNull() ?: 0.0).toFloat()
    val maxValue = (values.maxOrNull() ?: 100.0).toFloat()
    val valueSpan = (maxValue - minValue).coerceAtLeast(1f)

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(140.dp)
            .padding(horizontal = 8.dp, vertical = 8.dp)
    ) {
        val width = size.width
        val height = size.height

        // Draw horizontal grid lines
        drawLine(
            color = Color.LightGray.copy(alpha = 0.3f),
            start = Offset(0f, 0f),
            end = Offset(width, 0f),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.LightGray.copy(alpha = 0.3f),
            start = Offset(0f, height / 2),
            end = Offset(width, height / 2),
            strokeWidth = 1.dp.toPx()
        )
        drawLine(
            color = Color.LightGray.copy(alpha = 0.3f),
            start = Offset(0f, height),
            end = Offset(width, height),
            strokeWidth = 1.dp.toPx()
        )

        // Draw baseline normal band if available
        baselineRange?.let { (bMin, bMax) ->
            val effectiveYMax = height - (((bMax.toFloat() - minValue) / valueSpan) * height).coerceIn(0f, height)
            val effectiveYMin = height - (((bMin.toFloat() - minValue) / valueSpan) * height).coerceIn(0f, height)
            val topY = minOf(effectiveYMax, effectiveYMin)
            val bandHeight = kotlin.math.abs(effectiveYMax - effectiveYMin)

            drawRect(
                color = lineColor.copy(alpha = 0.15f),
                topLeft = Offset(0f, topY),
                size = androidx.compose.ui.geometry.Size(width, bandHeight)
            )
        }

        // Draw line chart
        val path = Path()
        validPoints.forEachIndexed { index, (time, value) ->
            val x = ((time - minTime).toFloat() / timeSpan) * width
            val y = height - (((value.toFloat() - minValue) / valueSpan) * height)

            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }

            drawCircle(
                color = lineColor,
                radius = 3.dp.toPx(),
                center = Offset(x, y)
            )
        }

        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(width = 2.5.dp.toPx())
        )
    }
}
