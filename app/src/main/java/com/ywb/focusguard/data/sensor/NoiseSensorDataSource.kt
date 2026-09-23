package com.ywb.focusguard.data.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import com.ywb.focusguard.domain.model.NoiseLevel
import com.ywb.focusguard.domain.model.NoiseSample
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.sqrt

private const val TAG = "NoiseSensorDataSource"

/**
 * 使用 AudioRecord 采集环境噪声，计算 RMS 和相对 dB，输出 Flow。
 *
 * **为什么不使用 MediaRecorder？**
 * MediaRecorder 只能录音文件，无法获取原始 PCM 数据。
 * AudioRecord 可以实时读取音频帧，自己计算 RMS 和分贝。
 *
 * **关于"相对 dB"的说明：**
 * 手机麦克风没有经过专业校准，计算出的分贝值不是绝对声压级。
 * 这个值用于判断环境是否适合专注（安静/正常/吵闹），不是专业测量。
 */
@Singleton
class NoiseSensorDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /** 采样率：44100 Hz 是 CD 音质标准，所有 Android 设备都支持。 */
    private val sampleRate = 44100

    /** 声道配置：单声道足够，节省计算量。 */
    private val channelConfig = AudioFormat.CHANNEL_IN_MONO

    /** 音频格式：16 位 PCM，计算 RMS 更准确。 */
    private val audioFormat = AudioFormat.ENCODING_PCM_16BIT

    /** 缓冲区大小：使用系统推荐值，避免丢帧。 */
    private val bufferSize = AudioRecord.getMinBufferSize(sampleRate, channelConfig, audioFormat)

    /**
     * 观察环境噪声。
     *
     * **权限检查**：如果没有录音权限，返回一条默认样本后结束 Flow。
     *
     * **采样频率**：AudioRecord 持续读取，但每 500ms 发射一次样本，
     * 避免高频发射导致 UI 频繁重组。
     */
    fun observeNoise(): Flow<NoiseSample> = callbackFlow {
        // 检查权限
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO)
            != PackageManager.PERMISSION_GRANTED) {
            Log.w(TAG, "没有录音权限，返回默认样本")
            trySend(defaultSample())
            close()
            return@callbackFlow
        }

        // 创建 AudioRecord
        val audioRecord = AudioRecord(
            MediaRecorder.AudioSource.MIC,  // 使用麦克风
            sampleRate,
            channelConfig,
            audioFormat,
            bufferSize.coerceAtLeast(2048)  // 确保缓冲区足够
        )

        if (audioRecord.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord 初始化失败")
            trySend(defaultSample())
            close()
            return@callbackFlow
        }

        // 在 IO 线程持续读取

        // 启动录制
        audioRecord.startRecording()
        Log.d(TAG, "AudioRecord 已启动，开始采集噪声")

        // 在 IO 线程持续读取
        val recordJob = withContext(Dispatchers.IO) {
            while (isActive) {
                val buffer = ShortArray(bufferSize / 2)
                val readCount = audioRecord.read(buffer, 0, buffer.size)
                if (readCount > 0) {
                    val rms = calculateRMS(buffer, readCount)
                    val decibel = rmsToDb(rms)
                    val level = classifyNoiseLevel(decibel)

                    trySend(NoiseSample(
                        timestamp = System.currentTimeMillis(),
                        decibel = decibel,
                        level = level
                    ))

                    // 每 500ms 发射一次，避免高频发射
                    delay(500)
                }
            }
        }

        // Flow 停止时释放资源
        awaitClose {
            Log.d(TAG, "停止 AudioRecord")
            audioRecord.stop()
            audioRecord.release()
        }
    }

    /**
     * 计算 RMS（均方根）。
     *
     * RMS 反映音频信号的"平均能量"，用于判断声音大小。
     *
     * @param buffer PCM 采样数据（16 位有符号整数）。
     * @param count 实际读取的采样数。
     * @return RMS 值。
     */
    private fun calculateRMS(buffer: ShortArray, count: Int): Float {
        var sum = 0.0
        for (i in 0 until count) {
            sum += buffer[i] * buffer[i]
        }
        return sqrt((sum / count).toFloat())
    }

    /**
     * 将 RMS 转换为相对 dB。
     *
     * 公式：dB = 20 * log10(rms / reference)
     *
     * @param rms RMS 值。
     * @param reference 参考值，16 位 PCM 的最大值是 32767。
     * @return 相对分贝值。
     */
    private fun rmsToDb(rms: Float, reference: Float = 32767f): Float {
        if (rms <= 0) return 0f
        val db = 20 * log10(rms / reference)
        // 调整到人类可感知范围（0-100 dB）
        return (db + 90).coerceIn(0f, 100f)
    }

    /** 无权限或初始化失败时的默认样本。 */
    private fun defaultSample(): NoiseSample = NoiseSample(
        timestamp = System.currentTimeMillis(),
        decibel = 0f,
        level = NoiseLevel.QUIET
    )
}
