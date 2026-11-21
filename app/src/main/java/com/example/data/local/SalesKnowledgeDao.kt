package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.SalesKnowledgeItem
import kotlinx.coroutines.flow.Flow

@Dao
interface SalesKnowledgeDao {

    @Query("SELECT * FROM sales_knowledge ORDER BY successScore DESC, createdAt DESC")
    fun getAllKnowledge(): Flow<List<SalesKnowledgeItem>>

    @Query("SELECT * FROM sales_knowledge ORDER BY successScore DESC LIMIT :limit")
    suspend fun getTopWinningKnowledge(limit: Int = 10): List<SalesKnowledgeItem>

    @Query("SELECT * FROM sales_knowledge WHERE isQuestion = 1 ORDER BY successScore DESC LIMIT :limit")
    suspend fun getTopQuestions(limit: Int = 6): List<SalesKnowledgeItem>

    @Query("SELECT * FROM sales_knowledge WHERE isQuestion = 0 ORDER BY successScore DESC LIMIT :limit")
    suspend fun getTopResponses(limit: Int = 6): List<SalesKnowledgeItem>

    @Query("SELECT * FROM sales_knowledge WHERE id = :id LIMIT 1")
    suspend fun getKnowledgeById(id: String): SalesKnowledgeItem?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledge(item: SalesKnowledgeItem)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertKnowledgeList(items: List<SalesKnowledgeItem>)

    @Update
    suspend fun updateKnowledge(item: SalesKnowledgeItem)

    @Query("UPDATE sales_knowledge SET successScore = successScore + :increment, usageCount = usageCount + 1 WHERE id = :id")
    suspend fun incrementSuccessScore(id: String, increment: Int = 1)

    @Query("DELETE FROM sales_knowledge WHERE id = :id")
    suspend fun deleteKnowledge(id: String)

    @Query("DELETE FROM sales_knowledge")
    suspend fun clearAllKnowledge()
}
