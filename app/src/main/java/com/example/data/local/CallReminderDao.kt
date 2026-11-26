package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.CallReminderEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface CallReminderDao {
    @Query("SELECT * FROM psa_call_reminders ORDER BY triggerAtMs ASC")
    fun getAllReminders(): Flow<List<CallReminderEntity>>

    @Query("SELECT * FROM psa_call_reminders ORDER BY triggerAtMs ASC")
    suspend fun getAllRemindersOnce(): List<CallReminderEntity>

    @Query("SELECT * FROM psa_call_reminders WHERE id = :id LIMIT 1")
    suspend fun getReminderById(id: String): CallReminderEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertReminder(reminder: CallReminderEntity)

    @Query("UPDATE psa_call_reminders SET isEnabled = :enabled WHERE id = :id")
    suspend fun setReminderEnabled(id: String, enabled: Boolean)

    @Query("DELETE FROM psa_call_reminders WHERE id = :id")
    suspend fun deleteReminderById(id: String)

    @Query("DELETE FROM psa_call_reminders")
    suspend fun clearAll()
}
