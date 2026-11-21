package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.UserToken
import kotlinx.coroutines.flow.Flow

@Dao
interface UserTokenDao {
    @Query("SELECT * FROM UserTokens ORDER BY createdAt DESC")
    fun getAllTokens(): Flow<List<UserToken>>

    @Query("SELECT * FROM UserTokens WHERE role != 'SUPERUSER' ORDER BY createdAt DESC")
    fun getSecondaryUserTokens(): Flow<List<UserToken>>

    @Query("SELECT * FROM UserTokens WHERE token = :tokenString LIMIT 1")
    suspend fun findByToken(tokenString: String): UserToken?

    @Query("SELECT * FROM UserTokens WHERE LOWER(username) = LOWER(:username) LIMIT 1")
    suspend fun findByUsername(username: String): UserToken?

    @Query("SELECT * FROM UserTokens WHERE userId = :userId LIMIT 1")
    suspend fun findByUserId(userId: String): UserToken?

    @Query("SELECT * FROM UserTokens WHERE role = 'SUPERUSER' LIMIT 1")
    suspend fun getSuperuserToken(): UserToken?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertToken(userToken: UserToken)

    @Update
    suspend fun updateToken(userToken: UserToken)

    @Delete
    suspend fun deleteToken(userToken: UserToken)

    @Query("DELETE FROM UserTokens WHERE userId = :userId")
    suspend fun deleteByUserId(userId: String)

    @Query("UPDATE UserTokens SET expiresAtMs = expiresAtMs + :additionalMillis, daysGranted = daysGranted + :additionalDays, isBlocked = 0 WHERE token = :tokenString")
    suspend fun extendTokenDays(tokenString: String, additionalMillis: Long, additionalDays: Int)

    @Query("UPDATE UserTokens SET isBlocked = :blocked WHERE token = :tokenString")
    suspend fun setTokenBlocked(tokenString: String, blocked: Boolean)

    @Query("UPDATE UserTokens SET isBlocked = :blocked WHERE userId = :userId")
    suspend fun setTokenBlockedByUserId(userId: String, blocked: Boolean)

    @Query("UPDATE UserTokens SET lastValidatedAt = :timestamp WHERE token = :tokenString")
    suspend fun updateLastValidated(tokenString: String, timestamp: Long)
}
