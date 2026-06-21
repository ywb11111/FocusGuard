package com.ywb.focusguard.data.repository

import com.ywb.focusguard.data.local.dao.FocusSessionDao
import com.ywb.focusguard.data.local.entity.FocusSessionEntity
import com.ywb.focusguard.data.local.mapper.toDomain
import com.ywb.focusguard.domain.analyzer.FocusScoreAnalyzer
import com.ywb.focusguard.domain.model.FocusConfig
import com.ywb.focusguard.domain.model.FocusScore
import com.ywb.focusguard.domain.model.FocusSession
import com.ywb.focusguard.domain.model.LightLevel
import com.ywb.focusguard.domain.model.LightSample
import com.ywb.focusguard.domain.model.MotionSample
import com.ywb.focusguard.domain.model.NoiseLevel
import com.ywb.focusguard.domain.model.NoiseSample
import com.ywb.focusguard.domain.model.SessionDetail
import com.ywb.focusguard.domain.model.TodaySummary
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusRepositoryImpl @Inject constructor(
    private val focusSessionDao: FocusSessionDao,
    private val scoreAnalyzer: FocusScoreAnalyzer
) : FocusRepository {
    // 今日统计先在 Repository 中由 Room Flow 派生，后续数据量变大后可以下沉为 Room 聚合 SQL。
    override fun observeTodaySummary(): Flow<TodaySummary> = observeSessions().map { items ->
        val todayStart = todayStartMillis()
        val todaySessions = items.filter { it.startTime >= todayStart && it.endTime != null }
        TodaySummary(
            totalFocusMillis = todaySessions.sumOf { it.durationMillis },
            averageScore = todaySessions.map { it.score }.average().takeIf { !it.isNaN() }?.toInt() ?: 0,
            distractionCount = todaySessions.sumOf { it.distractionCount },
            sessionCount = todaySessions.size
        )
    }

    override fun observeSessions(): Flow<List<FocusSession>> =
        focusSessionDao.observeSessions().map { entities ->
            entities
                .filter { it.endTime != null }
                .map { it.toDomain() }
        }

    // 详情页先读取真实会话记录，采样曲线仍使用 demo 样本；传感器阶段再替换为采样表查询。
    override fun observeSessionDetail(sessionId: Long): Flow<SessionDetail?> =
        focusSessionDao.observeSession(sessionId).map { entity ->
            val session = entity?.toDomain() ?: return@map null
            val noiseSamples = demoNoiseSamples(session.startTime)
            val lightSamples = demoLightSamples(session.startTime)
            val motionEvents = demoMotionSamples(session.startTime)
            SessionDetail(
                session = session,
                noiseSamples = noiseSamples,
                lightSamples = lightSamples,
                motionEvents = motionEvents,
                score = FocusScore(
                    total = session.score,
                    noisePenalty = 8,
                    lightPenalty = 0,
                    motionPenalty = 3,
                    distractionPenalty = 5,
                    suggestions = listOf("整体环境稳定，下次可以尝试延长到 45 分钟。")
                )
            )
        }

    override suspend fun startSession(config: FocusConfig): Long {
        val now = System.currentTimeMillis()
        // 进行中记录先把 endTime 设为 null，其他分析字段使用默认值。
        // 结束时 Repository 会用同一个 id 更新完整结果。
        return focusSessionDao.insertSession(
            FocusSessionEntity(
                startTime = now,
                endTime = null,
                durationMillis = 0L,
                averageNoiseDb = 0f,
                maxNoiseDb = 0f,
                averageLightLux = 0f,
                movementCount = 0,
                distractionCount = 0,
                score = 0,
                note = "进行中"
            )
        )
    }

    override suspend fun finishSession(sessionId: Long, durationMillis: Long): FocusSession {
        val finishedAt = System.currentTimeMillis()
        val existing = focusSessionDao.getSession(sessionId)
        val startedAt = existing?.startTime ?: finishedAt
        val focusDurationMillis = durationMillis.coerceAtLeast(0L)
        val score = scoreAnalyzer.calculate(
            averageNoiseDb = 44f,
            averageLightLux = 220f,
            movementCount = 1,
            distractionCount = 0,
            durationMillis = focusDurationMillis
        )
        focusSessionDao.updateFinishedSession(
            sessionId = sessionId,
            endTime = finishedAt,
            durationMillis = focusDurationMillis,
            averageNoiseDb = 44f,
            maxNoiseDb = 60f,
            averageLightLux = 220f,
            movementCount = 1,
            distractionCount = 0,
            score = score.total,
            note = "手动结束"
        )
        return FocusSession(
            id = sessionId,
            startTime = startedAt,
            endTime = finishedAt,
            durationMillis = focusDurationMillis,
            averageNoiseDb = 44f,
            maxNoiseDb = 60f,
            averageLightLux = 220f,
            movementCount = 1,
            distractionCount = 0,
            score = score.total,
            note = "手动结束"
        )
    }

    // 采样保存接口先留空，后续接入光照、移动、噪声时逐步实现。
    override suspend fun saveNoiseSample(sessionId: Long, sample: NoiseSample) = Unit
    override suspend fun saveLightSample(sessionId: Long, sample: LightSample) = Unit
    override suspend fun saveMotionEvent(sessionId: Long, event: MotionSample) = Unit

    private fun demoNoiseSamples(start: Long): List<NoiseSample> = List(12) { index ->
        NoiseSample(
            timestamp = start + index * 60_000L,
            decibel = 38f + (index % 4) * 4f,
            level = if (index % 5 == 0) NoiseLevel.NORMAL else NoiseLevel.QUIET
        )
    }

    private fun demoLightSamples(start: Long): List<LightSample> = List(12) { index ->
        LightSample(
            timestamp = start + index * 60_000L,
            lux = 160f + (index % 3) * 35f,
            level = LightLevel.COMFORTABLE
        )
    }

    private fun demoMotionSamples(start: Long): List<MotionSample> = listOf(
        MotionSample(start + 12 * 60_000L, 11.2f, true),
        MotionSample(start + 31 * 60_000L, 10.6f, true)
    )

    private fun todayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}
