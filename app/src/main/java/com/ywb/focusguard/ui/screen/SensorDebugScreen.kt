package com.ywb.focusguard.ui.screen

import android.Manifest
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.data.sensor.SleepEpochAccumulator
import com.ywb.focusguard.data.sensor.SleepSoundEventDetector
import com.ywb.focusguard.domain.analyzer.SleepAnalyzer
import com.ywb.focusguard.ui.component.FocusDivider
import com.ywb.focusguard.ui.component.FocusPageHeader
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.component.SimpleLineChart
import com.ywb.focusguard.ui.state.SensorDebugUiState
import com.ywb.focusguard.ui.viewmodel.SensorDebugViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * 传感器调试页路由：处理麦克风授权和 CSV 导出的文件选择。
 *
 * 导出用 SAF（CreateDocument）：用户自己选择保存位置，App 不需要任何存储权限。
 */
@Composable
fun SensorDebugRoute(
    onBack: () -> Unit,
    viewModel: SensorDebugViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val audioLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.onPermissionChanged() }
    val exportLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.CreateDocument("text/csv")
    ) { uri -> uri?.let(viewModel::exportLatestSleep) }

    SensorDebugScreen(
        uiState = uiState,
        onBack = onBack,
        onRequestAudio = { audioLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onExport = {
            val date = SimpleDateFormat("yyyyMMdd-HHmm", Locale.getDefault()).format(Date())
            exportLauncher.launch("focusguard-sleep-$date.csv")
        },
        onDismissExportMessage = viewModel::dismissExportMessage
    )
}

/** 调试页：体动 → 声音 → 光照 → 当前算法参数 → 导出。 */
@Composable
fun SensorDebugScreen(
    uiState: SensorDebugUiState,
    onBack: () -> Unit,
    onRequestAudio: () -> Unit,
    onExport: () -> Unit,
    onDismissExportMessage: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        FocusPageHeader(
            title = "传感器调试",
            subtitle = "实时信号与阈值，用于真机校准",
            leading = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            }
        )

        if (uiState.sleepMonitoring) {
            DebugCard {
                Text(
                    "睡眠监测正在后台运行。为避免与它抢占麦克风和传感器，调试采集已暂停，结束睡眠监测后自动恢复。",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        } else {
            Column {
                SectionHeader(title = "体动 |Δa|（m/s²）")
                Spacer(Modifier.height(10.dp))
                DebugCard {
                    LiveChart(uiState.motionValues)
                    DebugValueRow("当前阈值", "%.3f".format(uiState.motionThreshold))
                    FocusDivider()
                    DebugValueRow("最近 60 秒体动", "${uiState.movementSecondsLastMinute} 秒")
                    Text(
                        "校准方法：手机放在枕边床垫上，静躺时数值应低于阈值；翻身时应明显超过阈值。",
                        modifier = Modifier.padding(top = 8.dp),
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Column {
                SectionHeader(title = "声音（相对 dB）")
                Spacer(Modifier.height(10.dp))
                DebugCard {
                    if (!uiState.audioGranted) {
                        Text(
                            "需要麦克风权限才能显示声音信号，只计算音量，不保存录音。",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        TextButton(onClick = onRequestAudio) { Text("允许麦克风") }
                    } else {
                        LiveChart(uiState.soundDbValues)
                        DebugValueRow("底噪 / 触发线", "${uiState.baselineDb.toInt()} / ${uiState.triggerDb.toInt()} dB")
                        FocusDivider()
                        DebugValueRow("主频（过零率估计）", "${uiState.dominantHz.toInt()} Hz")
                        FocusDivider()
                        Text(
                            "最近的声音事件",
                            modifier = Modifier.padding(top = 10.dp, bottom = 4.dp),
                            style = MaterialTheme.typography.titleSmall
                        )
                        if (uiState.recentSoundEvents.isEmpty()) {
                            Text(
                                "暂无。拍手或说话试试，持续 0.2 秒以上且高于触发线才算一次事件。",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        uiState.recentSoundEvents.forEach { event ->
                            Row(
                                modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(event.timeText, style = MaterialTheme.typography.bodySmall)
                                Text(
                                    if (event.isSnoreLike) "${event.detailText} · 疑似鼾声" else event.detailText,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = if (event.isSnoreLike) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.onSurfaceVariant
                                    }
                                )
                            }
                        }
                    }
                }
            }

            Column {
                SectionHeader(title = "光照")
                Spacer(Modifier.height(10.dp))
                DebugCard { DebugValueRow("当前读数", "${uiState.lux.toInt()} lux") }
            }
        }

        Column {
            SectionHeader(title = "当前算法参数")
            Spacer(Modifier.height(10.dp))
            DebugCard {
                DebugValueRow("体动阈值下限", "${SleepEpochAccumulator.DEFAULT_MIN_MOTION_THRESHOLD} m/s²")
                FocusDivider()
                DebugValueRow("声音事件触发", "底噪 + ${SleepSoundEventDetector.DEFAULT_TRIGGER_ABOVE_BASELINE_DB.toInt()} dB")
                FocusDivider()
                DebugValueRow("鼾声主频上限", "${SleepSoundEventDetector.DEFAULT_SNORE_MAX_HZ.toInt()} Hz")
                FocusDivider()
                DebugValueRow("判定清醒", "加权活动 ≥ ${SleepAnalyzer.WAKE_ACTIVITY_THRESHOLD.toInt()} 体动秒")
                FocusDivider()
                DebugValueRow("判定入睡", "连续 ${SleepAnalyzer.SLEEP_ONSET_RUN} 分钟睡眠")
            }
        }

        Column {
            SectionHeader(title = "导出数据")
            Spacer(Modifier.height(10.dp))
            DebugCard {
                Text(
                    "把最近一晚每分钟的体动、噪声、事件数和判定结果导出为 CSV，在电脑上对照自己的真实感受调整阈值。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Spacer(Modifier.height(10.dp))
                OutlinedButton(onClick = onExport, modifier = Modifier.fillMaxWidth()) {
                    Text("导出最近一晚（CSV）")
                }
                uiState.exportMessage?.let { message ->
                    Row(
                        modifier = Modifier.fillMaxWidth().padding(top = 8.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            message,
                            modifier = Modifier.weight(1f),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.primary
                        )
                        TextButton(onClick = onDismissExportMessage) { Text("知道了") }
                    }
                }
            }
        }
        Spacer(Modifier.height(4.dp))
    }
}

/** 实时曲线：数据不足两个点时显示等待提示。 */
@Composable
private fun LiveChart(values: List<Float>) {
    if (values.size >= 2) {
        // 实时曲线每 250ms 刷新一次，不需要入场描线动画
        SimpleLineChart(values = values, animateIn = false)
    } else {
        Text(
            "等待传感器数据…",
            modifier = Modifier.padding(vertical = 16.dp),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 一行"名称 - 数值"。 */
@Composable
private fun DebugValueRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(label, style = MaterialTheme.typography.bodyMedium)
        Text(value, style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.primary)
    }
}

/** 调试页统一卡片容器。 */
@Composable
private fun DebugCard(content: @Composable () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(18.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(modifier = Modifier.padding(16.dp)) { content() }
    }
}
