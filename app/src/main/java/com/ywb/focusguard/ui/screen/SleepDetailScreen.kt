package com.ywb.focusguard.ui.screen

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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.domain.analyzer.SleepAnalyzer
import com.ywb.focusguard.domain.model.SleepDetail
import com.ywb.focusguard.domain.model.SleepPlacement
import com.ywb.focusguard.ui.component.EnvironmentScoreGauge
import com.ywb.focusguard.ui.component.FocusDivider
import com.ywb.focusguard.ui.component.FocusPageHeader
import com.ywb.focusguard.ui.component.MetricCard
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.component.SimpleLineChart
import com.ywb.focusguard.ui.component.SleepStageChart
import com.ywb.focusguard.ui.state.SleepDetailUiState
import com.ywb.focusguard.ui.viewmodel.SleepDetailViewModel
import kotlin.math.roundToInt

/** 睡眠报告路由层：从 ViewModel 取状态交给纯页面。 */
@Composable
fun SleepDetailRoute(
    onBack: () -> Unit,
    viewModel: SleepDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SleepDetailScreen(uiState = uiState, onBack = onBack)
}

/** 睡眠报告页：Loading / Empty / Content 三种展示。 */
@Composable
fun SleepDetailScreen(uiState: SleepDetailUiState, onBack: () -> Unit) {
    when (uiState) {
        SleepDetailUiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            CircularProgressIndicator()
        }
        SleepDetailUiState.Empty -> Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 20.dp, vertical = 18.dp)
        ) {
            FocusPageHeader(title = "睡眠报告", subtitle = "没有找到这条记录", leading = { BackButton(onBack) })
        }
        is SleepDetailUiState.Content -> SleepDetailContent(uiState.detail, onBack)
    }
}

/**
 * 报告内容：结论（评分 + 时长）→ 关键指标 → 阶段图 → 原始曲线 → 评分拆解 → 建议。
 * 先给结论再给依据，和专注详情页的信息层级一致。
 */
@Composable
private fun SleepDetailContent(detail: SleepDetail, onBack: () -> Unit) {
    val session = detail.session
    val analysis = detail.analysis
    val epochs = detail.epochs
    val onsetTime = epochs.getOrNull(analysis.sleepOnsetIndex)?.startTime
    val wakeTime = epochs.getOrNull(analysis.finalWakeIndex)?.startTime?.plus(SleepAnalyzer.EPOCH_MILLIS)
    val calmPercent = if (analysis.totalSleepMillis > 0) {
        (analysis.calmMillis * 100f / analysis.totalSleepMillis).roundToInt()
    } else {
        0
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        FocusPageHeader(
            title = "睡眠报告",
            subtitle = sleepNightLabel(session.startTime),
            leading = { BackButton(onBack) }
        )

        Row(modifier = Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
            EnvironmentScoreGauge(score = analysis.score.total, size = 140.dp, label = "睡眠评分")
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(formatHoursMinutes(analysis.totalSleepMillis), style = MaterialTheme.typography.headlineMedium)
                Text(
                    "估算睡眠时长",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = if (onsetTime != null && wakeTime != null) {
                        "${clockText(onsetTime)} 入睡 · ${clockText(wakeTime)} 醒来"
                    } else {
                        "未识别到入睡"
                    },
                    style = MaterialTheme.typography.bodyMedium
                )
                Text(
                    text = "记录 ${formatHoursMinutes(analysis.monitoredMillis)} · ${session.placement.label}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        session.note?.let { note -> NoteCard(note) }

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(title = "关键指标")
            MetricPair(
                first = { MetricCard("入睡耗时", "${analysis.sleepLatencyMillis / 60_000L} 分钟", it) },
                second = {
                    MetricCard("睡眠效率", "${(analysis.sleepEfficiency * 100).roundToInt()}%", it, "睡眠 / 记录时长")
                }
            )
            MetricPair(
                first = { MetricCard("夜醒次数", "${analysis.wakeCount} 次", it, "持续 3 分钟以上") },
                second = {
                    MetricCard("安稳睡眠", formatHoursMinutes(analysis.calmMillis), it, "占睡眠 $calmPercent%")
                }
            )
            MetricPair(
                first = {
                    MetricCard(
                        "疑似鼾声",
                        if (session.audioEnabled) "${analysis.snoreEventCount} 次" else "未开启",
                        it
                    )
                },
                second = {
                    MetricCard(
                        "卧室环境",
                        if (session.audioEnabled) "${analysis.averageNoiseDb.roundToInt()} dB" else "-- dB",
                        it,
                        "平均光照 ${analysis.averageLightLux.roundToInt()} lux"
                    )
                }
            )
        }

        if (epochs.isNotEmpty()) {
            Column {
                SectionHeader(
                    title = "睡眠阶段",
                    action = {
                        Text(
                            "基于体动与声音估算",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Spacer(Modifier.height(10.dp))
                ChartCard {
                    SleepStageChart(
                        stages = analysis.stages,
                        startLabel = clockText(epochs.first().startTime),
                        endLabel = clockText(epochs.last().startTime + SleepAnalyzer.EPOCH_MILLIS)
                    )
                }
            }
        }

        if (session.placement == SleepPlacement.BED && epochs.size >= 2) {
            Column {
                SectionHeader(title = "体动（秒 / 分钟）")
                Spacer(Modifier.height(10.dp))
                ChartCard { SimpleLineChart(values = epochs.map { it.movementSeconds.toFloat() }) }
            }
        }

        if (session.audioEnabled && epochs.size >= 2) {
            Column {
                SectionHeader(title = "噪声峰值（相对 dB）")
                Spacer(Modifier.height(10.dp))
                ChartCard { SimpleLineChart(values = epochs.map { it.maxNoiseDb }) }
            }
        }

        Column {
            SectionHeader(title = "评分拆解")
            Spacer(Modifier.height(10.dp))
            ChartCard {
                val score = analysis.score
                PenaltyRow("睡眠时长", score.durationPenalty)
                FocusDivider()
                PenaltyRow("睡眠效率", score.efficiencyPenalty)
                FocusDivider()
                PenaltyRow("入睡耗时", score.latencyPenalty)
                FocusDivider()
                PenaltyRow("夜醒", score.wakePenalty)
                FocusDivider()
                PenaltyRow("鼾声", score.snorePenalty)
                FocusDivider()
                PenaltyRow("卧室环境", score.environmentPenalty)
            }
        }

        Card(
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text("改善建议", style = MaterialTheme.typography.titleMedium)
                    analysis.score.suggestions.forEach { suggestion ->
                        Text(
                            "· $suggestion",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }

        Text(
            text = SLEEP_DISCLAIMER,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
    }
}

/** 返回按钮。 */
@Composable
private fun BackButton(onBack: () -> Unit) {
    IconButton(onClick = onBack) {
        Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
    }
}

/** 并排两张指标卡，各占一半宽度；由调用方传入带 weight 的 Modifier。 */
@Composable
private fun MetricPair(
    first: @Composable (Modifier) -> Unit,
    second: @Composable (Modifier) -> Unit
) {
    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        first(Modifier.weight(1f))
        second(Modifier.weight(1f))
    }
}

/** 图表和列表共用的白色卡片容器。 */
@Composable
private fun ChartCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}

/** 评分拆解的一行：没有扣分显示"满分"，否则显示扣分值。 */
@Composable
private fun PenaltyRow(label: String, penalty: Int) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(
            text = if (penalty > 0) "-$penalty" else "满分",
            style = MaterialTheme.typography.titleSmall,
            color = if (penalty > 0) MaterialTheme.colorScheme.tertiary else MaterialTheme.colorScheme.primary
        )
    }
}

/** 系统说明卡片，例如记录被中断。 */
@Composable
private fun NoteCard(note: String) {
    Card(
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Outlined.Info, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary)
            Spacer(Modifier.width(10.dp))
            Text(note, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}
