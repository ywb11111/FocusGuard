package com.ywb.focusguard.data.repository

import com.ywb.focusguard.data.sensor.LightSensorDataSource
import com.ywb.focusguard.data.sensor.MotionSensorDataSource
import com.ywb.focusguard.data.sensor.NoiseSensorDataSource
import com.ywb.focusguard.domain.analyzer.EnvironmentAnalyzer
import com.ywb.focusguard.domain.model.EnvironmentSnapshot
import com.ywb.focusguard.domain.model.LightSample
import com.ywb.focusguard.domain.model.MotionSample
import com.ywb.focusguard.domain.model.NoiseSample
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 环境 Repository 的默认实现：协调各硬件数据源，并将最新样本组合为环境快照。
 *
 * 三类数据源均使用真实传感器：
 * - 光照：TYPE_LIGHT
 * - 移动：TYPE_LINEAR_ACCELERATION
 * - 噪声：AudioRecord
 *
 * 权限处理：
 * - 噪声数据源会检查 RECORD_AUDIO 权限，无权限时返回默认样本。
 * - 不阻塞其他功能，用户可以正常使用光照和移动检测。
 */
@Singleton
class EnvironmentRepositoryImpl @Inject constructor(
    /** 提供真实 TYPE_LIGHT 光照流。 */
    private val lightSensorDataSource: LightSensorDataSource,
    /** 提供经过防抖的真实线性加速度移动流。 */
    private val motionSensorDataSource: MotionSensorDataSource,
    /** 提供 AudioRecord 噪声流（需要录音权限）。 */
    private val noiseSensorDataSource: NoiseSensorDataSource,
    /** 把三类样本归纳为一个整体环境状态。 */
    private val environmentAnalyzer: EnvironmentAnalyzer
) : EnvironmentRepository {

    /** 观察噪声：使用 AudioRecord 采集，计算 RMS 和相对 dB。 */
    override fun observeNoise(): Flow<NoiseSample> = noiseSensorDataSource.observeNoise()

    /** 观察光照：使用 SensorManager TYPE_LIGHT。 */
    override fun observeLight(): Flow<LightSample> = lightSensorDataSource.observeLight()

    /** 观察移动：使用 TYPE_LINEAR_ACCELERATION，各方向敏感。 */
    override fun observeMotion(): Flow<MotionSample> = motionSensorDataSource.observeMotion()

    /**
     * 组合三条独立数据流为环境快照。
     *
     * ViewModel 只需要收集一个 Flow，就能拿到完整的环境状态。
     * 任一数据源更新时都会发射新快照。
     */
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