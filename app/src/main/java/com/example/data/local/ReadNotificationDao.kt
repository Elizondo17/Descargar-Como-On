package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.ReadNotificationEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ReadNotificationDao {
    @Query("SELECT notifId FROM read_notification_ids")
    fun getReadNotificationIds(): Flow<List<String>>

    @Query("SELECT notifId FROM read_notification_ids")
    suspend fun getReadNotificationIdsOnce(): List<String>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markRead(entity: ReadNotificationEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun markAllRead(entities: List<ReadNotificationEntity>)

    @Query("DELETE FROM read_notification_ids")
    suspend fun clearAll()
}
