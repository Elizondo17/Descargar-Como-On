package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BusinessInviteEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessInviteDao {
    @Query("SELECT * FROM business_invites ORDER BY CASE WHEN presentationDate = '' THEN 1 ELSE 0 END ASC, presentationDate ASC, presentationTime ASC, createdAtMs DESC")
    fun getAllInvites(): Flow<List<BusinessInviteEntity>>

    @Query("SELECT * FROM business_invites")
    suspend fun getAllInvitesOnce(): List<BusinessInviteEntity>

    @Query("SELECT * FROM business_invites WHERE id = :id LIMIT 1")
    suspend fun getInviteById(id: String): BusinessInviteEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvite(invite: BusinessInviteEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInvites(invites: List<BusinessInviteEntity>)

    @Query("DELETE FROM business_invites WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM business_invites")
    suspend fun clearAll()
}
