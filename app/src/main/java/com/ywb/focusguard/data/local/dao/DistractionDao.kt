package com.ywb.focusguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import com.ywb.focusguard.data.local.entity.DistractionEventEntity
import kotlinx.coroutines.flow.Flow

/**
 * 分心记录表的数据库访问入口。
 *
 * 写入方：DistractionRepositoryImpl（专注结束时一次性写入整场会话的分心片段）。
 * 读取方：FocusRepositoryImpl（结束时统计次数参与评分、详情页展示明细）。
 */
@Dao
interface DistractionDao {
    /** 批量写入一场会话的全部分心片段。 */
    @Insert
    suspend fun insertAll(events: List<DistractionEventEntity>)

    /** 统计一场会话的分心次数，结束评分时使用。 */
    @Query("SELECT COUNT(*) FROM distraction_events WHERE sessionId = :sessionId")
    suspend fun countForSession(sessionId: Long): Int

    /** 观察一场会话的分心明细，详情页使用。 */
    @Query("SELECT * FROM distraction_events WHERE sessionId = :sessionId ORDER BY startTime ASC")
    fun observeEvents(sessionId: Long): Flow<List<DistractionEventEntity>>

    /**
     * 标记这场会话"已开启分心检测"。
     * 单独一个更新方法而不是改动原有的 updateFinishedSession，避免影响已有的结束流程。
     */
    @Query("UPDATE focus_sessions SET distractionTracked = 1 WHERE id = :sessionId")
    suspend fun markSessionTracked(sessionId: Long)
}
