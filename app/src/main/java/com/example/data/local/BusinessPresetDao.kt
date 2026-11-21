package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.example.data.model.BusinessPreset
import kotlinx.coroutines.flow.Flow

@Dao
interface BusinessPresetDao {
    @Query("SELECT * FROM business_presets ORDER BY isCustom ASC, createdAt DESC")
    fun getAllPresets(): Flow<List<BusinessPreset>>

    @Query("SELECT * FROM business_presets WHERE id = :id LIMIT 1")
    suspend fun getPresetById(id: String): BusinessPreset?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPreset(preset: BusinessPreset)

    @Delete
    suspend fun deletePreset(preset: BusinessPreset)
}
