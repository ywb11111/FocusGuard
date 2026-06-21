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
    // Route 层负责把 ViewModel 和纯 UI 组件接起来；SessionScreen 本身不直接知道 ViewModel。
    SessionScreen(
        uiState = uiState,
        onStart = viewModel::startSession,
        onPause = viewModel::pauseSession,
        onResume = viewModel::resumeSession,
        onFinish = viewModel::finishSession,
        onReset = viewModel::resetSession,
        onOpenDetail = onFinish
    )
}

@Composable
fun SessionScreen(
    uiState: SessionUiState,
    onStart: () -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    onOpenDetail: (Long) -> Unit
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

        // UI 只根据状态分支显示不同内容；真正的状态切换发生在 SessionViewModel。
        when (uiState) {
            SessionUiState.Idle -> Text("准备专注")
            is SessionUiState.Ready -> ReadySessionContent(uiState, onStart)
            is SessionUiState.Running -> RunningSessionContent(uiState, onPause, onFinish)
            is SessionUiState.Paused -> PausedSessionContent(uiState, onResume, onFinish)
            is SessionUiState.Finished -> FinishedSessionContent(uiState, onReset, onOpenDetail)
        }
    }
}

@Composable
private fun ReadySessionContent(
    uiState: SessionUiState.Ready,
    onStart: () -> Unit
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
        // 点击事件只上报给 ViewModel，不在 Composable 内部自己启动计时器。
        onClick = onStart,
        modifier = Modifier.fillMaxWidth()
    ) {
        Icon(Icons.Outlined.PlayArrow, contentDescription = null)
        Text("开始专注", modifier = Modifier.padding(start = 8.dp))
    }
}

@Composable
private fun RunningSessionContent(
    uiState: SessionUiState.Running,
    onPause: () -> Unit,
    onFinish: () -> Unit
) {
    Text(
        text = formatDuration(uiState.remainingMillis ?: 0L),
        style = MaterialTheme.typography.displayMedium,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        text = "已专注 ${formatDuration(uiState.elapsedMillis)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
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
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            // 暂停逻辑在 ViewModel 中保存 accumulatedMillis，UI 这里只负责触发事件。
            onClick = onPause,
            modifier = Modifier.weight(1f)
        ) {
            Text("暂停")
        }
        OutlinedButton(
            // 结束后 ViewModel 会生成 Finished 状态，UI 随状态自动切换到总结区。
            onClick = onFinish,
            modifier = Modifier.weight(1f)
        ) {
            Text("结束")
        }
    }
}

@Composable
private fun PausedSessionContent(
    uiState: SessionUiState.Paused,
    onResume: () -> Unit,
    onFinish: () -> Unit
) {
    Text(
        text = "已暂停",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        text = "已专注 ${formatDuration(uiState.elapsedMillis)} · 剩余 ${formatDuration(uiState.remainingMillis ?: 0L)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            // 继续时不会清空已用时长，而是从 Paused 状态恢复计时。
            onClick = onResume,
            modifier = Modifier.weight(1f)
        ) {
            Text("继续")
        }
        OutlinedButton(
            onClick = onFinish,
            modifier = Modifier.weight(1f)
        ) {
            Text("结束")
        }
    }
}

@Composable
private fun FinishedSessionContent(
    uiState: SessionUiState.Finished,
    onReset: () -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    Text(
        text = "本次评分 ${uiState.session.score}",
        style = MaterialTheme.typography.displaySmall,
        fontWeight = FontWeight.SemiBold
    )
    Text(
        text = "专注 ${formatDuration(uiState.session.durationMillis)}",
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant
    )
    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        Button(
            onClick = { onOpenDetail(uiState.session.id) },
            modifier = Modifier.weight(1f)
        ) {
            Text("查看详情")
        }
        OutlinedButton(
            onClick = onReset,
            modifier = Modifier.weight(1f)
        ) {
            Text("再来一次")
        }
    }
}
