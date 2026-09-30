package com.ywb.focusguard.data.sensor

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import androidx.core.content.ContextCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.math.log10
import kotlin.math.sqrt

private const val TAG = "SleepSoundDataSource"

/**
 * 睡眠场景的声音数据源：连续读取麦克风 PCM，每 100ms 输出一帧音量和主频估计。
 * 只在内存中计算数值，不保存、不上传任何录音。
 *
 * **和专注用的 NoiseSensorDataSource 的区别**：
 * 1. 连续读取、不 delay。专注版读一次后 delay(500ms)，用来判断环境"吵不吵"足够；
 *    但一声鼾声只有几百毫秒，很可能落在 delay 里被漏掉。
 * 2. 采样率 16kHz。鼾声和说话的能量主要在 4kHz 以下，16kHz 足够，计算量和耗电约为 44.1kHz 的 1/3。
 * 3. 音源用 VOICE_RECOGNITION。部分机型的 MIC 音源会开自动增益，夜里安静时把底噪放大，
 *    导致相对 dB 失真；VOICE_RECOGNITION 通常关闭这类处理，更适合测音量。
 *
 * **为什么用 flow {} 而不是 callbackFlow**：
 * AudioRecord 是"拉"模式（我们主动 read），不是回调"推"模式；用普通 flow 循环 read 更直接，
 * 协程取消时循环退出，finally 里释放 AudioRecord。
 *
 * @param context Application Context，用于检查录音权限。
 */
@Singleton
class SleepSoundDataSource @Inject constructor(
    @param:ApplicationContext private val context: Context
) {
    /** 采样率 16kHz。 */
    private val sampleRate = 16_000

    /** 每帧 1600 个采样 = 100ms，既能分辨几百毫秒的鼾声，又不会让事件检测过于频繁。 */
    private val frameSize = 1_600

    /** 是否已有录音权限；Service 据此决定是否申请麦克风类型的前台服务。 */
    fun hasPermission(): Boolean =
        ContextCompat.checkSelfPermission(context, Manifest.permission.RECORD_AUDIO) ==
            PackageManager.PERMISSION_GRANTED

    /**
     * 观察声音帧。
     *
     * 没有权限或初始化失败时直接结束 Flow（不发射任何数据），由上层把本晚标记为"未启用声音分析"。
     * read() 是阻塞调用，因此通过 flowOn(Dispatchers.IO) 放到 IO 线程池。
     */
    fun observeFrames(): Flow<SoundFrame> = flow {
        if (!hasPermission()) {
            Log.w(TAG, "没有录音权限，跳过声音分析")
            return@flow
        }
        val minBuffer = AudioRecord.getMinBufferSize(
            sampleRate,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT
        )
        if (minBuffer <= 0) {
            Log.e(TAG, "设备不支持 16kHz 单声道录音")
            return@flow
        }
        val record = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION,
                sampleRate,
                AudioFormat.CHANNEL_IN_MONO,
                AudioFormat.ENCODING_PCM_16BIT,
                // 系统缓冲至少留 4 帧，避免 CPU 偶尔繁忙时丢帧；单位是字节，16bit = 2 字节
                maxOf(minBuffer, frameSize * 2 * 4)
            )
        } catch (e: SecurityException) {
            // 权限可能在检查之后、创建之前被用户撤销
            Log.e(TAG, "创建 AudioRecord 被拒绝", e)
            return@flow
        }
        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord 初始化失败，可能被其他应用占用")
            record.release()
            return@flow
        }

        val buffer = ShortArray(frameSize)
        try {
            record.startRecording()
            while (currentCoroutineContext().isActive) {
                // 阻塞约 100ms 直到读满一帧；取消后最多再等一帧就会退出循环
                val read = record.read(buffer, 0, frameSize)
                if (read < 0) {
                    Log.e(TAG, "AudioRecord.read 出错：$read")
                    break
                }
                if (read > 0) {
                    emit(
                        SoundFrame(
                            timestamp = System.currentTimeMillis(),
                            decibel = toRelativeDb(buffer, read),
                            dominantHz = estimateDominantHz(buffer, read)
                        )
                    )
                }
            }
        } finally {
            // 无论正常结束、取消还是异常，都要释放麦克风，否则其他 App 无法录音
            runCatching { record.stop() }
            record.release()
            Log.d(TAG, "AudioRecord 已释放")
        }
    }.flowOn(Dispatchers.IO)

    /**
     * RMS → 相对 dB，公式与 NoiseSensorDataSource 一致，保证两处读数可比较。
     * 手机麦克风未经校准，这不是绝对声压级。
     */
    private fun toRelativeDb(buffer: ShortArray, count: Int): Float {
        var sum = 0.0
        for (i in 0 until count) {
            val value = buffer[i].toDouble()
            sum += value * value
        }
        val rms = sqrt(sum / count)
        if (rms <= 0.0) return 0f
        return (20 * log10(rms / 32767.0) + 90).toFloat().coerceIn(0f, 100f)
    }

    /**
     * 用过零率粗略估计主频：信号每穿过 0 两次约等于一个周期。
     *
     * 为什么不用 FFT：只需要区分"低沉的鼾声"（主频通常几百 Hz 以下）和"尖锐的说话/摩擦声"，
     * 过零率 O(n) 且无需额外依赖，足够做这个粗分类；需要精确频谱时再换 FFT。
     */
    private fun estimateDominantHz(buffer: ShortArray, count: Int): Float {
        if (count < 2) return 0f
        var crossings = 0
        for (i in 1 until count) {
            val previous = buffer[i - 1]
            val current = buffer[i]
            if ((previous >= 0 && current < 0) || (previous < 0 && current >= 0)) crossings++
        }
        return crossings * sampleRate / (2f * count)
    }
}
