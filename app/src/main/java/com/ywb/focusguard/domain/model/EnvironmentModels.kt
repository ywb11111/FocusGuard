package com.ywb.focusguard.domain.model

data class NoiseSample(
    val timestamp: Long,
    val decibel: Float,
    val level: NoiseLevel
)

enum class NoiseLevel {
    QUIET,
    NORMAL,
    NOISY,
    LOUD
}

data class LightSample(
    val timestamp: Long,
    val lux: Float,
    val level: LightLevel
)

enum class LightLevel {
    DARK,
    DIM,
    COMFORTABLE,
    BRIGHT
}

data class MotionSample(
    val timestamp: Long,
    val magnitude: Float,
    val isSignificantMove: Boolean
)

data class EnvironmentSnapshot(
    val noise: NoiseSample,
    val light: LightSample,
    val motion: MotionSample,
    val status: EnvironmentStatus
)

enum class EnvironmentStatus {
    FOCUSED,
    NOISY,
    TOO_DARK,
    TOO_BRIGHT,
    MOVING
}
