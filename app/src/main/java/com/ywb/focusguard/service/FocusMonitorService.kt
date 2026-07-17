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
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch

/**
 * 专注期间的前台服务，保证 App 切后台后传感器继续采集。
 *
 * **职责边界**：
 * - 创建并维护前台通知（显示剩余时间、暂停/结束按钮）
 * - 不直接操作传感器，传感器由 SessionViewModel 通过 EnvironmentRepository 管理
 * - 通过 Binder 提供 sessionId 查询接口，供 Activity 判断服务是否运行
 * - 订阅 SessionStateHolder 更新通知内容
 *
 * **生命周期**：
 * - startSession 时由 Activity 调用 startForegroundService
 * - finishSession 时由 Activity 调用 stopService
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

        /** Intent Extra：剩余时间（毫秒）。 */
        const val EXTRA_REMAINING_MILLIS = "remaining_millis"

        /**
         * 启动前台服务。
         *
         * @param context Activity 或 Application Context。
         * @param sessionId Room 会话 ID。
         */
        fun start(context: Context, sessionId: Long) {
            val intent = Intent(context, FocusMonitorService::class.java).apply {
                putExtra(EXTRA_SESSION_ID, sessionId)
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

    /** 当前会话 ID，由 startIntent 传入。 */
    private var sessionId: Long = 0L

    /** 用于 Activity 绑定查询状态。 */
    private val binder = LocalBinder()

    /** 通知管理器，用于更新通知内容。 */
    private lateinit var notificationManager: NotificationManager

    /** 协程作用域，用于订阅状态更新。 */
    private val serviceScope = CoroutineScope(Dispatchers.Default)

    /** 订阅任务。 */
    private var stateObserverJob: Job? = null

    // ==================== 生命周期回调 ====================

    /**
     * 服务首次创建时初始化通知渠道。
     */
    override fun onCreate() {
        super.onCreate()
        notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        createNotificationChannel()
    }

    /**
     * 每次 startService 调用时触发。
     *
     * 解析 Intent 中的 sessionId，构建前台通知并启动前台服务。
     */
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        sessionId = intent?.getLongExtra(EXTRA_SESSION_ID, 0L) ?: 0L

        // 启动前台服务
        val notification = buildNotification("专注进行中", "剩余时间计算中...")
        ServiceCompat.startForeground(
            this,
            NOTIFICATION_ID,
            notification,
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MICROPHONE
        )

        // 订阅状态更新
        observeSessionState()

        return START_STICKY
    }

    /**
     * Activity 可通过绑定获取 sessionId。
     */
    override fun onBind(intent: Intent?): IBinder = binder

    /**
     * 服务销毁时移除通知，取消订阅。
     */
    override fun onDestroy() {
        stateObserverJob?.cancel()
        notificationManager.cancel(NOTIFICATION_ID)
        super.onDestroy()
    }

    // ==================== 公开方法 ====================

    /**
     * 更新通知显示的剩余时间。
     *
     * @param remainingMillis 剩余毫秒数。
     */
    fun updateNotification(remainingMillis: Long) {
        val minutes = (remainingMillis / 1000 / 60).toInt()
        val seconds = ((remainingMillis / 1000) % 60).toInt()
        val timeText = String.format("%02d:%02d", minutes, seconds)

        val notification = buildNotification("专注进行中", "剩余 $timeText")
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 更新通知为暂停状态。
     */
    fun showPaused() {
        val notification = buildNotification("专注已暂停", "点击继续")
        notificationManager.notify(NOTIFICATION_ID, notification)
    }

    /**
     * 更新通知为运行状态。
     */
    fun showRunning(remainingMillis: Long) {
        updateNotification(remainingMillis)
    }

    // ==================== 私有方法 ====================

    /**
     * 订阅 SessionStateHolder 的状态更新。
     */
    private fun observeSessionState() {
        stateObserverJob?.cancel()
        stateObserverJob = serviceScope.launch {
            // 订阅剩余时间更新
            SessionStateHolder.remainingMillis.collect { remaining ->
                if (SessionStateHolder.isPaused.value) {
                    showPaused()
                } else if (remaining > 0) {
                    updateNotification(remaining)
                }
            }
        }
    }

    /**
     * 创建通知渠道（Android 8.0+ 必须）。
     *
     * 渠道设置为低重要性，避免发出声音干扰用户。
     */
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

    /**
     * 构建前台通知。
     *
     * @param title 通知标题。
     * @param content 通知内容。
     * @return 可直接用于 startForeground 的 Notification。
     */
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

        val isPaused = SessionStateHolder.isPaused.value

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
    }
}