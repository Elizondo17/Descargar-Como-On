package com.example.data.repository

import com.example.data.model.PsaContactLead
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaPostSaleInstallation

data class SellerWorkspaceSnapshot<T>(
    val items: List<T>,
    val isFromCache: Boolean
)

/**
 * Abstraction over Firestore workspace sync operations used by [EzoRepository]
 * so Room offline-first outbox flushing and conflict-safe merges can be tested
 * deterministically in Robolectric unit tests while running against real Firestore in production.
 */
interface AgendaCloudSyncGateway {
    fun currentOwnerUid(): String
    suspend fun upsertVisit(ownerUid: String, visit: PsaHomeVisit): Result<Unit>
    suspend fun deleteVisit(ownerUid: String, visitId: String): Result<Unit>
    suspend fun upsertPostSale(ownerUid: String, record: PsaPostSaleInstallation): Result<Unit>
    suspend fun deletePostSale(ownerUid: String, recordId: String): Result<Unit>
    suspend fun upsertContact(ownerUid: String, lead: PsaContactLead): Result<Unit>
    suspend fun deleteContact(ownerUid: String, contactId: String): Result<Unit>
}
