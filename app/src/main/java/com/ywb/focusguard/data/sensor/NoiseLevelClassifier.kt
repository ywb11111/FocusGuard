package com.ywb.focusguard.data.sensor

import com.ywb.focusguard.domain.model.NoiseLevel

/**
 * 将相对 dB 值分类为噪声等级。
 *
 * 注意：手机麦克风不是专业分贝仪，这些阈值基于"相对值"，
 * 用于判断环境是否适合专注，不代表绝对物理声压级。
 *
 * @param decibel 相对噪声值（dB）。
 * @return 对应的噪声等级。
 */
fun classifyNoiseLevel(decibel: Float): NoiseLevel = when {
    decibel < 40f -> NoiseLevel.QUIET    // 图书馆、深夜卧室
    decibel < 60f -> NoiseLevel.NORMAL   // 普通办公室、安静咖啡厅
    decibel < 75f -> NoiseLevel.NOISY    // 繁忙办公室、餐厅
    else -> NoiseLevel.LOUD              // 工地、吵闹街道
}