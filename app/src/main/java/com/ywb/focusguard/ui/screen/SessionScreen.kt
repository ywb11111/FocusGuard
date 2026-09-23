package com.ywb.focusguard.ui.screen

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.SizeTransform
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.StopCircle
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.domain.model.EnvironmentSnapshot
import com.ywb.focusguard.domain.model.LightLevel
import com.ywb.focusguard.domain.model.NoiseLevel
import com.ywb.focusguard.ui.component.EnvironmentMetricRow
import com.ywb.focusguard.ui.component.FocusDivider
import com.ywb.focusguard.ui.component.FocusPageHeader
import com.ywb.focusguard.ui.component.PositiveStatus
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.state.SessionUiState
import com.ywb.focusguard.ui.state.visualPhase
import com.ywb.focusguard.ui.theme.FocusAmberSoft
import com.ywb.focusguard.ui.theme.FocusTealSoft
import com.ywb.focusguard.ui.viewmodel.SessionViewModel

/** 专注流程路由：把状态机方法转换为 UI 事件。 */
@Composable
fun SessionRoute(
    onFinish: (Long) -> Unit,
    viewModel: SessionViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SessionScreen(
        uiState = uiState,
        onStart = viewModel::startSession,
        onSelectDuration = viewModel::selectDuration,
        onPause = viewModel::pauseSession,
        onResume = viewModel::resumeSession,
        onFinish = viewModel::finishSession,
        onReset = viewModel::resetSession,
        onOpenDetail = onFinish
    )
}

/** 单一状态机驱动完整专注流程，避免准备、运行和完成页面同时存在。 */
@Composable
fun SessionScreen(
    uiState: SessionUiState,
    onStart: () -> Unit,
    onSelectDuration: (Int) -> Unit,
    onPause: () -> Unit,
    onResume: () -> Unit,
    onFinish: () -> Unit,
    onReset: () -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    AnimatedContent(
        targetState = uiState,
        // 高频字段变化只重组当前内容；只有 Ready -> Running 等阶段变化才执行整页转场。
        contentKey = { state -> state.visualPhase() },
        transitionSpec = { fadeIn() togetherWith fadeOut() using SizeTransform(clip = false) },
        label = "session-state"
    ) { state ->
        when (state) {
            SessionUiState.Idle -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator()
            }
            is SessionUiState.Ready -> ReadySessionContent(state, onStart, onSelectDuration)
            is SessionUiState.Running -> RunningSessionContent(state, onPause, onFinish)
            is SessionUiState.Paused -> PausedSessionContent(state, onResume, onFinish)
            is SessionUiState.Finished -> FinishedSessionContent(state, onReset, onOpenDetail)
        }
    }
}

/** 准备页完成时长选择与环境预检，开始后配置被冻结。 */
@Composable
private fun ReadySessionContent(
    uiState: SessionUiState.Ready,
    onStart: () -> Unit,
    onSelectDuration: (Int) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        FocusPageHeader(title = "专注准备", subtitle = "先设置时长，再确认此刻的环境")

        Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            SectionHeader(title = "本次时长")
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surfaceVariant, RoundedCornerShape(16.dp))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                listOf(25, 45, 60).forEach { minutes ->
                    val selected = uiState.config.durationMinutes == minutes
                    Surface(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(12.dp))
                            .clickable { onSelectDuration(minutes) },
                        shape = RoundedCornerShape(12.dp),
                        color = if (selected) MaterialTheme.colorScheme.primaryContainer else androidx.compose.ui.graphics.Color.Transparent,
                        tonalElevation = if (selected) 2.dp else 0.dp
                    ) {
                        Column(
                            modifier = Modifier.padding(vertical = 14.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Text(
                                "$minutes",
                                style = MaterialTheme.typography.titleLarge,
                                color = if (selected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                            )
                            Text("分钟", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
            }
        }

        Column {
            SectionHeader(title = "环境预检", action = { EnvironmentPrecheckStatus(uiState.environment) })
            Spacer(Modifier.height(10.dp))
            Card(
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    ReadyNoiseRow(uiState.environment)
                    FocusDivider()
                    ReadyLightRow(uiState.environment)
                    FocusDivider()
                    ReadyMotionRow(uiState.environment)
                }
            }
        }

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("开始 ${uiState.config.durationMinutes} 分钟专注", style = MaterialTheme.typography.titleMedium)
        }
        Text(
            "专注期间会采集相对音量、环境光照和手机移动事件，不保存原始音频。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 运行页主动降低信息密度，倒计时是唯一视觉中心。 */
@Composable
private fun RunningSessionContent(
    uiState: SessionUiState.Running,
    onPause: () -> Unit,
    onFinish: () -> Unit
) {
    val total = uiState.elapsedMillis + (uiState.remainingMillis ?: 0L)
    val progress = if (total > 0L) uiState.elapsedMillis.toFloat() / total else 0f
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 24.dp, vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("保持专注", style = MaterialTheme.typography.titleLarge)
        Text("环境变化会被安静地记录", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.weight(0.8f))
        Box(contentAlignment = Alignment.Center, modifier = Modifier.size(250.dp)) {
            CircularProgressIndicator(
                progress = { progress.coerceIn(0f, 1f) },
                modifier = Modifier.fillMaxSize(),
                strokeWidth = 12.dp,
                trackColor = MaterialTheme.colorScheme.surfaceVariant
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(formatDuration(uiState.remainingMillis ?: 0L), style = MaterialTheme.typography.displayLarge)
                Text("剩余时间", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        Spacer(Modifier.height(34.dp))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceEvenly) {
            LiveMetric(Icons.Outlined.GraphicEq, uiState.noiseSamples.lastOrNull()?.let { "${it.decibel.toInt()} dB" } ?: "检测中", "噪声")
            LiveMetric(Icons.Outlined.LightMode, lightLabel(uiState.lightLevel), "光照")
            LiveMetric(Icons.Outlined.PhoneAndroid, "${uiState.movementCount} 次", "移动")
        }
        Spacer(Modifier.weight(1f))
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onPause, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Outlined.Pause, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("暂停")
            }
            Button(onClick = onFinish, modifier = Modifier.weight(1f).height(54.dp), shape = RoundedCornerShape(14.dp)) {
                Icon(Icons.Outlined.StopCircle, contentDescription = null)
                Spacer(Modifier.width(6.dp))
                Text("完成")
            }
        }
    }
}

/** 暂停页冻结计时，并明确继续与提前完成两条路径。 */
@Composable
private fun PausedSessionContent(
    uiState: SessionUiState.Paused,
    onResume: () -> Unit,
    onFinish: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Surface(shape = CircleShape, color = FocusAmberSoft, modifier = Modifier.size(76.dp)) {
            Box(contentAlignment = Alignment.Center) {
                Icon(Icons.Outlined.Pause, contentDescription = null, tint = MaterialTheme.colorScheme.tertiary, modifier = Modifier.size(34.dp))
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("专注已暂停", style = MaterialTheme.typography.headlineMedium)
        Text(
            "已完成 ${formatDuration(uiState.elapsedMillis)}，休息好再继续。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(36.dp))
        Button(onClick = onResume, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("继续专注")
        }
        Spacer(Modifier.height(10.dp))
        OutlinedButton(onClick = onFinish, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Text("提前完成")
        }
    }
}

/** 完成页用可解释扣分和一条建议替代单纯庆祝。 */
@Composable
private fun FinishedSessionContent(
    uiState: SessionUiState.Finished,
    onReset: () -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    val session = uiState.session
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        FocusPageHeader(title = "专注完成", subtitle = "记录已保存，看看这次发生了什么")
        Card(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(20.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth().padding(22.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text("本次评分", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Text(session.score.toString(), style = MaterialTheme.typography.displayMedium)
                }
                Column(horizontalAlignment = Alignment.End) {
                    PositiveStatus(if (session.score >= 80) "表现稳定" else "已完成")
                    Spacer(Modifier.height(10.dp))
                    Text(formatDuration(session.durationMillis), style = MaterialTheme.typography.titleLarge)
                }
            }
        }

        Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SectionHeader(title = "环境表现")
            ScoreBar("噪声", session.averageNoiseDb.coerceIn(0f, 100f) / 100f, "${session.averageNoiseDb.toInt()} dB")
            ScoreBar("光照", (session.averageLightLux.coerceIn(0f, 800f) / 800f), "${session.averageLightLux.toInt()} lux")
            ScoreBar("手机移动", (session.movementCount / 10f).coerceIn(0f, 1f), "${session.movementCount} 次")
        }

        Card(shape = RoundedCornerShape(16.dp), colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)) {
            Row(modifier = Modifier.fillMaxWidth().padding(16.dp), verticalAlignment = Alignment.Top) {
                Icon(Icons.Outlined.Analytics, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("下一次建议", style = MaterialTheme.typography.titleMedium)
                    Text(
                        completionSuggestion(session.averageNoiseDb, session.averageLightLux, session.movementCount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        Button(onClick = { onOpenDetail(session.id) }, modifier = Modifier.fillMaxWidth().height(56.dp), shape = RoundedCornerShape(14.dp)) {
            Text("查看完整分析")
        }
        OutlinedButton(onClick = onReset, modifier = Modifier.fillMaxWidth().height(52.dp), shape = RoundedCornerShape(14.dp)) {
            Icon(Icons.Outlined.Refresh, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("再次专注")
        }
    }
}

@Composable
private fun EnvironmentPrecheckStatus(environment: EnvironmentSnapshot?) {
    if (environment == null) {
        Text("检测中", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
    } else if (environment.status == com.ywb.focusguard.domain.model.EnvironmentStatus.FOCUSED) {
        PositiveStatus("状态良好")
    } else {
        Text("建议先调整", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.tertiary)
    }
}

@Composable
private fun ReadyNoiseRow(environment: EnvironmentSnapshot?) {
    val level = environment?.noise?.level
    val healthy = level == NoiseLevel.QUIET || level == NoiseLevel.NORMAL
    EnvironmentMetricRow(
        Icons.Outlined.GraphicEq,
        "噪声",
        environment?.let { "${it.noise.decibel.toInt()} dB" } ?: "-- dB",
        when (level) { NoiseLevel.QUIET -> "安静"; NoiseLevel.NORMAL -> "适中"; NoiseLevel.NOISY -> "偏吵"; NoiseLevel.LOUD -> "嘈杂"; null -> "检测中" },
        if (level == null) "等待麦克风数据" else if (healthy) "适合专注" else "建议降低噪声",
        healthy
    )
}

@Composable
private fun ReadyLightRow(environment: EnvironmentSnapshot?) {
    val level = environment?.light?.level
    val healthy = level == LightLevel.COMFORTABLE
    EnvironmentMetricRow(
        Icons.Outlined.LightMode,
        "光照",
        environment?.let { "${it.light.lux.toInt()} lux" } ?: "-- lux",
        lightLabel(level),
        if (level == null) "等待光照传感器" else if (healthy) "适合阅读" else "建议调整光源",
        healthy
    )
}

@Composable
private fun ReadyMotionRow(environment: EnvironmentSnapshot?) {
    val healthy = environment != null && !environment.motion.isMoving
    EnvironmentMetricRow(
        Icons.Outlined.PhoneAndroid,
        "手机",
        if (environment == null) "--" else if (environment.motion.isMoving) "移动中" else "稳定",
        if (environment == null) "检测中" else if (healthy) "稳定" else "移动",
        if (environment == null) "等待移动传感器" else if (healthy) "未检测到明显移动" else "请先放稳手机",
        healthy
    )
}

@Composable
private fun LiveMetric(icon: ImageVector, value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Surface(shape = CircleShape, color = FocusTealSoft, modifier = Modifier.size(44.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, null, tint = MaterialTheme.colorScheme.primary) }
        }
        Spacer(Modifier.height(8.dp))
        Text(value, style = MaterialTheme.typography.titleSmall)
        Text(label, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ScoreBar(label: String, value: Float, trailing: String) {
    Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(label, style = MaterialTheme.typography.bodyMedium)
            Text(trailing, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        LinearProgressIndicator(
            progress = { value.coerceIn(0f, 1f) },
            modifier = Modifier.fillMaxWidth().height(7.dp).clip(CircleShape),
            trackColor = MaterialTheme.colorScheme.surfaceVariant
        )
    }
}

private fun lightLabel(level: LightLevel?): String = when (level) {
    LightLevel.DARK -> "过暗"
    LightLevel.DIM -> "偏暗"
    LightLevel.COMFORTABLE -> "适宜"
    LightLevel.BRIGHT -> "偏亮"
    null -> "检测中"
}

private fun completionSuggestion(noise: Float, light: Float, moves: Int): String = when {
    noise >= 65f -> "本次噪声偏高。下次优先换到更安静的位置，通常比延长时长更有效。"
    light < 100f -> "本次光照偏暗。下次开始前先调整台灯，能降低长时间阅读的疲劳。"
    light > 800f -> "本次光线偏亮。减少直射光或调整屏幕角度会更舒适。"
    moves >= 3 -> "手机移动较多。下次把它放在固定位置，并提前开启勿扰模式。"
    else -> "这次环境整体稳定，可以保留当前地点和时间段作为你的专注模板。"
}
