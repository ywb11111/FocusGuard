package com.ywb.focusguard.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.ywb.focusguard.data.local.entity.FocusSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface FocusSessionDao {
    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC")
    fun observeSessions(): Flow<List<FocusSessionEntity>>

    @Query("SELECT * FROM focus_sessions WHERE id = :sessionId LIMIT 1")
    fun observeSession(sessionId: Long): Flow<FocusSessionEntity?>

    @Query("SELECT * FROM focus_sessions ORDER BY startTime DESC LIMIT 1")
    fun observeLatestSession(): Flow<FocusSessionEntity?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSession(session: FocusSessionEntity): Long
}
