package com.ywb.focusguard.ui.screen

import android.Manifest
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.domain.analyzer.EnvironmentAnalyzer
import com.ywb.focusguard.ui.component.MetricCard
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.state.TodayUiState
import com.ywb.focusguard.ui.theme.MintAccent
import com.ywb.focusguard.ui.theme.MintPrimary
import com.ywb.focusguard.ui.theme.MintPrimaryDark
import com.ywb.focusguard.ui.theme.MintSecondary
import com.ywb.focusguard.ui.theme.MintWarning
import com.ywb.focusguard.ui.viewmodel.TodayViewModel

/**
 * 今日页路由层：从 Hilt 获取 ViewModel、按生命周期收集状态，并把导航事件传给纯 UI。
 */
@Composable
fun TodayRoute(
    onStartFocus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSessionDetail: (Long) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // 录音权限请求启动器
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.refreshPermissions()
    }

    // 通知权限请求启动器（Android 13+）
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        viewModel.refreshPermissions()
    }

    // 页面进入时检查权限状态
    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    TodayScreen(
        uiState = uiState,
        onStartFocus = onStartFocus,
        onOpenSettings = onOpenSettings,
        onOpenSessionDetail = onOpenSessionDetail,
        onRequestAudioPermission = {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )
}

/**
 * 今日页纯 UI，只依赖 [TodayUiState] 和事件回调，因此可独立预览和测试。
 *
 * @param uiState 当前可渲染状态。
 * @param onStartFocus 进入专注页。
 * @param onOpenSettings 打开设置页。
 * @param onOpenSessionDetail 按真实会话 id 打开详情页。
 * @param onRequestAudioPermission 请求录音权限。
 * @param onRequestNotificationPermission 请求通知权限（Android 13+）。
 */
@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onStartFocus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSessionDetail: (Long) -> Unit,
    onRequestAudioPermission: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(18.dp)
    ) {
        // 顶部问候语 - 原型设计风格
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "早上好",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = "今天准备好专注了吗？",
                style = MaterialTheme.typography.headlineSmall,
                fontWeight = FontWeight.Bold
            )
        }

        // 环境状态卡片 - 原型设计风格
        val environment = uiState.environment
        Row(
            horizontalArrangement = Arrangement.spacedBy(10.dp),
            modifier = Modifier.fillMaxWidth()
        ) {
            // 噪声卡片
            EnvCard(
                icon = "🔊",
                label = "噪声",
                value = "${environment?.noise?.decibel?.toInt() ?: "--"} dB",
                status = getNoiseStatus(environment?.noise?.decibel?.toInt()),
                modifier = Modifier.weight(1f)
            )

            // 光照卡片
            EnvCard(
                icon = "💡",
                label = "光照",
                value = "${environment?.light?.lux?.toInt() ?: "--"} lux",
                status = getLightStatus(environment?.light?.lux?.toInt()),
                modifier = Modifier.weight(1f)
            )

            // 移动卡片
            EnvCard(
                icon = "📱",
                label = "移动",
                value = if (environment?.motion?.isMoving == true) "活动" else "稳定",
                status = if (environment?.motion?.isMoving == true) "warning" else "good",
                modifier = Modifier.weight(1f)
            )
        }

        // 开始专注按钮 - 原型设计风格（渐变背景 + 点击动画）
        var isButtonPressed by remember { mutableStateOf(false) }
        val buttonScale by animateFloatAsState(
            targetValue = if (isButtonPressed) 0.95f else 1f,
            animationSpec = tween(100)
        )

        Button(
            onClick = onStartFocus,
            modifier = Modifier
                .fillMaxWidth()
                .height(80.dp)
                .scale(buttonScale),
            shape = RoundedCornerShape(16.dp),
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
                        shape = RoundedCornerShape(16.dp)
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
                        modifier = Modifier.size(32.dp),
                        tint = Color(0xFF065F46)
                    )
                    Spacer(modifier = Modifier.width(12.dp))
                    Column {
                        Text(
                            text = "开始专注",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF065F46)
                        )
                        Text(
                            text = "环境适合专注，现在开始吧",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF047857)
                        )
                    }
                }
            }
        }

        // 今日概览 - 原型设计风格
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "今日概览",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                val summary = uiState.todaySummary
                MetricCard(
                    label = "专注时长",
                    value = formatDuration(summary?.totalFocusMillis ?: 0L),
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "平均评分",
                    value = "${summary?.averageScore ?: 0}",
                    modifier = Modifier.weight(1f)
                )
                MetricCard(
                    label = "专注次数",
                    value = "${summary?.distractionCount ?: 0}",
                    modifier = Modifier.weight(1f)
                )
            }
        }

        // 最近记录 - 原型设计风格
        Column(modifier = Modifier.fillMaxWidth()) {
            Text(
                text = "最近记录",
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            val latest = uiState.latestSession
            Card(
                onClick = { latest?.let { onOpenSessionDetail(it.id) } },
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(16.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column {
                        Text(
                            text = latest?.note ?: "还没有专注记录",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = latest?.let { "${formatDuration(it.durationMillis)} · 环境良好" }
                                ?: "完成一次专注后，这里会显示最近结果。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    if (latest != null) {
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .background(MintPrimary)
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        ) {
                            Text(
                                text = "评分 ${latest.score}",
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = Color(0xFF065F46)
                            )
                        }
                    }
                }
            }
        }

        // 录音权限缺失提示
        if (!uiState.permissionState.audioGranted) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "需要录音权限",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "噪声检测需要录音权限来分析环境声音，不会保存原始音频。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = onRequestAudioPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("授权录音权限")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }

        // 通知权限缺失提示（Android 13+ 需要）
        if (!uiState.permissionState.notificationGranted) {
            Card(
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.errorContainer,
                    contentColor = MaterialTheme.colorScheme.onErrorContainer
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier.padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = "需要通知权限",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        text = "专注期间需要通知权限才能在前台服务中显示剩余时间和控制按钮。",
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Button(
                        onClick = onRequestNotificationPermission,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text("授权通知权限")
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
        }
    }
}

/** 首页顶部的整体环境结论卡片。 */
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

/**
 * 环境指标卡片 - 原型设计风格
 * 显示图标、标签、数值和状态指示
 */
@Composable
private fun EnvCard(
    icon: String,
    label: String,
    value: String,
    status: String,
    modifier: Modifier = Modifier
) {
    val statusColor = when (status) {
        "good" -> MintPrimaryDark
        "normal" -> MintAccent
        "warning" -> MintWarning
        else -> MaterialTheme.colorScheme.onSurfaceVariant
    }

    val statusText = when (status) {
        "good" -> "✓ 适中"
        "normal" -> "○ 一般"
        "warning" -> "⚠ 偏高"
        else -> ""
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier.padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = icon,
                fontSize = 20.sp
            )
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                text = value,
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                text = statusText,
                style = MaterialTheme.typography.labelSmall,
                color = statusColor,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

/** 根据噪声分贝值返回状态：good/normal/warning */
private fun getNoiseStatus(decibel: Int?): String {
    return when {
        decibel == null -> "normal"
        decibel < 50 -> "good"
        decibel < 65 -> "normal"
        else -> "warning"
    }
}

/** 根据光照值返回状态：good/normal/warning */
private fun getLightStatus(lux: Int?): String {
    return when {
        lux == null -> "normal"
        lux in 100..500 -> "good"
        lux in 50..99 || lux in 501..700 -> "normal"
        else -> "warning"
    }
}

/** 把毫秒时长格式化为页面使用的小时/分钟文本。 */
fun formatDuration(durationMillis: Long): String {
    val totalMinutes = durationMillis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
