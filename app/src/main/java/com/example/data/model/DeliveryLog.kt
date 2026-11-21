package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "TaskLogs")
data class DeliveryLog(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val recipientId: String,
    val recipientPreview: String = "",
    val crmFolder: String,
    val scriptTitle: String,
    val scriptSnippet: String,
    val status: String, // "SUCCESS" or "FAILED"
    val errorMessage: String? = null,
    val delaySeconds: Int = 35,
    val timestamp: Long = System.currentTimeMillis()
)

typealias TaskLog = DeliveryLog

data class DeliveryStats(
    val totalCount: Int = 0,
    val successCount: Int = 0,
    val failedCount: Int = 0,
    val successRate: Float = 0f,
    val failureRate: Float = 0f
)
