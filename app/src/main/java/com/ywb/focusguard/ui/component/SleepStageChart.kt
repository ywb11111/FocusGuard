package com.ywb.focusguard.ui.component

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.ywb.focusguard.domain.model.SleepStage

/** 阶段图绘制区高度，左侧标签列使用同一高度保证三条泳道对齐。 */
private val CHART_HEIGHT = 120.dp

/** 左侧泳道标签列宽度，固定宽度才能让下方时间轴与 Canvas 精确对齐。 */
private val LABEL_WIDTH = 56.dp

/** 标签列与绘制区之间的间距。 */
private val LABEL_GAP = 8.dp

/**
 * 一段连续相同的睡眠阶段。
 *
 * @property stage 阶段。
 * @property start 起始分钟下标（包含）。
 * @property endExclusive 结束分钟下标（不包含）。
 */
internal data class StageSegment(
    val stage: SleepStage,
    val start: Int,
    val endExclusive: Int
)

/**
 * 把逐分钟阶段合并为连续段。
 *
 * 性能意义：一晚约 480 分钟，但阶段切换通常只有几十次。先合并再绘制，
 * drawRoundRect 调用从几百次降到几十次；配合 remember(stages)，只有数据变化时才重新合并，
 * 普通重组和重绘都直接复用结果。
 */
internal fun mergeStageSegments(stages: List<SleepStage>): List<StageSegment> {
    if (stages.isEmpty()) return emptyList()
    val segments = mutableListOf<StageSegment>()
    var segmentStart = 0
    for (index in 1..stages.size) {
        if (index == stages.size || stages[index] != stages[segmentStart]) {
            segments += StageSegment(stages[segmentStart], segmentStart, index)
            segmentStart = index
        }
    }
    return segments
}

/**
 * 睡眠阶段图（hypnogram）：横轴是时间，三条泳道从上到下为 清醒 / 浅睡·体动 / 安稳。
 *
 * 用 Canvas 自绘而不是图表库：阶段图本质是"按时间排列的色块"，图表库的折线/柱状模型不贴合，
 * 自绘几十行代码就能完全控制样式，也是 Compose Canvas 的练习点。
 *
 * @param stages 逐分钟阶段，按时间升序。
 * @param startLabel 左下角时间文字，例如入睡前开始监测的时间。
 * @param endLabel 右下角时间文字。
 */
@Composable
fun SleepStageChart(
    stages: List<SleepStage>,
    startLabel: String,
    endLabel: String,
    modifier: Modifier = Modifier
) {
    val awakeColor = MaterialTheme.colorScheme.tertiary
    val restlessColor = MaterialTheme.colorScheme.secondary
    val calmColor = MaterialTheme.colorScheme.primary
    val gridColor = MaterialTheme.colorScheme.outlineVariant
    val segments = remember(stages) { mergeStageSegments(stages) }
    val lanes = listOf(SleepStage.AWAKE, SleepStage.RESTLESS, SleepStage.CALM)

    Column(modifier = modifier.fillMaxWidth()) {
        Row(modifier = Modifier.fillMaxWidth()) {
            Column(modifier = Modifier.width(LABEL_WIDTH).height(CHART_HEIGHT)) {
                lanes.forEach { stage ->
                    Box(modifier = Modifier.weight(1f), contentAlignment = Alignment.CenterStart) {
                        Text(
                            text = stage.label,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
            Spacer(Modifier.width(LABEL_GAP))
            Canvas(
                modifier = Modifier
                    .weight(1f)
                    .height(CHART_HEIGHT)
                    .semantics { contentDescription = "睡眠阶段图，共 ${stages.size} 分钟" }
            ) {
                if (stages.isEmpty()) return@Canvas
                val laneHeight = size.height / lanes.size
                val minuteWidth = size.width / stages.size

                // 泳道分隔线
                for (lane in 1 until lanes.size) {
                    val y = lane * laneHeight
                    drawLine(gridColor, Offset(0f, y), Offset(size.width, y), strokeWidth = 1.dp.toPx())
                }

                val barHeight = laneHeight * 0.64f
                val corner = CornerRadius(3.dp.toPx())
                segments.forEach { segment ->
                    val lane = lanes.indexOf(segment.stage)
                    val color = when (segment.stage) {
                        SleepStage.AWAKE -> awakeColor
                        SleepStage.RESTLESS -> restlessColor
                        SleepStage.CALM -> calmColor
                    }
                    val left = segment.start * minuteWidth
                    // 至少 1px 宽，保证 1 分钟的短段在很长的夜里也看得见
                    val width = ((segment.endExclusive - segment.start) * minuteWidth).coerceAtLeast(1f)
                    drawRoundRect(
                        color = color,
                        topLeft = Offset(left, lane * laneHeight + (laneHeight - barHeight) / 2f),
                        size = Size(width, barHeight),
                        cornerRadius = corner
                    )
                }
            }
        }
        // 时间轴与 Canvas 左边缘对齐，而不是与标签列对齐
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(start = LABEL_WIDTH + LABEL_GAP, top = 6.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(startLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(endLabel, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
