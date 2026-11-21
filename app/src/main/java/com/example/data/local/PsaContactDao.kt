package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.PsaContactInteraction
import com.example.data.model.PsaContactLead
import kotlinx.coroutines.flow.Flow

@Dao
interface PsaContactDao {
    // Leads
    @Query("SELECT * FROM psa_contact_leads ORDER BY createdAtMs DESC")
    fun getAllLeads(): Flow<List<PsaContactLead>>

    @Query("SELECT * FROM psa_contact_leads WHERE id = :id LIMIT 1")
    suspend fun getLeadById(id: String): PsaContactLead?

    @Query("SELECT * FROM psa_contact_leads WHERE id IN (:ids)")
    suspend fun getLeadsByIds(ids: List<String>): List<PsaContactLead>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLead(lead: PsaContactLead)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertLeads(leads: List<PsaContactLead>)

    @Update
    suspend fun updateLead(lead: PsaContactLead)

    @Update
    suspend fun updateLeads(leads: List<PsaContactLead>)

    @Delete
    suspend fun deleteLead(lead: PsaContactLead)

    @Query("DELETE FROM psa_contact_leads WHERE id = :id")
    suspend fun deleteLeadById(id: String)

    @Query("DELETE FROM psa_contact_leads WHERE id IN (:ids)")
    suspend fun deleteLeadsByIds(ids: List<String>)

    @Query("DELETE FROM psa_contact_leads")
    suspend fun clearAllLeads()

    // Interactions
    @Query("SELECT * FROM psa_contact_interactions ORDER BY timestampMs DESC")
    fun getAllInteractions(): Flow<List<PsaContactInteraction>>

    @Query("SELECT * FROM psa_contact_interactions WHERE dateKey = :dateKey ORDER BY timestampMs DESC")
    fun getInteractionsForDate(dateKey: String): Flow<List<PsaContactInteraction>>

    @Query("SELECT * FROM psa_contact_interactions WHERE leadId = :leadId ORDER BY timestampMs DESC")
    fun getInteractionsForLead(leadId: String): Flow<List<PsaContactInteraction>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInteraction(interaction: PsaContactInteraction)

    @Delete
    suspend fun deleteInteraction(interaction: PsaContactInteraction)

    @Query("DELETE FROM psa_contact_interactions")
    suspend fun clearAllInteractions()
}
