package com.ywb.focusguard.data.repository

import com.ywb.focusguard.data.sensor.LightSensorDataSource
import com.ywb.focusguard.data.sensor.MotionSensorDataSource
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

/**
 * 环境 Repository 的默认实现：协调各硬件数据源，并将最新样本组合为环境快照。
 * 该类不直接注册传感器监听，资源生命周期由对应 DataSource 管理。
 */
@Singleton
class EnvironmentRepositoryImpl @Inject constructor(
    /** 提供真实 TYPE_LIGHT 光照流。 */
    private val lightSensorDataSource: LightSensorDataSource,
    /** 提供经过防抖的真实加速度计移动流。 */
    private val motionSensorDataSource: MotionSensorDataSource,
    /** 把三类样本归纳为一个整体环境状态。 */
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

    override fun observeMotion(): Flow<MotionSample> = motionSensorDataSource.observeMotion()

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
