package com.ywb.focusguard.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import com.ywb.focusguard.domain.model.MotionSample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton

private const val TAG = "MotionSensorDataSource"

/**
 * 将 Android 加速度传感器回调转换为经过防抖的 [Flow]。
 *
 * 使用 TYPE_LINEAR_ACCELERATION（线性加速度传感器），自动分离重力成分，
 * 能够检测各个方向的移动（上下、左右、前后）。
 *
 * 若设备不支持 LINEAR_ACCELERATION，降级使用 TYPE_ACCELEROMETER（只能检测模长变化）。
 */
@Singleton
class MotionSensorDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    /** 应用级 SensorManager，生命周期长于页面且不会持有 Activity。 */
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /**
     * 观察移动样本。
     *
     * 优先使用 TYPE_LINEAR_ACCELERATION，不支持时降级为 TYPE_ACCELEROMETER。
     */
    fun observeMotion(): Flow<MotionSample> = callbackFlow {
        val linearAccelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LINEAR_ACCELERATION)
        val accelSensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)

        when {
            linearAccelSensor != null -> {
                Log.d(TAG, "使用 TYPE_LINEAR_ACCELERATION 检测移动（推荐）")
                val detector = MotionEventDetector()
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val x = event.values.getOrNull(0) ?: return
                        val y = event.values.getOrNull(1) ?: return
                        val z = event.values.getOrNull(2) ?: return
                        val magnitude = calculateMagnitude(x, y, z)
                        val result = detector.onSample(x, y, z, event.timestamp / 1_000_000L)
                        trySend(MotionSample(
                            timestamp = System.currentTimeMillis(),
                            magnitude = magnitude,
                            isSignificantMove = result.isSignificantMove,
                            isMoving = result.isMoving
                        ))
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                sensorManager.registerListener(listener, linearAccelSensor, SensorManager.SENSOR_DELAY_NORMAL)
                awaitClose { sensorManager.unregisterListener(listener) }
            }
            accelSensor != null -> {
                Log.d(TAG, "降级使用 TYPE_ACCELEROMETER（对平移不敏感）")
                val detector = LegacyMotionDetector()
                val listener = object : SensorEventListener {
                    override fun onSensorChanged(event: SensorEvent) {
                        val x = event.values.getOrNull(0) ?: return
                        val y = event.values.getOrNull(1) ?: return
                        val z = event.values.getOrNull(2) ?: return
                        val magnitude = calculateMagnitude(x, y, z)
                        val result = detector.onSample(magnitude, event.timestamp / 1_000_000L)
                        trySend(MotionSample(
                            timestamp = System.currentTimeMillis(),
                            magnitude = magnitude,
                            isSignificantMove = result.isSignificantMove,
                            isMoving = result.isMoving
                        ))
                    }
                    override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
                }
                sensorManager.registerListener(listener, accelSensor, SensorManager.SENSOR_DELAY_NORMAL)
                awaitClose { sensorManager.unregisterListener(listener) }
            }
            else -> {
                Log.w(TAG, "设备没有加速度传感器")
                trySend(defaultSample())
                close()
            }
        }
    }

    /** 无传感器时的默认样本。 */
    private fun defaultSample(): MotionSample = MotionSample(
        timestamp = System.currentTimeMillis(),
        magnitude = 0f,
        isSignificantMove = false,
        isMoving = false
    )
}