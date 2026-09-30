package com.ywb.focusguard.ui.component

import com.ywb.focusguard.domain.model.SleepStage
import org.junit.Assert.assertEquals
import org.junit.Test

/** 验证阶段图绘制前的合并逻辑：连续相同阶段合并成一段，边界下标正确。 */
class SleepStageChartTest {

    @Test
    fun `连续相同阶段合并为一段`() {
        val stages = listOf(
            SleepStage.AWAKE, SleepStage.AWAKE,
            SleepStage.CALM, SleepStage.CALM, SleepStage.CALM,
            SleepStage.RESTLESS
        )

        val segments = mergeStageSegments(stages)

        assertEquals(
            listOf(
                StageSegment(SleepStage.AWAKE, 0, 2),
                StageSegment(SleepStage.CALM, 2, 5),
                StageSegment(SleepStage.RESTLESS, 5, 6)
            ),
            segments
        )
    }

    @Test
    fun `空列表没有分段`() {
        assertEquals(emptyList<StageSegment>(), mergeStageSegments(emptyList()))
    }
}
