package com.ywb.focusguard.data.repository

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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FocusRepositoryImpl @Inject constructor(
    private val scoreAnalyzer: FocusScoreAnalyzer
) : FocusRepository {
    private val now = System.currentTimeMillis()
    private val sessions = MutableStateFlow(
        listOf(
            FocusSession(
                id = 1,
                startTime = now - 2 * 60 * 60 * 1000L,
                endTime = now - 75 * 60 * 1000L,
                durationMillis = 45 * 60 * 1000L,
                averageNoiseDb = 42f,
                maxNoiseDb = 58f,
                averageLightLux = 180f,
                movementCount = 2,
                distractionCount = 1,
                score = 88,
                note = "晚间学习"
            )
        )
    )

    override fun observeTodaySummary(): Flow<TodaySummary> = sessions.map { items ->
        TodaySummary(
            totalFocusMillis = items.sumOf { it.durationMillis },
            averageScore = items.map { it.score }.average().takeIf { !it.isNaN() }?.toInt() ?: 0,
            distractionCount = items.sumOf { it.distractionCount },
            sessionCount = items.size
        )
    }

    override fun observeSessions(): Flow<List<FocusSession>> = sessions

    override fun observeSessionDetail(sessionId: Long): Flow<SessionDetail?> = sessions.map { items ->
        val session = items.firstOrNull { it.id == sessionId } ?: return@map null
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
        val nextId = (sessions.value.maxOfOrNull { it.id } ?: 0L) + 1L
        return nextId
    }

    override suspend fun finishSession(sessionId: Long): FocusSession {
        val finishedAt = System.currentTimeMillis()
        val startedAt = finishedAt - 25 * 60 * 1000L
        val score = scoreAnalyzer.calculate(
            averageNoiseDb = 44f,
            averageLightLux = 220f,
            movementCount = 1,
            distractionCount = 0,
            durationMillis = 25 * 60 * 1000L
        )
        val session = FocusSession(
            id = sessionId,
            startTime = startedAt,
            endTime = finishedAt,
            durationMillis = 25 * 60 * 1000L,
            averageNoiseDb = 44f,
            maxNoiseDb = 60f,
            averageLightLux = 220f,
            movementCount = 1,
            distractionCount = 0,
            score = score.total,
            note = "手动结束"
        )
        sessions.value = listOf(session) + sessions.value.filterNot { it.id == sessionId }
        return session
    }

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
}
