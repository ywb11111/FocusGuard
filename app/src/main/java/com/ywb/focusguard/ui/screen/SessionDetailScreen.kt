package com.ywb.focusguard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.ui.component.MetricCard
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.component.SimpleLineChart
import com.ywb.focusguard.ui.state.SessionDetailUiState
import com.ywb.focusguard.ui.viewmodel.SessionDetailViewModel

@Composable
fun SessionDetailRoute(
    viewModel: SessionDetailViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SessionDetailScreen(uiState = uiState)
}

@Composable
fun SessionDetailScreen(
    uiState: SessionDetailUiState
) {
    when (uiState) {
        SessionDetailUiState.Loading -> SessionDetailMessage("正在读取专注详情")
        is SessionDetailUiState.Empty -> SessionDetailMessage(uiState.message)
        is SessionDetailUiState.Content -> SessionDetailContent(uiState)
    }
}

@Composable
private fun SessionDetailContent(
    uiState: SessionDetailUiState.Content
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = uiState.title,
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            MetricCard("时长", uiState.durationText, Modifier.weight(1f))
            MetricCard("评分", uiState.scoreText, Modifier.weight(1f))
        }
        SectionHeader(title = "评分拆解")
        Text(uiState.scoreBreakdownText)
        SectionHeader(title = "噪声曲线")
        SimpleLineChart(values = uiState.noiseValues.ifEmpty { listOf(0f) })
        SectionHeader(title = "光照曲线")
        SimpleLineChart(values = uiState.lightValues.ifEmpty { listOf(0f) })
        SectionHeader(title = "建议")
        Text(
            text = uiState.suggestionText,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun SessionDetailMessage(message: String) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = "专注详情",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )
        Text(
            text = message,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
