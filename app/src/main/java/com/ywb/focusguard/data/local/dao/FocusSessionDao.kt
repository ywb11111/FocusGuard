package com.ywb.focusguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ywb.focusguard.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

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

    // suspend 表示这是耗时数据库写入，应在协程中调用，避免阻塞主线程。
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long
}
