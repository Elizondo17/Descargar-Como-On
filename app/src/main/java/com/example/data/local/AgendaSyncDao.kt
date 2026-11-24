package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.PendingAgendaSyncEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface AgendaSyncDao {

    @Query("SELECT * FROM pending_agenda_sync ORDER BY queuedAtMs ASC")
    fun observePendingOps(): Flow<List<PendingAgendaSyncEntity>>

    @Query("SELECT COUNT(*) FROM pending_agenda_sync")
    fun observePendingCount(): Flow<Int>

    @Query("SELECT * FROM pending_agenda_sync ORDER BY queuedAtMs ASC")
    suspend fun getAllPendingOpsOnce(): List<PendingAgendaSyncEntity>

    @Query("SELECT * FROM pending_agenda_sync WHERE entityType = :entityType ORDER BY queuedAtMs ASC")
    suspend fun getPendingOpsByTypeOnce(entityType: String): List<PendingAgendaSyncEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueOp(op: PendingAgendaSyncEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun enqueueOps(ops: List<PendingAgendaSyncEntity>)

    @Query("DELETE FROM pending_agenda_sync WHERE queueKey = :queueKey")
    suspend fun deleteByKey(queueKey: String)

    @Query("DELETE FROM pending_agenda_sync WHERE queueKey = :queueKey AND queuedAtMs <= :expectedQueuedAtMs")
    suspend fun deleteIfUnchanged(queueKey: String, expectedQueuedAtMs: Long)

    @Query(
        "UPDATE pending_agenda_sync SET retryCount = :retryCount, lastError = :lastError " +
            "WHERE queueKey = :queueKey AND queuedAtMs <= :expectedQueuedAtMs"
    )
    suspend fun markAttemptFailed(
        queueKey: String,
        expectedQueuedAtMs: Long,
        retryCount: Int,
        lastError: String
    )

    @Query("DELETE FROM pending_agenda_sync")
    suspend fun clearAll()
}
