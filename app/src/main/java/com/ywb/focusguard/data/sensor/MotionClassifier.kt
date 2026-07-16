package com.ywb.focusguard.data.sensor

import kotlin.math.abs
import kotlin.math.sqrt

/**
 * 线性加速度阈值：任一轴超过此值视为候选移动帧。
 *
 * 使用 TYPE_LINEAR_ACCELERATION 时，静止状态下三轴都接近 0。
 * 典型阈值：
 * - 0.5f：敏感，轻微移动即可触发
 * - 1.0f：中等，正常手持抖动不会触发
 * - 1.5f：不敏感，需要明显移动才触发
 */
const val LINEAR_ACCELERATION_THRESHOLD = 1.0f

/**
 * 计算三轴加速度的合成模长。
 *
 * @param x X 轴加速度（m/s²）。
 * @param y Y 轴加速度（m/s²）。
 * @param z Z 轴加速度（m/s²）。
 * @return 合成模长，反映整体加速度大小。
 */
fun calculateMagnitude(x: Float, y: Float, z: Float): Float =
    sqrt(x * x + y * y + z * z)

/**
 * 判断单帧线性加速度是否达到候选移动阈值。
 *
 * 使用 TYPE_LINEAR_ACCELERATION 时，静止状态下三轴接近 0，
 * 任一轴有明显变化即表示设备正在移动。
 *
 * @param x X 轴线性加速度（已去除重力）。
 * @param y Y 轴线性加速度（已去除重力）。
 * @param z Z 轴线性加速度（已去除重力）。
 * @param threshold 触发阈值，默认 1.0 m/s²。
 * @return 是否达到候选移动阈值。
 */
fun isSignificantLinearAcceleration(
    x: Float,
    y: Float,
    z: Float,
    threshold: Float = LINEAR_ACCELERATION_THRESHOLD
): Boolean = abs(x) > threshold || abs(y) > threshold || abs(z) > threshold