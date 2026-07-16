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

/**
 * 使用 Canvas 绘制的轻量折线图，仅负责把一组 Float 等比映射到固定高度区域。
 * 当前不包含坐标轴、标签和交互，适合实时预览与详情页基础曲线。
 */
@Composable
fun SimpleLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier
) {
    /** 折线跟随主题主色，主题切换时自动重组。 */
    val lineColor = MaterialTheme.colorScheme.primary

    /** 网格线使用弱对比轮廓色，避免抢夺数据视觉焦点。 */
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    Canvas(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
    ) {
        if (values.size < 2) return@Canvas
        // 先归一化到 0..1，再映射到 Canvas 高度，避免不同量纲直接超出绘制区域。
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
