package com.ywb.focusguard.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Analytics
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.TrackChanges
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.domain.analyzer.EnvironmentAnalyzer
import com.ywb.focusguard.domain.model.EnvironmentSnapshot
import com.ywb.focusguard.domain.model.EnvironmentStatus
import com.ywb.focusguard.domain.model.LightLevel
import com.ywb.focusguard.domain.model.NoiseLevel
import com.ywb.focusguard.ui.component.EnvironmentMetricRow
import com.ywb.focusguard.ui.component.EnvironmentScoreGauge
import com.ywb.focusguard.ui.component.FocusDivider
import com.ywb.focusguard.ui.component.FocusPageHeader
import com.ywb.focusguard.ui.component.FocusStat
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.state.TodayUiState
import com.ywb.focusguard.ui.viewmodel.TodayViewModel
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** 今日页路由层：处理权限结果与生命周期，纯页面只接收可渲染状态。 */
@Composable
fun TodayRoute(
    onStartFocus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSessionDetail: (Long) -> Unit,
    viewModel: TodayViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }

    LaunchedEffect(Unit) { viewModel.refreshPermissions() }

    TodayScreen(
        uiState = uiState,
        onStartFocus = onStartFocus,
        onOpenSettings = onOpenSettings,
        onOpenSessionDetail = onOpenSessionDetail,
        onRequestAudioPermission = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    )
}

/**
 * FocusGuard 的主任务入口。
 *
 * 信息层级严格遵循：环境结论 → 开始行动 → 诊断依据 → 今日统计 → 最近记录。
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
    val environment = uiState.environment
    val analyzer = EnvironmentAnalyzer()
    val score = environment?.let(analyzer::readinessScore) ?: 0
    val headline = environment?.let(analyzer::headline) ?: "正在读取环境"
    val guidance = environment?.let(analyzer::guidance) ?: "请稍候，传感器正在准备。"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        FocusPageHeader(
            title = "今日",
            subtitle = todayDateText(),
            trailing = {
                Text(
                    text = greetingText(),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            EnvironmentScoreGauge(score = score, size = 146.dp)
            Spacer(Modifier.width(18.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(text = headline, style = MaterialTheme.typography.headlineMedium)
                Spacer(Modifier.height(6.dp))
                Text(
                    text = guidance,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Button(
            onClick = onStartFocus,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("开始专注", style = MaterialTheme.typography.titleMedium)
        }

        Column {
            SectionHeader(
                title = "环境监测",
                action = {
                    Text(
                        "实时状态",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Spacer(Modifier.height(8.dp))
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(18.dp),
                colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
            ) {
                Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                    NoiseMetric(environment)
                    FocusDivider()
                    LightMetric(environment)
                    FocusDivider()
                    MotionMetric(environment)
                }
            }
        }

        if (!uiState.permissionState.audioGranted || !uiState.permissionState.notificationGranted) {
            PermissionReminder(
                audioGranted = uiState.permissionState.audioGranted,
                notificationGranted = uiState.permissionState.notificationGranted,
                onRequestAudioPermission = onRequestAudioPermission,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onOpenSettings = onOpenSettings
            )
        }

        Column {
            SectionHeader(
                title = "今天",
                action = {
                    Text(
                        "每一次专注都算数",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            )
            Spacer(Modifier.height(16.dp))
            Row(modifier = Modifier.fillMaxWidth()) {
                val summary = uiState.todaySummary
                FocusStat(
                    value = formatDurationCompact(summary?.totalFocusMillis ?: 0L),
                    label = "专注时长",
                    modifier = Modifier.weight(1f)
                )
                FocusStat(
                    value = "${summary?.sessionCount ?: 0} 次",
                    label = "专注次数",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.TrackChanges,
                    tint = MaterialTheme.colorScheme.secondary
                )
                FocusStat(
                    value = "${summary?.averageScore ?: 0}",
                    label = "平均评分",
                    modifier = Modifier.weight(1f),
                    icon = Icons.Outlined.Analytics
                )
            }
        }

        uiState.latestSession?.let { latest ->
            Column {
                SectionHeader(title = "最近一次")
                Spacer(Modifier.height(10.dp))
                Card(
                    onClick = { onOpenSessionDetail(latest.id) },
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = latest.note?.takeIf { it.isNotBlank() } ?: "专注已完成",
                                style = MaterialTheme.typography.titleMedium
                            )
                            Text(
                                text = "${formatDuration(latest.durationMillis)} · ${sessionTimeText(latest.startTime)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        Text(
                            text = latest.score.toString(),
                            style = MaterialTheme.typography.headlineSmall,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }

        uiState.errorMessage?.let {
            Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(4.dp))
    }
}

@Composable
private fun NoiseMetric(environment: EnvironmentSnapshot?) {
    val level = environment?.noise?.level
    val healthy = level == NoiseLevel.QUIET || level == NoiseLevel.NORMAL
    EnvironmentMetricRow(
        icon = Icons.Outlined.GraphicEq,
        label = "噪声",
        value = environment?.let { "${it.noise.decibel.toInt()} dB" } ?: "-- dB",
        status = when (level) {
            NoiseLevel.QUIET -> "安静"
            NoiseLevel.NORMAL -> "适中"
            NoiseLevel.NOISY -> "偏吵"
            NoiseLevel.LOUD -> "嘈杂"
            null -> "检测中"
        },
        detail = if (healthy) "处于舒适范围" else if (level == null) "等待麦克风数据" else "建议降低环境噪声",
        healthy = healthy
    )
}

@Composable
private fun LightMetric(environment: EnvironmentSnapshot?) {
    val level = environment?.light?.level
    val healthy = level == LightLevel.COMFORTABLE
    EnvironmentMetricRow(
        icon = Icons.Outlined.LightMode,
        label = "光照",
        value = environment?.let { "${it.light.lux.toInt()} lux" } ?: "-- lux",
        status = when (level) {
            LightLevel.COMFORTABLE -> "适宜"
            LightLevel.DARK -> "过暗"
            LightLevel.DIM -> "偏暗"
            LightLevel.BRIGHT -> "偏亮"
            null -> "检测中"
        },
        detail = if (healthy) "光线充足且柔和" else if (level == null) "等待光照传感器" else "调整光源会更舒适",
        healthy = healthy
    )
}

@Composable
private fun MotionMetric(environment: EnvironmentSnapshot?) {
    val available = environment != null
    val healthy = available && environment?.motion?.isMoving == false
    EnvironmentMetricRow(
        icon = Icons.Outlined.PhoneAndroid,
        label = "手机",
        value = when {
            !available -> "--"
            environment?.motion?.isMoving == true -> "移动中"
            else -> "稳定"
        },
        status = when {
            !available -> "检测中"
            healthy -> "稳定"
            else -> "移动"
        },
        detail = when {
            !available -> "等待移动传感器"
            healthy -> "未检测到明显移动"
            else -> "放稳手机更易专注"
        },
        healthy = healthy
    )
}

@Composable
private fun PermissionReminder(
    audioGranted: Boolean,
    notificationGranted: Boolean,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenSettings: () -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("完成监测设置", style = MaterialTheme.typography.titleMedium)
            Text(
                "麦克风只计算相对音量，不保存录音；通知用于后台显示计时状态。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                if (!audioGranted) TextButton(onClick = onRequestAudioPermission) { Text("允许噪声检测") }
                if (!notificationGranted) TextButton(onClick = onRequestNotificationPermission) { Text("允许通知") }
                TextButton(onClick = onOpenSettings) { Text("了解更多") }
            }
        }
    }
}

private fun greetingText(): String = when (Calendar.getInstance().get(Calendar.HOUR_OF_DAY)) {
    in 5..10 -> "早上好"
    in 11..13 -> "中午好"
    in 14..17 -> "下午好"
    else -> "晚上好"
}

private fun todayDateText(): String = SimpleDateFormat("M月d日 EEEE", Locale.CHINA).format(Date())

private fun sessionTimeText(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

/** 首页紧凑时长：避免“0 分钟 0 秒”破坏统计区的数字节奏。 */
fun formatDurationCompact(millis: Long): String {
    val minutes = millis / 60_000L
    return if (minutes >= 60) "${minutes / 60}h ${minutes % 60}m" else "$minutes 分钟"
}

/** 全局通用时长格式，专注页和详情页共同使用。 */
fun formatDuration(millis: Long): String {
    val totalSeconds = millis.coerceAtLeast(0L) / 1_000L
    val minutes = totalSeconds / 60
    val seconds = totalSeconds % 60
    return "%02d:%02d".format(minutes, seconds)
}
