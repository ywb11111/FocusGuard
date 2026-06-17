package com.ywb.focusguard.ui.screen

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import com.ywb.focusguard.ui.state.SessionUiState
import com.ywb.focusguard.ui.viewmodel.SessionViewModel

@Composable
fun SessionRoute(
    onFinish: (Long) -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SessionScreen(
        uiState = uiState,
        onStart = {},
        onFinish = {
            viewModel.finishDemoSession()
            onFinish(2L)
        }
    )
}

@Composable
fun SessionScreen(
    uiState: SessionUiState,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "专注",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.SemiBold
        )

        when (uiState) {
            SessionUiState.Idle -> Text("准备专注")
            is SessionUiState.Ready -> ReadySessionContent(uiState, onStart, onFinish)
            is SessionUiState.Running -> RunningSessionContent(uiState, onFinish)
            is SessionUiState.Finished -> Text("本次评分 ${uiState.session.score}")
        }
    }
}

@Composable
private fun ReadySessionContent(
    uiState: SessionUiState.Ready,
    onStart: () -> Unit,
    onFinish: () -> Unit
) {
    SectionHeader(title = "专注模式")
    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        listOf("25 min", "45 min", "自定义").forEach { label ->
            ElevatedAssistChip(
                onClick = {},
                label = { Text(label) }
            )
        }
    }

    SectionHeader(title = "环境预检")
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        val environment = uiState.environment
        MetricCard(
            label = "噪声",
            value = environment?.noise?.level?.name ?: "WAIT",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "光照",
            value = environment?.light?.level?.name ?: "WAIT",
            modifier = Modifier.weight(1f)
        )
        MetricCard(
            label = "手机",
            value = if (environment?.motion?.isSignificantMove == true) "活动" else "稳定",
            modifier = Modifier.weight(1f)
        )
    }

    Button(
        onClick = onStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
        Text("开始专注", modifier = Modifier.padding(start = 8.dp))
    }

    OutlinedButton(
        onClick = onFinish,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text("生成一条演示记录")
    }
}

@Composable
private fun RunningSessionContent(
    uiState: SessionUiState.Running,
    onFinish: () -> Unit
) {
    Text(
        text = formatDuration(uiState.remainingMillis ?: 0L),
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.SemiBold
    )
    SectionHeader(title = "当前环境")
    SimpleLineChart(values = listOf(42f, 44f, 41f, 50f, 47f, 43f, 46f))
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        MetricCard("光照", uiState.lightLevel.name, Modifier.weight(1f))
        MetricCard("移动", "${uiState.movementCount} 次", Modifier.weight(1f))
    }
    OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth()) {
        Text("结束")
    }
}
