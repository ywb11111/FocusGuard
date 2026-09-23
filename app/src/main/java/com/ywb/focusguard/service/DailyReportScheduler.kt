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
 * 每日总结的调度边界。
 *
 * UI 只表达“是否开启”，这里集中处理 WorkManager 的唯一任务名称、触发时间和取消逻辑，
 * 防止用户反复切换开关时创建多份重复任务。
 */
@Singleton
class DailyReportScheduler @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    fun setEnabled(enabled: Boolean) {
        val workManager = WorkManager.getInstance(context)
        if (!enabled) {
            workManager.cancelUniqueWork(UNIQUE_WORK_NAME)
            return
        }

        val request = PeriodicWorkRequestBuilder<DailyReportWorker>(24, TimeUnit.HOURS)
            .setInitialDelay(delayUntilNextReport(), TimeUnit.MILLISECONDS)
            .build()
        workManager.enqueueUniquePeriodicWork(
            UNIQUE_WORK_NAME,
            ExistingPeriodicWorkPolicy.REPLACE,
            request
        )
    }

    /** 计算到下一次本地时间 21:00 的延迟，避免首次开启后立刻弹出“日报”。 */
    private fun delayUntilNextReport(nowMillis: Long = System.currentTimeMillis()): Long {
        val next = Calendar.getInstance().apply {
            timeInMillis = nowMillis
            set(Calendar.HOUR_OF_DAY, REPORT_HOUR)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            if (timeInMillis <= nowMillis) add(Calendar.DAY_OF_YEAR, 1)
        }
        return next.timeInMillis - nowMillis
    }

    companion object {
        private const val UNIQUE_WORK_NAME = "focusguard_daily_report"
        private const val REPORT_HOUR = 21
    }
}
