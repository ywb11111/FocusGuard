package com.ywb.focusguard.data.local.mapper

import com.ywb.focusguard.data.local.entity.FocusSessionEntity
import com.ywb.focusguard.domain.model.FocusSession
import org.junit.Assert.assertEquals
import org.junit.Test

class FocusSessionMapperTest {
    @Test
    fun `FocusSessionEntity 转换为领域模型时保留所有字段`() {
        val entity = FocusSessionEntity(
            id = 7L,
            startTime = 1_000L,
            endTime = 2_000L,
            durationMillis = 1_000L,
            averageNoiseDb = 41.5f,
            maxNoiseDb = 56f,
            averageLightLux = 180f,
            movementCount = 2,
            distractionCount = 1,
            score = 88,
            note = "测试记录"
        )

        val domain = entity.toDomain()

        assertEquals(7L, domain.id)
        assertEquals(1_000L, domain.startTime)
        assertEquals(2_000L, domain.endTime)
        assertEquals(1_000L, domain.durationMillis)
        assertEquals(41.5f, domain.averageNoiseDb)
        assertEquals(56f, domain.maxNoiseDb)
        assertEquals(180f, domain.averageLightLux)
        assertEquals(2, domain.movementCount)
        assertEquals(1, domain.distractionCount)
        assertEquals(88, domain.score)
        assertEquals("测试记录", domain.note)
    }

    @Test
    fun `FocusSession 转换为 Entity 时保留所有字段`() {
        val session = FocusSession(
            id = 8L,
            startTime = 3_000L,
            endTime = 4_000L,
            durationMillis = 1_000L,
            averageNoiseDb = 39f,
            maxNoiseDb = 50f,
            averageLightLux = 220f,
            movementCount = 0,
            distractionCount = 0,
            score = 92,
            note = "完成记录"
        )

        val entity = session.toEntity()

        assertEquals(8L, entity.id)
        assertEquals(3_000L, entity.startTime)
        assertEquals(4_000L, entity.endTime)
        assertEquals(1_000L, entity.durationMillis)
        assertEquals(39f, entity.averageNoiseDb)
        assertEquals(50f, entity.maxNoiseDb)
        assertEquals(220f, entity.averageLightLux)
        assertEquals(0, entity.movementCount)
        assertEquals(0, entity.distractionCount)
        assertEquals(92, entity.score)
        assertEquals("完成记录", entity.note)
    }
}
