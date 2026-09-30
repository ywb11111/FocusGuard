package com.ywb.focusguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ywb.focusguard.MainActivity
import com.ywb.focusguard.R
import com.ywb.focusguard.data.repository.SleepRepository
import com.ywb.focusguard.data.sensor.LightSensorDataSource
import com.ywb.focusguard.data.sensor.SleepEpochAccumulator
import com.ywb.focusguard.data.sensor.SleepMotionDataSource
import com.ywb.focusguard.data.sensor.SleepSoundDataSource
import com.ywb.focusguard.data.sensor.SleepSoundEventDetector
import com.ywb.focusguard.domain.analyzer.SleepAnalyzer
import com.ywb.focusguard.domain.model.SleepEpoch
import com.ywb.focusguard.domain.model.SleepLiveStatus
import com.ywb.focusguard.domain.model.SleepPlacement
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

private const val TAG = "SleepMonitorService"

/**
 * 睡眠监测前台服务：整晚在后台采集体动、声音、光照和亮灭屏，每分钟聚合写入 Room。
 *
 * **和 FocusMonitorService 最大的区别：采集逻辑在 Service 里，而不是 ViewModel 里。**
 * 专注只有 25 分钟，采集放在 ViewModel 问题不大；睡眠要跑 8 小时，用户划掉任务后 ViewModel 会被销毁，
 * 采集就停了。只有 Service（配合前台通知）的生命周期能覆盖整晚，所以这里 Service 是数据的生产者，
 * 页面只是通过 Room 和 SleepStateHolder 观察结果。
 *
 * **整晚运行要处理的真实 Android 问题**：
 * 1. Doze / CPU 休眠：熄屏后 CPU 会休眠，传感器回调和协程 delay 都会停。持有 PARTIAL_WAKE_LOCK 让 CPU 保持运行，
 *    并设置超时，即使代码出错也不会让手机永远无法休眠。
 * 2. 前台服务类型（Android 14）：必须声明并传入类型。麦克风用 microphone；只有体动时用 health，
 *    health 的前置条件通过在 Manifest 声明 HIGH_SAMPLING_RATE_SENSORS 满足。启动失败时逐级降级。
 * 3. 进程被杀：返回 START_STICKY，系统可能在内存充足后重建 Service（intent 为 null），
 *    这时从 Room 找回进行中的记录继续写入；Android 12+ 可能不允许后台重新进入前台，那就保留记录，
 *    由页面提示"记录被中断"。
 * 4. 多线程：体动、声音、光照在不同协程回调，亮灭屏在主线程广播回调，共享的 SleepEpochAccumulator 内部加锁。
 *
 * **生命周期**：
 * - 睡眠页点击"开始"→ [start] → onStartCommand(ACTION_START) → 进入前台 → 获取 WakeLock → 开始采集
 * - 页面或通知点击"结束"→ [stop] → onStartCommand(ACTION_STOP) → 停止采集 → 分析并写库 → stopSelf
 * - onDestroy 释放 WakeLock、注销广播、取消全部协程
 */
@AndroidEntryPoint
class SleepMonitorService : Service() {

    companion object {
        /** 通知渠道 ID，系统设置中可见。 */
        const val CHANNEL_ID = "sleep_monitor_channel"

        /** 前台通知 ID，与专注服务的 1001 区分。 */
        const val NOTIFICATION_ID = 1002

        /** 开始监测。 */
        const val ACTION_START = "com.ywb.focusguard.action.SLEEP_START"

        /** 结束监测并生成报告。 */
        const val ACTION_STOP = "com.ywb.focusguard.action.SLEEP_STOP"

        /** Intent Extra：手机摆放位置的枚举名。 */
        const val EXTRA_PLACEMENT = "placement"

        /** 单晚最长记录时长，超过自动结束，防止忘记关闭导致整天耗电。 */
        private const val MAX_SESSION_MILLIS = 14 * 60 * 60 * 1000L

        /** WakeLock 超时，比最长记录多 10 分钟，保证正常流程先释放。 */
        private const val WAKE_LOCK_TIMEOUT_MILLIS = MAX_SESSION_MILLIS + 10 * 60 * 1000L

        /** 结束时最后不足一分钟的数据，至少 30 秒才保存，太短的片段没有统计意义。 */
        private const val MIN_PARTIAL_EPOCH_MILLIS = 30_000L

        /**
         * 开始睡眠监测。必须在 App 位于前台时调用（点击按钮时），
         * 否则 Android 12+ 不允许启动前台服务，Android 11+ 也不允许后台使用麦克风。
         */
        fun start(context: Context, placement: SleepPlacement) {
            val intent = Intent(context, SleepMonitorService::class.java).apply {
                action = ACTION_START
                putExtra(EXTRA_PLACEMENT, placement.name)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /**
         * 结束睡眠监测。用 startService 发送 ACTION_STOP 而不是 stopService：
         * 直接 stopService 会立即销毁 Service，来不及保存最后一分钟数据和生成报告。
         */
        fun stop(context: Context) {
            val intent = Intent(context, SleepMonitorService::class.java).apply {
                action = ACTION_STOP
            }
            context.startService(intent)
        }
    }

    @Inject lateinit var sleepRepository: SleepRepository
    @Inject lateinit var motionDataSource: SleepMotionDataSource
    @Inject lateinit var soundDataSource: SleepSoundDataSource
    @Inject lateinit var lightDataSource: LightSensorDataSource

    /**
     * Service 自己的协程作用域。
     * SupervisorJob：某个子协程失败不影响其他协程；
     * CoroutineExceptionHandler：兜底记录日志，避免未捕获异常直接让整个 App 崩溃。
     */
    private val serviceScope = CoroutineScope(
        SupervisorJob() + Dispatchers.Default + CoroutineExceptionHandler { _, throwable ->
            Log.e(TAG, "睡眠采集协程异常", throwable)
        }
    )

    /** 全部采集协程的父 Job，结束时一次性取消。 */
    private var collectJob: Job? = null

    /** 是否正在结束流程中，防止重复点击"结束"或重复开始。 */
    @Volatile private var finishing = false

    /** 当前记录的 Room id；0 表示尚未创建或恢复。 */
    @Volatile private var sessionId = 0L

    /** 当前记录开始时间，用于计算已记录时长和 14 小时上限。 */
    @Volatile private var sessionStartedAt = 0L

    /** 本晚手机摆放位置。 */
    @Volatile private var placement = SleepPlacement.BED

    /** 麦克风是否真正可用（有权限且前台服务类型申请成功）。 */
    @Volatile private var audioEnabled = false

    /** 已写入数据库的分钟数。 */
    @Volatile private var recordedEpochs = 0

    /** 当前分钟的聚合器；结束流程需要读取最后不足一分钟的数据。 */
    @Volatile private var accumulator: SleepEpochAccumulator? = null

    /** 保持 CPU 运行的部分唤醒锁。 */
    private var wakeLock: PowerManager.WakeLock? = null

    /** 亮灭屏广播是否已注册，避免重复注销抛异常。 */
    private var screenReceiverRegistered = false

    private lateinit var notificationManager: NotificationManager

    /**
     * 亮灭屏广播接收器。SCREEN_ON/OFF 只能动态注册（Manifest 静态注册收不到），
     * 所以跟随 Service 生命周期注册和注销。
     */
    private val screenReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val now = System.currentTimeMillis()
            when (intent.action) {
                Intent.ACTION_SCREEN_ON -> accumulator?.onScreenChanged(true, now)
                Intent.ACTION_SCREEN_OFF -> accumulator?.onScreenChanged(false, now)
            }
        }
    }

    // ==================== 生命周期 ====================

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
        SleepStateHolder.setServiceRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            finishAndStop(note = null)
            return START_NOT_STICKY
        }
        // 重复点击开始、或结束流程进行中：忽略
        if (collectJob?.isActive == true || finishing) return START_STICKY

        // intent 为 null 说明是进程被杀后系统按 START_STICKY 重建的
        val restarted = intent == null
        val requestedPlacement = SleepPlacement.fromName(intent?.getStringExtra(EXTRA_PLACEMENT))
        if (!enterForeground()) {
            // 通常是 Android 12+ 不允许后台进入前台。Room 里的进行中记录保留，页面会提示中断。
            Log.w(TAG, "无法进入前台，停止睡眠服务（restarted=$restarted）")
            stopSelf()
            return START_NOT_STICKY
        }
        acquireWakeLock()
        collectJob = serviceScope.launch { runSession(requestedPlacement, restarted) }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        serviceScope.cancel()
        unregisterScreenReceiver()
        releaseWakeLock()
        notificationManager.cancel(NOTIFICATION_ID)
        SleepStateHolder.setServiceRunning(false)
        super.onDestroy()
    }

    // ==================== 采集流程 ====================

    /**
     * 创建或恢复一晚记录，然后并行启动各路采集和每分钟计时，直到被取消。
     *
     * @param requestedPlacement 用户在页面选择的摆放位置；恢复旧记录时以数据库为准。
     * @param restarted 是否为系统重建的 Service。
     */
    private suspend fun runSession(requestedPlacement: SleepPlacement, restarted: Boolean) {
        val existing = sleepRepository.getActiveSession()
        when {
            existing != null -> {
                // 恢复：继续往同一晚写数据，而不是新开一条记录
                sessionId = existing.id
                sessionStartedAt = existing.startTime
                placement = existing.placement
                Log.d(TAG, "恢复进行中的睡眠记录 ${existing.id}")
            }
            restarted -> {
                // 系统重建了 Service，但数据库里没有进行中的记录，没有需要继续的工作
                withContext(Dispatchers.Main) { stopSelfCompletely() }
                return
            }
            else -> {
                placement = requestedPlacement
                sessionStartedAt = System.currentTimeMillis()
                sessionId = sleepRepository.startSession(placement, audioEnabled)
            }
        }

        val acc = SleepEpochAccumulator(epochStart = System.currentTimeMillis())
        accumulator = acc
        // 记录开始那一刻屏幕通常是亮的（用户刚点完按钮），要先告诉聚合器初始状态
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        acc.onScreenChanged(powerManager.isInteractive, System.currentTimeMillis())
        registerScreenReceiver()
        publishStatus(lastEpoch = null)

        // coroutineScope：等待所有子协程；外部取消 collectJob 时它们一起被取消，各数据源在 awaitClose/finally 中释放硬件
        coroutineScope {
            if (placement == SleepPlacement.BED) {
                launch {
                    motionDataSource.observeMotionDeltas().collect { delta ->
                        acc.onMotionDelta(delta, System.currentTimeMillis())
                    }
                }
            }
            if (audioEnabled) {
                launch {
                    // 检测器有内部状态，只在这一个协程里顺序使用，不需要加锁
                    val detector = SleepSoundEventDetector()
                    soundDataSource.observeFrames().collect { frame ->
                        acc.onSoundFrame(frame)
                        detector.onFrame(frame)?.let(acc::onSoundEvent)
                    }
                }
            }
            launch {
                lightDataSource.observeLight().collect { sample -> acc.onLight(sample.lux) }
            }
            launch { runEpochTicker(acc) }
        }
    }

    /**
     * 每分钟把聚合器的数据取出写库，并更新通知和页面状态。
     *
     * 用"绝对边界时间"而不是每次 delay(60s)：后者每轮都有几毫秒误差，一晚累积下来会漂移。
     */
    private suspend fun runEpochTicker(acc: SleepEpochAccumulator) {
        var boundary = acc.currentEpochStart() + SleepAnalyzer.EPOCH_MILLIS
        while (currentCoroutineContext().isActive) {
            val wait = boundary - System.currentTimeMillis()
            if (wait > 0) delay(wait)

            val now = System.currentTimeMillis()
            if (now - boundary > SleepAnalyzer.EPOCH_MILLIS) {
                // 严重延迟（CPU 曾被挂起）：不补造空白分钟，否则缺失的时间会被误判成"安稳睡眠"
                Log.w(TAG, "计时延迟 ${now - boundary}ms，跳过缺失分钟")
                boundary = now
            }
            val epoch = acc.drain(boundary)
            sleepRepository.saveEpoch(sessionId, epoch)
            recordedEpochs++
            publishStatus(epoch)
            boundary += SleepAnalyzer.EPOCH_MILLIS

            if (System.currentTimeMillis() - sessionStartedAt >= MAX_SESSION_MILLIS) {
                finishAndStop(note = "已达到 14 小时上限，自动结束记录")
                return
            }
        }
    }

    /**
     * 结束流程：停止采集 → 保存最后不足一分钟的数据 → 分析写库 → 通知页面 → 退出前台并停止。
     *
     * @param note 写入记录的系统说明；null 表示正常结束。
     */
    private fun finishAndStop(note: String?) {
        if (finishing) return
        finishing = true
        serviceScope.launch {
            // 先确保采集协程完全停止，之后聚合器不会再有新数据写入
            collectJob?.cancelAndJoin()

            val acc = accumulator
            val activeId = sessionId
            val finishedId = if (activeId != 0L) {
                val endTime = System.currentTimeMillis()
                if (acc != null && endTime - acc.currentEpochStart() >= MIN_PARTIAL_EPOCH_MILLIS) {
                    sleepRepository.saveEpoch(activeId, acc.drain(endTime))
                }
                sleepRepository.finishSession(activeId, endTime, note)?.id
            } else {
                // 本进程里没有在采集（进程曾被杀、Service 仅为结束而启动）：按中断处理
                sleepRepository.getActiveSession()?.let { sleepRepository.finishInterruptedSession(it.id)?.id }
            }
            finishedId?.let(SleepStateHolder::emitFinished)

            withContext(Dispatchers.Main) { stopSelfCompletely() }
        }
    }

    /** 退出前台（移除通知）并停止 Service。 */
    private fun stopSelfCompletely() {
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** 把最新状态发布给页面，并刷新通知文字。 */
    private fun publishStatus(lastEpoch: SleepEpoch?) {
        SleepStateHolder.updateLiveStatus(
            SleepLiveStatus(
                sessionId = sessionId,
                startedAt = sessionStartedAt,
                recordedEpochs = recordedEpochs,
                lastMovementSeconds = lastEpoch?.movementSeconds ?: 0,
                lastNoiseDb = lastEpoch?.averageNoiseDb ?: 0f,
                audioEnabled = audioEnabled,
                placement = placement
            )
        )
        notificationManager.notify(NOTIFICATION_ID, buildNotification(elapsedText()))
    }

    // ==================== 前台服务与系统资源 ====================

    /**
     * 按候选类型依次尝试进入前台，成功一个即返回。
     *
     * 为什么要"逐级降级"：Android 14 对每种类型都有运行时前置条件（例如麦克风类型要求已授权且 App 在前台），
     * 条件不满足会抛 SecurityException。与其整晚监测失败，不如退一步只用体动继续记录。
     *
     * @return 是否成功进入前台。
     */
    private fun enterForeground(): Boolean {
        val wantAudio = soundDataSource.hasPermission()
        val notification = buildNotification("正在准备传感器…")
        for ((type, withAudio) in foregroundTypeCandidates(wantAudio)) {
            try {
                ServiceCompat.startForeground(this, NOTIFICATION_ID, notification, type)
                audioEnabled = withAudio
                return true
            } catch (e: RuntimeException) {
                // SecurityException / ForegroundServiceStartNotAllowedException 等都继承自 RuntimeException
                Log.w(TAG, "前台服务类型 $type 启动失败，尝试降级", e)
            }
        }
        return false
    }

    /**
     * 不同系统版本可用的前台服务类型候选，按优先级排列。
     *
     * @return (类型, 是否启用麦克风) 列表。
     */
    private fun foregroundTypeCandidates(wantAudio: Boolean): List<Pair<Int, Boolean>> = when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE -> buildList {
            val health = ServiceInfo.FOREGROUND_SERVICE_TYPE_HEALTH
            val microphone = ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
            if (wantAudio) add((health or microphone) to true)
            add(health to false)
            if (wantAudio) add(microphone to true)
        }
        // Android 11~13：后台用麦克风需要 microphone 类型；不用麦克风时不需要声明类型
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.R -> buildList {
            if (wantAudio) add(ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE to true)
            add(0 to false)
        }
        // Android 10 及以下没有麦克风后台限制
        else -> listOf(0 to wantAudio)
    }

    /** 获取部分唤醒锁：只保持 CPU 运行，不点亮屏幕。 */
    private fun acquireWakeLock() {
        if (wakeLock?.isHeld == true) return
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, "FocusGuard:SleepMonitor").apply {
            setReferenceCounted(false)
            acquire(WAKE_LOCK_TIMEOUT_MILLIS)
        }
    }

    /** 释放唤醒锁；忘记释放会让手机整晚无法休眠，是最常见的耗电 bug。 */
    private fun releaseWakeLock() {
        wakeLock?.let { if (it.isHeld) it.release() }
        wakeLock = null
    }

    /** 注册亮灭屏广播。RECEIVER_NOT_EXPORTED 只接收系统和本 App 的广播。 */
    private fun registerScreenReceiver() {
        if (screenReceiverRegistered) return
        val filter = IntentFilter().apply {
            addAction(Intent.ACTION_SCREEN_ON)
            addAction(Intent.ACTION_SCREEN_OFF)
        }
        ContextCompat.registerReceiver(this, screenReceiver, filter, ContextCompat.RECEIVER_NOT_EXPORTED)
        screenReceiverRegistered = true
    }

    /** 注销亮灭屏广播。 */
    private fun unregisterScreenReceiver() {
        if (!screenReceiverRegistered) return
        runCatching { unregisterReceiver(screenReceiver) }
        screenReceiverRegistered = false
    }

    // ==================== 通知 ====================

    /** 已记录时长文案，例如"已记录 6 小时 12 分"。 */
    private fun elapsedText(): String {
        val startedAt = sessionStartedAt
        if (startedAt == 0L) return "正在准备传感器…"
        val minutes = ((System.currentTimeMillis() - startedAt) / 60_000L).coerceAtLeast(0L)
        return "已记录 ${minutes / 60} 小时 ${minutes % 60} 分 · 可以锁屏睡觉了"
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "睡眠监测",
                // LOW：不响铃不振动，夜间不会吵醒用户
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "睡眠监测期间显示记录状态和结束按钮"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(content: String): Notification {
        val openAppIntent = PendingIntent.getActivity(
            this,
            10,
            Intent(this, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_SINGLE_TOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        // 通知按钮直接发给 Service 自己，不需要额外的 BroadcastReceiver
        val stopIntent = PendingIntent.getService(
            this,
            11,
            Intent(this, SleepMonitorService::class.java).apply { action = ACTION_STOP },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("睡眠监测中")
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppIntent)
            .addAction(NotificationCompat.Action(0, "起床，结束记录", stopIntent))
            .setOngoing(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
}
