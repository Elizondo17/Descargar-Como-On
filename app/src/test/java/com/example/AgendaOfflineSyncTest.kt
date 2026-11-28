package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.AgendaSyncEntityType
import com.example.data.model.AgendaSyncOperation
import com.example.data.model.BusinessInviteEntity
import com.example.data.model.BusinessInviteStatus
import com.example.data.model.PsaContactLead
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaLeadStatus
import com.example.data.model.PsaPostSaleInstallation
import com.example.data.model.PsaVisitStatus
import com.example.data.repository.AgendaCloudSyncGateway
import com.example.data.repository.EzoRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class AgendaOfflineSyncTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase

    private class FakeAgendaCloudGateway(
        var uid: String = "seller-uid-1"
    ) : AgendaCloudSyncGateway {
        val remoteVisits = linkedMapOf<String, PsaHomeVisit>()
        val remotePostSale = linkedMapOf<String, PsaPostSaleInstallation>()
        val remoteContacts = linkedMapOf<String, PsaContactLead>()
        val remoteBusinessInvites = linkedMapOf<String, BusinessInviteEntity>()
        val failVisitIds = mutableSetOf<String>()
        val operationLog = mutableListOf<String>()

        override fun currentOwnerUid(): String = uid

        override suspend fun upsertVisit(ownerUid: String, visit: PsaHomeVisit): Result<Unit> {
            operationLog += "UPSERT_VISIT:${visit.id}"
            if (visit.id in failVisitIds) {
                return Result.failure(IllegalStateException("Firestore unavailable"))
            }
            remoteVisits[visit.id] = visit.copy(ownerUid = ownerUid)
            return Result.success(Unit)
        }

        override suspend fun deleteVisit(ownerUid: String, visitId: String): Result<Unit> {
            operationLog += "DELETE_VISIT:$visitId"
            if (visitId in failVisitIds) {
                return Result.failure(IllegalStateException("Firestore unavailable"))
            }
            remoteVisits.remove(visitId)
            return Result.success(Unit)
        }

        override suspend fun upsertPostSale(ownerUid: String, record: PsaPostSaleInstallation): Result<Unit> {
            operationLog += "UPSERT_POST_SALE:${record.id}"
            remotePostSale[record.id] = record.copy(ownerUid = ownerUid)
            return Result.success(Unit)
        }

        override suspend fun deletePostSale(ownerUid: String, recordId: String): Result<Unit> {
            operationLog += "DELETE_POST_SALE:$recordId"
            remotePostSale.remove(recordId)
            return Result.success(Unit)
        }

        override suspend fun upsertContact(ownerUid: String, lead: PsaContactLead): Result<Unit> {
            operationLog += "UPSERT_CONTACT:${lead.id}"
            remoteContacts[lead.id] = lead
            return Result.success(Unit)
        }

        override suspend fun deleteContact(ownerUid: String, contactId: String): Result<Unit> {
            operationLog += "DELETE_CONTACT:$contactId"
            remoteContacts.remove(contactId)
            return Result.success(Unit)
        }

        override suspend fun upsertBusinessInvite(ownerUid: String, invite: BusinessInviteEntity): Result<Unit> {
            operationLog += "UPSERT_BUSINESS_INVITE:${invite.id}"
            remoteBusinessInvites[invite.id] = invite.copy(ownerUid = ownerUid)
            return Result.success(Unit)
        }

        override suspend fun deleteBusinessInvite(ownerUid: String, inviteId: String): Result<Unit> {
            operationLog += "DELETE_BUSINESS_INVITE:$inviteId"
            remoteBusinessInvites.remove(inviteId)
            return Result.success(Unit)
        }
    }

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `offline agenda writes persist in Room queue and auto sync cleanly when connectivity returns`() = runBlocking {
        var online = false
        val gateway = FakeAgendaCloudGateway(uid = "seller-uid-1")
        val repo = EzoRepository(
            database = db,
            context = context,
            cloudGatewayOverride = gateway,
            networkOnlineProvider = { online }
        )

        val offlineVisit1 = PsaHomeVisit(
            id = "visit-off-1",
            customerName = "Lucía Fernández",
            phone = "1144556677",
            address = "Av. Callao 450",
            neighborhood = "Balvanera",
            scheduledDate = "2026-11-24",
            scheduledTime = "10:00",
            status = PsaVisitStatus.AGENDADA
        )
        val offlineVisit2 = PsaHomeVisit(
            id = "visit-off-2",
            customerName = "Marcos Díaz",
            phone = "1199887766",
            address = "Rivadavia 3100",
            scheduledDate = "2026-11-24",
            scheduledTime = "15:00",
            status = PsaVisitStatus.AGENDADA
        )
        val offlinePostSale = PsaPostSaleInstallation(
            id = "ps-off-1",
            customerName = "Lucía Fernández",
            purifierId = "senior4",
            purifierName = "PSA Senior 4"
        )
        val offlineLead = PsaContactLead(
            id = "lead-off-1",
            name = "Jorge Paz",
            phone = "1122003344",
            status = PsaLeadStatus.INTERESTED
        )

        // Create & mutate completely offline
        repo.insertPsaVisit(offlineVisit1)
        repo.updatePsaVisit(offlineVisit1.copy(status = PsaVisitStatus.CONFIRMADA, notes = "Confirmó 10am"))
        repo.insertPsaVisit(offlineVisit2)
        repo.deletePsaVisit(offlineVisit2.id)
        repo.insertPostSaleInstallation(offlinePostSale)
        repo.insertPsaLead(offlineLead)

        // Nothing touched remote Firestore while offline
        assertTrue(gateway.operationLog.isEmpty())
        assertTrue(gateway.remoteVisits.isEmpty())

        // Room holds exact offline state and pending outbox items
        val storedVisits = db.psaVisitDao().getAllVisitsOnce()
        assertEquals(1, storedVisits.size)
        assertEquals(PsaVisitStatus.CONFIRMADA, storedVisits.first().status)
        assertEquals("Confirmó 10am", storedVisits.first().notes)

        val pendingOps = db.agendaSyncDao().getAllPendingOpsOnce()
        // visit-off-1 (UPSERT coalesced), visit-off-2 (DELETE), ps-off-1 (UPSERT), lead-off-1 (UPSERT)
        assertEquals(4, pendingOps.size)
        assertEquals(4, repo.pendingAgendaSyncCount.first())
        val deleteOp = pendingOps.first { it.entityId == "visit-off-2" }
        assertEquals(AgendaSyncOperation.DELETE.name, deleteOp.operation)
        assertEquals(AgendaSyncEntityType.VISIT.name, deleteOp.entityType)

        // Restore connectivity and run automatic sync flush
        online = true
        val syncResult = repo.syncPendingAgendaWithFirestore(explicitUid = "seller-uid-1", publishStats = false)
        assertTrue(syncResult.isSuccess)
        assertEquals(4, syncResult.getOrThrow())

        // Outbox must now be empty and remote cloud gateway updated
        assertEquals(0, db.agendaSyncDao().getAllPendingOpsOnce().size)
        assertEquals(0, repo.pendingAgendaSyncCount.first())
        assertEquals(1, gateway.remoteVisits.size)
        assertEquals(PsaVisitStatus.CONFIRMADA, gateway.remoteVisits["visit-off-1"]?.status)
        assertFalse(gateway.remoteVisits.containsKey("visit-off-2"))
        assertTrue(gateway.remotePostSale.containsKey("ps-off-1"))
        assertTrue(gateway.remoteContacts.containsKey("lead-off-1"))
        assertTrue(repo.agendaSyncStatus.value.lastSyncedAtMs > 0L)
        assertNull(repo.agendaSyncStatus.value.lastErrorMessage)
    }

    @Test
    fun `remote snapshot merge preserves pending offline edits and prevents resurrecting deleted visits`() = runBlocking {
        val online = false
        val gateway = FakeAgendaCloudGateway(uid = "seller-uid-1")
        val repo = EzoRepository(
            database = db,
            context = context,
            cloudGatewayOverride = gateway,
            networkOnlineProvider = { online }
        )

        val localEdited = PsaHomeVisit(
            id = "v-edit",
            customerName = "Cliente Editado Offline",
            phone = "111",
            address = "Calle 1",
            scheduledDate = "2026-11-24",
            scheduledTime = "09:00",
            status = PsaVisitStatus.CERRADA_EXITO,
            ownerUid = "seller-uid-1"
        )
        val localDeleted = PsaHomeVisit(
            id = "v-deleted",
            customerName = "Cliente Borrado Offline",
            phone = "222",
            address = "Calle 2",
            scheduledDate = "2026-11-24",
            scheduledTime = "11:00",
            status = PsaVisitStatus.AGENDADA,
            ownerUid = "seller-uid-1"
        )
        val cloudOnly = PsaHomeVisit(
            id = "v-cloud-other",
            customerName = "Cliente De Otro Equipo",
            phone = "333",
            address = "Calle 3",
            scheduledDate = "2026-11-24",
            scheduledTime = "16:00",
            status = PsaVisitStatus.AGENDADA,
            ownerUid = "seller-uid-1"
        )

        repo.insertPsaVisit(localEdited)
        repo.insertPsaVisit(localDeleted)
        repo.deletePsaVisit(localDeleted.id)

        // Simulate empty cache snapshot from Firestore while offline -> MUST NOT wipe Room
        repo.replaceRemoteVisits(emptyList(), isFromCache = true)
        assertEquals(1, db.psaVisitDao().getAllVisitsOnce().size)

        // Simulate stale server snapshot containing old status for v-edit, resurrected v-deleted, and new v-cloud-other
        val staleRemoteEdited = localEdited.copy(status = PsaVisitStatus.AGENDADA, customerName = "Nombre Viejo Nube")
        repo.replaceRemoteVisits(listOf(staleRemoteEdited, localDeleted, cloudOnly), isFromCache = false)

        val afterMerge = db.psaVisitDao().getAllVisitsOnce()
        assertEquals(2, afterMerge.size)
        val preservedEdited = afterMerge.firstOrNull { it.id == "v-edit" }
        assertNotNull(preservedEdited)
        assertEquals(PsaVisitStatus.CERRADA_EXITO, preservedEdited!!.status)
        assertEquals("Cliente Editado Offline", preservedEdited.customerName)
        assertNull("Locally deleted visit must not be resurrected", afterMerge.firstOrNull { it.id == "v-deleted" })
        assertNotNull("Cloud-only visit should still merge cleanly", afterMerge.firstOrNull { it.id == "v-cloud-other" })
    }

    @Test
    fun `failed cloud write keeps item queued with retry metadata and succeeds on subsequent recovery`() = runBlocking {
        val online = true
        val gateway = FakeAgendaCloudGateway(uid = "seller-uid-1").apply {
            failVisitIds += "visit-flaky"
        }
        val repo = EzoRepository(
            database = db,
            context = context,
            cloudGatewayOverride = gateway,
            networkOnlineProvider = { online }
        )

        // Seed directly into Room + outbox with network temporarily failing on visit-flaky
        val flakyVisit = PsaHomeVisit(
            id = "visit-flaky",
            customerName = "Cliente Reintento",
            phone = "555",
            address = "Mitre 100",
            scheduledDate = "2026-11-25",
            scheduledTime = "12:00"
        )
        db.psaVisitDao().insertVisit(flakyVisit)
        db.agendaSyncDao().enqueueOp(
            com.example.data.model.PendingAgendaSyncEntity(
                queueKey = "VISIT:visit-flaky",
                entityType = AgendaSyncEntityType.VISIT.name,
                entityId = "visit-flaky",
                operation = AgendaSyncOperation.UPSERT.name,
                ownerUid = "seller-uid-1",
                queuedAtMs = 1000L
            )
        )

        val firstAttempt = repo.syncPendingAgendaWithFirestore(explicitUid = "seller-uid-1", publishStats = false)
        assertTrue(firstAttempt.isFailure)
        val remainingAfterFail = db.agendaSyncDao().getAllPendingOpsOnce()
        assertEquals(1, remainingAfterFail.size)
        assertEquals(1, remainingAfterFail.first().retryCount)
        assertTrue(remainingAfterFail.first().lastError.contains("Firestore unavailable"))

        // Network/backend recovers -> next sync flushes successfully
        gateway.failVisitIds.clear()
        val secondAttempt = repo.syncPendingAgendaWithFirestore(explicitUid = "seller-uid-1", publishStats = false)
        assertTrue(secondAttempt.isSuccess)
        assertEquals(1, secondAttempt.getOrThrow())
        assertTrue(db.agendaSyncDao().getAllPendingOpsOnce().isEmpty())
        assertTrue(gateway.remoteVisits.containsKey("visit-flaky"))
    }

    @Test
    fun `migration 15 to 16 creates pending_agenda_sync while preserving existing offline visits`() {
        val dbName = "migration_15_16_test.db"
        context.deleteDatabase(dbName)

        // Create full schema first so all 13 pre-existing v15 tables match Room's schema validator,
        // then strip pending_agenda_sync, insert a v15 visit, and downgrade PRAGMA user_version = 15.
        val seedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        seedDb.openHelper.writableDatabase
        seedDb.close()

        val rawDb = SQLiteDatabase.openDatabase(
            context.getDatabasePath(dbName).path,
            null,
            SQLiteDatabase.OPEN_READWRITE
        )
        rawDb.use { sqlite ->
            sqlite.execSQL("DROP TABLE IF EXISTS `pending_agenda_sync`")
            sqlite.execSQL("DROP TABLE IF EXISTS `psa_call_reminders`")
            sqlite.execSQL("DROP TABLE IF EXISTS `read_notification_ids`")
            sqlite.execSQL("DROP TABLE IF EXISTS `business_invites`")
            sqlite.execSQL(
                """
                INSERT INTO `psa_home_visits` (
                    `id`, `customerName`, `phone`, `address`, `neighborhood`,
                    `scheduledDate`, `scheduledTime`, `modelInterest`, `status`,
                    `chlorinePpm`, `bottlesPerWeek`, `quotedPlan`, `notes`,
                    `createdAtMs`, `ownerUid`, `waterSource`, `hardnessPpm`,
                    `realLitersPerDay`, `cartSummary`, `soldCartSummary`,
                    `testPhotoPath`, `diagnosticTimestampMs`, `updatedAtMs`
                ) VALUES (
                    'legacy-v15-visit', 'Elena V15', '11334455', 'San Martin 900', 'Centro',
                    '2026-11-26', '17:00', 'PSA Senior 4', 'AGENDADA',
                    1.5, 4, '', 'Offline v15',
                    1000, '', 'AGUA_CORRIENTE', 0,
                    8.0, '', '', '', 0, 1000
                )
                """.trimIndent()
            )
            sqlite.execSQL("PRAGMA user_version = 15")
        }

        val migratedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(
                AppDatabase.MIGRATION_15_16,
                AppDatabase.MIGRATION_16_17,
                AppDatabase.MIGRATION_17_18,
                AppDatabase.MIGRATION_18_19,
                AppDatabase.MIGRATION_19_20
            )
            .allowMainThreadQueries()
            .build()
        try {
            runBlocking {
                val visits = migratedDb.psaVisitDao().getAllVisitsOnce()
                assertEquals(1, visits.size)
                assertEquals("legacy-v15-visit", visits.first().id)
                assertEquals("Elena V15", visits.first().customerName)

                // New pending_agenda_sync & business_invites tables exist and are usable immediately
                assertTrue(migratedDb.agendaSyncDao().getAllPendingOpsOnce().isEmpty())
                assertTrue(migratedDb.businessInviteDao().getAllInvitesOnce().isEmpty())
            }
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `offline business invite writes persist locally and sync cleanly when online`() = runBlocking {
        var online = false
        val gateway = FakeAgendaCloudGateway(uid = "seller-uid-1")
        val repo = EzoRepository(
            database = db,
            context = context,
            cloudGatewayOverride = gateway,
            networkOnlineProvider = { online }
        )

        val invite = BusinessInviteEntity(
            id = "biz-inv-1",
            name = "Marcos Silva",
            phone = "+5491166554433",
            email = "marcos@gmail.com",
            status = BusinessInviteStatus.INVITED.name,
            presentationDate = "2026-11-28",
            presentationTime = "19:30",
            reminderEnabled = true,
            reminderAtMs = 1_800_000_000_000L
        )
        repo.saveBusinessInvite(invite)

        assertEquals(1, db.businessInviteDao().getAllInvitesOnce().size)
        assertEquals(1, db.agendaSyncDao().getAllPendingOpsOnce().size)
        assertTrue(gateway.remoteBusinessInvites.isEmpty())

        online = true
        val syncResult = repo.syncPendingAgendaWithFirestore(explicitUid = "seller-uid-1", publishStats = false)
        assertTrue(syncResult.isSuccess)
        assertEquals(1, syncResult.getOrThrow())
        assertEquals(0, db.agendaSyncDao().getAllPendingOpsOnce().size)
        assertEquals("Marcos Silva", gateway.remoteBusinessInvites["biz-inv-1"]?.name)
    }
}
