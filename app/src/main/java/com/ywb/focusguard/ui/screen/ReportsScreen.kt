package com.ywb.focusguard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.ui.component.MetricCard
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.component.SimpleLineChart
import com.ywb.focusguard.ui.state.ReportPeriod
import com.ywb.focusguard.ui.state.ReportsUiState
import com.ywb.focusguard.ui.viewmodel.ReportsViewModel

/** 报告页路由层：收集 Room 驱动的报告状态并处理详情导航。 */
@Composable
fun ReportsRoute(
    onOpenSessionDetail: (Long) -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportsScreen(
        uiState = uiState,
        onOpenSessionDetail = onOpenSessionDetail,
        onSwitchPeriod = viewModel::switchPeriod
    )
}

/**
 * 报告页纯 UI，展示周/月汇总、趋势和历史列表。
 * 所有数据来自 Room，无 demo 占位。
 */
@Composable
fun ReportsScreen(
    uiState: ReportsUiState,
    onOpenSessionDetail: (Long) -> Unit,
    onSwitchPeriod: (ReportPeriod) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        item {
            Text(
                text = "报告",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.SemiBold
            )
        }
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ReportPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = uiState.period == period,
                        onClick = { onSwitchPeriod(period) },
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = ReportPeriod.entries.size
                        )
                    ) {
                        Text(period.label)
                    }
                }
            }
        }
        item {
            val summary = uiState.periodSummary
            SectionHeader(title = "${uiState.period.label}总结")
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                MetricCard("总专注", formatDuration(summary.totalFocusMillis), Modifier.weight(1f))
                MetricCard("平均分", "${summary.averageScore}", Modifier.weight(1f))
                MetricCard("次数", "${summary.sessionCount}", Modifier.weight(1f))
            }
            if (summary.sessionCount > 0) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 10.dp)
                ) {
                    MetricCard("平均噪声", "${summary.averageNoiseDb.toInt()} dB", Modifier.weight(1f))
                    MetricCard("平均光照", "${summary.averageLightLux.toInt()} lux", Modifier.weight(1f))
                    MetricCard("移动", "${summary.totalMovementCount} 次", Modifier.weight(1f))
                }
            }
        }
        item {
            SectionHeader(title = "趋势")
            if (uiState.trendValues.isNotEmpty()) {
                SimpleLineChart(
                    values = uiState.trendValues,
                    modifier = Modifier.padding(top = 10.dp)
                )
            } else {
                Text(
                    text = "暂无数据，完成专注后这里会显示评分趋势。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 10.dp)
                )
            }
        }
        item {
            SectionHeader(title = "记录列表")
        }
        if (uiState.sessions.isEmpty()) {
            item {
                Text(
                    text = "本${if (uiState.period == ReportPeriod.WEEK) "周" else "月"}暂无专注记录。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            items(uiState.sessions, key = { it.id }) { session ->
                SessionListItem(
                    session = session,
                    onClick = { onOpenSessionDetail(session.id) }
                )
            }
        }
    }
}

/** 一条可点击的历史专注记录。 */
@Composable
private fun SessionListItem(
    session: FocusSession,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = session.note ?: "专注记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = "${formatDuration(session.durationMillis)} · 评分 ${session.score} · 平均噪声 ${session.averageNoiseDb.toInt()} dB",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
