package com.ywb.focusguard.ui.screen

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ElevatedAssistChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.ui.component.MetricCard
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.component.SimpleLineChart
import com.ywb.focusguard.ui.state.SessionUiState
import com.ywb.focusguard.ui.theme.MintAccent
import com.ywb.focusguard.ui.theme.MintPrimary
import com.ywb.focusguard.ui.theme.MintPrimaryDark
import com.ywb.focusguard.ui.theme.MintSecondary
import com.ywb.focusguard.ui.theme.MintWarning
import com.ywb.focusguard.ui.viewmodel.SessionViewModel

/** 专注页路由层：收集 ViewModel 状态，并把用户事件映射为状态机方法。 */
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

/**
 * 专注页纯 UI，根据 sealed UiState 只展示当前合法阶段。
 * 所有计时、数据库和传感器逻辑都留在 ViewModel/Repository/DataSource。
 */
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
    // 状态切换动画：使用 AnimatedVisibility 实现淡入淡出 + 滑动效果
    var previousState by remember { mutableStateOf<SessionUiState?>(null) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        Text(
            text = "专注",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // UI 只根据状态分支显示不同内容；真正的状态切换发生在 SessionViewModel。
        // 使用 AnimatedVisibility 实现状态切换动画
        AnimatedVisibility(
            visible = uiState is SessionUiState.Idle,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300))
        ) {
            Text("准备专注")
        }

        AnimatedVisibility(
            visible = uiState is SessionUiState.Ready,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300))
        ) {
            if (uiState is SessionUiState.Ready) {
                ReadySessionContent(uiState, onStart)
            }
        }

        AnimatedVisibility(
            visible = uiState is SessionUiState.Running,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300))
        ) {
            if (uiState is SessionUiState.Running) {
                RunningSessionContent(uiState, onPause, onFinish)
            }
        }

        AnimatedVisibility(
            visible = uiState is SessionUiState.Paused,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300))
        ) {
            if (uiState is SessionUiState.Paused) {
                PausedSessionContent(uiState, onResume, onFinish)
            }
        }

        AnimatedVisibility(
            visible = uiState is SessionUiState.Finished,
            enter = fadeIn(animationSpec = tween(300)) + slideInVertically(animationSpec = tween(300)),
            exit = fadeOut(animationSpec = tween(300)) + slideOutVertically(animationSpec = tween(300))
        ) {
            if (uiState is SessionUiState.Finished) {
                FinishedSessionContent(uiState, onReset, onOpenDetail)
            }
        }
    }
}

/** 准备状态内容：展示模式、环境预检和开始入口。 */
@Composable
private fun ReadySessionContent(
    uiState: SessionUiState.Ready,
    onStart: () -> Unit
) {
    // 时长选择 - 原型设计风格
    SectionHeader(title = "选择时长")
    Row(
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        listOf(25, 45, 60).forEach { minutes ->
            // 按钮点击动画：按下时缩放，释放时恢复
            var isPressed by remember { mutableStateOf(false) }
            val scale by animateFloatAsState(
                targetValue = if (isPressed) 0.95f else 1f,
                animationSpec = tween(100)
            )

            Card(
                onClick = { /* TODO: 设置选择的时长 */ },
                modifier = Modifier
                    .weight(1f)
                    .scale(scale),
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (minutes == 25) MintPrimary else MaterialTheme.colorScheme.surface
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        text = "$minutes",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (minutes == 25) Color(0xFF065F46) else MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "分钟",
                        style = MaterialTheme.typography.bodySmall,
                        color = if (minutes == 25) Color(0xFF047857) else MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }

    // 环境预检 - 原型设计风格
    SectionHeader(title = "环境预检")
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            val environment = uiState.environment
            EnvironmentCheckItem(
                icon = "🔊",
                label = "噪声",
                status = environment?.noise?.level?.name ?: "WAIT"
            )
            EnvironmentCheckItem(
                icon = "💡",
                label = "光照",
                status = environment?.light?.level?.name ?: "WAIT"
            )
            EnvironmentCheckItem(
                icon = "📱",
                label = "手机",
                status = if (environment?.motion?.isSignificantMove == true) "活动" else "稳定"
            )
        }
    }

    // 开始按钮 - 原型设计风格（渐变背景 + 点击动画）
    var isButtonPressed by remember { mutableStateOf(false) }
    val buttonScale by animateFloatAsState(
        targetValue = if (isButtonPressed) 0.95f else 1f,
        animationSpec = tween(100)
    )

    Button(
        onClick = onStart,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(buttonScale),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = Color.Transparent
        ),
        contentPadding = ButtonDefaults.TextButtonContentPadding
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    brush = Brush.horizontalGradient(
                        colors = listOf(MintPrimary, MintPrimaryDark)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Icon(
                    Icons.Outlined.PlayArrow,
                    contentDescription = null,
                    tint = Color(0xFF065F46)
                )
                Text(
                    text = "开始专注",
                    modifier = Modifier.padding(start = 8.dp),
                    fontWeight = FontWeight.SemiBold,
                    color = Color(0xFF065F46)
                )
            }
        }
    }
}

/** 运行状态内容：展示倒计时、环境摘要和暂停/结束操作。 */
@Composable
private fun RunningSessionContent(
    uiState: SessionUiState.Running,
    onPause: () -> Unit,
    onFinish: () -> Unit
) {
    // 倒计时显示 - 原型设计风格
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 倒计时数字动画：使用 animateFloatAsState 实现数字变化动画
        val animatedRemainingTime by animateFloatAsState(
            targetValue = (uiState.remainingMillis ?: 0L).toFloat(),
            animationSpec = tween(300)
        )

        Text(
            text = formatDuration(uiState.remainingMillis ?: 0L),
            style = MaterialTheme.typography.displayLarge,
            fontWeight = FontWeight.Bold
        )
        Text(
            text = "剩余时间",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }

    // 环境实时数据 - 原型设计风格
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            EnvironmentDataItem(icon = "🔊", value = "${uiState.noiseSamples.lastOrNull()?.decibel?.toInt() ?: 0} dB")
            EnvironmentDataItem(icon = "💡", value = "${uiState.lightLevel.name}")
            EnvironmentDataItem(icon = "📱", value = "${uiState.movementCount} 次")
        }
    }

    // 控制按钮 - 原型设计风格
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        // 暂停按钮动画
        var isPausePressed by remember { mutableStateOf(false) }
        val pauseScale by animateFloatAsState(
            targetValue = if (isPausePressed) 0.95f else 1f,
            animationSpec = tween(100)
        )

        OutlinedButton(
            onClick = onPause,
            modifier = Modifier
                .weight(1f)
                .scale(pauseScale),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text("暂停")
        }

        // 结束按钮动画
        var isFinishPressed by remember { mutableStateOf(false) }
        val finishScale by animateFloatAsState(
            targetValue = if (isFinishPressed) 0.95f else 1f,
            animationSpec = tween(100)
        )

        Button(
            onClick = onFinish,
            modifier = Modifier
                .weight(1f)
                .scale(finishScale),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = MintWarning
            )
        ) {
            Text("结束", color = Color.White)
        }
    }
}

/** 暂停状态内容：冻结时间并提供继续或结束操作。 */
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

/** 环境预检项目 */
@Composable
private fun EnvironmentCheckItem(
    icon: String,
    label: String,
    status: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = label,
            modifier = Modifier
                .weight(1f)
                .padding(start = 12.dp),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "✓ $status",
            style = MaterialTheme.typography.bodyMedium,
            color = MintPrimaryDark,
            fontWeight = FontWeight.Medium
        )
    }
}

/** 环境数据项 - 运行中状态显示 */
@Composable
private fun EnvironmentDataItem(
    icon: String,
    value: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = icon,
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = value,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(top = 4.dp)
        )
    }
}

/** 评分拆解项 - 带进度条 */
@Composable
private fun ScoreBreakdownItem(
    label: String,
    score: Int,
    maxScore: Int,
    color: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.width(40.dp)
        )
        Box(
            modifier = Modifier
                .weight(1f)
                .height(6.dp)
                .background(MaterialTheme.colorScheme.outlineVariant, RoundedCornerShape(3.dp))
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(score.toFloat() / maxScore)
                    .height(6.dp)
                    .background(color, RoundedCornerShape(3.dp))
            )
        }
        Text(
            text = "$score/$maxScore",
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

/** 完成状态内容：展示最终评分，并提供详情和重置入口。 */
@Composable
private fun FinishedSessionContent(
    uiState: SessionUiState.Finished,
    onReset: () -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    // 完成卡片 - 原型设计风格
    Column(
        modifier = Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // 庆祝动画：使用 animateFloatAsState 实现弹性动画
        val celebrationScale by animateFloatAsState(
            targetValue = 1f,
            animationSpec = tween(500)
        )

        Text(
            text = "🎉",
            style = MaterialTheme.typography.displayLarge,
            modifier = Modifier.scale(celebrationScale)
        )
        Text(
            text = "专注完成！",
            style = MaterialTheme.typography.headlineMedium,
            fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(top = 16.dp)
        )
        Text(
            text = "你专注了 ${formatDuration(uiState.session.durationMillis)}",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 8.dp)
        )
    }

    // 评分卡片 - 原型设计风格
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // 评分数字动画：从 0 到目标值的计数动画
            var animatedScore by remember { mutableStateOf(0) }
            val targetScore = uiState.session.score

            // 使用 LaunchedEffect 触发计数动画
            androidx.compose.runtime.LaunchedEffect(targetScore) {
                for (i in 0..targetScore) {
                    animatedScore = i
                    kotlinx.coroutines.delay(10) // 10ms 间隔，实现流畅计数
                }
            }

            Text(
                text = "$animatedScore",
                style = MaterialTheme.typography.displayLarge,
                fontWeight = FontWeight.Bold,
                color = MintPrimaryDark
            )
            Text(
                text = "环境评分",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(bottom = 16.dp)
            )

            // 评分拆解 - 原型设计风格
            ScoreBreakdownItem(label = "噪声", score = 24, maxScore = 25, color = MintPrimaryDark)
            ScoreBreakdownItem(label = "光照", score = 18, maxScore = 20, color = MintAccent)
            ScoreBreakdownItem(label = "移动", score = 18, maxScore = 20, color = MintSecondary)
            ScoreBreakdownItem(label = "时长", score = 28, maxScore = 35, color = MintPrimary)
        }
    }

    // 按钮 - 原型设计风格
    var isDetailPressed by remember { mutableStateOf(false) }
    val detailScale by animateFloatAsState(
        targetValue = if (isDetailPressed) 0.95f else 1f,
        animationSpec = tween(100)
    )

    Button(
        onClick = { onOpenDetail(uiState.session.id) },
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .scale(detailScale),
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MintPrimary
        )
    ) {
        Text(
            text = "查看详情",
            fontWeight = FontWeight.SemiBold,
            color = Color(0xFF065F46)
        )
    }
}
