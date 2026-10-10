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
import com.ywb.focusguard.data.repository.SleepRepository
import com.ywb.focusguard.domain.analyzer.InsightAnalyzer
import com.ywb.focusguard.domain.model.InsightKind
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first

/**
 * 每周洞察推送：每周日晚上用最近 28 天的数据生成"睡眠 × 专注"洞察，挑最有价值的一条推送。
 *
 * 复用"每日总结"开关：两者都是回顾类通知，用户关掉日报时一并不推送，避免多一个设置项。
 * 依赖获取方式与 DailyReportWorker 相同（Hilt EntryPoint），统计口径与报告页完全一致。
 */
class WeeklyInsightWorker(
    appContext: Context,
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {

    override suspend fun doWork(): Result {
        val dependencies = EntryPointAccessors.fromApplication(applicationContext, WeeklyInsightDependencies::class.java)
        // 任务可能在用户关闭开关之前就进入了执行队列，执行前再确认一次
        if (!dependencies.settingsRepository().settings.first().dailyReportEnabled) return Result.success()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(applicationContext, Manifest.permission.POST_NOTIFICATIONS) !=
            PackageManager.PERMISSION_GRANTED
        ) {
            return Result.success()
        }

        val now = System.currentTimeMillis()
        val rangeStart = now - LOOKBACK_MILLIS
        val focusSessions = dependencies.focusRepository().getSessionsInRange(rangeStart, now)
        val sleepSessions = dependencies.sleepRepository().observeSessions().first().filter { it.startTime >= rangeStart }
        val insights = dependencies.insightAnalyzer().analyze(focusSessions, sleepSessions, now)
        // 优先推送有结论的洞察；只有"数据不足"时推送本周记录概况，鼓励继续记录
        val top = insights.firstOrNull { it.kind != InsightKind.NEED_MORE_DATA }
        val weekStart = now - WEEK_MILLIS
        val title = top?.title ?: "本周记录回顾"
        val content = top?.detail
            ?: "本周专注 ${focusSessions.count { it.startTime >= weekStart }} 次 · " +
            "睡眠记录 ${sleepSessions.count { it.startTime >= weekStart }} 晚，继续记录可以解锁睡眠与专注的关联洞察"

        createNotificationChannel()
        val openAppIntent = PendingIntent.getActivity(
            applicationContext,
            0,
            Intent(applicationContext, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(applicationContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(content)
            .setStyle(NotificationCompat.BigTextStyle().bigText(content))
            .setContentIntent(openAppIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .build()
        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    /** 重复创建同一渠道是幂等的。 */
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(
                NotificationChannel(CHANNEL_ID, "每周洞察", NotificationManager.IMPORTANCE_DEFAULT).apply {
                    description = "每周回顾睡眠与专注的关联"
                }
            )
        }
    }

    companion object {
        const val CHANNEL_ID = "weekly_insight"

        /** 与 1001~1004 区分。 */
        const val NOTIFICATION_ID = 1005

        private const val LOOKBACK_MILLIS = 28 * 24 * 60 * 60 * 1000L
        private const val WEEK_MILLIS = 7 * 24 * 60 * 60 * 1000L
    }
}

/** Worker 不由 Hilt 构造，通过受限 EntryPoint 只暴露需要的依赖。 */
@EntryPoint
@InstallIn(SingletonComponent::class)
interface WeeklyInsightDependencies {
    fun focusRepository(): FocusRepository
    fun sleepRepository(): SleepRepository
    fun settingsRepository(): SettingsRepository
    fun insightAnalyzer(): InsightAnalyzer
}
