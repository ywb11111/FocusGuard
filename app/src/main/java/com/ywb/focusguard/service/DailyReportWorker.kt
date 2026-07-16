package com.ywb.focusguard.service

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/**
 * 每日总结后台任务的占位实现。
 * 后续由 WorkManager 定时触发，读取 Room 聚合数据并生成日报；当前只返回成功。
 */
class DailyReportWorker(
    /** WorkManager 提供的应用上下文。 */
    appContext: Context,
    /** 本次任务的输入参数、重试次数等运行信息。 */
    params: WorkerParameters
) : CoroutineWorker(appContext, params) {
    /** 当前未执行真实业务，阶段 6 接入日报生成后再返回实际结果。 */
    override suspend fun doWork(): Result = Result.success()
}
