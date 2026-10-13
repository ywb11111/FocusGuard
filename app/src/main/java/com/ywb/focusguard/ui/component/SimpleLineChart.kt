package com.ywb.focusguard.ui.component

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.unit.dp

/**
 * 轻量折线图，仅负责把一组 Float 等比映射到固定高度区域。
 * 不包含坐标轴、标签和交互，适合实时预览与详情页基础曲线。
 *
 * **入场动画**：曲线从左往右"画"出来。实现方式是动画裁剪区域的右边界，而不是逐帧重建路径。
 *
 * **为什么用 drawWithCache 而不是 Canvas**：
 * Canvas 的绘制 lambda 每一帧都会重新执行，以前每帧都要把几百个点重新拼成 Path；
 * drawWithCache 把 Path 的构建放在缓存块里，只有尺寸或数据变化时才重建，
 * 动画期间每一帧只做"裁剪 + 绘制已缓存的 Path"。同时动画进度只在绘制阶段读取，
 * 播放动画不会触发重组和重新布局。
 *
 * @param values 数据点。
 * @param animateIn 首次显示时是否播放描线动画；实时刷新的曲线（如传感器调试页）应传 false。
 */
@Composable
fun SimpleLineChart(
    values: List<Float>,
    modifier: Modifier = Modifier,
    animateIn: Boolean = true
) {
    /** 折线跟随主题主色，主题切换时自动重组。 */
    val lineColor = MaterialTheme.colorScheme.primary

    /** 网格线使用弱对比轮廓色，避免抢夺数据视觉焦点。 */
    val gridColor = MaterialTheme.colorScheme.outlineVariant

    val inspectionMode = LocalInspectionMode.current
    /** 描线进度 0..1；只在首次进入组合时播放一次，数据刷新不会重放。 */
    val progress = remember { Animatable(if (animateIn && !inspectionMode) 0f else 1f) }
    LaunchedEffect(Unit) {
        if (progress.value < 1f) progress.animateTo(1f, tween(durationMillis = 700, easing = FastOutSlowInEasing))
    }

    Spacer(
        modifier = modifier
            .fillMaxWidth()
            .height(96.dp)
            .drawWithCache {
                // 缓存块：尺寸或 values 变化时才执行，负责构建 Path
                val path = Path()
                if (values.size >= 2) {
                    // 先归一化到 0..1，再映射到绘制高度，避免不同量纲直接超出绘制区域
                    val max = values.maxOrNull() ?: 1f
                    val min = values.minOrNull() ?: 0f
                    val range = (max - min).takeIf { it > 0f } ?: 1f
                    val stepX = size.width / values.lastIndex
                    values.forEachIndexed { index, value ->
                        val x = index * stepX
                        val y = size.height - ((value - min) / range * size.height)
                        if (index == 0) path.moveTo(x, y) else path.lineTo(x, y)
                    }
                }
                val stroke = Stroke(width = 2.dp.toPx(), cap = StrokeCap.Round)
                val gridStroke = 1.dp.toPx()

                // 绘制块：每帧执行，只读取动画进度做裁剪
                onDrawBehind {
                    if (values.size < 2) return@onDrawBehind
                    repeat(3) { index ->
                        val y = size.height * (index + 1) / 4f
                        drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = gridStroke)
                    }
                    clipRect(right = size.width * progress.value) {
                        drawPath(path = path, color = lineColor, style = stroke)
                    }
                }
            }
    )
}
