package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.ScriptFolder
import com.example.data.model.ScriptItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ScriptDao {
    @Query("SELECT * FROM UserFolders ORDER BY id ASC")
    fun getAllFolders(): Flow<List<ScriptFolder>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertFolder(folder: ScriptFolder): Long

    @Update
    suspend fun updateFolder(folder: ScriptFolder)

    @Delete
    suspend fun deleteFolder(folder: ScriptFolder)

    @Query("SELECT * FROM Scripts WHERE folderId = :folderId ORDER BY createdAt DESC")
    fun getScriptsForFolder(folderId: Long): Flow<List<ScriptItem>>

    @Query("SELECT * FROM Scripts ORDER BY createdAt DESC")
    fun getAllScripts(): Flow<List<ScriptItem>>

    @Query("SELECT * FROM Scripts WHERE id = :id LIMIT 1")
    suspend fun getScriptById(id: Long): ScriptItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertScript(script: ScriptItem): Long

    @Update
    suspend fun updateScript(script: ScriptItem)

    @Delete
    suspend fun deleteScript(script: ScriptItem)

    @Query("UPDATE Scripts SET usageCount = usageCount + 1 WHERE id = :scriptId")
    suspend fun incrementScriptUsage(scriptId: Long)
}
