package com.ywb.focusguard.service

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import dagger.hilt.android.qualifiers.ApplicationContext
import java.util.Calendar
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 每周洞察的调度边界，写法与 DailyReportScheduler 一致：唯一任务名 + 首次延迟到下一个触发时刻。
 *
 * 与日报调度的区别：App 每次启动都会调用 [ensureScheduled]，使用 KEEP 策略——
 * 已经排好的任务保持原计划不动，不会因为反复打开 App 而推迟或重复。
 */
@Singleton
class WeeklyInsightScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /** App 启动时调用：开关开启且尚未排期时才创建任务。 */
    fun ensureScheduled(enabled: Boolean) {
        if (!enabled) {
            cancel()
            return
        }
        enqueue(ExistingPeriodicWorkPolicy.KEEP)
    }

    /**
     * 用户切换"每日总结"开关时调用：开启时重新排期，关闭时取消。
     * 用 REPLACE 而不是 UPDATE：UPDATE 需要 WorkManager 2.8+，项目当前是 2.7.1。
     */
    fun setEnabled(enabled: Boolean) {
        if (enabled) enqueue(ExistingPeriodicWorkPolicy.REPLACE) else cancel()
    }

    private fun enqueue(policy: ExistingPeriodicWorkPolicy) {
        val request = PeriodicWorkRequestBuilder<WeeklyInsightWorker>(7, TimeUnit.DAYS)
            .setInitialDelay(delayUntilNextSundayEvening(), TimeUnit.MILLISECONDS)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(UNIQUE_WORK_NAME, policy, request)
    }

    private fun cancel() {
        WorkManager.getInstance(context).cancelUniqueWork(UNIQUE_WORK_NAME)
    }

    /** 计算到下一个周日 20:00 的延迟。 */
    private fun delayUntilNextSundayEvening(nowMillis: Long = System.currentTimeMillis()): Long {
        val next = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.DAY_OF_WEEK, Calendar.SUNDAY)
            set(Calendar.HOUR_OF_DAY, REPORT_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            while (timeInMillis <= nowMillis) add(Calendar.WEEK_OF_YEAR, 1)
        }
        return next.timeInMillis - nowMillis
    }

    private companion object {
        const val UNIQUE_WORK_NAME = "focusguard_weekly_insight"
        const val REPORT_HOUR = 20
    }
}
