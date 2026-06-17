package com.ywb.focusguard.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.unit.dp

@Composable
fun SimpleLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    val lineColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        if (values.size < 2) return@Canvas
        val max = values.maxOrNull() ?: 1f
        val min = values.minOrNull() ?: 0f
        val range = (max - min).takeIf { it > 0f } ?: 1f
        val stepX = size.width / (values.lastIndex)

        repeat(3) { index ->
            val y = size.height * (index + 1) / 4f
            drawLine(
                color = gridColor,
                start = Offset(0f, y),
                end = Offset(size.width, y),
                strokeWidth = 1.dp.toPx()
            )
        }

        val path = Path()
        values.forEachIndexed { index, value ->
            val x = index * stepX
            val y = size.height - ((value - min) / range * size.height)
            if (index == 0) {
                path.moveTo(x, y)
            } else {
                path.lineTo(x, y)
            }
        }
        drawPath(
            path = path,
            color = lineColor,
            style = Stroke(
                width = 2.dp.toPx(),
                cap = StrokeCap.Round
            )
        )
    }
}
