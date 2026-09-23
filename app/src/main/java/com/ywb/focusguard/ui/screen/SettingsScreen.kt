package com.ywb.focusguard.ui.screen

import android.Manifest
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material.icons.outlined.ChevronRight
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.GraphicEq
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PrivacyTip
import androidx.compose.material.icons.outlined.QueryStats
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.ui.component.FocusPageHeader
import com.ywb.focusguard.ui.component.PositiveStatus
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.state.SettingsUiState
import com.ywb.focusguard.ui.theme.FocusTealSoft
import com.ywb.focusguard.ui.viewmodel.SettingsViewModel

/** 设置页路由层：请求权限并把所有可编辑项转发给 ViewModel。 */
@Composable
fun SettingsRoute(
    onOpenPermissionGuide: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshPermissions() }

    LaunchedEffect(Unit) { viewModel.refreshPermissions() }

    SettingsScreen(
        uiState = uiState,
        onOpenPermissionGuide = onOpenPermissionGuide,
        onRequestAudioPermission = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onUpdateDefaultFocusMinutes = viewModel::updateDefaultFocusMinutes,
        onUpdateNoiseThreshold = viewModel::updateNoiseThreshold,
        onUpdateBackgroundMonitoring = viewModel::updateBackgroundMonitoringEnabled,
        onUpdateDailyReport = viewModel::updateDailyReportEnabled
    )
}

/** 设置项按“专注偏好、后台能力、权限与隐私”分组，所有可见开关均真实可操作。 */
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onOpenPermissionGuide: () -> Unit,
    onRequestAudioPermission: () -> Unit = {},
    onRequestNotificationPermission: () -> Unit = {},
    onUpdateDefaultFocusMinutes: (Int) -> Unit = {},
    onUpdateNoiseThreshold: (Float) -> Unit = {},
    onUpdateBackgroundMonitoring: (Boolean) -> Unit = {},
    onUpdateDailyReport: (Boolean) -> Unit = {}
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        FocusPageHeader(title = "设置", subtitle = "管理专注偏好、监测能力和隐私")

        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.primaryContainer)
        ) {
            Row(modifier = Modifier.fillMaxWidth().padding(18.dp), verticalAlignment = Alignment.Top) {
                Surface(shape = CircleShape, color = FocusTealSoft, modifier = Modifier.size(46.dp)) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(Icons.Outlined.PrivacyTip, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                    }
                }
                Spacer(Modifier.width(12.dp))
                Column {
                    Text("数据只保存在本机", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "麦克风仅用于计算相对音量，不保存、上传或回放原始音频。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }

        SettingsSection("专注偏好") {
            ValueSettingRow(
                icon = Icons.Outlined.AccessTime,
                label = "默认时长",
                supporting = "新会话默认选中的专注时长",
                value = "${uiState.settings.defaultFocusMinutes} 分钟",
                onClick = {
                    val presets = listOf(15, 25, 30, 45, 60)
                    val current = presets.indexOf(uiState.settings.defaultFocusMinutes)
                    onUpdateDefaultFocusMinutes(presets[if (current == -1) 1 else (current + 1) % presets.size])
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ValueSettingRow(
                icon = Icons.Outlined.GraphicEq,
                label = "噪声提醒",
                supporting = "超过阈值一段时间后再提醒",
                value = "${uiState.settings.noiseThresholdDb.toInt()} dB",
                onClick = {
                    val presets = listOf(50f, 55f, 60f, 65f, 70f)
                    val current = presets.indexOf(uiState.settings.noiseThresholdDb)
                    onUpdateNoiseThreshold(presets[if (current == -1) 2 else (current + 1) % presets.size])
                }
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ValueSettingRow(
                icon = Icons.Outlined.LightMode,
                label = "舒适光照",
                supporting = "用于环境预检和报告分析",
                value = "${uiState.settings.comfortableLightMinLux.toInt()}–${uiState.settings.comfortableLightMaxLux.toInt()} lux"
            )
        }

        SettingsSection("后台与提醒") {
            ToggleSettingRow(
                icon = Icons.Outlined.Shield,
                label = "后台监测",
                supporting = "锁屏或切换应用后继续记录",
                checked = uiState.settings.backgroundMonitoringEnabled,
                onCheckedChange = onUpdateBackgroundMonitoring
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ToggleSettingRow(
                icon = Icons.Outlined.QueryStats,
                label = "每日总结",
                supporting = "每天回顾专注时长和环境表现",
                checked = uiState.settings.dailyReportEnabled,
                onCheckedChange = onUpdateDailyReport
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ValueSettingRow(
                icon = Icons.Outlined.DarkMode,
                label = "外观模式",
                supporting = "自动适应系统深色模式",
                value = "跟随系统"
            )
        }

        SettingsSection("权限与隐私") {
            PermissionSettingRow(
                icon = Icons.Outlined.Mic,
                label = "录音权限",
                supporting = "用于实时计算相对噪声",
                granted = uiState.permissionState.audioGranted,
                onRequest = onRequestAudioPermission
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            PermissionSettingRow(
                icon = Icons.Outlined.Notifications,
                label = "通知权限",
                supporting = "用于后台计时和控制",
                granted = uiState.permissionState.notificationGranted,
                onRequest = onRequestNotificationPermission
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
            ValueSettingRow(
                icon = Icons.Outlined.PrivacyTip,
                label = "权限用途说明",
                supporting = "查看每项权限的使用方式",
                value = "",
                onClick = onOpenPermissionGuide
            )
        }

        Text(
            "FocusGuard 1.0 · 数据默认仅存储在当前设备",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.align(Alignment.CenterHorizontally)
        )
        Spacer(Modifier.size(4.dp))
    }
}

@Composable
private fun SettingsSection(title: String, content: @Composable ColumnScope.() -> Unit) {
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        SectionHeader(title = title)
        Card(
            shape = RoundedCornerShape(18.dp),
            colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
        ) {
            Column(modifier = Modifier.padding(horizontal = 16.dp), content = content)
        }
    }
}

@Composable
private fun ValueSettingRow(
    icon: ImageVector,
    label: String,
    supporting: String,
    value: String,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (value.isNotEmpty()) {
            Text(value, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(Modifier.width(4.dp))
        }
        if (onClick != null) Icon(Icons.Outlined.ChevronRight, contentDescription = "打开$label", tint = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
private fun ToggleSettingRow(
    icon: ImageVector,
    label: String,
    supporting: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PermissionSettingRow(
    icon: ImageVector,
    label: String,
    supporting: String,
    granted: Boolean,
    onRequest: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .then(if (!granted) Modifier.clickable(onClick = onRequest) else Modifier)
            .padding(vertical = 15.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(label, style = MaterialTheme.typography.titleSmall)
            Text(supporting, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        if (granted) PositiveStatus("已开启") else Text("去授权", style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
    }
}
