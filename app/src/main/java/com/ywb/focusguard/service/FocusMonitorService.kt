package com.ywb.focusguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Binder
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import com.ywb.focusguard.MainActivity
import com.ywb.focusguard.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

/**
 * 专注期间的前台服务，保证 App 切后台后传感器继续采集。
 *
 * **核心设计**：
 * - Service 内部独立维护计时，不依赖 ViewModel
 * - 启动时从 Intent 读取目标时长，自己倒计时
 * - 订阅 SessionStateHolder 的状态变化（暂停/继续/结束）
 * - 每秒更新通知显示剩余时间
 *
 * **生命周期**：
 * - startSession 时由 Activity 调用 startForegroundService
 * - finishSession 时由 Activity 调用 stopService，或 Service 自己倒计时结束
 */
@AndroidEntryPoint
class FocusMonitorService : Service() {

    companion object {
        /** 通知渠道 ID，系统设置中可见。 */
        const val CHANNEL_ID = "focus_monitor_channel"

        /** 前台通知 ID，更新通知时使用同一个 ID。 */
        const val NOTIFICATION_ID = 1001

        /** Intent Extra：会话 ID。 */
        const val EXTRA_SESSION_ID = "session_id"

        /** Intent Extra：目标时长（毫秒）。 */
        const val EXTRA_DURATION_MILLIS = "duration_millis"

        /**
         * 启动前台服务。
         *
         * @param context Activity 或 Application Context。
         * @param sessionId Room 会话 ID。
         * @param durationMillis 目标专注时长（毫秒）。
         */
        fun start(context: Context, sessionId: Long, durationMillis: Long) {
            val intent = Intent(context, FocusMonitorService::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
                putExtra(EXTRA_DURATION_MILLIS, durationMillis)
            }
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                context.startForegroundService(intent)
            } else {
                context.startService(intent)
        }
        }

        /**
         * 停止前台服务。
         */
        fun stop(context: Context) {
            context.stopService(Intent(context, FocusMonitorService::class.java))
        }
    }

    /** 当前会话 ID。 */
    private var sessionId: Long = 0L

    /** 目标时长。 */
    private var targetDurationMillis: Long = 0L

    /** 剩余时间。 */
    private var remainingMillis: Long = 0L

    /** 是否暂停。 */
    private var isPaused: Boolean = false

    /** 用于 Activity 绑定查询状态。 */
    private val binder = LocalBinder()

    /** 通知管理器。 */
    private lateinit var notificationManager: NotificationManager

    /** 协程作用域。 */
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    /** 计时任务。 */
    private var tickerJob: Job? = null

    /** 状态订阅任务。 */
    private var stateObserverJob: Job? = null

    // ==================== 生命周期回调 ====================

    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sessionId = intent?.getLongExtra(EXTRA_SESSION_ID, 0L) ?: 0L
        targetDurationMillis = intent?.getLongExtra(EXTRA_DURATION_MILLIS, 25 * 60 * 1000L) ?: 25 * 60 * 1000L
        remainingMillis = targetDurationMillis
        isPaused = false

        // 启动前台服务
        val notification = buildNotification("专注进行中", formatTime(remainingMillis))
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        // 启动内部计时
        startInternalTicker()

        // 订阅外部控制事件
        observeControlEvents()

        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder = binder

    override fun onDestroy() {
        tickerJob?.cancel()
        stateObserverJob?.cancel()
        notificationManager.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    // ==================== 内部计时逻辑 ====================

    /**
     * Service 内部独立计时，不依赖 ViewModel。
     *
     * 这样即使 Activity 被销毁，计时仍能继续。
     */
    private fun startInternalTicker() {
        tickerJob?.cancel()
        tickerJob = serviceScope.launch {
            while (remainingMillis > 0) {
                delay(1000)
                if (!isPaused) {
                    remainingMillis -= 1000
                    updateNotification(remainingMillis)

                    // 同步状态给 ViewModel（如果还活着）
                    SessionStateHolder.updateRemaining(remainingMillis)
                }
            }

            // 倒计时结束，自动停止服务
            if (remainingMillis <= 0) {
                stopSelf()
            }
        }
    }

    /**
     * 订阅外部控制事件（暂停/继续/结束）。
     */
    private fun observeControlEvents() {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            // 订阅暂停状态
            SessionStateHolder.isPaused.collect { paused ->
                isPaused = paused
                if (paused) {
                    showPaused()
                } else {
                    updateNotification(remainingMillis)
                }
            }
        }
    }

    // ==================== 通知相关 ====================

    private fun updateNotification(remaining: Long) {
        val notification = buildNotification("专注进行中", formatTime(remaining))
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun showPaused() {
        val notification = buildNotification("专注已暂停", "点击继续")
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    private fun formatTime(millis: Long): String {
        val totalSeconds = millis / 1000
        val minutes = totalSeconds / 60
        val seconds = totalSeconds % 60
        return String.format("剩余 %02d:%02d", minutes, seconds)
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "专注监测",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "专注期间显示剩余时间和控制按钮"
                setShowBadge(false)
            }
            notificationManager.createNotificationChannel(channel)
        }
    }

    private fun buildNotification(title: String, content: String): Notification {
        // 点击通知打开 App
        val openAppIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val openAppPendingIntent = PendingIntent.getActivity(
            this,
            0,
            openAppIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 暂停/继续按钮
        val pausePendingIntent = PendingIntent.getBroadcast(
            this,
            1,
            SessionActionReceiver.createTogglePauseIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // 结束按钮
        val finishPendingIntent = PendingIntent.getBroadcast(
            this,
            2,
            SessionActionReceiver.createFinishIntent(this),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(content)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(openAppPendingIntent)
            .addAction(
                NotificationCompat.Action(
                    0,
                    if (isPaused) "继续" else "暂停",
                    pausePendingIntent
                )
            )
            .addAction(NotificationCompat.Action(0, "结束", finishPendingIntent))
            .setOngoing(true)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }

    /**
     * 本地 Binder，供 Activity 查询当前会话状态。
     */
    inner class LocalBinder : Binder() {
        fun getService(): FocusMonitorService = this@FocusMonitorService
        fun getSessionId(): Long = sessionId
        fun getRemainingMillis(): Long = remainingMillis
    }
}