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
import com.ywb.focusguard.ui.state.ReportsUiState
import com.ywb.focusguard.ui.viewmodel.ReportsViewModel

@Composable
fun ReportsRoute(
    onOpenSessionDetail: (Long) -> Unit,
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    ReportsScreen(
        uiState = uiState,
        onOpenSessionDetail = onOpenSessionDetail
    )
}

@Composable
fun ReportsScreen(
    uiState: ReportsUiState,
    onOpenSessionDetail: (Long) -> Unit
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
            SingleChoiceSegmentedButtonRow {
                listOf("本周", "本月").forEachIndexed { index, label ->
                    SegmentedButton(
                        selected = index == 0,
                        onClick = {},
                        shape = SegmentedButtonDefaults.itemShape(
                            index = index,
                            count = 2
                        )
                    ) {
                        Text(label)
                    }
                }
            }
        }
        item {
            SectionHeader(title = "本周总结")
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 10.dp)
            ) {
                MetricCard("总专注", formatDuration(uiState.summary?.totalFocusMillis ?: 0L), Modifier.weight(1f))
                MetricCard("平均分", "${uiState.summary?.averageScore ?: 0}", Modifier.weight(1f))
            }
        }
        item {
            SectionHeader(title = "趋势")
            SimpleLineChart(
                values = uiState.sessions.ifEmpty { demoSessions() }.map { it.score.toFloat() },
                modifier = Modifier.padding(top = 10.dp)
            )
        }
        item {
            SectionHeader(title = "记录列表")
        }
        items(uiState.sessions.ifEmpty { demoSessions() }, key = { it.id }) { session ->
            SessionListItem(
                session = session,
                onClick = { onOpenSessionDetail(session.id) }
            )
        }
    }
}

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

private fun demoSessions(): List<FocusSession> = listOf(
    FocusSession(1, 0, 0, 45 * 60 * 1000L, 42f, 58f, 180f, 2, 1, 88, "晚间学习"),
    FocusSession(2, 0, 0, 30 * 60 * 1000L, 48f, 64f, 140f, 4, 2, 76, "午后复习")
)
