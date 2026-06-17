package com.ywb.focusguard.data.repository

import com.ywb.focusguard.domain.analyzer.EnvironmentAnalyzer
import com.ywb.focusguard.domain.model.EnvironmentSnapshot
import com.ywb.focusguard.domain.model.LightLevel
import com.ywb.focusguard.domain.model.LightSample
import com.ywb.focusguard.domain.model.MotionSample
import com.ywb.focusguard.domain.model.NoiseLevel
import com.ywb.focusguard.domain.model.NoiseSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flowOf
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class EnvironmentRepositoryImpl @Inject constructor(
    private val environmentAnalyzer: EnvironmentAnalyzer
) : EnvironmentRepository {
    override fun observeNoise(): Flow<NoiseSample> = flowOf(
        NoiseSample(
            timestamp = System.currentTimeMillis(),
            decibel = 42f,
            level = NoiseLevel.NORMAL
        )
    )

    override fun observeLight(): Flow<LightSample> = flowOf(
        LightSample(
            timestamp = System.currentTimeMillis(),
            lux = 186f,
            level = LightLevel.COMFORTABLE
        )
    )

    override fun observeMotion(): Flow<MotionSample> = flowOf(
        MotionSample(
            timestamp = System.currentTimeMillis(),
            magnitude = 9.8f,
            isSignificantMove = false
        )
    )

    override fun observeEnvironmentSnapshot(): Flow<EnvironmentSnapshot> =
        combine(observeNoise(), observeLight(), observeMotion()) { noise, light, motion ->
            EnvironmentSnapshot(
                noise = noise,
                light = light,
                motion = motion,
                status = environmentAnalyzer.statusFor(noise.level, light.level, motion)
            )
        }
}
