package com.ywb.focusguard.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.state.SettingsUiState
import com.ywb.focusguard.ui.viewmodel.SettingsViewModel

/** 主题模式枚举 */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

/** 设置页路由层：收集设置状态并转发权限说明导航事件。 */
@Composable
fun SettingsRoute(
    onOpenPermissionGuide: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()

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

    // 页面进入时刷新权限状态
    LaunchedEffect(Unit) {
        viewModel.refreshPermissions()
    }

    SettingsScreen(
        uiState = uiState,
        onOpenPermissionGuide = onOpenPermissionGuide,
        onRequestAudioPermission = {
            audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO)
        },
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onUpdateDefaultFocusMinutes = viewModel::updateDefaultFocusMinutes,
        onUpdateNoiseThreshold = viewModel::updateNoiseThreshold
    )
}

/**
 * 设置页纯 UI。
 *
 * @param onRequestAudioPermission 请求录音权限的回调。
 * @param onRequestNotificationPermission 请求通知权限的回调。
 * @param onUpdateDefaultFocusMinutes 更新默认专注时长的回调，接收新的分钟数。
 * @param onUpdateNoiseThreshold 更新噪声阈值的回调，接收新的 dB 值。
 */
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onOpenPermissionGuide: () -> Unit,
    onRequestAudioPermission: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onUpdateDefaultFocusMinutes: (Int) -> Unit = {},
    onUpdateNoiseThreshold: (Float) -> Unit = {}
) {
    // 主题模式状态
    var themeMode by remember { mutableStateOf(ThemeMode.SYSTEM) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Text(
            text = "设置",
            style = MaterialTheme.typography.headlineSmall,
            fontWeight = FontWeight.Bold
        )

        // 主题设置 - 深色模式支持
        SectionHeader(title = "主题设置")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "外观模式",
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    ThemeMode.entries.forEach { mode ->
                        val isSelected = themeMode == mode
                        val label = when (mode) {
                            ThemeMode.SYSTEM -> "跟随系统"
                            ThemeMode.LIGHT -> "浅色"
                            ThemeMode.DARK -> "深色"
                        }

                        Card(
                            onClick = { themeMode = mode },
                            colors = CardDefaults.cardColors(
                                containerColor = if (isSelected) MaterialTheme.colorScheme.primary
                                else MaterialTheme.colorScheme.surfaceVariant
                            ),
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                text = label,
                                modifier = Modifier.padding(12.dp),
                                style = MaterialTheme.typography.bodyMedium,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                color = if (isSelected) MaterialTheme.colorScheme.onPrimary
                                else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                }
            }
        }

        SectionHeader(title = "专注偏好")
        SettingRow("默认时长", "${uiState.settings.defaultFocusMinutes} 分钟") {
            // 预设值：15, 25, 30, 45, 60 分钟，点击循环切换
            val presets = listOf(15, 25, 30, 45, 60)
            val currentIndex = presets.indexOf(uiState.settings.defaultFocusMinutes)
            // 如果当前值不在预设列表中，从 25 分钟开始；否则切换到下一个
            val nextIndex = if (currentIndex == -1) 1 else (currentIndex + 1) % presets.size
            onUpdateDefaultFocusMinutes(presets[nextIndex])
        }
        SettingRow("噪声提醒", "${uiState.settings.noiseThresholdDb.toInt()} dB") {
            // 预设值：50, 55, 60, 65, 70 dB，点击循环切换
            val presets = listOf(50f, 55f, 60f, 65f, 70f)
            val currentIndex = presets.indexOf(uiState.settings.noiseThresholdDb)
            // 如果当前值不在预设列表中，从 60 dB 开始；否则切换到下一个
            val nextIndex = if (currentIndex == -1) 2 else (currentIndex + 1) % presets.size
            onUpdateNoiseThreshold(presets[nextIndex])
        }
        SettingRow(
            "舒适光照",
            "${uiState.settings.comfortableLightMinLux.toInt()}-${uiState.settings.comfortableLightMaxLux.toInt()} lux"
        )

        SectionHeader(title = "后台能力")
        ToggleRow("后台监测", uiState.settings.backgroundMonitoringEnabled)
        ToggleRow("每日总结", uiState.settings.dailyReportEnabled)

        SectionHeader(title = "权限")
        Card(
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceContainer),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(
                modifier = Modifier.padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                PermissionLine(
                    label = "录音权限",
                    granted = uiState.permissionState.audioGranted,
                    onRequest = if (!uiState.permissionState.audioGranted) onRequestAudioPermission else null
                )
                PermissionLine(
                    label = "通知权限",
                    granted = uiState.permissionState.notificationGranted,
                    onRequest = if (!uiState.permissionState.notificationGranted) onRequestNotificationPermission else null
                )
                PermissionLine("使用情况访问", uiState.permissionState.usageStatsGranted)
                Button(onClick = onOpenPermissionGuide, modifier = Modifier.fillMaxWidth()) {
                    Text("查看权限说明")
                }
            }
        }

        SectionHeader(title = "关于")
        Text(
            text = "噪声检测、前台服务、报告聚合和设置持久化已完成。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 展示一个名称和当前设置值，可选点击切换预设值。 */
@Composable
private fun SettingRow(
    label: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            value,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 展示一个布尔设置；当前不接收点击事件，因此只反映状态。 */
@Composable
private fun ToggleRow(
    label: String,
    checked: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Switch(checked = checked, onCheckedChange = null)
    }
}

/** 展示单项权限名称和授权状态。 */
@Composable
private fun PermissionLine(
    label: String,
    granted: Boolean,
    onRequest: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(label)
        if (onRequest != null && !granted) {
            Button(onClick = onRequest) {
                Text("去授权")
            }
        } else {
            Text(
                text = if (granted) "已开启" else "未开启",
                color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
            )
        }
    }
}
