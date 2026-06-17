package com.ywb.focusguard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.domain.analyzer.EnvironmentAnalyzer
import com.ywb.focusguard.ui.component.MetricCard
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.state.TodayUiState
import com.ywb.focusguard.ui.viewmodel.TodayViewModel

@Composable
fun TodayRoute(
    onStartFocus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSessionDetail: (Long) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    TodayScreen(
        uiState = uiState,
        onStartFocus = onStartFocus,
        onOpenSettings = onOpenSettings,
        onOpenSessionDetail = onOpenSessionDetail
    )
}

@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onStartFocus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSessionDetail: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column {
                Text(
                    text = "FocusGuard",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = "专注环境助手",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onOpenSettings) {
                Icon(Icons.Outlined.Settings, contentDescription = "设置")
            }
        }

        val environment = uiState.environment
        StatusCard(
            title = environment?.let { EnvironmentAnalyzer().headline(it) } ?: "正在读取环境",
            subtitle = "噪声、光照和移动状态会在后续阶段接入真实传感器。"
        )

        SectionHeader(title = "实时指标")
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricCard(
                label = "噪声",
                value = "${environment?.noise?.decibel?.toInt() ?: "--"} dB",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "光照",
                value = "${environment?.light?.lux?.toInt() ?: "--"} lux",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "移动",
                value = if (environment?.motion?.isSignificantMove == true) "活动" else "稳定",
                modifier = Modifier.weight(1f)
            )
        }

        SectionHeader(title = "今日概览")
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            val summary = uiState.todaySummary
            MetricCard(
                label = "专注",
                value = formatDuration(summary?.totalFocusMillis ?: 0L),
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "评分",
                value = "${summary?.averageScore ?: 0}",
                modifier = Modifier.weight(1f)
            )
            MetricCard(
                label = "分心",
                value = "${summary?.distractionCount ?: 0} 次",
                modifier = Modifier.weight(1f)
            )
        }

        SectionHeader(title = "最近记录")
        val latest = uiState.latestSession
        Card(
            onClick = { latest?.let { onOpenSessionDetail(it.id) } },
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    text = latest?.note ?: "还没有专注记录",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold
                )
                Text(
                    text = latest?.let { "${formatDuration(it.durationMillis)} · 评分 ${it.score}" }
                        ?: "完成一次专注后，这里会显示最近结果。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = onStartFocus,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
            Text(text = "开始专注", modifier = Modifier.padding(start = 8.dp))
        }
    }
}

@Composable
private fun StatusCard(
    title: String,
    subtitle: String
) {
    Card(
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer,
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer
        ),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.SemiBold
            )
            Text(text = subtitle, style = MaterialTheme.typography.bodyMedium)
        }
    }
}

fun formatDuration(durationMillis: Long): String {
    val totalMinutes = durationMillis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
