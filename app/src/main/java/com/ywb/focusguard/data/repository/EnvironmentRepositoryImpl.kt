package com.ywb.focusguard.data.repository

import com.ywb.focusguard.data.sensor.LightSensorDataSource
import com.ywb.focusguard.domain.analyzer.EnvironmentAnalyzer
import com.ywb.focusguard.domain.model.EnvironmentSnapshot
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
    private val lightSensorDataSource: LightSensorDataSource,
    private val environmentAnalyzer: EnvironmentAnalyzer
) : EnvironmentRepository {
    // 当前是固定演示数据。真实噪声数据会来自 AudioRecord，并且需要权限、线程和资源释放处理。
    override fun observeNoise(): Flow<NoiseSample> = flowOf(
        NoiseSample(
            timestamp = System.currentTimeMillis(),
            decibel = 42f,
            level = NoiseLevel.NORMAL
        )
    )

    override fun observeLight(): Flow<LightSample> = lightSensorDataSource.observeLight()

    // 当前是固定演示数据。真实移动数据会来自加速度计，并经过防抖/阈值判断。
    override fun observeMotion(): Flow<MotionSample> = flowOf(
        MotionSample(
            timestamp = System.currentTimeMillis(),
            magnitude = 9.8f,
            isSignificantMove = false
        )
    )

    // combine 把三条独立数据流合成一个环境快照，ViewModel 收一个 Flow 就能拿到完整环境状态。
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
