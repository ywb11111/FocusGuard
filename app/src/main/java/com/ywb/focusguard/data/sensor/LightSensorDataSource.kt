package com.ywb.focusguard.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.ywb.focusguard.domain.model.LightSample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 将 Android TYPE_LIGHT 回调封装为冷 [Flow]。
 * 每个收集者开始收集时注册监听，停止收集时由 awaitClose 注销监听。
 */
@Singleton
class LightSensorDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    /** 应用级 SensorManager；使用 ApplicationContext 避免持有 Activity 导致泄漏。 */
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /**
     * 观察实时光照样本。设备没有光照传感器时发射一条 0 lux 默认样本后结束 Flow。
     */
    fun observeLight(): Flow<LightSample> = callbackFlow {
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (lightSensor == null) {
            trySend(defaultSample())
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            /** 每次硬件回调都读取 lux，并转换为领域层 [LightSample]。 */
            override fun onSensorChanged(event: SensorEvent) {
                val lux = event.values.firstOrNull() ?: return
                trySend(
                    LightSample(
                        timestamp = System.currentTimeMillis(),
                        lux = lux,
                        level = classifyLightLevel(lux)
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(
            listener,
            lightSensor,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        // callbackFlow 不会自动知道页面何时停止收集；awaitClose 是释放传感器监听的关键。
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    /** 无光照传感器时使用的降级样本，让上层仍能形成完整环境快照。 */
    private fun defaultSample(): LightSample {
        val lux = 0f
        return LightSample(
            timestamp = System.currentTimeMillis(),
            lux = lux,
            level = classifyLightLevel(lux)
        )
    }
}
