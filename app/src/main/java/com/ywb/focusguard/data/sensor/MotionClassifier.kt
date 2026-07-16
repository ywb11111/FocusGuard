package com.ywb.focusguard.data.sensor

import kotlin.math.abs
import kotlin.math.sqrt

/** 静止设备加速度模长的近似基准，单位 m/s²。 */
private const val GRAVITY_EARTH = 9.8f

/** 单帧模长与重力基准的最小偏差，超过该值才算一次候选命中。 */
private const val SIGNIFICANT_MOVE_THRESHOLD = 2.0f

/** 三轴加速度合成一个模长，便于用一个数描述手机当前整体受力情况。 */
fun calculateMagnitude(x: Float, y: Float, z: Float): Float =
    sqrt(x * x + y * y + z * z)

/**
 * 判断单帧是否达到候选移动阈值。
 * 这里只判断原始帧，是否形成一次有效事件由 [MotionEventDetector] 决定。
 */
fun isSignificantMove(magnitude: Float): Boolean =
    abs(magnitude - GRAVITY_EARTH) > SIGNIFICANT_MOVE_THRESHOLD
