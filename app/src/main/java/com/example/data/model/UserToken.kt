package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "UserTokens")
data class UserToken(
    @PrimaryKey val token: String,
    val userId: String,
    val username: String,
    val role: String, // "SUPERUSER" or "OPERATOR"
    val validFromMs: Long,
    val expiresAtMs: Long,
    val daysGranted: Int,
    val isBlocked: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val lastValidatedAt: Long = System.currentTimeMillis(),
    val notes: String = ""
) {
    val isExpired: Boolean
        get() = role != "SUPERUSER" && System.currentTimeMillis() > expiresAtMs

    val remainingDays: Long
        get() {
            if (role == "SUPERUSER") return 9999L
            val diff = expiresAtMs - System.currentTimeMillis()
            return if (diff > 0) (diff / (1000L * 60 * 60 * 24)) else 0L
        }

    val remainingHours: Long
        get() {
            if (role == "SUPERUSER") return 99999L
            val diff = expiresAtMs - System.currentTimeMillis()
            return if (diff > 0) (diff / (1000L * 60 * 60)) else 0L
        }
}
