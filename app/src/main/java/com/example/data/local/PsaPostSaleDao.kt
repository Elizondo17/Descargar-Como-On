package com.example.data.local

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PsaPostSaleInstallation
import kotlinx.coroutines.flow.Flow

@Dao
interface PsaPostSaleDao {
    @Query("SELECT * FROM psa_post_sale_installations ORDER BY nextReplacementAtMs ASC")
    fun getAllInstallations(): Flow<List<PsaPostSaleInstallation>>

    @Query("SELECT * FROM psa_post_sale_installations WHERE id = :id LIMIT 1")
    suspend fun getInstallationById(id: String): PsaPostSaleInstallation?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInstallation(item: PsaPostSaleInstallation)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<PsaPostSaleInstallation>)

    @Update
    suspend fun updateInstallation(item: PsaPostSaleInstallation)

    @Query("DELETE FROM psa_post_sale_installations WHERE id = :id")
    suspend fun deleteById(id: String)

    @Query("DELETE FROM psa_post_sale_installations")
    suspend fun clearAll()
}
