package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "Scripts")
data class ScriptItem(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val folderId: Long,
    val title: String,
    val content: String,
    val isAutoResponse: Boolean = false,
    val triggerKeyword: String = "",
    val usageCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)

typealias Script = ScriptItem
