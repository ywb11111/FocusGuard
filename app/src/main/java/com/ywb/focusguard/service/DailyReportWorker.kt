package com.ywb.focusguard.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.ywb.focusguard.MainActivity
import com.ywb.focusguard.R
import com.ywb.focusguard.data.repository.FocusRepository
import com.ywb.focusguard.data.repository.SettingsRepository
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import java.util.Calendar

/**
 * 每日总结后台任务。
 *
 * WorkManager 负责跨进程重启保留调度；Worker 通过 Hilt EntryPoint 取得现有 Repository，
 * 因而统计口径与报告页完全一致，也不会在后台重复打开一套数据库。
 */
class DailyReportWorker(
    /** WorkManager 提供的应用上下文。 */
    appContext: Context,
    /** 本次任务的输入参数、重试次数等运行信息。 */
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(
            applicationContext,
            DailyReportDependencies::class.java
        )
        // 任务可能已经进入执行队列后用户才关闭开关，因此执行前再次读取设置。
        if (!dependencies.settingsRepository().settings.first().dailyReportEnabled) {
            return Result.success()
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val dayStart = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
        val sessions = dependencies.focusRepository()
            .getSessionsInRange(dayStart, dayStart + DAY_MILLIS)
        val totalMinutes = sessions.sumOf { it.durationMillis } / 60_000L
        val averageScore = sessions.map { it.score }.takeIf { it.isNotEmpty() }?.average()?.toInt()

        createNotificationChannel()
        val openAppIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val content = if (sessions.isEmpty()) {
            "今天还没有完成专注，明天从一次短专注开始吧"
        } else {
            "完成 ${sessions.size} 次 · ${totalMinutes} 分钟 · 平均 ${averageScore ?: 0} 分"
        }
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("今日专注总结")
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    /** Android 8+ 必须先创建渠道；重复创建同一 ID 是幂等操作。 */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = applicationContext.getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "每日总结", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "每天回顾专注时长与环境表现"
                }
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "daily_focus_report"
        const val NOTIFICATION_ID = 1002
        private const val DAY_MILLIS = 24 * 60 * 60 * 1000L
    }
}

/** Worker 不是由 Hilt 直接构造，因此用受限 EntryPoint 只暴露任务真正需要的依赖。 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface DailyReportDependencies {
    fun focusRepository(): FocusRepository
    fun settingsRepository(): SettingsRepository
}
