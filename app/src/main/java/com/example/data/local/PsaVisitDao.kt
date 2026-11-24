package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PsaHomeVisit
import kotlinx.coroutines.flow.Flow

@Dao
interface PsaVisitDao {
    @Query("SELECT * FROM psa_home_visits ORDER BY scheduledDate ASC, scheduledTime ASC")
    fun getAllVisits(): Flow<List<PsaHomeVisit>>

    @Query("SELECT * FROM psa_home_visits ORDER BY scheduledDate ASC, scheduledTime ASC")
    suspend fun getAllVisitsOnce(): List<PsaHomeVisit>

    @Query("SELECT * FROM psa_home_visits WHERE scheduledDate = :dateKey ORDER BY scheduledTime ASC")
    fun getVisitsForDate(dateKey: String): Flow<List<PsaHomeVisit>>

    @Query("SELECT * FROM psa_home_visits WHERE id = :id LIMIT 1")
    suspend fun getVisitById(id: String): PsaHomeVisit?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisit(visit: PsaHomeVisit)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertVisits(visits: List<PsaHomeVisit>)

    @Update
    suspend fun updateVisit(visit: PsaHomeVisit)

    @Delete
    suspend fun deleteVisit(visit: PsaHomeVisit)

    @Query("DELETE FROM psa_home_visits WHERE id = :id")
    suspend fun deleteVisitById(id: String)

    @Query("DELETE FROM psa_home_visits")
    suspend fun clearAllVisits()
}
