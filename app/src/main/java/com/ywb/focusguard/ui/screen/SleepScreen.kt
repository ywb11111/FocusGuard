package com.ywb.focusguard.ui.screen

import android.Manifest
import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
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
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.outlined.BatteryChargingFull
import androidx.compose.material.icons.outlined.Bed
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Mic
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.ywb.focusguard.domain.model.SleepPlacement
import com.ywb.focusguard.domain.model.SleepSession
import com.ywb.focusguard.ui.component.FocusDivider
import com.ywb.focusguard.ui.component.FocusPageHeader
import com.ywb.focusguard.ui.component.InteractiveLineChart
import com.ywb.focusguard.ui.component.PositiveStatus
import com.ywb.focusguard.ui.component.SectionHeader
import com.ywb.focusguard.ui.state.SleepChecklist
import com.ywb.focusguard.ui.state.SleepUiState
import com.ywb.focusguard.ui.theme.FocusAmberSoft
import com.ywb.focusguard.ui.theme.FocusDarkBackground
import com.ywb.focusguard.ui.theme.FocusDarkInk
import com.ywb.focusguard.ui.theme.FocusDarkInkMuted
import com.ywb.focusguard.ui.theme.FocusDarkOutline
import com.ywb.focusguard.ui.viewmodel.SleepViewModel
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 睡眠结果的统一免责声明：手机只能估算，不能冒充医学监测。 */
internal const val SLEEP_DISCLAIMER =
    "睡眠阶段根据手机检测到的体动和声音估算，无法区分医学意义上的深睡或 REM，不能替代医学睡眠监测。"

/**
 * 睡眠页路由层：处理权限请求、系统设置跳转、页面恢复刷新和一次性导航事件，
 * 纯页面 [SleepScreen] 只接收状态和回调。
 */
@Composable
fun SleepRoute(
    onBack: () -> Unit,
    onOpenDetail: (Long) -> Unit,
    viewModel: SleepViewModel = hiltViewModel()
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val audioPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshChecklist() }
    val notificationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { viewModel.refreshChecklist() }

    // 从系统电池优化设置返回时页面会 onResume，需要重新检查一次
    LifecycleResumeEffect(Unit) {
        viewModel.refreshChecklist()
        onPauseOrDispose { }
    }

    // 记录分析完成后跳转报告页；事件来自 Channel，只会被消费一次
    LaunchedEffect(Unit) {
        viewModel.finishedEvents.collect { sleepId -> onOpenDetail(sleepId) }
    }

    SleepScreen(
        uiState = uiState,
        onBack = onBack,
        onSelectPlacement = viewModel::selectPlacement,
        onRequestAudioPermission = { audioPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO) },
        onRequestNotificationPermission = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        },
        onOpenBatterySettings = { openBatteryOptimizationSettings(context) },
        onStart = viewModel::startSleep,
        onStop = viewModel::stopSleep,
        onFinishInterrupted = viewModel::finishInterrupted,
        onOpenDetail = onOpenDetail
    )
}

/** 睡眠页：按状态机切换 准备 / 监测中 / 中断 / 分析中 四种内容。 */
@Composable
fun SleepScreen(
    uiState: SleepUiState,
    onBack: () -> Unit,
    onSelectPlacement: (SleepPlacement) -> Unit,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onStart: () -> Unit,
    onStop: () -> Unit,
    onFinishInterrupted: (Long) -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    AnimatedContent(
        targetState = uiState,
        // 与专注页相同的做法：只按阶段类型做转场，实时数据变化只触发普通重组，避免每分钟闪一次
        contentKey = { state -> state::class },
        transitionSpec = { fadeIn() togetherWith fadeOut() },
        label = "sleep-state"
    ) { state ->
        when (state) {
            SleepUiState.Loading -> CenteredProgress(text = null)
            is SleepUiState.Ready -> SleepReadyContent(
                state = state,
                onBack = onBack,
                onSelectPlacement = onSelectPlacement,
                onRequestAudioPermission = onRequestAudioPermission,
                onRequestNotificationPermission = onRequestNotificationPermission,
                onOpenBatterySettings = onOpenBatterySettings,
                onStart = onStart,
                onOpenDetail = onOpenDetail
            )
            is SleepUiState.Recording -> SleepRecordingContent(state, onStop)
            is SleepUiState.Interrupted -> SleepInterruptedContent(state, onFinishInterrupted)
            SleepUiState.Finishing -> CenteredProgress(text = "正在分析睡眠数据…")
        }
    }
}

// ==================== 准备 ====================

/** 准备页：摆放位置 → 睡前检查 → 开始按钮 → 最近记录。 */
@Composable
private fun SleepReadyContent(
    state: SleepUiState.Ready,
    onBack: () -> Unit,
    onSelectPlacement: (SleepPlacement) -> Unit,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit,
    onStart: () -> Unit,
    onOpenDetail: (Long) -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 20.dp, vertical = 18.dp),
        verticalArrangement = Arrangement.spacedBy(22.dp)
    ) {
        FocusPageHeader(
            title = "睡眠监测",
            subtitle = "睡前开启，起床后查看睡眠报告",
            leading = {
                IconButton(onClick = onBack) {
                    Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                }
            }
        )

        Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
            SectionHeader(title = "手机放在哪里")
            SleepPlacement.entries.forEach { placement ->
                PlacementOption(
                    placement = placement,
                    selected = placement == state.placement,
                    onClick = { onSelectPlacement(placement) }
                )
            }
        }

        SleepChecklistSection(
            checklist = state.checklist,
            onRequestAudioPermission = onRequestAudioPermission,
            onRequestNotificationPermission = onRequestNotificationPermission,
            onOpenBatterySettings = onOpenBatterySettings
        )

        Button(
            onClick = onStart,
            modifier = Modifier
                .fillMaxWidth()
                .height(58.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Icon(Icons.Outlined.Bedtime, contentDescription = null)
            Spacer(Modifier.width(8.dp))
            Text("开始睡眠监测", style = MaterialTheme.typography.titleMedium)
        }

        if (state.recentSessions.isNotEmpty()) {
            Column {
                SectionHeader(
                    title = "最近 ${state.recentSessions.size} 晚",
                    action = {
                        Text(
                            "睡眠评分",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                )
                Spacer(Modifier.height(8.dp))
                InteractiveLineChart(values = state.trendScores)
                Spacer(Modifier.height(10.dp))
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(18.dp),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
                ) {
                    Column(modifier = Modifier.padding(horizontal = 16.dp)) {
                        state.recentSessions.forEachIndexed { index, session ->
                            if (index > 0) FocusDivider()
                            SleepHistoryRow(session = session, onClick = { onOpenDetail(session.id) })
                        }
                    }
                }
            }
        }

        Text(
            text = SLEEP_DISCLAIMER,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 摆放位置单选项，说明文字直接写出两种方式的准确度差异。 */
@Composable
private fun PlacementOption(
    placement: SleepPlacement,
    selected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (selected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = if (placement == SleepPlacement.BED) Icons.Outlined.Bed else Icons.Outlined.PhoneAndroid,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Spacer(Modifier.width(12.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(placement.label, style = MaterialTheme.typography.titleMedium)
                Text(
                    placement.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            RadioButton(selected = selected, onClick = onClick)
        }
    }
}

/** 睡前检查：每一项都说明"为什么需要"，已完成的显示绿色标签。 */
@Composable
private fun SleepChecklistSection(
    checklist: SleepChecklist,
    onRequestAudioPermission: () -> Unit,
    onRequestNotificationPermission: () -> Unit,
    onOpenBatterySettings: () -> Unit
) {
    Column {
        SectionHeader(
            title = "睡前检查",
            action = {
                Text(
                    "可选，但会影响完整度",
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
                ChecklistRow(
                    icon = Icons.Outlined.Mic,
                    title = "麦克风",
                    detail = "识别鼾声和噪声，只计算音量，不保存录音",
                    done = checklist.audioGranted,
                    actionLabel = "允许",
                    onAction = onRequestAudioPermission
                )
                FocusDivider()
                ChecklistRow(
                    icon = Icons.Outlined.Notifications,
                    title = "通知",
                    detail = "显示记录状态和结束按钮",
                    done = checklist.notificationGranted,
                    actionLabel = "允许",
                    onAction = onRequestNotificationPermission
                )
                FocusDivider()
                ChecklistRow(
                    icon = Icons.Outlined.BatteryChargingFull,
                    title = "后台保活",
                    detail = "关闭电池优化，避免半夜被系统清理",
                    done = checklist.batteryOptimizationIgnored,
                    actionLabel = "去设置",
                    onAction = onOpenBatterySettings
                )
            }
        }
        Spacer(Modifier.height(8.dp))
        Text(
            "整晚监测耗电较多，建议连接充电器；部分手机还需要在系统设置中允许自启动。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/** 单条睡前检查。 */
@Composable
private fun ChecklistRow(
    icon: ImageVector,
    title: String,
    detail: String,
    done: Boolean,
    actionLabel: String,
    onAction: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleSmall)
            Text(
                detail,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        if (done) {
            PositiveStatus("已完成")
        } else {
            TextButton(onClick = onAction) { Text(actionLabel) }
        }
    }
}

/** 最近记录中的一晚。 */
@Composable
private fun SleepHistoryRow(session: SleepSession, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(sleepNightLabel(session.startTime), style = MaterialTheme.typography.titleMedium)
            Text(
                text = "${clockText(session.sleepOnsetTime ?: session.startTime)} – " +
                    "${clockText(session.finalWakeTime ?: session.endTime ?: session.startTime)} · " +
                    "睡眠 ${formatHoursMinutes(session.totalSleepMillis)}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            text = session.score.toString(),
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            fontWeight = FontWeight.Bold
        )
    }
}

// ==================== 监测中 ====================

/**
 * 监测中：深色低亮度界面，避免睡前被强光刺激。
 * 页面只是"观察窗口"，关掉它不会影响后台记录。
 */
@Composable
private fun SleepRecordingContent(
    state: SleepUiState.Recording,
    onStop: () -> Unit
) {
    val now by rememberMinuteClock()
    val elapsedMinutes = ((now - state.session.startTime) / 60_000L).coerceAtLeast(0L)
    val live = state.live

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(FocusDarkBackground)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(Modifier.weight(0.6f))
            Icon(
                Icons.Outlined.NightsStay,
                contentDescription = null,
                tint = FocusDarkInkMuted,
                modifier = Modifier.size(40.dp)
            )
            Spacer(Modifier.height(16.dp))
            Text(clockText(now), style = MaterialTheme.typography.displayLarge, color = FocusDarkInk)
            Text(
                "已记录 ${elapsedMinutes / 60} 小时 ${elapsedMinutes % 60} 分",
                style = MaterialTheme.typography.bodyLarge,
                color = FocusDarkInkMuted
            )
            Spacer(Modifier.height(28.dp))
            Text(
                text = buildString {
                    append(state.session.placement.label)
                    append(" · ")
                    append(if (live?.audioEnabled ?: state.session.audioEnabled) "声音分析已开启" else "未开启声音分析")
                },
                style = MaterialTheme.typography.bodyMedium,
                color = FocusDarkInkMuted
            )
            if (live != null && live.recordedEpochs > 0) {
                Spacer(Modifier.height(6.dp))
                Text(
                    text = buildString {
                        append("上一分钟")
                        if (state.session.placement == SleepPlacement.BED) append(" · 体动 ${live.lastMovementSeconds} 秒")
                        if (live.audioEnabled) append(" · 噪声 ${live.lastNoiseDb.toInt()} dB")
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = FocusDarkInkMuted
                )
            }
            Spacer(Modifier.weight(1f))
            Text(
                "可以锁屏睡觉了，监测会在后台继续",
                style = MaterialTheme.typography.bodySmall,
                color = FocusDarkInkMuted,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(
                onClick = onStop,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, FocusDarkOutline),
                colors = ButtonDefaults.outlinedButtonColors(contentColor = FocusDarkInk)
            ) {
                Text("起床，结束记录")
            }
        }
    }
}

/**
 * 每到整分钟刷新一次的时钟。
 * 只在整分钟唤醒，而不是每秒刷新：夜间页面没必要高频重组。
 */
@Composable
private fun rememberMinuteClock() = produceState(initialValue = System.currentTimeMillis()) {
    while (true) {
        val now = System.currentTimeMillis()
        delay(60_000L - now % 60_000L)
        value = System.currentTimeMillis()
    }
}

// ==================== 中断 / 加载 ====================

/** 进程被系统杀掉后回到 App 时显示：说明原因，并允许用已保存的数据生成报告。 */
@Composable
private fun SleepInterruptedContent(
    state: SleepUiState.Interrupted,
    onFinishInterrupted: (Long) -> Unit
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
                Icon(
                    Icons.Outlined.WarningAmber,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.tertiary,
                    modifier = Modifier.size(34.dp)
                )
            }
        }
        Spacer(Modifier.height(20.dp))
        Text("睡眠记录被中断", style = MaterialTheme.typography.headlineMedium)
        Spacer(Modifier.height(8.dp))
        Text(
            "从 ${clockText(state.session.startTime)} 开始的记录在后台被系统停止，之前的数据已保存。" +
                "为避免再次中断，请在睡前检查中关闭电池优化。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(32.dp))
        Button(
            onClick = { onFinishInterrupted(state.session.id) },
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp),
            shape = RoundedCornerShape(14.dp)
        ) {
            Text("用已保存的数据生成报告")
        }
    }
}

/** 居中加载指示，可附带说明文字。 */
@Composable
private fun CenteredProgress(text: String?) {
    Column(
        modifier = Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator()
        if (text != null) {
            Spacer(Modifier.height(16.dp))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

// ==================== 工具函数 ====================

/**
 * 打开系统"电池优化"列表，让用户把 FocusGuard 设为不优化。
 *
 * 为什么不直接弹窗请求豁免（ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS）：
 * 那需要声明 REQUEST_IGNORE_BATTERY_OPTIMIZATIONS 权限，Google Play 只允许少数类型的应用使用。
 * 部分国产 ROM 没有这个页面，就退回到本应用的详情设置页。
 */
private fun openBatteryOptimizationSettings(context: Context) {
    val candidates = listOf(
        Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS),
        Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", context.packageName, null))
    )
    for (intent in candidates) {
        try {
            context.startActivity(intent)
            return
        } catch (_: ActivityNotFoundException) {
            // 继续尝试下一个
        }
    }
}

/** 时长显示为"7 小时 12 分"，不足 1 小时显示"45 分钟"。 */
internal fun formatHoursMinutes(millis: Long): String {
    val minutes = (millis / 60_000L).coerceAtLeast(0L)
    return if (minutes >= 60) "${minutes / 60} 小时 ${minutes % 60} 分" else "$minutes 分钟"
}

/** HH:mm 时刻文本。 */
internal fun clockText(timestamp: Long): String =
    SimpleDateFormat("HH:mm", Locale.getDefault()).format(Date(timestamp))

/**
 * "哪一晚"的日期标签。凌晨 1 点开始的睡眠属于前一天晚上，
 * 所以先减去 12 小时再取日期：中午 12 点之前开始的都算作前一天晚上。
 */
internal fun sleepNightLabel(startTime: Long): String =
    SimpleDateFormat("M月d日 EEEE 晚", Locale.CHINA).format(Date(startTime - 12 * 60 * 60 * 1000L))
