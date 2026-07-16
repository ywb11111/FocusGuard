package com.ywb.focusguard.data.sensor

import kotlin.math.abs
import kotlin.math.sqrt

private const val GRAVITY_EARTH = 9.8f
private const val SIGNIFICANT_MOVE_THRESHOLD = 2.0f

// 三轴加速度合成一个模长，便于用一个数描述手机当前整体受力情况。
fun calculateMagnitude(x: Float, y: Float, z: Float): Float =
    sqrt(x * x + y * y + z * z)

// 静止放在桌面上时，模长通常接近重力加速度 9.8。
// 和 9.8 偏离较大时，先粗略认为发生了明显移动。
fun isSignificantMove(magnitude: Float): Boolean =
    abs(magnitude - GRAVITY_EARTH) > SIGNIFICANT_MOVE_THRESHOLD
