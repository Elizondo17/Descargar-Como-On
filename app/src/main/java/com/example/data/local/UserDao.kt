package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserAccount
import kotlinx.coroutines.flow.Flow

@Dao
interface UserDao {
    @Query("SELECT * FROM users ORDER BY createdAt DESC")
    fun getAllUsers(): Flow<List<UserAccount>>

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun getUserByUsername(username: String): UserAccount?

    @Query("SELECT * FROM users WHERE LOWER(email) = LOWER(:email) LIMIT 1")
    suspend fun getUserByEmail(email: String): UserAccount?

    @Query("SELECT * FROM users WHERE LOWER(username) = LOWER(:username) AND token = :token LIMIT 1")
    suspend fun getUserByCredentials(username: String, token: String): UserAccount?

    @Query("SELECT * FROM users WHERE token = :token LIMIT 1")
    suspend fun getUserByToken(token: String): UserAccount?

    @Query("SELECT * FROM users WHERE id = :id LIMIT 1")
    suspend fun getUserById(id: String): UserAccount?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertUser(user: UserAccount)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(users: List<UserAccount>)

    @Update
    suspend fun updateUser(user: UserAccount)

    @Delete
    suspend fun deleteUser(user: UserAccount)

    @Query("DELETE FROM users WHERE id = :id")
    suspend fun deleteUserById(id: String)

    @Query("UPDATE users SET isBlocked = :blocked WHERE id = :userId")
    suspend fun setUserBlocked(userId: String, blocked: Boolean)

    @Query("UPDATE users SET expiresAtMs = :newExpiresAtMs, daysGranted = :newDaysGranted WHERE id = :userId")
    suspend fun updateExpiration(userId: String, newExpiresAtMs: Long, newDaysGranted: Int)

    @Query("UPDATE users SET username = :newUsername, notes = :newNotes, expiresAtMs = :newExpiresAtMs, daysGranted = :newDaysGranted WHERE id = :userId")
    suspend fun updateUserDetails(
        userId: String,
        newUsername: String,
        newNotes: String,
        newExpiresAtMs: Long,
        newDaysGranted: Int
    )

    @Query("UPDATE users SET token = :newPassword, hasCustomPassword = 1, mustChangePassword = 0, passwordChangedAt = :changedAt WHERE id = :userId")
    suspend fun updatePersonalPassword(userId: String, newPassword: String, changedAt: Long)

    @Query("UPDATE users SET canAccessLegacyModules = :allow WHERE id = :userId")
    suspend fun setUserLegacyAccess(userId: String, allow: Boolean)

    @Query("UPDATE users SET updateRequired = :updateRequired, requiredVersionCode = :requiredVersionCode, updateLockReason = :reason WHERE id = :userId")
    suspend fun setUserUpdateLock(userId: String, updateRequired: Boolean, requiredVersionCode: Int, reason: String)
}
