package com.ywb.focusguard.data.repository

import android.util.Log
import com.ywb.focusguard.data.local.dao.DistractionDao
import com.ywb.focusguard.data.local.entity.DistractionEventEntity
import com.ywb.focusguard.data.system.AppUsageDataSource
import com.ywb.focusguard.domain.analyzer.DistractionAnalyzer
import com.ywb.focusguard.domain.model.ActiveInterval
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "DistractionRepository"

/**
 * 分心检测实现：查询系统使用记录 → DistractionAnalyzer 计算 → 写入 Room。
 *
 * 只在专注结束时执行一次，不在专注过程中轮询：UsageStatsManager 本身就会记录全部事件，
 * 事后回看一次即可得到完整结果，不需要后台常驻，也不额外耗电。
 */
@Singleton
class DistractionRepositoryImpl @Inject constructor(
    /** 系统使用记录。 */
    private val appUsageDataSource: AppUsageDataSource,
    /** 分心记录表。 */
    private val distractionDao: DistractionDao,
    /** 纯计算的分心分析器。 */
    private val distractionAnalyzer: DistractionAnalyzer
) : DistractionRepository {

    override fun hasPermission(): Boolean = appUsageDataSource.hasPermission()

    override suspend fun recordSessionDistractions(
        sessionId: Long,
        intervals: List<ActiveInterval>
    ): Boolean {
        if (intervals.isEmpty() || !appUsageDataSource.hasPermission()) return false
        return try {
            val events = appUsageDataSource.queryForegroundEvents(
                startMillis = intervals.minOf { it.start },
                endMillis = intervals.maxOf { it.end }
            )
            val episodes = distractionAnalyzer.detect(events, intervals, appUsageDataSource.ignoredPackages())
            // 同一个 App 只查一次名称，避免列表里有十几次分心就查十几次 PackageManager
            val labels = episodes.map { it.packageName }.distinct()
                .associateWith(appUsageDataSource::appLabel)
            if (episodes.isNotEmpty()) {
                distractionDao.insertAll(
                    episodes.map { episode ->
                        DistractionEventEntity(
                            sessionId = sessionId,
                            packageName = episode.packageName,
                            appLabel = labels[episode.packageName] ?: episode.packageName,
                            startTime = episode.start,
                            endTime = episode.end
                        )
                    }
                )
            }
            distractionDao.markSessionTracked(sessionId)
            true
        } catch (e: SecurityException) {
            // 授权可能在检查之后被用户撤销，此时按"未检测"处理，不影响专注结束
            Log.w(TAG, "读取使用记录被拒绝", e)
            false
        }
    }
}
