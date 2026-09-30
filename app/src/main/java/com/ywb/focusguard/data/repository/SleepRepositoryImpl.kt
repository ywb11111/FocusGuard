package com.ywb.focusguard.data.repository

import com.ywb.focusguard.data.local.dao.SleepDao
import com.ywb.focusguard.data.local.entity.SleepSessionEntity
import com.ywb.focusguard.data.local.mapper.toDomain
import com.ywb.focusguard.data.local.mapper.toEntity
import com.ywb.focusguard.domain.analyzer.SleepAnalyzer
import com.ywb.focusguard.domain.model.SleepDetail
import com.ywb.focusguard.domain.model.SleepEpoch
import com.ywb.focusguard.domain.model.SleepPlacement
import com.ywb.focusguard.domain.model.SleepSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 睡眠数据的 Room 实现。
 *
 * 职责划分：
 * - 写：开始插入主记录 → 每分钟插入 epoch → 结束时调用 SleepAnalyzer 汇总并回写主记录。
 * - 读：列表只读主记录；详情页读取分钟数据后实时重算分析结果（算法是确定的，重算比存储每分钟状态更灵活，
 *   以后调参后旧记录的阶段图也会跟着更新）。
 */
@Singleton
class SleepRepositoryImpl @Inject constructor(
    /** 睡眠两张表的读写入口。 */
    private val sleepDao: SleepDao,
    /** 纯计算的睡眠分析器。 */
    private val sleepAnalyzer: SleepAnalyzer
) : SleepRepository {

    override fun observeActiveSession(): Flow<SleepSession?> =
        sleepDao.observeActiveSession().map { it?.toDomain() }

    override fun observeSessions(): Flow<List<SleepSession>> =
        sleepDao.observeFinishedSessions().map { list -> list.map { it.toDomain() } }

    /** combine 主记录和分钟数据两路 Flow，任意一路变化都会重新计算详情。 */
    override fun observeSleepDetail(sessionId: Long): Flow<SleepDetail?> =
        combine(
            sleepDao.observeSession(sessionId),
            sleepDao.observeEpochs(sessionId)
        ) { entity, epochEntities ->
            val session = entity?.toDomain() ?: return@combine null
            val epochs = epochEntities.map { it.toDomain() }
            SleepDetail(
                session = session,
                epochs = epochs,
                analysis = sleepAnalyzer.analyze(epochs, session.placement, session.audioEnabled)
            )
        }

    override suspend fun getActiveSession(): SleepSession? = sleepDao.getActiveSession()?.toDomain()

    override suspend fun startSession(placement: SleepPlacement, audioEnabled: Boolean): Long =
        sleepDao.insertSession(
            SleepSessionEntity(
                startTime = System.currentTimeMillis(),
                endTime = null,
                placement = placement.name,
                audioEnabled = audioEnabled,
                sleepOnsetTime = null,
                finalWakeTime = null,
                monitoredMillis = 0L,
                totalSleepMillis = 0L,
                sleepLatencyMillis = 0L,
                wakeCount = 0,
                snoreEventCount = 0,
                averageNoiseDb = 0f,
                averageLightLux = 0f,
                score = 0,
                note = null
            )
        )

    override suspend fun saveEpoch(sessionId: Long, epoch: SleepEpoch) {
        sleepDao.insertEpoch(epoch.toEntity(sessionId))
    }

    /**
     * 结束记录。
     *
     * 入睡/醒来时间由分析结果的 epoch 下标换算：第 i 分钟的开始时间就是 epochs[i].startTime，
     * 醒来时间取最后一个睡眠分钟的结束时间。
     */
    override suspend fun finishSession(sessionId: Long, endTime: Long, note: String?): SleepSession? {
        val entity = sleepDao.getSession(sessionId) ?: return null
        val placement = SleepPlacement.fromName(entity.placement)
        val epochs = sleepDao.getEpochs(sessionId).map { it.toDomain() }
        val analysis = sleepAnalyzer.analyze(epochs, placement, entity.audioEnabled)

        val finished = entity.copy(
            endTime = endTime.coerceAtLeast(entity.startTime),
            sleepOnsetTime = epochs.getOrNull(analysis.sleepOnsetIndex)?.startTime,
            finalWakeTime = epochs.getOrNull(analysis.finalWakeIndex)?.startTime
                ?.plus(SleepAnalyzer.EPOCH_MILLIS),
            monitoredMillis = analysis.monitoredMillis,
            totalSleepMillis = analysis.totalSleepMillis,
            sleepLatencyMillis = analysis.sleepLatencyMillis,
            wakeCount = analysis.wakeCount,
            snoreEventCount = analysis.snoreEventCount,
            averageNoiseDb = analysis.averageNoiseDb,
            averageLightLux = analysis.averageLightLux,
            score = analysis.score.total,
            note = note
        )
        sleepDao.updateSession(finished)
        return finished.toDomain()
    }

    override suspend fun finishInterruptedSession(sessionId: Long): SleepSession? {
        val entity = sleepDao.getSession(sessionId) ?: return null
        val lastEpochEnd = sleepDao.getEpochs(sessionId).lastOrNull()
            ?.startTime
            ?.plus(SleepAnalyzer.EPOCH_MILLIS)
        return finishSession(
            sessionId = sessionId,
            endTime = lastEpochEnd ?: entity.startTime,
            note = INTERRUPTED_NOTE
        )
    }

    private companion object {
        /** 中断记录的统一说明，详情页直接展示。 */
        const val INTERRUPTED_NOTE = "记录被系统中断，报告只包含中断前的数据"
    }
}
