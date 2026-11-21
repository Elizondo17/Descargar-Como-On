package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScannedChat
import kotlinx.coroutines.flow.Flow

@Dao
interface ScannedChatDao {
    @Query("SELECT * FROM scanned_chats ORDER BY scannedAt DESC")
    fun getAllScannedChats(): Flow<List<ScannedChat>>

    @Query("SELECT * FROM scanned_chats WHERE isSelected = 1 ORDER BY scannedAt DESC")
    fun getSelectedChats(): Flow<List<ScannedChat>>

    @Query("SELECT * FROM scanned_chats WHERE id = :id LIMIT 1")
    suspend fun getChatById(id: String): ScannedChat?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChats(chats: List<ScannedChat>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChat(chat: ScannedChat)

    @Update
    suspend fun updateChat(chat: ScannedChat)

    @Query("UPDATE scanned_chats SET isSelected = :selected WHERE id = :id")
    suspend fun setSelected(id: String, selected: Boolean)

    @Query("UPDATE scanned_chats SET isSelected = :selected WHERE id IN (:ids)")
    suspend fun setSelectedForIds(ids: List<String>, selected: Boolean)

    @Query("UPDATE scanned_chats SET isSelected = :selected")
    suspend fun setAllSelected(selected: Boolean)

    @Query("UPDATE scanned_chats SET isSent = 1 WHERE id = :id")
    suspend fun markAsSent(id: String)

    @Query("DELETE FROM scanned_chats WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM scanned_chats")
    suspend fun clearAll()
}
