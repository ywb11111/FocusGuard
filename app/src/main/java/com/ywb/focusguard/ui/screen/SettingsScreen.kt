package com.ywb.focusguard.ui.screen

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
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.state.SettingsUiState
import com.ywb.focusguard.ui.viewmodel.SettingsViewModel

/** 设置页路由层：收集设置状态并转发权限说明导航事件。 */
@Composable
fun SettingsRoute(
    onOpenPermissionGuide: () -> Unit,
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    SettingsScreen(
        uiState = uiState,
        onOpenPermissionGuide = onOpenPermissionGuide
    )
}

/**
 * 设置页纯 UI。当前设置值来自内存 Repository，开关仍为只读展示，阶段性接入 DataStore 后可编辑。
 */
@Composable
fun SettingsScreen(
    uiState: SettingsUiState,
    onOpenPermissionGuide: () -> Unit
) {
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
            fontWeight = FontWeight.SemiBold
        )

        SectionHeader(title = "专注偏好")
        SettingRow("默认时长", "${uiState.settings.defaultFocusMinutes} 分钟")
        SettingRow("噪声提醒", "${uiState.settings.noiseThresholdDb.toInt()} dB")
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
                PermissionLine("录音权限", uiState.permissionState.audioGranted)
                PermissionLine("通知权限", uiState.permissionState.notificationGranted)
                PermissionLine("使用情况访问", uiState.permissionState.usageStatsGranted)
                Button(onClick = onOpenPermissionGuide, modifier = Modifier.fillMaxWidth()) {
                    Text("查看权限说明")
                }
            }
        }

        SectionHeader(title = "关于")
        Text(
            text = "当前已接入 Room、光照与移动传感器；噪声检测、前台服务和性能报告将在后续阶段完成。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 展示一个名称和当前设置值。 */
@Composable
private fun SettingRow(
    label: String,
    value: String
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
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
    granted: Boolean
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label)
        Text(
            text = if (granted) "已开启" else "未开启",
            color = if (granted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
        )
    }
}
