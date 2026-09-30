package com.ywb.focusguard.data.sensor

import android.content.Context
import android.hardware.Sensor
import android.hardware.SensorEvent
import android.hardware.SensorEventListener
import android.hardware.SensorManager
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.sqrt

private const val TAG = "SleepMotionDataSource"

/**
 * 睡眠场景的体动数据源：输出相邻两次加速度读数的变化量 |Δa|（m/s²）。
 *
 * **为什么不复用专注用的 MotionSensorDataSource？**
 * 1. 专注场景检测的是"拿起手机"这类大动作，阈值很高；翻身通过床垫传到手机上只有零点几 m/s²，会被全部过滤掉。
 * 2. 专注用的 TYPE_LINEAR_ACCELERATION 是融合虚拟传感器，底层要同时开陀螺仪，整晚 8 小时耗电明显。
 *    睡眠只关心"有没有在动"，用原始 TYPE_ACCELEROMETER 的相邻差分即可：
 *    手机静止时重力分量每次读数几乎一样，相减后只剩噪声，天然去掉了重力，不需要陀螺仪。
 *
 * **生命周期**：由 SleepMonitorService collect 时注册监听，Service 取消协程时 awaitClose 注销。
 *
 * @param context Application Context，用于获取 SensorManager。
 */
@Singleton
class SleepMotionDataSource @Inject constructor(
    @ApplicationContext context: Context
) {
    /** 应用级 SensorManager，不持有 Activity。 */
    private val sensorManager = context.getSystemService(Context.SENSOR_SERVICE) as SensorManager

    /** 设备是否有加速度计；没有时 Service 会把本晚按"无体动数据"处理。 */
    fun isAvailable(): Boolean = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER) != null

    /**
     * 观察体动变化量。
     *
     * 采样频率：SENSOR_DELAY_NORMAL 约 200ms 一次（5Hz），足够捕捉持续 1 秒以上的翻身，
     * 又比 SENSOR_DELAY_GAME（约 50Hz）省电得多。
     *
     * @return 每次传感器回调发射一个 |Δa|；第一次回调没有上一帧可比较，不发射。
     */
    fun observeMotionDeltas(): Flow<Float> = callbackFlow {
        val sensor = sensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER)
        if (sensor == null) {
            Log.w(TAG, "设备没有加速度计，睡眠体动不可用")
            close()
            return@callbackFlow
        }

        val listener = object : SensorEventListener {
            /** 上一帧三轴读数；NaN 表示还没有上一帧。 */
            private var lastX = Float.NaN
            private var lastY = Float.NaN
            private var lastZ = Float.NaN

            override fun onSensorChanged(event: SensorEvent) {
                val x = event.values.getOrNull(0) ?: return
                val y = event.values.getOrNull(1) ?: return
                val z = event.values.getOrNull(2) ?: return
                if (!lastX.isNaN()) {
                    val dx = x - lastX
                    val dy = y - lastY
                    val dz = z - lastZ
                    trySend(sqrt(dx * dx + dy * dy + dz * dz))
                }
                lastX = x
                lastY = y
                lastZ = z
            }

            override fun onAccuracyChanged(sensor: Sensor?, accuracy: Int) = Unit
        }

        sensorManager.registerListener(listener, sensor, SensorManager.SENSOR_DELAY_NORMAL)
        awaitClose { sensorManager.unregisterListener(listener) }
    }
}
