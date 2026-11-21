package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "business_presets")
data class BusinessPreset(
    @PrimaryKey
    val id: String,
    val title: String,
    val businessName: String,
    val businessType: String,
    val offeringDescription: String,
    val toneAndStyle: String,
    val salesObjective: String,
    val frequentQuestionsAndObjections: String,
    val isCustom: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
