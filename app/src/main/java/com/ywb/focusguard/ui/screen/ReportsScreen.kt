package com.ywb.focusguard.ui.screen

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarMonth
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.Insights
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SegmentedButton
import androidx.compose.material3.SegmentedButtonDefaults
import androidx.compose.material3.SingleChoiceSegmentedButtonRow
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.ui.component.FocusPageHeader
import com.ywb.focusguard.ui.component.FocusStat
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.component.SimpleLineChart
import com.ywb.focusguard.ui.state.ReportPeriod
import com.ywb.focusguard.ui.state.ReportsUiState
import com.ywb.focusguard.ui.theme.FocusBlueSoft
import com.ywb.focusguard.ui.theme.FocusTealSoft
import com.ywb.focusguard.ui.viewmodel.ReportsViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 报告页路由层：收集 Room 的真实统计并处理周期选择。 */
@Composable
fun ReportsRoute(
    onOpenSessionDetail: (Long) -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportsScreen(uiState, onOpenSessionDetail, viewModel::switchPeriod)
}

/** 报告页优先回答“表现如何、主要受什么影响、下一步做什么”。 */
@Composable
fun ReportsScreen(
    uiState: ReportsUiState,
    onOpenSessionDetail: (Long) -> Unit,
    onSwitchPeriod: (ReportPeriod) -> Unit = {}
) {
    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        item { FocusPageHeader(title = "报告", subtitle = "从真实记录中找到你的专注规律") }
        item {
            SingleChoiceSegmentedButtonRow(modifier = Modifier.fillMaxWidth()) {
                ReportPeriod.entries.forEachIndexed { index, period ->
                    SegmentedButton(
                        selected = uiState.period == period,
                        onClick = { onSwitchPeriod(period) },
                        shape = SegmentedButtonDefaults.itemShape(index, ReportPeriod.entries.size)
                    ) { Text(period.label) }
                }
            }
        }

        if (uiState.sessions.isEmpty()) {
            item { EmptyReport(uiState.period) }
        } else {
            item { SummaryHero(uiState) }
            item { InsightCard(uiState) }
            item {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    SectionHeader(title = "评分趋势", action = {
                        Text("按完成时间", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    })
                    Card(
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            SimpleLineChart(values = uiState.trendValues)
                            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                                Text("较早", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                                Text("最近", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            }
                        }
                    }
                }
            }
            item { EnvironmentAverages(uiState) }
            item { SectionHeader(title = "专注记录", action = {
                Text("${uiState.sessions.size} 次", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }) }
            item {
                Card(
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column {
                        uiState.sessions.forEachIndexed { index, session ->
                            SessionListRow(session) { onOpenSessionDetail(session.id) }
                            if (index != uiState.sessions.lastIndex) {
                                HorizontalDivider(
                                    modifier = Modifier.padding(horizontal = 16.dp),
                                    color = MaterialTheme.colorScheme.outlineVariant
                                )
                            }
                        }
                    }
                }
            }
        }
        item { Spacer(Modifier.size(4.dp)) }
    }
}

@Composable
private fun SummaryHero(uiState: ReportsUiState) {
    val summary = uiState.periodSummary
    Card(
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
    ) {
        Column(modifier = Modifier.fillMaxWidth().padding(20.dp), verticalArrangement = Arrangement.spacedBy(18.dp)) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Top) {
                Column {
                    Text("${uiState.period.label}专注", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(formatDurationCompact(summary.totalFocusMillis), style = MaterialTheme.typography.headlineLarge)
                }
                Column(horizontalAlignment = Alignment.End) {
                    Text("平均评分", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(summary.averageScore.toString(), style = MaterialTheme.typography.headlineLarge, color = MaterialTheme.colorScheme.primary)
                }
            }
            Row(modifier = Modifier.fillMaxWidth()) {
                FocusStat("${summary.sessionCount} 次", "完成次数", Modifier.weight(1f), icon = Icons.Outlined.CalendarMonth)
                FocusStat("${summary.totalMovementCount} 次", "手机移动", Modifier.weight(1f), icon = Icons.Outlined.PhoneAndroid)
            }
        }
    }
}

@Composable
private fun InsightCard(uiState: ReportsUiState) {
    val best = uiState.sessions.maxByOrNull { it.score }
    Card(
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.Top) {
            Surface(shape = CircleShape, color = FocusTealSoft, modifier = Modifier.size(46.dp)) {
                Box(contentAlignment = Alignment.Center) {
                    Icon(Icons.Outlined.Insights, null, tint = MaterialTheme.colorScheme.primary)
                }
            }
            Spacer(Modifier.width(12.dp))
            Column {
                Text("本期洞察", style = MaterialTheme.typography.titleMedium)
                Text(
                    reportInsight(uiState, best),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun EnvironmentAverages(uiState: ReportsUiState) {
    val summary = uiState.periodSummary
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = "环境均值")
        Card(shape = RoundedCornerShape(18.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                AverageRow(Icons.Outlined.GraphicEq, "平均噪声", "${summary.averageNoiseDb.toInt()} dB")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AverageRow(Icons.Outlined.LightMode, "平均光照", "${summary.averageLightLux.toInt()} lux")
                HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                AverageRow(Icons.Outlined.PhoneAndroid, "移动事件", "${summary.totalMovementCount} 次")
            }
        }
    }
}

@Composable
private fun AverageRow(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, value: String) {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 15.dp), verticalAlignment = Alignment.CenterVertically) {
        Surface(shape = CircleShape, color = FocusBlueSoft, modifier = Modifier.size(40.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.secondary, modifier = Modifier.size(20.dp)) }
        }
        Spacer(Modifier.width(12.dp))
        Text(label, style = MaterialTheme.typography.titleSmall, modifier = Modifier.weight(1f))
        Text(value, style = MaterialTheme.typography.titleMedium)
    }
}

@Composable
private fun SessionListRow(session: FocusSession, onClick: () -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(session.note?.takeIf { it.isNotBlank() } ?: "专注记录", style = MaterialTheme.typography.titleSmall)
            Text(
                "${sessionDateText(session.startTime)} · ${formatDuration(session.durationMillis)} · ${session.averageNoiseDb.toInt()} dB",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(session.score.toString(), style = MaterialTheme.typography.titleLarge, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
        Spacer(Modifier.width(4.dp))
        Icon(Icons.Outlined.ChevronRight, "查看详情", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun EmptyReport(period: ReportPeriod) {
    Column(
        modifier = Modifier.fillMaxWidth().padding(vertical = 72.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Surface(shape = CircleShape, color = FocusTealSoft, modifier = Modifier.size(72.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(Icons.Outlined.Insights, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(32.dp)) }
        }
        Spacer(Modifier.size(16.dp))
        Text("还没有${period.label}记录", style = MaterialTheme.typography.titleLarge)
        Text("完成一次专注后，这里会开始形成你的环境规律。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

private fun reportInsight(uiState: ReportsUiState, best: FocusSession?): String {
    val summary = uiState.periodSummary
    return when {
        best == null -> "完成更多专注后，这里会生成个性化洞察。"
        summary.averageNoiseDb >= 65f -> "噪声是本期最明显的影响因素。你最高分的一次为 ${best.score} 分，优先复用那次的地点与时间。"
        summary.averageLightLux < 100f -> "本期平均光照偏暗。开始前先调整台灯，是最容易落实的改善。"
        summary.totalMovementCount > summary.sessionCount * 2 -> "手机移动比较频繁。把手机放在固定位置，有助于减少注意力切换。"
        else -> "本期环境整体稳定，最高分为 ${best.score} 分。继续保持当前地点和专注节奏。"
    }
}

private fun sessionDateText(timestamp: Long): String =
    SimpleDateFormat("M月d日 HH:mm", Locale.getDefault()).format(Date(timestamp))
