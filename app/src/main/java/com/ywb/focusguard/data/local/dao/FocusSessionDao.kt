package com.ywb.focusguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ywb.focusguard.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

/** 专注会话表的查询与写入入口，Repository 通过它管理会话生命周期。 */
@Dao
interface FocusSessionDao {
    // 返回 Flow 的好处：数据库内容变化后，Room 会自动重新发射列表，UI 可以跟着刷新。
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun observeSessions(): Flow<List<FocusSessionEntity>>

    // 详情页按 id 观察单条记录；后续 SessionDetailScreen 会用这个查询真实数据。
    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId LIMIT 1")
    fun observeSession(sessionId: Long): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC LIMIT 1")
    fun observeLatestSession(): Flow<FocusSessionEntity?>

    // 结束会话前需要一次性读取开始时间；这里不需要持续观察，所以使用 suspend 查询。
    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId LIMIT 1")
    suspend fun getSession(sessionId: Long): FocusSessionEntity?

    // ---- 按日期范围查询（用于周报/月报聚合） ----

    /**
     * 按日期范围观察已完成会话。
     * startTime >= startMillis 且 startTime < endMillis，且 endTime 不为空（已完成）。
     * 用于周报/月报页面持续监听指定时间段内的会话数据。
     */
    @Query(
        """
        SELECT * FROM focus_sessions
        WHERE startTime >= :startMillis AND startTime < :endMillis AND endTime IS NOT NULL
        ORDER BY startTime DESC
        """
    )
    fun observeSessionsInRange(startMillis: Long, endMillis: Long): Flow<List<FocusSessionEntity>>

    /**
     * 一次性获取指定日期范围内的已完成会话。
     * 用于需要一次性读取完整列表的场景（如导出、统计计算）。
     */
    @Query(
        """
        SELECT * FROM focus_sessions
        WHERE startTime >= :startMillis AND startTime < :endMillis AND endTime IS NOT NULL
        ORDER BY startTime DESC
        """
    )
    suspend fun getSessionsInRange(startMillis: Long, endMillis: Long): List<FocusSessionEntity>

    // suspend 表示这是耗时数据库写入，应在协程中调用，避免阻塞主线程。
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long

    // 开始专注时先插入一条进行中记录；结束专注时只更新同一条记录的结果字段。
    // 这样 sessionId 从开始到结束保持稳定，后续采样表也可以通过 sessionId 关联。
    @Query(
        """
        UPDATE focus_sessions
        SET endTime = :endTime,
            durationMillis = :durationMillis,
            averageNoiseDb = :averageNoiseDb,
            maxNoiseDb = :maxNoiseDb,
            averageLightLux = :averageLightLux,
            movementCount = :movementCount,
            distractionCount = :distractionCount,
            score = :score,
            note = :note
        WHERE id = :sessionId
        """
    )
    suspend fun updateFinishedSession(
        sessionId: Long,
        endTime: Long,
        durationMillis: Long,
        averageNoiseDb: Float,
        maxNoiseDb: Float,
        averageLightLux: Float,
        movementCount: Int,
        distractionCount: Int,
        score: Int,
        note: String?
    )
}
