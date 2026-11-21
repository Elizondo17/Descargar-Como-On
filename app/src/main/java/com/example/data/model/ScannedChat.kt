package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "scanned_chats")
data class ScannedChat(
    @PrimaryKey val id: String, // Phone number or unique contact ID
    val name: String,
    val phoneNumber: String,
    val dateText: String = "",
    val lastMessage: String = "",
    val folderName: String = "",
    val isSelected: Boolean = true,
    val isSent: Boolean = false,
    val scannedAt: Long = System.currentTimeMillis()
)
