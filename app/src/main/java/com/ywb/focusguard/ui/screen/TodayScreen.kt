package com.ywb.focusguard.ui.screen

import android.Manifest
import android.content.pm.PackageManager
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.domain.analyzer.EnvironmentAnalyzer
import com.ywb.focusguard.ui.component.MetricCard
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.state.TodayUiState
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
        // 用户授权后立即刷新状态
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
 */
@Composable
fun TodayScreen(
    uiState: TodayUiState,
    onStartFocus: () -> Unit,
    onOpenSettings: () -> Unit,
    onOpenSessionDetail: (Long) -> Unit,
    onRequestAudioPermission: () -> Unit = {}
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
            subtitle = "光照和移动来自手机传感器，噪声检测将在下一阶段接入。"
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
                value = if (environment?.motion?.isMoving == true) "活动" else "稳定",
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

        Button(
            onClick = onStartFocus,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(Icons.Outlined.PlayArrow, contentDescription = null)
            Text(text = "开始专注", modifier = Modifier.padding(start = 8.dp))
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

/** 把毫秒时长格式化为页面使用的小时/分钟文本。 */
fun formatDuration(durationMillis: Long): String {
    val totalMinutes = durationMillis / 60_000L
    val hours = totalMinutes / 60
    val minutes = totalMinutes % 60
    return if (hours > 0) "${hours}h ${minutes}m" else "${minutes}m"
}
