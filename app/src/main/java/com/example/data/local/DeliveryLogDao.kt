package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.DeliveryLog
import kotlinx.coroutines.flow.Flow

@Dao
interface DeliveryLogDao {
    @Query("SELECT * FROM TaskLogs ORDER BY timestamp DESC")
    fun getAllLogs(): Flow<List<DeliveryLog>>

    @Query("SELECT * FROM TaskLogs ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentLogs(limit: Int): Flow<List<DeliveryLog>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLog(log: DeliveryLog): Long

    @Query("DELETE FROM TaskLogs")
    suspend fun clearLogs()

    @Query("SELECT COUNT(*) FROM TaskLogs WHERE status = 'SUCCESS'")
    fun getSuccessCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM TaskLogs WHERE status = 'FAILED'")
    fun getFailedCount(): Flow<Int>

    @Query("SELECT COUNT(*) FROM TaskLogs")
    fun getTotalCount(): Flow<Int>
}

typealias TaskLogDao = DeliveryLogDao
