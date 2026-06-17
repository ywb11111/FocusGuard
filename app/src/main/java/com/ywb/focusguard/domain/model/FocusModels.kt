package com.ywb.focusguard.domain.model

data class FocusSession(
    val id: Long,
    val startTime: Long,
    val endTime: Long?,
    val durationMillis: Long,
    val averageNoiseDb: Float,
    val maxNoiseDb: Float,
    val averageLightLux: Float,
    val movementCount: Int,
    val distractionCount: Int,
    val score: Int,
    val note: String?
)

data class FocusConfig(
    val durationMinutes: Int,
    val monitorNoise: Boolean = true,
    val monitorLight: Boolean = true,
    val monitorMotion: Boolean = true
)

data class FocusScore(
    val total: Int,
    val noisePenalty: Int,
    val lightPenalty: Int,
    val motionPenalty: Int,
    val distractionPenalty: Int,
    val suggestions: List<String>
)

data class TodaySummary(
    val totalFocusMillis: Long,
    val averageScore: Int,
    val distractionCount: Int,
    val sessionCount: Int
)

data class SessionDetail(
    val session: FocusSession,
    val noiseSamples: List<NoiseSample>,
    val lightSamples: List<LightSample>,
    val motionEvents: List<MotionSample>,
    val score: FocusScore
)

data class ActiveSession(
    val sessionId: Long,
    val startedAt: Long,
    val elapsedMillis: Long
)
