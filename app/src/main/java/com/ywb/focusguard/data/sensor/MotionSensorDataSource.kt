package com.ywb.focusguard.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import com.ywb.focusguard.domain.model.MotionSample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * 将 Android 加速度计回调转换为经过防抖的 [Flow]。
 * 输出的每一帧仍携带模长，但 isSignificantMove 只在确认新事件时为 true。
 */
@Singleton
class MotionSensorDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    /** 应用级 SensorManager，生命周期长于页面且不会持有 Activity。 */
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /**
     * 观察移动样本。设备没有加速度计时发射一条稳定默认样本后结束 Flow。
     */
    fun observeMotion(): Flow<MotionSample> = callbackFlow {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            trySend(defaultSample())
            close()
            return@callbackFlow
        }

        // detector 属于本次 Flow 收集生命周期，重新进入页面时会创建全新的检测状态。
        val detector = MotionEventDetector()

        val listener = object : SensorEventListener {
            /** 将 x/y/z 转为模长，再交给 detector 做窗口和冷却判断。 */
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values.getOrNull(0) ?: return
                val y = event.values.getOrNull(1) ?: return
                val z = event.values.getOrNull(2) ?: return
                val magnitude = calculateMagnitude(x, y, z)
                val confirmedMove = detector.onSample(
                    magnitude = magnitude,
                    elapsedRealtimeMillis = event.timestamp / 1_000_000L
                )
                trySend(
                    MotionSample(
                        timestamp = System.currentTimeMillis(),
                        magnitude = magnitude,
                        // true 只表示“刚确认了一次新移动事件”，不是持续移动状态。
                        isSignificantMove = confirmedMove
                    )
                )
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(
            listener,
            accelerometer,
            SensorManager.SENSOR_DELAY_NORMAL
        )

        // 和光照传感器一样，Flow 停止收集时必须释放监听，避免后台继续消耗电量。
        awaitClose {
            sensorManager.unregisterListener(listener)
        }
    }

    /** 无加速度计时使用的稳定降级样本。 */
    private fun defaultSample(): MotionSample = MotionSample(
        timestamp = System.currentTimeMillis(),
        magnitude = 9.8f,
        isSignificantMove = false
    )
}
