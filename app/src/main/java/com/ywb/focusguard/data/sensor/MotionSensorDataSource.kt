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

@Singleton
class MotionSensorDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun observeMotion(): Flow<MotionSample> = callbackFlow {
        val accelerometer = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (accelerometer == null) {
            trySend(defaultSample())
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values.getOrNull(0) ?: return
                val y = event.values.getOrNull(1) ?: return
                val z = event.values.getOrNull(2) ?: return
                val magnitude = calculateMagnitude(x, y, z)
                trySend(
                    MotionSample(
                        timestamp = System.currentTimeMillis(),
                        magnitude = magnitude,
                        isSignificantMove = isSignificantMove(magnitude)
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

    private fun defaultSample(): MotionSample = MotionSample(
        timestamp = System.currentTimeMillis(),
        magnitude = 9.8f,
        isSignificantMove = false
    )
}
