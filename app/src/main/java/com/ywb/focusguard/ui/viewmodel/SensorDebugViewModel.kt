package com.ywb.focusguard.ui.viewmodel

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.ywb.focusguard.data.repository.SleepRepository
import com.ywb.focusguard.data.sensor.LightSensorDataSource
import com.ywb.focusguard.data.sensor.SleepEpochAccumulator
import com.ywb.focusguard.data.sensor.SleepMotionDataSource
import com.ywb.focusguard.data.sensor.SleepSoundDataSource
import com.ywb.focusguard.data.sensor.SleepSoundEventDetector
import com.ywb.focusguard.service.SleepStateHolder
import com.ywb.focusguard.ui.state.SensorDebugUiState
import com.ywb.focusguard.ui.state.SoundEventUi
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject

/**
 * 传感器调试页 ViewModel：复用睡眠监测的同一套数据源和算法类，实时展示原始信号与阈值。
 *
 * **生命周期（重点）**：
 * 采集写在 channelFlow 里，再用 stateIn(WhileSubscribed) 暴露给页面。
 * 页面可见时开始收集 → 注册传感器、打开麦克风；页面离开 1 秒后停止收集 → channelFlow 被取消 →
 * 各数据源在 awaitClose / finally 中释放硬件。调试页不会在后台偷偷占用麦克风。
 *
 * **为什么每 250ms 才发一次状态**：
 * 麦克风每秒 10 帧、加速度计每秒 5 次，如果每个样本都更新 StateFlow，页面每秒要重组 15 次以上；
 * 先写进内存缓冲区，再按固定节奏取快照发给页面，重组频率稳定在 4 次/秒，曲线看起来同样流畅。
 */
@OptIn(ExperimentalCoroutinesApi::class)
@HiltViewModel
class SensorDebugViewModel @Inject constructor(
    application: Application,
    private val motionDataSource: SleepMotionDataSource,
    private val soundDataSource: SleepSoundDataSource,
    private val lightDataSource: LightSensorDataSource,
    /** 导出最近一晚睡眠数据。 */
    private val sleepRepository: SleepRepository
) : AndroidViewModel(application) {

    /** 权限变化后自增，触发采集流重新启动（新授权的麦克风才能被打开）。 */
    private val restartTrigger = MutableStateFlow(0)

    /** 导出结果提示，和采集状态分开管理，避免导出时重启采集。 */
    private val exportMessage = MutableStateFlow<String?>(null)

    /** 页面状态：睡眠监测运行时只返回"已暂停"，否则启动调试采集。 */
    val uiState: StateFlow<SensorDebugUiState> = combine(
        SleepStateHolder.serviceRunning,
        restartTrigger
    ) { running, _ -> running }
        .flatMapLatest { sleepRunning ->
            if (sleepRunning) flowOf(SensorDebugUiState(sleepMonitoring = true)) else debugSamplingFlow()
        }
        .combine(exportMessage) { state, message -> state.copy(exportMessage = message) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(1_000),
            initialValue = SensorDebugUiState()
        )

    /** 页面拿到麦克风授权后调用，重启采集流。 */
    fun onPermissionChanged() {
        restartTrigger.value++
    }

    /**
     * 实时采集流：三路传感器写入内存缓冲区，定时取快照发射。
     * 所有回调都在同一个收集协程（主线程）里执行，缓冲区不需要加锁。
     */
    private fun debugSamplingFlow(): Flow<SensorDebugUiState> = channelFlow {
        val audioGranted = soundDataSource.hasPermission()
        // 直接复用睡眠用的聚合器和检测器，保证调试页看到的阈值就是睡眠记录时用的阈值
        val accumulator = SleepEpochAccumulator(epochStart = System.currentTimeMillis())
        val detector = SleepSoundEventDetector()
        val motionValues = ArrayDeque<Float>()
        val movementSeconds = ArrayDeque<Long>()
        val soundValues = ArrayDeque<Float>()
        val soundEvents = ArrayDeque<SoundEventUi>()
        var dominantHz = 0f
        var lux = 0f

        launch {
            motionDataSource.observeMotionDeltas().collect { delta ->
                val now = System.currentTimeMillis()
                accumulator.onMotionDelta(delta, now)
                motionValues.pushLimited(delta, MOTION_WINDOW)
                if (delta >= accumulator.currentMotionThreshold()) {
                    val second = now / 1_000L
                    if (movementSeconds.lastOrNull() != second) movementSeconds.addLast(second)
                }
            }
        }
        if (audioGranted) {
            launch {
                soundDataSource.observeFrames().collect { frame ->
                    soundValues.pushLimited(frame.decibel, SOUND_WINDOW)
                    dominantHz = frame.dominantHz
                    detector.onFrame(frame)?.let { event ->
                        soundEvents.addFirst(
                            SoundEventUi(
                                timeText = timeFormat.format(Date(event.startTime)),
                                detailText = "%.1f 秒 · 峰值 %d dB · %d Hz".format(
                                    (event.endTime - event.startTime) / 1_000f,
                                    event.peakDb.toInt(),
                                    event.averageHz.toInt()
                                ),
                                isSnoreLike = event.isSnoreLike
                            )
                        )
                        while (soundEvents.size > MAX_EVENTS) soundEvents.removeLast()
                    }
                }
            }
        }
        launch {
            lightDataSource.observeLight().collect { sample -> lux = sample.lux }
        }

        while (true) {
            val nowSecond = System.currentTimeMillis() / 1_000L
            while (movementSeconds.isNotEmpty() && movementSeconds.first() <= nowSecond - 60) {
                movementSeconds.removeFirst()
            }
            send(
                SensorDebugUiState(
                    sleepMonitoring = false,
                    audioGranted = audioGranted,
                    motionValues = motionValues.toList(),
                    motionThreshold = accumulator.currentMotionThreshold(),
                    movementSecondsLastMinute = movementSeconds.size,
                    soundDbValues = soundValues.toList(),
                    baselineDb = detector.baselineDb.takeIf { !it.isNaN() } ?: 0f,
                    triggerDb = detector.triggerLevelDb.takeIf { !it.isNaN() } ?: 0f,
                    dominantHz = dominantHz,
                    recentSoundEvents = soundEvents.toList(),
                    lux = lux
                )
            )
            delay(EMIT_INTERVAL_MILLIS)
        }
    }

    /**
     * 把最近一晚的分钟数据导出成 CSV，便于在电脑上用 Excel 画图、对照自己的真实感受调阈值。
     *
     * @param uri 用户通过系统文件选择器（SAF）选定的保存位置；不需要任何存储权限。
     */
    fun exportLatestSleep(uri: Uri) {
        viewModelScope.launch {
            val latest = sleepRepository.observeSessions().first().firstOrNull()
            if (latest == null) {
                exportMessage.value = "还没有已完成的睡眠记录"
                return@launch
            }
            val detail = sleepRepository.observeSleepDetail(latest.id).first()
            if (detail == null) {
                exportMessage.value = "读取睡眠记录失败"
                return@launch
            }
            val result = runCatching {
                withContext(Dispatchers.IO) {
                    val csvTimeFormat = SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault())
                    getApplication<Application>().contentResolver.openOutputStream(uri)?.bufferedWriter()?.use { writer ->
                        writer.appendLine("time,movementSeconds,averageNoiseDb,maxNoiseDb,soundEvents,snoreEvents,lux,screenOnSeconds,stage")
                        detail.epochs.forEachIndexed { index, epoch ->
                            val stage = detail.analysis.stages.getOrNull(index)?.name ?: ""
                            writer.appendLine(
                                listOf(
                                    csvTimeFormat.format(Date(epoch.startTime)),
                                    epoch.movementSeconds,
                                    "%.1f".format(Locale.US, epoch.averageNoiseDb),
                                    "%.1f".format(Locale.US, epoch.maxNoiseDb),
                                    epoch.soundEventCount,
                                    epoch.snoreEventCount,
                                    "%.1f".format(Locale.US, epoch.averageLightLux),
                                    epoch.screenOnSeconds,
                                    stage
                                ).joinToString(",")
                            )
                        }
                    } ?: error("无法打开文件")
                }
            }
            exportMessage.value = if (result.isSuccess) {
                "已导出 ${SimpleDateFormat("M月d日", Locale.CHINA).format(Date(latest.startTime))} 那晚的 ${detail.epochs.size} 分钟数据"
            } else {
                "导出失败：${result.exceptionOrNull()?.message}"
            }
        }
    }

    /** 关闭导出提示。 */
    fun dismissExportMessage() {
        exportMessage.value = null
    }

    /** 追加一个值并保持缓冲区长度不超过 [limit]，最旧的先丢弃。 */
    private fun <T> ArrayDeque<T>.pushLimited(value: T, limit: Int) {
        addLast(value)
        while (size > limit) removeFirst()
    }

    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.getDefault())

    private companion object {
        /** 体动曲线保留约 30 秒（5 次/秒）。 */
        const val MOTION_WINDOW = 150

        /** 声音曲线保留约 15 秒（10 帧/秒）。 */
        const val SOUND_WINDOW = 150

        /** 最多展示的声音事件条数。 */
        const val MAX_EVENTS = 8

        /** 页面状态发射间隔。 */
        const val EMIT_INTERVAL_MILLIS = 250L
    }
}
