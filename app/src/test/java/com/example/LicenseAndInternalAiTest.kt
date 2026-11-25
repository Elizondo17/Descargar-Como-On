package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.UserAccount
import com.example.data.repository.EzoRepository
import com.example.data.repository.PsaFinancingConfig
import com.example.util.AccessLockReason
import com.example.util.AppUpdateChecker
import com.example.util.AppUpdateInfo
import com.example.util.LicenseSecurityGuard
import com.example.util.PsaAiAudience
import com.example.util.PsaCountryRegistry
import com.example.util.PsaDemoAiContext
import com.example.util.PsaInternalKnowledgeEngine
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class LicenseAndInternalAiTest {

    private lateinit var context: Context
    private lateinit var db: AppDatabase
    private lateinit var repo: EzoRepository

    @Before
    fun setup() {
        context = ApplicationProvider.getApplicationContext()
        LicenseSecurityGuard.clearState(context)
        db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        repo = EzoRepository(db, context)
    }

    @After
    fun teardown() {
        db.close()
    }

    @Test
    fun `ezoRepository block and expire at 0 days lock legacy operator cleanly`() = runBlocking {
        val seller = UserAccount(
            id = "op_local_1",
            username = "Vendedor Uno",
            token = "TOKEN123",
            role = "OPERATOR",
            daysGranted = 10,
            validFromMs = System.currentTimeMillis() - 60_000L,
            expiresAtMs = System.currentTimeMillis() + 10L * 24 * 60 * 60 * 1000L,
            isBlocked = false,
            email = "" // local operator without cloud binding so test verifies local persistence
        )
        repo.upsertUserLocally(seller)
        repo.setCurrentUser(seller)

        // 1. Update remaining days to 0 -> must expire immediately and block
        val expireRes = repo.updateUserDetails(
            userId = seller.id,
            newName = seller.username,
            newNotes = "Vencido manualmente",
            remainingDays = 0,
            maxDevices = 1
        )
        assertTrue(expireRes.isSuccess)
        val expiredUser = db.userDao().getUserById(seller.id)!!
        assertEquals(0, expiredUser.daysGranted)
        assertTrue(expiredUser.isExpired)
        assertTrue(expiredUser.isBlocked)

        // 2. Unblock via toggleBlockUser(false), verify unblocked
        val unblockRes = repo.toggleBlockUser(seller.id, false)
        assertTrue(unblockRes.isSuccess)
        assertFalse(db.userDao().getUserById(seller.id)!!.isBlocked)

        // 3. Re-block via toggleBlockUser(true)
        val blockRes = repo.toggleBlockUser(seller.id, true)
        assertTrue(blockRes.isSuccess)
        assertTrue(db.userDao().getUserById(seller.id)!!.isBlocked)

        // 4. syncRemoteUserStatusLocally with newDays = 0 sets daysGranted = 0 cleanly
        repo.syncRemoteUserStatusLocally(
            username = seller.username,
            isBlocked = false,
            newExpiresAtMs = System.currentTimeMillis() - 1000L,
            newDays = 0
        )
        val synced = db.userDao().getUserById(seller.id)!!
        assertEquals(0, synced.daysGranted)
        assertTrue(synced.isExpired)
    }

    @Test
    fun `appUpdateChecker locks seller on global forceUpdate or per-user updateRequired and clears on upgrade`() {
        val seller = UserAccount(
            id = "op_cloud_1",
            username = "Vendedor Cloud",
            token = "TOK",
            role = "OPERATOR",
            daysGranted = 30,
            validFromMs = System.currentTimeMillis() - 60_000L,
            expiresAtMs = System.currentTimeMillis() + 30L * 24 * 60 * 60 * 1000L,
            email = "vendedor@example.com",
            updateRequired = false,
            requiredVersionCode = 0
        )
        val globalForceServer = AppUpdateInfo(
            latestVersionCode = 5,
            latestVersionName = "1.4",
            apkDownloadUrl = "https://github.com/org/repo/releases/latest/download/app.apk",
            releaseNotes = "Actualización crítica de seguridad",
            forceUpdate = true
        )

        val globalEval = AppUpdateChecker.evaluateUpdateLockState(
            installedVersionCode = 3,
            user = seller,
            serverInfo = globalForceServer
        )
        assertTrue(globalEval.isLocked)
        assertEquals(5, globalEval.requiredVersionCode)
        assertTrue(globalEval.reasonText.contains("Actualización crítica"))

        val perUserLocked = seller.copy(
            updateRequired = true,
            requiredVersionCode = 4,
            updateLockReason = "Actualiza a la v1.3 para continuar"
        )
        val optionalServer = globalForceServer.copy(latestVersionCode = 4, forceUpdate = false)
        val perUserEval = AppUpdateChecker.evaluateUpdateLockState(
            installedVersionCode = 3,
            user = perUserLocked,
            serverInfo = optionalServer
        )
        assertTrue(perUserEval.isLocked)
        assertFalse(perUserEval.shouldAutoClearUserFlag)
        assertEquals("Actualiza a la v1.3 para continuar", perUserEval.reasonText)

        // Once seller installs build 4 -> unlocks and sets shouldAutoClearUserFlag = true
        val upgradedEval = AppUpdateChecker.evaluateUpdateLockState(
            installedVersionCode = 4,
            user = perUserLocked,
            serverInfo = optionalServer
        )
        assertFalse(upgradedEval.isLocked)
        assertTrue(upgradedEval.shouldAutoClearUserFlag)
    }

    @Test
    fun `licenseSecurityGuard enforces 24h offline lock and cross-reboot clock rollback without false positives`() {
        val baseWall = 1_750_000_000_000L
        val seller = UserAccount(
            id = "op_sec_1",
            username = "Operador Seguro",
            token = "SEC",
            role = "OPERATOR",
            email = "seguro@example.com",
            validFromMs = baseWall - 60_000L,
            expiresAtMs = baseWall + 15L * 24 * 60 * 60 * 1000L,
            daysGranted = 15
        )
        // Server clock is +20 minutes ahead of device clock (normal timezone/NTP skew -> must NOT false tamper!)
        LicenseSecurityGuard.recordOnlineVerification(
            context = context,
            user = seller,
            serverWallMs = baseWall + 20L * 60_000L,
            deviceWallMs = baseWall,
            elapsedMs = 10_000L,
            bootCount = 10
        )

        // 2 hours later, same boot: normal operation -> NONE
        val okEval = LicenseSecurityGuard.evaluateAccess(
            context = context,
            user = seller,
            nowDeviceWallMs = baseWall + 2L * 3_600_000L,
            nowElapsedMs = 10_000L + 2L * 3_600_000L,
            currentBootCount = 10
        )
        assertEquals(AccessLockReason.NONE, okEval.reason)

        // 25 hours offline in same boot -> OFFLINE_OVER_24H
        val offline25h = LicenseSecurityGuard.evaluateAccess(
            context = context,
            user = seller,
            nowDeviceWallMs = baseWall + 25L * 3_600_000L,
            nowElapsedMs = 10_000L + 25L * 3_600_000L,
            currentBootCount = 10
        )
        assertEquals(AccessLockReason.OFFLINE_OVER_24H, offline25h.reason)

        // User rolls clock back 3 hours and even reboots device (bootCount = 11) -> CLOCK_TAMPER_DETECTED
        val rollbackAfterReboot = LicenseSecurityGuard.evaluateAccess(
            context = context,
            user = seller,
            nowDeviceWallMs = baseWall + 22L * 3_600_000L,
            nowElapsedMs = 5_000L,
            currentBootCount = 11
        )
        assertEquals(AccessLockReason.CLOCK_TAMPER_DETECTED, rollbackAfterReboot.reason)
    }

    @Test
    fun `psaInternalKnowledgeEngine grounds answers in country pricing and live water diagnostic`() {
        val arPricing = PsaCountryRegistry.defaultPricingForCountry("AR")
        val arCountry = PsaCountryRegistry.getByCode("AR")
        val uyPricing = PsaCountryRegistry.defaultPricingForCountry("UY")
        val uyCountry = PsaCountryRegistry.getByCode("UY")
        val financing = PsaFinancingConfig()

        val wellContext = PsaDemoAiContext(
            sourceOrigin = "Demostración",
            waterSource = "AGUA_DE_POZO",
            chlorinePpm = 0f,
            hardnessPpm = 425,
            realLitersPerDay = 12.5,
            annualBottledSpend = 950_000.0,
            clientName = "Familia Pérez"
        )
        val kqs = PsaInternalKnowledgeEngine.getKeyQuestions(PsaAiAudience.VENDEDOR)
        assertTrue(kqs.size >= 10)
        val diagKq = kqs.first { it.id == "kq_diag_ideal" }

        val wellAnswer = PsaInternalKnowledgeEngine.answerKeyQuestion(
            keyQuestion = diagKq,
            audience = PsaAiAudience.VENDEDOR,
            pricingConfig = arPricing,
            financingConfig = financing,
            country = arCountry,
            demoContext = wellContext
        )
        assertTrue(wellAnswer.recommendedModelName.contains("ROtech", ignoreCase = true))
        assertTrue(wellAnswer.demoOrClientTip.contains("Familia Pérez"))
        assertTrue(wellAnswer.technicalSummary.contains("425 ppm"))

        // Switch audience to Cliente and ask free-text about PSA Ducha in Uruguay pricing
        val showerClientAnswer = PsaInternalKnowledgeEngine.answerQuestion(
            rawQuestion = "Para qué sirve el filtro de ducha para el pelo",
            audience = PsaAiAudience.CLIENTE,
            pricingConfig = uyPricing,
            financingConfig = financing,
            country = uyCountry,
            demoContext = PsaDemoAiContext(waterSource = "AGUA_CORRIENTE", chlorinePpm = 1.4f)
        )
        assertTrue(showerClientAnswer.recommendedModelName.contains("Ducha", ignoreCase = true))
        assertTrue(showerClientAnswer.priceAndInstallmentText.contains("UY"))
        assertTrue(showerClientAnswer.demoOrClientTip.contains("cabello", ignoreCase = true))
    }
}
