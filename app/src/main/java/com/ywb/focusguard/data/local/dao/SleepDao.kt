package com.ywb.focusguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.ywb.focusguard.data.local.entity.SleepEpochEntity
import com.ywb.focusguard.data.local.entity.SleepSessionEntity
import kotlinx.coroutines.flow.Flow

/**
 * 睡眠两张表（sleep_sessions / sleep_epochs）的数据库访问入口。
 *
 * 和 SampleDao 一样：Flow 查询给页面实时观察，suspend 查询给 Service 和结束分析一次性读取。
 */
@Dao
interface SleepDao {
    // ==================== 主记录 ====================

    /** 插入一条进行中的睡眠记录，返回自增 id。 */
    @Insert
    suspend fun insertSession(session: SleepSessionEntity): Long

    /** 结束分析后整行回写统计字段。 */
    @Update
    suspend fun updateSession(session: SleepSessionEntity)

    /** 一次性读取指定记录。 */
    @Query("SELECT * FROM sleep_sessions WHERE id = :sessionId")
    suspend fun getSession(sessionId: Long): SleepSessionEntity?

    /** 观察指定记录，详情页使用。 */
    @Query("SELECT * FROM sleep_sessions WHERE id = :sessionId")
    fun observeSession(sessionId: Long): Flow<SleepSessionEntity?>

    /**
     * 一次性读取进行中的记录。
     *
     * Service 被系统杀掉后重启（START_STICKY 且 intent 为 null）时，靠它找回要继续写入的那一晚。
     */
    @Query("SELECT * FROM sleep_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    suspend fun getActiveSession(): SleepSessionEntity?

    /** 观察进行中的记录；页面据此判断是"准备"还是"监测中"。 */
    @Query("SELECT * FROM sleep_sessions WHERE endTime IS NULL ORDER BY startTime DESC LIMIT 1")
    fun observeActiveSession(): Flow<SleepSessionEntity?>

    /** 按开始时间倒序观察已结束的记录。 */
    @Query("SELECT * FROM sleep_sessions WHERE endTime IS NOT NULL ORDER BY startTime DESC")
    fun observeFinishedSessions(): Flow<List<SleepSessionEntity>>

    // ==================== 分钟数据 ====================

    /** 保存一分钟聚合数据，Service 每分钟调用一次。 */
    @Insert
    suspend fun insertEpoch(epoch: SleepEpochEntity)

    /** 一次性读取整晚分钟数据，结束分析时使用。 */
    @Query("SELECT * FROM sleep_epochs WHERE sessionId = :sessionId ORDER BY startTime ASC")
    suspend fun getEpochs(sessionId: Long): List<SleepEpochEntity>

    /** 观察整晚分钟数据，详情页使用。 */
    @Query("SELECT * FROM sleep_epochs WHERE sessionId = :sessionId ORDER BY startTime ASC")
    fun observeEpochs(sessionId: Long): Flow<List<SleepEpochEntity>>
}
