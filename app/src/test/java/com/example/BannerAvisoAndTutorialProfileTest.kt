package com.example

import android.content.Context
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.room.Room
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.MensajeGlobal
import com.example.data.model.MensajeGlobalPrioridad
import com.example.data.model.PsaContactLead
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaPostSaleInstallation
import com.example.data.model.UserAccount
import com.example.data.repository.AgendaCloudSyncGateway
import com.example.data.repository.EzoRepository
import com.example.ui.components.BannerAviso
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BannerAvisoAndTutorialProfileTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun noOpGateway() = object : AgendaCloudSyncGateway {
        override fun currentOwnerUid(): String = "uid-test"
        override suspend fun upsertVisit(ownerUid: String, visit: PsaHomeVisit): Result<Unit> = Result.success(Unit)
        override suspend fun deleteVisit(ownerUid: String, visitId: String): Result<Unit> = Result.success(Unit)
        override suspend fun upsertPostSale(ownerUid: String, record: PsaPostSaleInstallation): Result<Unit> = Result.success(Unit)
        override suspend fun deletePostSale(ownerUid: String, recordId: String): Result<Unit> = Result.success(Unit)
        override suspend fun upsertContact(ownerUid: String, lead: PsaContactLead): Result<Unit> = Result.success(Unit)
        override suspend fun deleteContact(ownerUid: String, contactId: String): Result<Unit> = Result.success(Unit)
    }

    @Test
    fun `tutorialCompleted flag is tracked independently per seller and survives local cache sync`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("como_on_auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repo = EzoRepository(db, context, cloudGatewayOverride = noOpGateway())
            val sellerA = UserAccount(
                id = "op_a",
                username = "Vendedor A",
                token = "GOOGLE-AUTH",
                role = "OPERATOR",
                daysGranted = 30,
                validFromMs = 1000L,
                expiresAtMs = System.currentTimeMillis() + 86_400_000L,
                email = "vendedora@gmail.com",
                tutorialCompleted = false
            )
            val sellerB = UserAccount(
                id = "op_b",
                username = "Vendedor B",
                token = "GOOGLE-AUTH",
                role = "OPERATOR",
                daysGranted = 30,
                validFromMs = 1000L,
                expiresAtMs = System.currentTimeMillis() + 86_400_000L,
                email = "vendedorb@gmail.com",
                tutorialCompleted = false
            )

            runBlocking {
                repo.markWelcomeTutorialCompletedLocally(sellerA, true)
                val storedA = db.userDao().getUserById("op_a")
                assertNotNull(storedA)
                assertTrue(storedA!!.tutorialCompleted)
                assertTrue(repo.hasSeenWelcomeTutorialForUser("vendedora@gmail.com"))
                assertFalse(repo.hasSeenWelcomeTutorialForUser("vendedorb@gmail.com"))

                // Even if an un-updated cached cloud snapshot temporarily emits tutorialCompleted=false, local completion stays true
                repo.upsertUserLocally(sellerA.copy(tutorialCompleted = false))
                assertTrue(db.userDao().getUserById("op_a")!!.tutorialCompleted)

                // Switching to Seller B on same phone keeps Seller B eligible to see tutorial once
                repo.upsertUserLocally(sellerB)
                assertFalse(db.userDao().getUserById("op_b")!!.tutorialCompleted)

                // If Seller B already completed tutorial on another device (cloud tutorialCompleted=true), sync reflects it immediately
                repo.upsertUserLocally(sellerB.copy(tutorialCompleted = true))
                assertTrue(db.userDao().getUserById("op_b")!!.tutorialCompleted)
                assertTrue(repo.hasSeenWelcomeTutorialForUser("vendedorb@gmail.com"))
            }
        } finally {
            db.close()
        }
    }

    @Test
    fun `room MIGRATION_17_18 adds tutorialCompleted column preserving existing user rows`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val dbName = "migration_17_18_test.db"
        context.deleteDatabase(dbName)

        val seedDb = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .allowMainThreadQueries()
            .build()
        seedDb.openHelper.writableDatabase
        seedDb.close()

        val rawSqlite = android.database.sqlite.SQLiteDatabase.openDatabase(
            context.getDatabasePath(dbName).path,
            null,
            android.database.sqlite.SQLiteDatabase.OPEN_READWRITE
        )
        rawSqlite.use { sqlite ->
            sqlite.execSQL("DROP TABLE IF EXISTS `users`")
            sqlite.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `users` (
                    `id` TEXT NOT NULL PRIMARY KEY,
                    `username` TEXT NOT NULL,
                    `token` TEXT NOT NULL,
                    `role` TEXT NOT NULL,
                    `daysGranted` INTEGER NOT NULL,
                    `validFromMs` INTEGER NOT NULL,
                    `expiresAtMs` INTEGER NOT NULL,
                    `isBlocked` INTEGER NOT NULL,
                    `notes` TEXT NOT NULL,
                    `createdAt` INTEGER NOT NULL,
                    `mustChangePassword` INTEGER NOT NULL,
                    `hasCustomPassword` INTEGER NOT NULL,
                    `passwordChangedAt` INTEGER,
                    `canAccessLegacyModules` INTEGER NOT NULL,
                    `email` TEXT NOT NULL,
                    `maxDevices` INTEGER NOT NULL,
                    `boundDeviceIdsSerialized` TEXT NOT NULL,
                    `boundDeviceLabelsSerialized` TEXT NOT NULL,
                    `ownerUid` TEXT NOT NULL,
                    `lastOnlineVerifiedAtMs` INTEGER NOT NULL,
                    `lastTamperAlertAtMs` INTEGER NOT NULL,
                    `updateRequired` INTEGER NOT NULL,
                    `requiredVersionCode` INTEGER NOT NULL,
                    `updateLockReason` TEXT NOT NULL,
                    `parentEmail` TEXT NOT NULL,
                    `supervisorChainSerialized` TEXT NOT NULL,
                    `hierarchyDepth` INTEGER NOT NULL,
                    `approvalStatus` TEXT NOT NULL,
                    `canManageSubUsers` INTEGER NOT NULL,
                    `visibilityDepth` INTEGER NOT NULL,
                    `requestedByEmail` TEXT NOT NULL
                )
                """.trimIndent()
            )
            sqlite.execSQL(
                """
                INSERT INTO `users` VALUES (
                    'u_v17', 'Vendedor V17', 'GOOGLE-AUTH', 'OPERATOR',
                    30, 1000, 9999999999999, 0, '', 1000,
                    0, 0, NULL, 0, 'v17@gmail.com', 1, '', '', '', 0, 0,
                    0, 0, '', '', '', 0, 'ACTIVE', 0, 0, ''
                )
                """.trimIndent()
            )
            sqlite.execSQL("DROP TABLE IF EXISTS `business_invites`")
            sqlite.execSQL("PRAGMA user_version = 17")
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
                val user = migratedDb.userDao().getUserById("u_v17")
                assertNotNull(user)
                assertFalse(user!!.tutorialCompleted)
                assertTrue(user.fcmToken.isEmpty())
                assertTrue(user.notifyCatalogUpdates)
                assertTrue(user.notifyAssignedContacts)
            }
        } finally {
            migratedDb.close()
            context.deleteDatabase(dbName)
        }
    }

    @Test
    fun `BannerAviso renders active global announcements and supports temporary dismissal`() {
        val dismissedState = mutableStateOf(emptySet<String>())
        val mensajes = listOf(
            MensajeGlobal(
                id = "msg_inactive",
                titulo = "Borrador Oculto",
                mensaje = "No debe verse en pantalla.",
                prioridad = MensajeGlobalPrioridad.INFO.name,
                activo = false,
                autorNombre = "Admin"
            ),
            MensajeGlobal(
                id = "msg_urgente_1",
                titulo = "Nueva Lista PSA Vigente",
                mensaje = "Recuerden actualizar la cotizacion antes de visitar clientes hoy.",
                prioridad = MensajeGlobalPrioridad.URGENTE.name,
                activo = true,
                autorNombre = "Leo Admin"
            ),
            MensajeGlobal(
                id = "msg_info_2",
                titulo = "Capacitacion Jueves",
                mensaje = "Sumate a las 19hs para repasar cierre de ventas.",
                prioridad = MensajeGlobalPrioridad.INFO.name,
                activo = true,
                autorNombre = "Leo Admin"
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                BannerAviso(
                    mensajes = mensajes,
                    dismissedIds = dismissedState.value,
                    onDismissTemporarily = { id ->
                        dismissedState.value = dismissedState.value + id
                    }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("banner_aviso_container").assertIsDisplayed()
        composeTestRule.onNodeWithText("Nueva Lista PSA Vigente").assertIsDisplayed()

        // Temporarily dismiss first active announcement -> reveals next active one
        composeTestRule.onNodeWithTag("banner_aviso_close_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue("msg_urgente_1" in dismissedState.value)
        composeTestRule.onNodeWithText("Capacitacion Jueves").assertIsDisplayed()

        // Temporarily dismiss second active announcement -> banner collapses cleanly (inactive draft stays hidden)
        composeTestRule.onNodeWithTag("banner_aviso_hide_button").performClick()
        composeTestRule.waitForIdle()
        assertTrue("msg_info_2" in dismissedState.value)
        composeTestRule.onNodeWithTag("banner_aviso_container").assertDoesNotExist()
    }
}
