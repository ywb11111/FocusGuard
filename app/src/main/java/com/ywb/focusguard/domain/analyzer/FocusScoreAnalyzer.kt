package com.ywb.focusguard.domain.analyzer

import com.ywb.focusguard.domain.model.FocusScore

class FocusScoreAnalyzer {
    fun calculate(
        averageNoiseDb: Float,
        averageLightLux: Float,
        movementCount: Int,
        distractionCount: Int,
        durationMillis: Long
    ): FocusScore {
        val noisePenalty = when {
            averageNoiseDb >= 75f -> 25
            averageNoiseDb >= 65f -> 15
            averageNoiseDb >= 55f -> 8
            else -> 0
        }
        val lightPenalty = when {
            averageLightLux < 50f -> 20
            averageLightLux < 100f -> 10
            averageLightLux > 800f -> 12
            else -> 0
        }
        val motionPenalty = (movementCount * 3).coerceAtMost(20)
        val distractionPenalty = (distractionCount * 5).coerceAtMost(25)
        val durationPenalty = if (durationMillis < 10 * 60 * 1000L) 10 else 0
        val total = (100 - noisePenalty - lightPenalty - motionPenalty - distractionPenalty - durationPenalty)
            .coerceIn(0, 100)

        val suggestions = buildList {
            if (noisePenalty > 0) add("环境声音偏高，可以换到更安静的位置。")
            if (lightPenalty > 0) add("光照不够舒适，调整台灯或屏幕亮度。")
            if (motionPenalty > 0) add("手机移动较多，建议专注时放在固定位置。")
            if (distractionPenalty > 0) add("分心次数偏多，下次可以开启勿扰模式。")
            if (isEmpty()) add("这次环境很稳定，适合继续保持。")
        }

        return FocusScore(
            total = total,
            noisePenalty = noisePenalty,
            lightPenalty = lightPenalty,
            motionPenalty = motionPenalty,
            distractionPenalty = distractionPenalty,
            suggestions = suggestions
        )
    }
}
