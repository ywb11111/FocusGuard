package com.ywb.focusguard.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import com.ywb.focusguard.R
import com.ywb.focusguard.ui.alarm.AlarmActivity
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

private const val TAG = "AlarmRingService"

/**
 * 闹钟响铃前台服务：播放铃声、振动，并通过全屏通知在锁屏上弹出 AlarmActivity。
 *
 * **为什么响铃要单独一个服务，不复用 SleepMonitorService**：
 * 闹钟必须在睡眠服务已经被系统杀掉的情况下也能响。AlarmManager → SleepAlarmReceiver → 本服务，
 * 这条链路完全不依赖睡眠服务是否存活。
 *
 * **前台服务类型**：铃声播放属于 mediaPlayback；精确闹钟触发后系统允许从后台启动前台服务。
 *
 * **铃声走闹钟音频流（USAGE_ALARM）**：手机静音、勿扰时闹钟流默认仍会响，
 * 音量由系统"闹钟音量"控制，和媒体音量互不影响。
 *
 * **生命周期**：ring → 开始响铃；dismiss / snooze / 10 分钟超时 → 停止铃声和振动 → stopSelf；
 * onDestroy 兜底释放 MediaPlayer 和振动器。
 */
@AndroidEntryPoint
class AlarmRingService : Service() {

    companion object {
        private const val ACTION_RING = "com.ywb.focusguard.action.ALARM_RING"
        private const val ACTION_DISMISS = "com.ywb.focusguard.action.ALARM_DISMISS"
        private const val ACTION_SNOOZE = "com.ywb.focusguard.action.ALARM_SNOOZE"
        private const val EXTRA_SMART_EARLY = "smart_early"

        /** 通知渠道：重要性 HIGH 才能弹出全屏 / 悬浮通知。 */
        const val CHANNEL_ID = "sleep_alarm_channel"

        /** 通知 ID，与 1001 专注、1002 日报、1003 睡眠监测区分。 */
        const val NOTIFICATION_ID = 1004

        /** 贪睡时长。 */
        const val SNOOZE_MILLIS = 9 * 60_000L

        /** 无人操作时自动停止响铃的时长，避免人不在时一直响到没电。 */
        private const val AUTO_SILENCE_MILLIS = 10 * 60_000L

        /** 由 SleepAlarmReceiver 调用，开始响铃。 */
        fun ring(context: Context, smartEarly: Boolean) {
            val intent = Intent(context, AlarmRingService::class.java).apply {
                action = ACTION_RING
                putExtra(EXTRA_SMART_EARLY, smartEarly)
            }
            ContextCompat.startForegroundService(context, intent)
        }

        /** 关闭闹钟并结束睡眠记录。只能在前台（响铃页、睡眠页）调用。 */
        fun dismiss(context: Context) {
            context.startService(Intent(context, AlarmRingService::class.java).setAction(ACTION_DISMISS))
        }

        /** 贪睡：停止响铃，[SNOOZE_MILLIS] 后再响。只能在前台调用。 */
        fun snooze(context: Context) {
            context.startService(Intent(context, AlarmRingService::class.java).setAction(ACTION_SNOOZE))
        }
    }

    @Inject lateinit var alarmScheduler: SleepAlarmScheduler

    /** 播放铃声的 MediaPlayer；null 表示没有在播放。 */
    private var player: MediaPlayer? = null

    /** 当前振动器；停止时要显式 cancel，否则会一直振。 */
    private var vibrator: Vibrator? = null

    /** 主线程 Handler，用于 10 分钟自动停止。 */
    private val handler = Handler(Looper.getMainLooper())

    /** 自动停止任务。 */
    private val autoSilence = Runnable {
        Log.d(TAG, "响铃超时，自动停止")
        stopRingingAndSelf()
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_RING -> startRinging(intent.getBooleanExtra(EXTRA_SMART_EARLY, false))
            ACTION_SNOOZE -> {
                alarmScheduler.schedule(System.currentTimeMillis() + SNOOZE_MILLIS, smartEarly = false)
                stopRingingAndSelf()
            }
            ACTION_DISMISS -> {
                alarmScheduler.cancel()
                // 起床了：顺便结束睡眠记录并生成报告
                if (SleepStateHolder.serviceRunning.value) SleepMonitorService.stop(this)
                stopRingingAndSelf()
            }
            else -> stopSelf()
        }
        // 被系统杀掉后不重建：重建时 intent 为 null，没有可恢复的响铃上下文
        return START_NOT_STICKY
    }

    override fun onDestroy() {
        handler.removeCallbacks(autoSilence)
        releaseSoundAndVibration()
        AlarmStateHolder.stopRinging()
        super.onDestroy()
    }

    /** 进入前台、播放铃声、开始振动。重复触发时只刷新通知，不叠加播放。 */
    private fun startRinging(smartEarly: Boolean) {
        // 先写状态再发通知：全屏 Intent 拉起 AlarmActivity 时，它读到的一定是"正在响铃"
        AlarmStateHolder.startRinging(smartEarly)
        createNotificationChannel()
        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
        } else {
            0
        }
        try {
            ServiceCompat.startForeground(this, NOTIFICATION_ID, buildNotification(smartEarly), type)
        } catch (e: RuntimeException) {
            // 理论上精确闹钟触发后不会失败；万一失败也继续响铃，宁可没有通知也不能不响
            Log.e(TAG, "响铃服务进入前台失败", e)
        }
        if (player == null) startSound()
        if (vibrator == null) startVibration()
        handler.removeCallbacks(autoSilence)
        handler.postDelayed(autoSilence, AUTO_SILENCE_MILLIS)
    }

    /** 按 用户设置的闹钟铃声 → 系统默认闹钟 → 系统默认铃声 的顺序选择音源，全部失败时只振动。 */
    private fun startSound() {
        val uri = RingtoneManager.getActualDefaultRingtoneUri(this, RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_RINGTONE)
        player = try {
            MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build()
                )
                setDataSource(this@AlarmRingService, uri)
                isLooping = true
                prepare()
                start()
            }
        } catch (e: Exception) {
            Log.e(TAG, "铃声播放失败，只振动", e)
            null
        }
    }

    /** 循环振动：0ms 后振 800ms，停 600ms，从下标 0 开始重复。 */
    private fun startVibration() {
        val target = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        val pattern = longArrayOf(0, 800, 600)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            target.vibrate(VibrationEffect.createWaveform(pattern, 0))
        } else {
            @Suppress("DEPRECATION")
            target.vibrate(pattern, 0)
        }
        vibrator = target
    }

    /** 停止铃声和振动，退出前台并停止服务。 */
    private fun stopRingingAndSelf() {
        handler.removeCallbacks(autoSilence)
        releaseSoundAndVibration()
        AlarmStateHolder.stopRinging()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    /** 释放 MediaPlayer：不 release 会一直占用音频解码资源。 */
    private fun releaseSoundAndVibration() {
        player?.let {
            runCatching { it.stop() }
            it.release()
        }
        player = null
        vibrator?.cancel()
        vibrator = null
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(CHANNEL_ID, "智能闹钟", NotificationManager.IMPORTANCE_HIGH).apply {
                description = "起床闹钟响铃"
                // 铃声和振动由服务自己控制，通知本身保持安静，避免叠加两份声音
                setSound(null, null)
                enableVibration(false)
            }
            getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    /**
     * 响铃通知。setFullScreenIntent：锁屏或熄屏时直接全屏打开 AlarmActivity；
     * 亮屏使用其他 App 时显示为悬浮通知。Android 14+ 需要用户允许"全屏通知"，否则只显示悬浮通知。
     */
    private fun buildNotification(smartEarly: Boolean): Notification {
        val fullScreenIntent = PendingIntent.getActivity(
            this,
            30,
            Intent(this, AlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val snoozeIntent = PendingIntent.getService(
            this,
            31,
            Intent(this, AlarmRingService::class.java).setAction(ACTION_SNOOZE),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val dismissIntent = PendingIntent.getService(
            this,
            32,
            Intent(this, AlarmRingService::class.java).setAction(ACTION_DISMISS),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(if (smartEarly) "检测到浅睡，现在起床正好" else "起床时间到")
            .setContentText("关闭闹钟会同时结束睡眠记录")
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setFullScreenIntent(fullScreenIntent, true)
            .setContentIntent(fullScreenIntent)
            .setOngoing(true)
            .addAction(NotificationCompat.Action(0, "贪睡 9 分钟", snoozeIntent))
            .addAction(NotificationCompat.Action(0, "起床", dismissIntent))
            .build()
    }
}
