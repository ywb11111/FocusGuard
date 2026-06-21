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

@Singleton
class LightSensorDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    fun observeLight(): Flow<LightSample> = callbackFlow {
        val lightSensor = sensorManager.getDefaultSensor(Sensor.TYPE_LIGHT)
        if (lightSensor == null) {
            trySend(defaultSample())
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
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

    private fun defaultSample(): LightSample {
        val lux = 0f
        return LightSample(
            timestamp = System.currentTimeMillis(),
            lux = lux,
            level = classifyLightLevel(lux)
        )
    }
}
