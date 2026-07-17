package com.ywb.focusguard.data.repository

import com.ywb.focusguard.data.local.dao.FocusSessionDao
import com.ywb.focusguard.data.local.dao.SampleDao
import com.ywb.focusguard.data.local.entity.FocusSessionEntity
import com.ywb.focusguard.data.local.entity.LightSampleEntity
import com.ywb.focusguard.data.local.entity.MotionEventEntity
import com.ywb.focusguard.data.local.entity.NoiseSampleEntity
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
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.util.Calendar
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 专注数据的 Room 实现，负责会话生命周期、采样保存和领域模型转换。
 *
 * 会话主记录已接入 Room，光照和移动采样已切换为真实数据库数据。
 */
@Singleton
class FocusRepositoryImpl @Inject constructor(
    /** 负责 focus_sessions 表的查询与写入。 */
    private val focusSessionDao: FocusSessionDao,
    /** 负责采样表的查询与写入。 */
    private val sampleDao: SampleDao,
    /** 结束会话时根据聚合指标生成可解释评分。 */
    private val scoreAnalyzer: FocusScoreAnalyzer
) : FocusRepository {

    // ==================== 查询接口 ====================

    /** 今日统计由 Room Flow 派生。 */
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

    /** 按开始时间倒序观察全部已完成专注会话。 */
    override fun observeSessions(): Flow<List<FocusSession>> =
        focusSessionDao.observeSessions().map { entities ->
            entities.filter { it.endTime != null }.map { it.toDomain() }
        }

    /**
     * 观察会话详情：组合主记录和采样数据。
     *
     * 使用 combine 同时观察主记录和采样表，任一变化都会更新详情页。
     */
    override fun observeSessionDetail(sessionId: Long): Flow<SessionDetail?> =
        combine(
            focusSessionDao.observeSession(sessionId),
            sampleDao.observeNoiseSamples(sessionId),
            sampleDao.observeLightSamples(sessionId),
            sampleDao.observeMotionEvents(sessionId)
        ) { entity, noiseEntities, lightEntities, motionEntities ->
            val session = entity?.toDomain() ?: return@combine null

            // 噪声曲线：从采样表转换为领域模型
            val noiseSamples = noiseEntities.map { entity ->
                NoiseSample(
                    timestamp = entity.timestamp,
                    decibel = entity.decibel,
                    level = classifyNoiseLevel(entity.decibel)
                )
            }

            // 光照曲线：从采样表转换为领域模型
            val lightSamples = lightEntities.map { entity ->
                LightSample(
                    timestamp = entity.timestamp,
                    lux = entity.lux,
                    level = classifyLightLevel(entity.lux)
                )
            }

            // 移动事件：从采样表转换为领域模型
            val motionEvents = motionEntities.map { entity ->
                MotionSample(
                    timestamp = entity.timestamp,
                    magnitude = entity.magnitude,
                    isSignificantMove = true,
                    isMoving = true
                )
            }

            SessionDetail(
                session = session,
                noiseSamples = noiseSamples,
                lightSamples = lightSamples,
                motionEvents = motionEvents,
                score = FocusScore(
                    total = session.score,
                    noisePenalty = calculateNoisePenalty(noiseSamples),
                    lightPenalty = 0,
                    motionPenalty = motionEvents.size * 3,
                    distractionPenalty = 5,
                    suggestions = generateSuggestions(session, lightSamples, motionEvents)
                )
            )
        }

    // ==================== 会话生命周期 ====================

    /** 在 Room 创建一条进行中记录，返回自增 id。 */
    override suspend fun startSession(config: FocusConfig): Long {
        val now = System.currentTimeMillis()
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

    /** 结束会话：从采样表聚合统计，更新主记录并返回完整结果。 */
    override suspend fun finishSession(sessionId: Long, durationMillis: Long): FocusSession {
        val finishedAt = System.currentTimeMillis()
        val existing = focusSessionDao.getSession(sessionId)
        val startedAt = existing?.startTime ?: finishedAt

        // 从采样表聚合统计数据
        val noiseSamples = sampleDao.getNoiseSamplesOnce(sessionId)
        val lightSamples = sampleDao.getLightSamplesOnce(sessionId)
        val motionEvents = sampleDao.getMotionEventsOnce(sessionId)

        val averageNoiseDb = if (noiseSamples.isNotEmpty()) {
            noiseSamples.map { it.decibel }.average().toFloat()
        } else 0f

        val maxNoiseDb = if (noiseSamples.isNotEmpty()) {
            noiseSamples.maxOf { it.decibel }
        } else 0f

        val averageLightLux = if (lightSamples.isNotEmpty()) {
            lightSamples.map { it.lux }.average().toFloat()
        } else 0f

        val movementCount = motionEvents.size

        // 计算评分
        val score = scoreAnalyzer.calculate(
            averageNoiseDb = averageNoiseDb,
            averageLightLux = averageLightLux,
            movementCount = movementCount,
            distractionCount = 0,
            durationMillis = durationMillis
        )

        // 更新主记录
        focusSessionDao.updateFinishedSession(
            sessionId = sessionId,
            endTime = finishedAt,
            durationMillis = durationMillis,
            averageNoiseDb = averageNoiseDb,
            maxNoiseDb = maxNoiseDb,
            averageLightLux = averageLightLux,
            movementCount = movementCount,
            distractionCount = 0,
            score = score.total,
            note = "手动结束"
        )

        return FocusSession(
            id = sessionId,
            startTime = startedAt,
            endTime = finishedAt,
            durationMillis = durationMillis,
            averageNoiseDb = averageNoiseDb,
            maxNoiseDb = maxNoiseDb,
            averageLightLux = averageLightLux,
            movementCount = movementCount,
            distractionCount = 0,
            score = score.total,
            note = "手动结束"
        )
    }

    // ==================== 采样保存接口 ====================

    /** 保存一条光照采样到数据库。 */
    override suspend fun saveLightSample(sessionId: Long, sample: LightSample) {
        sampleDao.insertLightSample(
            LightSampleEntity(
                sessionId = sessionId,
                timestamp = sample.timestamp,
                lux = sample.lux
            )
        )
    }

    /** 保存一次已确认的移动事件到数据库。 */
    override suspend fun saveMotionEvent(sessionId: Long, event: MotionSample) {
        sampleDao.insertMotionEvent(
            MotionEventEntity(
                sessionId = sessionId,
                timestamp = event.timestamp,
                magnitude = event.magnitude
            )
        )
    }

    /** 噪声采样保存到数据库。 */
    override suspend fun saveNoiseSample(sessionId: Long, sample: NoiseSample) {
        sampleDao.insertNoiseSample(
            NoiseSampleEntity(
                sessionId = sessionId,
                timestamp = sample.timestamp,
                decibel = sample.decibel
            )
        )
    }

    // ==================== 私有方法 ====================

    /** 根据 lux 值分类光照等级。 */
    private fun classifyLightLevel(lux: Float): LightLevel = when {
        lux < 10f -> LightLevel.DARK
        lux < 100f -> LightLevel.DIM
        lux < 500f -> LightLevel.COMFORTABLE
        else -> LightLevel.BRIGHT
    }

    /** 根据 dB 值分类噪声等级。 */
    private fun classifyNoiseLevel(decibel: Float): NoiseLevel = when {
        decibel < 40f -> NoiseLevel.QUIET
        decibel < 60f -> NoiseLevel.NORMAL
        decibel < 75f -> NoiseLevel.NOISY
        else -> NoiseLevel.LOUD
    }

    /** 计算噪声扣分：基于平均噪声等级。 */
    private fun calculateNoisePenalty(samples: List<NoiseSample>): Int {
        if (samples.isEmpty()) return 0
        val avgDb = samples.map { it.decibel }.average()
        return when {
            avgDb > 75f -> 25
            avgDb > 65f -> 15
            avgDb > 55f -> 8
            else -> 0
        }
    }

    /** 生成个性化建议。 */
    private fun generateSuggestions(
        session: FocusSession,
        lightSamples: List<LightSample>,
        motionEvents: List<MotionSample>
    ): List<String> {
        val suggestions = mutableListOf<String>()

        // 基于移动次数
        when {
            motionEvents.size > 5 -> suggestions.add("本次专注移动较频繁，建议将手机放在固定位置。")
            motionEvents.size > 2 -> suggestions.add("有 ${motionEvents.size} 次移动，环境整体稳定。")
            else -> suggestions.add("专注期间几乎没有移动，表现很好！")
        }

        // 基于光照
        if (lightSamples.isNotEmpty()) {
            val avgLux = lightSamples.map { it.lux }.average()
            when {
                avgLux < 50 -> suggestions.add("光照偏暗，建议增加环境亮度保护眼睛。")
                avgLux > 800 -> suggestions.add("光照偏亮，可以适当调暗灯光。")
            }
        }

        // 基于时长
        val minutes = session.durationMillis / 60_000
        if (minutes < 10) {
            suggestions.add("本次专注时间较短，下次可以尝试延长到 25 分钟。")
        } else if (minutes >= 25) {
            suggestions.add("专注时长达标，继续保持！")
        }

        return suggestions.ifEmpty { listOf("整体环境稳定，继续保持！") }
    }

    /** 计算设备本地时区当天 00:00 的 Unix 毫秒时间戳。 */
    private fun todayStartMillis(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }
}