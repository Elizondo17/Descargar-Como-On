package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.PsaPostSaleInstallation
import com.example.data.model.PsaPostSaleStatus
import com.example.data.model.PsaPricingConfig
import com.example.data.model.ReplacementUrgency
import com.example.data.model.UserAccount
import com.example.data.model.calculateNextReplacementTimestamp
import com.example.util.DemoBudgetPaymentMode
import com.example.util.DemoCartLineDetail
import com.example.util.DemoCartSelection
import com.example.util.DemoContainerOption
import com.example.util.DemoToBudgetHandoff
import com.example.util.DemoWaterSource
import com.example.util.PsaCountryRegistry
import com.example.util.PsaDemoDiagnosticSummary
import com.example.util.PsaDemoReportExporter
import com.example.util.PsaHardnessStripCatalog
import com.example.util.PsaPriceTextParser
import com.example.util.PsaWaterColorAnalyzer
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import java.io.File
import java.io.FileOutputStream
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PsaDemoAndCloudTest {

    @Test
    fun `psa rotech uv included in purifiers catalog and country pricing defaults`() {
        val arPricing = PsaCountryRegistry.defaultPricingForCountry("AR")
        assertTrue(arPricing.rotechPrice > arPricing.ropotPrice)
        val rotechModel = arPricing.getAllPurifiers().firstOrNull { it.id == "rotech" }
        assertNotNull(rotechModel)
        assertTrue(rotechModel!!.name.contains("ROtech", ignoreCase = true))

        val parsed = PsaPriceTextParser.parseText(
            rawText = "PSA ROtech·UV PVP $ 2.290.000\nPSA Ropot PVP $ 1.540.000",
            currentConfig = arPricing,
            countryCode = "AR"
        )
        val rotechItem = parsed.items.firstOrNull { it.modelKey == "rotech" }
        val ropotItem = parsed.items.firstOrNull { it.modelKey == "ropot" }
        assertNotNull(rotechItem)
        assertNotNull(ropotItem)
        assertEquals(2290000.0, rotechItem!!.detectedPrice, 0.01)
        assertEquals(1540000.0, ropotItem!!.detectedPrice, 0.01)
    }

    @Test
    fun `psa hardness test strip has 6 verified levels from green to fuchsia`() {
        val stages = PsaHardnessStripCatalog.stages
        assertEquals(6, stages.size)
        assertEquals("0 ppm", stages.first().ppmCaCO3)
        assertEquals("0.00 mmol/L", stages.first().mmolPerLiter)
        assertEquals("425+ ppm", stages.last().ppmCaCO3)
        assertEquals("4.25+ mmol/L", stages.last().mmolPerLiter)
        assertTrue(stages.last().colorName.contains("Fucsia", ignoreCase = true))
    }

    @Test
    fun `real water consumption adds infusions and cooking to household people count`() {
        val baseOnly = PsaDemoReportExporter.calculateRealConsumptionStats(
            adultsCount = 2,
            childrenCount = 2,
            hasHotWeatherOrSport = false,
            hasPregnancyOrNursing = false,
            includeInfusions = false,
            includeCooking = false,
            containerOption = DemoContainerOption.BIDON_20L,
            containerUnitPrice = 5000.0
        )
        // 2*2.5 + 2*1.5 = 8.0 L/day
        assertEquals(8.0, baseOnly.baseHydrationLitersDay, 0.01)
        assertEquals(8.0, baseOnly.totalRealLitersDay, 0.01)

        val withExtras = PsaDemoReportExporter.calculateRealConsumptionStats(
            adultsCount = 2,
            childrenCount = 2,
            hasHotWeatherOrSport = false,
            hasPregnancyOrNursing = false,
            includeInfusions = true,
            includeCooking = true,
            containerOption = DemoContainerOption.BOTELLA_2L,
            containerUnitPrice = 1800.0
        )
        assertEquals(8.0, withExtras.baseHydrationLitersDay, 0.01)
        assertTrue(withExtras.infusionsExtraLitersDay > 0.0)
        assertTrue(withExtras.cookingExtraLitersDay > 0.0)
        assertTrue(withExtras.totalRealLitersDay > baseOnly.totalRealLitersDay)
        assertTrue(withExtras.annualBottledSpend > baseOnly.annualBottledSpend)
    }

    @Test
    fun `whatsapp structured summary and pdf generation succeed for tap and well water`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val country = PsaCountryRegistry.defaultCountry()
        val pricing = PsaPricingConfig()
        val rotech = pricing.getAllPurifiers().first { it.id == "rotech" }
        val domus = pricing.getAllPurifiers().first { it.id == "domus" }

        val stats = PsaDemoReportExporter.calculateRealConsumptionStats(
            adultsCount = 2,
            childrenCount = 1,
            hasHotWeatherOrSport = true,
            hasPregnancyOrNursing = false,
            includeInfusions = true,
            includeCooking = true,
            containerOption = DemoContainerOption.BIDON_20L,
            containerUnitPrice = 4800.0
        )

        val wellSummary = PsaDemoDiagnosticSummary(
            clientLabel = "Familia Gómez",
            country = country,
            waterSource = DemoWaterSource.AGUA_DE_POZO,
            hardnessStage = PsaHardnessStripCatalog.stages.last(),
            arsenicRiskSummary = "Napa con riesgo de arsénico y nitratos.",
            boilingSummary = "Hervir concentra sales un 35%.",
            consumptionStats = stats,
            primaryModel = rotech,
            secondaryModel = domus,
            annualPsaCostAmortized = rotech.price / 3.0,
            annualNetSavings = stats.annualBottledSpend - (rotech.price / 3.0),
            threeYearNetSavings = stats.threeYearBottledSpend - rotech.price,
            plasticBottlesAvoidedYear = 1200,
            showListPrice = false
        )

        val whatsappMsg = PsaDemoReportExporter.buildStructuredWhatsAppSummary(wellSummary)
        assertTrue(whatsappMsg.contains("Familia Gómez"))
        assertTrue(whatsappMsg.contains("425+ ppm"))
        assertTrue(whatsappMsg.contains("ROtech"))
        assertTrue(whatsappMsg.contains("Domus"))
        assertFalse(whatsappMsg.contains("2.150.000"))

        val pdfResult = PsaDemoReportExporter.generateDiagnosticPdf(context, wellSummary)
        assertTrue(
            "PDF generation failed: ${pdfResult.exceptionOrNull()?.stackTraceToString()}",
            pdfResult.isSuccess
        )
        val pdfFile = pdfResult.getOrThrow()
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 500L)
        val header = pdfFile.readBytes().take(4).toByteArray().decodeToString()
        assertEquals("%PDF", header)
    }

    @Test
    fun `multi equipment cart handoff and diagnostic export include all items and combined list price`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val country = PsaCountryRegistry.defaultCountry()
        val pricing = PsaPricingConfig()
        val senior4 = pricing.getAllPurifiers().first { it.id == "senior4" }
        val ducha = pricing.getAllPurifiers().first { it.id == "ducha" }

        val cartItems = listOf(
            DemoCartLineDetail(senior4, quantity = 1),
            DemoCartLineDetail(ducha, quantity = 2)
        )
        val expectedTotal = senior4.price + (ducha.price * 2)

        val handoff = DemoToBudgetHandoff(
            purifierId = senior4.id,
            items = listOf(
                DemoCartSelection(senior4.id, 1),
                DemoCartSelection(ducha.id, 2)
            ),
            paymentMode = DemoBudgetPaymentMode.CARD
        )
        assertEquals(2, handoff.items.size)
        assertEquals(2, handoff.items.last().quantity)

        val stats = PsaDemoReportExporter.calculateRealConsumptionStats(
            adultsCount = 2,
            childrenCount = 2,
            hasHotWeatherOrSport = false,
            hasPregnancyOrNursing = false,
            includeInfusions = true,
            includeCooking = true,
            containerOption = DemoContainerOption.BIDON_20L,
            containerUnitPrice = 4500.0
        )

        val cartSummary = PsaDemoDiagnosticSummary(
            clientLabel = "Casa Pérez",
            country = country,
            waterSource = DemoWaterSource.AGUA_CORRIENTE,
            chlorinePpm = 2.0f,
            chlorineRiskTitle = "Cloro residual alto (2.0 ppm)",
            consumptionStats = stats,
            primaryModel = senior4,
            secondaryModel = ducha,
            cartItems = cartItems,
            annualPsaCostAmortized = expectedTotal / 3.0,
            annualNetSavings = stats.annualBottledSpend - (expectedTotal / 3.0),
            threeYearNetSavings = stats.threeYearBottledSpend - expectedTotal,
            plasticBottlesAvoidedYear = 1400,
            showListPrice = true
        )

        assertEquals(expectedTotal, cartSummary.totalCartListPrice, 0.01)

        val whatsappMsg = PsaDemoReportExporter.buildStructuredWhatsAppSummary(cartSummary)
        assertTrue(whatsappMsg.contains("1x ${senior4.name}"))
        assertTrue(whatsappMsg.contains("2x ${ducha.name}"))
        assertTrue(whatsappMsg.contains("Total lista"))

        val pdfResult = PsaDemoReportExporter.generateDiagnosticPdf(context, cartSummary)
        assertTrue(pdfResult.isSuccess)
        val pdfFile = pdfResult.getOrThrow()
        assertTrue(pdfFile.exists())
        assertEquals("%PDF", pdfFile.readBytes().take(4).toByteArray().decodeToString())
    }

    @Test
    fun `user account supports configurable multi device limit`() {
        val now = System.currentTimeMillis()
        val multiUser = UserAccount(
            id = "juan@gmail.com",
            username = "Juan",
            token = "CLOUD",
            role = "OPERATOR",
            daysGranted = 30,
            validFromMs = now - 1000L,
            expiresAtMs = now + 86_400_000L * 30,
            email = "juan@gmail.com",
            maxDevices = 3,
            boundDeviceIdsSerialized = "dev-1|dev-2",
            boundDeviceLabelsSerialized = "Samsung A54|Pixel 8"
        )
        assertEquals(2, multiUser.boundDevicesCount)
        assertEquals(3, multiUser.maxDevices)
        assertTrue(multiUser.isDeviceAllowed("dev-1"))
        assertTrue(multiUser.isDeviceAllowed("dev-2"))
        assertFalse(multiUser.isDeviceAllowed("dev-3"))
        assertTrue(multiUser.boundDevicesCount < multiUser.maxDevices)
        assertTrue(multiUser.isActive)
    }

    @Test
    fun `chlorine photo hsv analyzer distinguishes clear yellow and orange red samples with interpretation and equipment`() {
        val clearPixels = IntArray(64) { Color.rgb(185, 228, 245) }
        val clearResult = PsaWaterColorAnalyzer.analyzeChlorineFromRgbPixels(clearPixels)
        assertEquals(0.0f, clearResult.detectedPpm, 0.01f)
        assertFalse(clearResult.requiresFiltering)
        assertTrue(clearResult.qualityStatusTitle.contains("Nivel óptimo", ignoreCase = true))
        assertTrue(clearResult.recommendedCartIds.contains("senior4"))

        val yellowPixels = IntArray(64) { Color.rgb(245, 215, 45) }
        val yellowResult = PsaWaterColorAnalyzer.analyzeChlorineFromRgbPixels(yellowPixels)
        assertEquals(1.0f, yellowResult.detectedPpm, 0.01f)
        assertTrue(yellowResult.requiresFiltering)
        assertTrue(yellowResult.qualityStatusTitle.contains("Requiere filtrado", ignoreCase = true))
        assertTrue(yellowResult.recommendedCartIds.containsAll(listOf("senior4", "ducha")))

        val orangeRedPixels = IntArray(64) { Color.rgb(235, 85, 35) }
        val highResult = PsaWaterColorAnalyzer.analyzeChlorineFromRgbPixels(orangeRedPixels)
        assertEquals(3.0f, highResult.detectedPpm, 0.01f)
        assertTrue(highResult.requiresFiltering)
        assertTrue(highResult.recommendedCartIds.contains("quantum"))
    }

    @Test
    fun `hardness strip hsv analyzer maps green to soft and fuchsia to very hard with interpretation and equipment`() {
        val softGreenPixels = IntArray(64) { Color.rgb(46, 125, 50) }
        val softResult = PsaWaterColorAnalyzer.analyzeHardnessFromRgbPixels(softGreenPixels)
        assertTrue(softResult.stageIndex in 0..1)
        assertFalse(softResult.requiresFiltering)
        assertTrue(softResult.qualityStatusTitle.contains("Nivel óptimo", ignoreCase = true))

        val fuchsiaPixels = IntArray(64) { Color.rgb(194, 24, 91) }
        val hardResult = PsaWaterColorAnalyzer.analyzeHardnessFromRgbPixels(fuchsiaPixels)
        assertTrue(hardResult.stageIndex in 4..5)
        assertTrue(hardResult.requiresFiltering)
        assertTrue(hardResult.qualityStatusTitle.contains("Requiere filtrado", ignoreCase = true))
        assertTrue(hardResult.recommendedCartIds.containsAll(listOf("rotech", "domus")))
    }

    @Test
    fun `analyzeTestPhotoUri reads center glass region ignoring outer dark border`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val bmp = Bitmap.createBitmap(200, 200, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.rgb(15, 20, 30)) // Dark table surround
        for (x in 70 until 130) {
            for (y in 70 until 130) {
                bmp.setPixel(x, y, Color.rgb(245, 210, 40)) // Yellow OTO center glass liquid
            }
        }
        val tempFile = File(context.cacheDir, "oto_glass_center_test.jpg")
        FileOutputStream(tempFile).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 95, out)
        }
        bmp.recycle()

        val outcome = PsaWaterColorAnalyzer.analyzeTestPhotoUri(
            context = context,
            sourceUri = android.net.Uri.fromFile(tempFile),
            isWellWater = false
        ).getOrThrow()

        assertNotNull(outcome.chlorineResult)
        assertEquals(1.0f, outcome.chlorineResult!!.detectedPpm, 0.01f)
        assertTrue(outcome.chlorineResult!!.requiresFiltering)
        assertTrue(outcome.chlorineResult!!.qualityStatusTitle.contains("Requiere filtrado", ignoreCase = true))
        assertTrue(File(outcome.savedPhotoPath).exists())
        File(outcome.savedPhotoPath).delete()
        tempFile.delete()
    }

    @Test
    fun `diagnostic pdf embeds real photo thumbnail when testPhotoPath exists`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val tempPhoto = File(context.cacheDir, "test_oto_sample.jpg")
        val bmp = Bitmap.createBitmap(120, 120, Bitmap.Config.ARGB_8888)
        bmp.eraseColor(Color.rgb(255, 214, 10))
        FileOutputStream(tempPhoto).use { out ->
            bmp.compress(Bitmap.CompressFormat.JPEG, 85, out)
        }
        bmp.recycle()

        val pricing = PsaPricingConfig()
        val senior4 = pricing.getAllPurifiers().first { it.id == "senior4" }
        val stats = PsaDemoReportExporter.calculateRealConsumptionStats(
            adultsCount = 2,
            childrenCount = 1,
            hasHotWeatherOrSport = false,
            hasPregnancyOrNursing = false,
            includeInfusions = true,
            includeCooking = true,
            containerOption = DemoContainerOption.BIDON_20L,
            containerUnitPrice = 5000.0
        )
        val summary = PsaDemoDiagnosticSummary(
            clientLabel = "Prueba Foto Real",
            country = PsaCountryRegistry.defaultCountry(),
            waterSource = DemoWaterSource.AGUA_CORRIENTE,
            chlorinePpm = 2.0f,
            chlorineRiskTitle = "Cloro residual alto",
            testPhotoPath = tempPhoto.absolutePath,
            consumptionStats = stats,
            primaryModel = senior4,
            cartItems = listOf(DemoCartLineDetail(senior4, 1)),
            annualPsaCostAmortized = senior4.price / 3.0,
            annualNetSavings = stats.annualBottledSpend - (senior4.price / 3.0),
            threeYearNetSavings = stats.threeYearBottledSpend - senior4.price,
            plasticBottlesAvoidedYear = 1000,
            showListPrice = false
        )

        val pdfResult = PsaDemoReportExporter.generateDiagnosticPdf(context, summary)
        assertTrue(pdfResult.isSuccess)
        val pdfFile = pdfResult.getOrThrow()
        assertTrue(pdfFile.exists())
        assertTrue(pdfFile.length() > 1000L)
        assertEquals("%PDF", pdfFile.readBytes().take(4).toByteArray().decodeToString())
        tempPhoto.delete()
    }

    @Test
    fun `post sale replacement cycle urgency and room dao v12 persistence work`() = runBlocking {
        val now = 1_760_000_000_000L
        val twelveMonthsAhead = calculateNextReplacementTimestamp(now, 12)
        assertTrue(twelveMonthsAhead > now + (360L * 86_400_000L))

        val overdue = PsaPostSaleInstallation(
            id = "ps_overdue",
            customerName = "Carlos",
            purifierId = "senior4",
            purifierName = "PSA Senior 4",
            installedAtMs = now - (400L * 86_400_000L),
            replacementCycleMonths = 12,
            nextReplacementAtMs = now - (5L * 86_400_000L),
            status = PsaPostSaleStatus.ACTIVO
        )
        assertEquals(ReplacementUrgency.VENCIDO, overdue.getUrgency(now))

        val soon = overdue.copy(id = "ps_soon", nextReplacementAtMs = now + (10L * 86_400_000L))
        assertEquals(ReplacementUrgency.PROXIMO, soon.getUrgency(now))

        val ok = overdue.copy(id = "ps_ok", nextReplacementAtMs = now + (60L * 86_400_000L))
        assertEquals(ReplacementUrgency.AL_DIA, ok.getUrgency(now))

        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        try {
            val dao = db.psaPostSaleDao()
            dao.insertInstallation(overdue)
            dao.insertInstallation(soon)
            val stored = dao.getAllInstallations().first()
            assertEquals(2, stored.size)
            assertEquals("ps_overdue", stored.first().id)

            dao.updateInstallation(overdue.copy(nextReplacementAtMs = now + (365L * 86_400_000L)))
            val afterUpdate = dao.getInstallationById("ps_overdue")
            assertNotNull(afterUpdate)
            assertEquals(ReplacementUrgency.AL_DIA, afterUpdate!!.getUrgency(now))
        } finally {
            db.close()
        }
    }

    @Test
    fun `google drive link converts to direct download and leonelizondo16 is sole superuser`() {
        assertEquals(
            "leonelizondo16@gmail.com",
            com.example.data.repository.FirestoreTeamRepository.BOOTSTRAP_ADMIN_EMAIL
        )
        assertEquals(
            "",
            com.example.data.repository.EzoRepository.normalizeApkDownloadUrl("   ")
        )
        assertEquals(
            "",
            com.example.data.repository.EzoRepository.normalizeApkDownloadUrl("https://gen-lang-client-0791441970.web.app")
        )
        assertEquals(
            "https://drive.google.com/uc?export=download&id=1AbCdEfGhIjKlMnOpQrStUvWxYz",
            com.example.data.repository.EzoRepository.normalizeApkDownloadUrl(
                "https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz/view?usp=sharing"
            )
        )
        assertEquals(
            "https://drive.google.com/uc?export=download&id=9ZyXwVuTsRqPoNmLkJiHg",
            com.example.data.repository.EzoRepository.normalizeApkDownloadUrl(
                "https://drive.google.com/open?id=9ZyXwVuTsRqPoNmLkJiHg"
            )
        )

        val driveInvite = com.example.data.repository.EzoRepository.buildOperatorInviteMessage(
            username = "Carlos PSA",
            email = "Carlos@Gmail.com",
            days = 30,
            maxDevices = 2,
            apkDownloadUrl = "https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz/view"
        )
        assertTrue(driveInvite.contains("Carlos PSA"))
        assertTrue(driveInvite.contains("carlos@gmail.com"))
        assertTrue(driveInvite.contains("https://drive.google.com/uc?export=download&id=1AbCdEfGhIjKlMnOpQrStUvWxYz"))
        assertTrue(driveInvite.contains("Continuar con Google"))

        val directApkInvite = com.example.data.repository.EzoRepository.buildOperatorInviteMessage(
            username = "Lucía PSA",
            email = "lucia@gmail.com",
            days = 15,
            maxDevices = 1,
            apkDownloadUrl = ""
        )
        assertTrue(directApkInvite.contains("ComoOn-PSA.apk"))
        assertFalse(directApkInvite.contains("web.app"))
    }

    @Test
    fun `github automatic apk release and pages urls build cleanly from user and repo`() {
        assertEquals(
            "leonelizondo16",
            com.example.data.repository.EzoRepository.cleanGithubOwner("@leonelizondo16")
        )
        assertEquals(
            "leonelizondo16",
            com.example.data.repository.EzoRepository.cleanGithubOwner("https://github.com/leonelizondo16/como-on-psa")
        )
        assertEquals(
            "como-on-psa",
            com.example.data.repository.EzoRepository.cleanGithubRepo("https://github.com/leonelizondo16/como-on-psa.git")
        )
        assertEquals(
            "https://github.com/leonelizondo16/como-on-psa/releases/latest/download/ComoOn-PSA.apk",
            com.example.data.repository.EzoRepository.buildGithubDirectApkUrl("@leonelizondo16", "como-on-psa")
        )
        assertEquals(
            "https://leonelizondo16.github.io/como-on-psa/",
            com.example.data.repository.EzoRepository.buildGithubPagesUrl("LeonelIzondo16", "como-on-psa")
        )
        assertEquals(
            "",
            com.example.data.repository.EzoRepository.buildGithubDirectApkUrl("leonelizondo16", "")
        )
        assertEquals("Elizondo17", com.example.data.repository.EzoRepository.DEFAULT_GITHUB_USERNAME)
        assertEquals("Descargar-Como-On", com.example.data.repository.EzoRepository.DEFAULT_GITHUB_REPO)
        assertEquals(
            "https://github.com/Elizondo17/Descargar-Como-On/releases/latest/download/ComoOn-PSA.apk",
            com.example.data.repository.EzoRepository.buildGithubDirectApkUrl(
                com.example.data.repository.EzoRepository.DEFAULT_GITHUB_USERNAME,
                com.example.data.repository.EzoRepository.DEFAULT_GITHUB_REPO
            )
        )
        assertEquals(
            "https://elizondo17.github.io/Descargar-Como-On/",
            com.example.data.repository.EzoRepository.buildGithubPagesUrl(
                com.example.data.repository.EzoRepository.DEFAULT_GITHUB_USERNAME,
                com.example.data.repository.EzoRepository.DEFAULT_GITHUB_REPO
            )
        )
    }

    @Test
    fun `license security guard enforces 24h offline lock reboot check clock tamper and 3 day warning`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.util.LicenseSecurityGuard.clearState(context)

        val baseServerWall = 1_760_000_000_000L
        val baseDeviceWall = baseServerWall
        val baseElapsed = 1_000_000L
        val baseBootCount = 12
        val dayMs = 24L * 60L * 60L * 1000L

        val operator = UserAccount(
            id = "op@gmail.com",
            username = "Operador",
            token = "CLOUD",
            role = "OPERATOR",
            daysGranted = 10,
            validFromMs = baseServerWall - (7 * dayMs),
            expiresAtMs = baseServerWall + (3 * dayMs), // 3 days left -> expiring soon
            email = "op@gmail.com",
            maxDevices = 1
        )

        com.example.util.LicenseSecurityGuard.recordOnlineVerification(
            context = context,
            user = operator,
            serverWallMs = baseServerWall,
            deviceWallMs = baseDeviceWall,
            elapsedMs = baseElapsed,
            bootCount = baseBootCount
        )

        // Immediately after online verification: active + 3-day warning
        val okEval = com.example.util.LicenseSecurityGuard.evaluateAccess(
            context = context,
            user = operator,
            nowDeviceWallMs = baseDeviceWall + 60_000L,
            nowElapsedMs = baseElapsed + 60_000L,
            currentBootCount = baseBootCount
        )
        assertEquals(com.example.util.AccessLockReason.NONE, okEval.reason)
        assertEquals(3L, okEval.remainingDays)
        assertTrue(okEval.isExpiringSoon)

        // >3 days remaining -> not expiring soon yet
        val fourDaysOperator = operator.copy(expiresAtMs = baseServerWall + (4 * dayMs))
        val fourDayEval = com.example.util.LicenseSecurityGuard.evaluateAccess(
            context = context,
            user = fourDaysOperator,
            nowDeviceWallMs = baseDeviceWall + 60_000L,
            nowElapsedMs = baseElapsed + 60_000L,
            currentBootCount = baseBootCount
        )
        assertEquals(com.example.util.AccessLockReason.NONE, fourDayEval.reason)
        assertEquals(4L, fourDayEval.remainingDays)
        assertFalse(fourDayEval.isExpiringSoon)

        // >24h elapsed without internet verification -> OFFLINE_OVER_24H
        val offlineEval = com.example.util.LicenseSecurityGuard.evaluateAccess(
            context = context,
            user = fourDaysOperator,
            nowDeviceWallMs = baseDeviceWall + dayMs + 60_000L,
            nowElapsedMs = baseElapsed + dayMs + 60_000L,
            currentBootCount = baseBootCount
        )
        assertEquals(com.example.util.AccessLockReason.OFFLINE_OVER_24H, offlineEval.reason)
        assertTrue(offlineEval.isLocked)

        // Reboot detected (boot count incremented + elapsed reset) -> REBOOT_VERIFICATION_REQUIRED
        val rebootEval = com.example.util.LicenseSecurityGuard.evaluateAccess(
            context = context,
            user = fourDaysOperator,
            nowDeviceWallMs = baseDeviceWall + 120_000L,
            nowElapsedMs = 50_000L,
            currentBootCount = baseBootCount + 1
        )
        assertEquals(com.example.util.AccessLockReason.REBOOT_VERIFICATION_REQUIRED, rebootEval.reason)

        // Re-verify online resets state; then rollback device clock -> CLOCK_TAMPER_DETECTED
        com.example.util.LicenseSecurityGuard.recordOnlineVerification(
            context = context,
            user = fourDaysOperator,
            serverWallMs = baseServerWall,
            deviceWallMs = baseDeviceWall,
            elapsedMs = baseElapsed,
            bootCount = baseBootCount,
            clearPendingTamperReport = true
        )
        val tamperEval = com.example.util.LicenseSecurityGuard.evaluateAccess(
            context = context,
            user = fourDaysOperator,
            nowDeviceWallMs = baseDeviceWall - (2 * dayMs),
            nowElapsedMs = baseElapsed + 120_000L,
            currentBootCount = baseBootCount
        )
        assertEquals(com.example.util.AccessLockReason.CLOCK_TAMPER_DETECTED, tamperEval.reason)
        assertTrue(tamperEval.pendingTamperAlertAtMs > 0L)

        // Superuser (leonelizondo16@gmail.com) stays exempt from offline / reboot / clock locks
        val superuser = UserAccount(
            id = "leonelizondo16@gmail.com",
            username = "Administrador",
            token = "MASTER",
            role = "SUPERUSER",
            daysGranted = 9999,
            validFromMs = baseServerWall,
            expiresAtMs = baseServerWall + 100L * dayMs,
            email = "leonelizondo16@gmail.com"
        )
        val superEval = com.example.util.LicenseSecurityGuard.evaluateAccess(
            context = context,
            user = superuser,
            nowDeviceWallMs = baseDeviceWall + (10 * dayMs),
            nowElapsedMs = baseElapsed + (10 * dayMs),
            currentBootCount = baseBootCount + 5
        )
        assertEquals(com.example.util.AccessLockReason.NONE, superEval.reason)
        assertFalse(superEval.isLocked)

        com.example.util.LicenseSecurityGuard.clearState(context)
    }

    @Test
    fun `psa cloud pricing sync auto applies without override and prompts seller with override`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.util.PsaPricingCloudSyncHelper.clearSellerOverrideState(context)

        assertEquals(
            com.example.util.AdminPricingSyncDecision.AUTO_APPLY,
            com.example.util.PsaPricingCloudSyncHelper.evaluateSyncDecision(
                hasLocalOverride = false,
                lastAppliedAdminVersionMs = 1000L,
                dismissedAdminVersionMs = 0L,
                incomingVersionMs = 2000L
            )
        )
        assertEquals(
            com.example.util.AdminPricingSyncDecision.PROMPT_SELLER,
            com.example.util.PsaPricingCloudSyncHelper.evaluateSyncDecision(
                hasLocalOverride = true,
                lastAppliedAdminVersionMs = 1000L,
                dismissedAdminVersionMs = 1000L,
                incomingVersionMs = 2000L
            )
        )
        assertEquals(
            com.example.util.AdminPricingSyncDecision.NO_OP,
            com.example.util.PsaPricingCloudSyncHelper.evaluateSyncDecision(
                hasLocalOverride = true,
                lastAppliedAdminVersionMs = 1000L,
                dismissedAdminVersionMs = 2000L,
                incomingVersionMs = 2000L
            )
        )

        val customArPricing = PsaCountryRegistry.defaultPricingForCountry("AR").copy(
            senior4Price = 1_395_000.0,
            installmentsCount = 18
        )
        val customFinancing = com.example.data.repository.PsaFinancingConfig(
            defaultInterest3Months = 12.5f,
            defaultInterest6Months = 22.0f,
            defaultInterest12Months = 38.0f,
            defaultInterest18Months = 54.0f,
            suggestedAdvancePercentage = 25.0f
        )
        val snapshot = com.example.util.PsaCountryPricingSnapshot(
            countryCode = "AR",
            versionMs = 555_000L,
            updatedBy = "leonelizondo16@gmail.com",
            pricingConfig = customArPricing,
            financingConfig = customFinancing
        )

        val serialized = com.example.util.PsaPricingCloudSyncHelper.mergeAndSerializeCountrySnapshot("", snapshot)
        val parsed = com.example.util.PsaPricingCloudSyncHelper.parseAllCountriesPayload(serialized)
        val restored = parsed["AR"]
        assertNotNull(restored)
        assertEquals(555_000L, restored!!.versionMs)
        assertEquals(1_395_000.0, restored.pricingConfig.senior4Price, 0.01)
        assertEquals(18, restored.pricingConfig.installmentsCount)
        assertEquals(12.5f, restored.financingConfig.defaultInterest3Months, 0.01f)
        assertEquals(54.0f, restored.financingConfig.defaultInterest18Months, 0.01f)
        assertEquals(25.0f, restored.financingConfig.suggestedAdvancePercentage, 0.01f)
    }

    @Test
    fun `app version metadata is populated for admin version banner`() {
        assertTrue(BuildConfig.VERSION_CODE >= 3)
        assertTrue(BuildConfig.VERSION_NAME.isNotBlank())
        assertFalse(com.example.util.FirebaseAppDistributionHelper.isTesterSignedIn())
    }

    @Test
    fun `app update checker compares server versionCode enforces https and builds external browser intent`() {
        val driveUpdate = com.example.util.AppUpdateInfo(
            latestVersionCode = 5,
            latestVersionName = "1.4",
            apkDownloadUrl = "https://drive.google.com/file/d/1AbCdEfGhIjKlMnOpQrStUvWxYz/view?usp=sharing",
            releaseNotes = "Mejoras PSA"
        )
        assertEquals(
            "https://drive.google.com/uc?export=download&id=1AbCdEfGhIjKlMnOpQrStUvWxYz",
            com.example.util.AppUpdateChecker.normalizeDownloadUrl(driveUpdate.apkDownloadUrl)
        )
        assertTrue(
            com.example.util.AppUpdateChecker.isUpdateAvailable(
                currentVersionCode = 2,
                serverInfo = driveUpdate
            )
        )
        assertFalse(
            com.example.util.AppUpdateChecker.isUpdateAvailable(
                currentVersionCode = 5,
                serverInfo = driveUpdate
            )
        )
        assertFalse(
            com.example.util.AppUpdateChecker.isUpdateAvailable(
                currentVersionCode = 2,
                serverInfo = driveUpdate,
                dismissedVersionCode = 5
            )
        )
        assertTrue(
            com.example.util.AppUpdateChecker.isUpdateAvailable(
                currentVersionCode = 2,
                serverInfo = driveUpdate.copy(latestVersionCode = 6),
                dismissedVersionCode = 5
            )
        )

        val insecureUpdate = driveUpdate.copy(apkDownloadUrl = "http://example.com/ComoOn.apk")
        assertEquals("", com.example.util.AppUpdateChecker.normalizeDownloadUrl(insecureUpdate.apkDownloadUrl))
        assertFalse(
            com.example.util.AppUpdateChecker.isUpdateAvailable(
                currentVersionCode = 2,
                serverInfo = insecureUpdate
            )
        )

        val intent = com.example.util.AppUpdateChecker.buildBrowserDownloadIntent(driveUpdate.apkDownloadUrl)
        assertNotNull(intent)
        assertEquals(android.content.Intent.ACTION_VIEW, intent!!.action)
        assertEquals(
            "https://drive.google.com/uc?export=download&id=1AbCdEfGhIjKlMnOpQrStUvWxYz",
            intent.dataString
        )
    }

    @Test
    fun `appErrorLogger filters firebase errors from logcat redacts tokens and enforces bounded deduplicated queue`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        com.example.util.AppErrorLogger.clearQueue(context)

        val rawLogcat = """
            11-22 10:00:00.100  1234  1234 I Choreographer: Skipped 42 frames!
            11-22 10:00:00.200  1234  1234 D ViewRootImpl: Surface created
            11-22 10:00:00.300  1234  1250 E FirebaseApp: Default FirebaseApp failed to initialize Authorization Bearer secretToken12345.abc
            11-22 10:00:00.400  1234  1251 W Firestore: UNAVAILABLE: Unable to resolve host
            11-22 10:00:00.500  1234  1252 E FirebaseAuth: SecurityException PERMISSION_DENIED on token check
        """.trimIndent()

        val filtered = com.example.util.AppErrorLogger.extractFirebaseLogcatLines(rawLogcat)
        assertFalse(filtered.contains("Choreographer"))
        assertFalse(filtered.contains("ViewRootImpl"))
        assertFalse(filtered.contains("secretToken12345"))
        assertTrue(filtered.contains("Bearer [REDACTED]"))
        assertTrue(filtered.contains("FirebaseApp"))
        assertTrue(filtered.contains("Firestore"))
        assertTrue(filtered.contains("FirebaseAuth"))
        assertTrue(com.example.util.AppErrorLogger.hasCriticalFirebaseSignal(filtered))
        assertFalse(com.example.util.AppErrorLogger.hasCriticalFirebaseSignal("I/Choreographer: ok"))

        // Bounded queue (max 15)
        val baseTime = 1_760_000_000_000L
        for (i in 1..20) {
            val added = com.example.util.AppErrorLogger.enqueuePendingError(
                context = context,
                stage = com.example.util.AppErrorStage.ON_CREATE,
                errorMessage = "Firebase init error #$i",
                stackTraceExcerpt = "java.lang.IllegalStateException: init #$i",
                logcatExcerpt = filtered,
                nowMs = baseTime + (i * 1000L)
            )
            assertTrue(added)
        }
        val queued = com.example.util.AppErrorLogger.getPendingErrors(context)
        assertEquals(15, queued.size)
        assertEquals("Firebase init error #6", queued.first().errorMessage)
        assertEquals("Firebase init error #20", queued.last().errorMessage)

        // Deduplication within 10-minute window
        com.example.util.AppErrorLogger.clearQueue(context)
        assertTrue(
            com.example.util.AppErrorLogger.enqueuePendingError(
                context = context,
                stage = com.example.util.AppErrorStage.LOGCAT_FIREBASE,
                errorMessage = "Duplicate Firebase failure",
                nowMs = baseTime
            )
        )
        assertFalse(
            com.example.util.AppErrorLogger.enqueuePendingError(
                context = context,
                stage = com.example.util.AppErrorStage.LOGCAT_FIREBASE,
                errorMessage = "Duplicate Firebase failure",
                nowMs = baseTime + (5L * 60L * 1000L)
            )
        )
        assertTrue(
            com.example.util.AppErrorLogger.enqueuePendingError(
                context = context,
                stage = com.example.util.AppErrorStage.LOGCAT_FIREBASE,
                errorMessage = "Duplicate Firebase failure",
                nowMs = baseTime + (11L * 60L * 1000L)
            )
        )
        val afterDedup = com.example.util.AppErrorLogger.getPendingErrors(context)
        assertEquals(2, afterDedup.size)

        com.example.util.AppErrorLogger.removePendingByIds(context, setOf(afterDedup.first().id))
        assertEquals(1, com.example.util.AppErrorLogger.getPendingCount(context))
        com.example.util.AppErrorLogger.clearQueue(context)
    }

    @Test
    fun `appUpdateChecker evaluates two stage update notice per user lock global force update and auto unlock`() {
        val now = 1_760_000_000_000L
        val dayMs = 86_400_000L
        val seller = UserAccount(
            id = "vendedor@gmail.com",
            username = "Vendedor PSA",
            token = "CLOUD",
            role = "OPERATOR",
            daysGranted = 30,
            validFromMs = now - dayMs,
            expiresAtMs = now + 29L * dayMs,
            email = "vendedor@gmail.com",
            updateRequired = true,
            requiredVersionCode = 5,
            updateLockReason = "Actualizá a v1.5 para seguir operando"
        )
        val optionalServer = com.example.util.AppUpdateInfo(
            latestVersionCode = 5,
            latestVersionName = "1.5",
            apkDownloadUrl = "https://github.com/Elizondo17/Descargar-Como-On/releases/latest/download/ComoOn-PSA.apk",
            releaseNotes = "Nueva versión disponible",
            forceUpdate = false
        )

        // Per-seller lock blocks older build (4 < 5)
        val lockedEval = com.example.util.AppUpdateChecker.evaluateUpdateLockState(
            installedVersionCode = 4,
            user = seller,
            serverInfo = optionalServer
        )
        assertTrue(lockedEval.isLocked)
        assertEquals(5, lockedEval.requiredVersionCode)
        assertFalse(lockedEval.shouldAutoClearUserFlag)
        assertEquals("Actualizá a v1.5 para seguir operando", lockedEval.reasonText)

        // Opening updated build (5 >= 5) unlocks automatically & flags self-clear
        val unlockedEval = com.example.util.AppUpdateChecker.evaluateUpdateLockState(
            installedVersionCode = 5,
            user = seller,
            serverInfo = optionalServer
        )
        assertFalse(unlockedEval.isLocked)
        assertTrue(unlockedEval.shouldAutoClearUserFlag)

        // Stage 1 optional notice (no per-user lock, forceUpdate = false): prompts without locking
        val unlockedSeller = seller.copy(updateRequired = false, requiredVersionCode = 0, updateLockReason = "")
        val optionalEval = com.example.util.AppUpdateChecker.evaluateUpdateLockState(
            installedVersionCode = 4,
            user = unlockedSeller,
            serverInfo = optionalServer
        )
        assertFalse(optionalEval.isLocked)
        assertTrue(com.example.util.AppUpdateChecker.isUpdateAvailable(4, optionalServer))

        // Stage 2 global forceUpdate = true: locks older seller even when updateRequired = false, ignores dismissal
        val forcedServer = optionalServer.copy(latestVersionCode = 6, latestVersionName = "1.6", forceUpdate = true)
        assertTrue(com.example.util.AppUpdateChecker.isUpdateAvailable(5, forcedServer, dismissedVersionCode = 6))
        val globalLockEval = com.example.util.AppUpdateChecker.evaluateUpdateLockState(
            installedVersionCode = 5,
            user = unlockedSeller,
            serverInfo = forcedServer
        )
        assertTrue(globalLockEval.isLocked)
        assertEquals(6, globalLockEval.requiredVersionCode)
        assertFalse(globalLockEval.shouldAutoClearUserFlag)

        // Seller on build 6 unlocks automatically from global forceUpdate
        val globalUnlockedEval = com.example.util.AppUpdateChecker.evaluateUpdateLockState(
            installedVersionCode = 6,
            user = unlockedSeller,
            serverInfo = forcedServer
        )
        assertFalse(globalUnlockedEval.isLocked)

        // Superuser stays exempt from update lock
        val superuser = unlockedSeller.copy(
            id = "leonelizondo16@gmail.com",
            email = "leonelizondo16@gmail.com",
            role = "SUPERUSER"
        )
        val superEval = com.example.util.AppUpdateChecker.evaluateUpdateLockState(
            installedVersionCode = 1,
            user = superuser,
            serverInfo = forcedServer
        )
        assertFalse(superEval.isLocked)
    }

    @Test
    fun `psaWorkspace keeps only agenda and live demo as primary foreground tabs`() {
        val primary = com.example.ui.screens.psa.PsaWorkspaceTab.entries.filter { it.isPrimaryTab }
        assertEquals(
            listOf(
                com.example.ui.screens.psa.PsaWorkspaceTab.AGENDA,
                com.example.ui.screens.psa.PsaWorkspaceTab.LIVE_DEMO
            ),
            primary
        )
        assertFalse(com.example.ui.screens.psa.PsaWorkspaceTab.BUDGET.isPrimaryTab)
        assertFalse(com.example.ui.screens.psa.PsaWorkspaceTab.STOCK.isPrimaryTab)
        assertFalse(com.example.ui.screens.psa.PsaWorkspaceTab.CONTACTS.isPrimaryTab)
        assertFalse(com.example.ui.screens.psa.PsaWorkspaceTab.SCRIPTS.isPrimaryTab)
        assertFalse(com.example.ui.screens.psa.PsaWorkspaceTab.AI_MEMORY.isPrimaryTab)
    }

    @Test
    fun `psaAgendaSearchHelper filters visits leads postSale and dates by name or date in real time`() {
        val nov22Cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 22, 10, 0, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val nov22Ms = nov22Cal.timeInMillis

        val nov25Cal = java.util.Calendar.getInstance().apply {
            set(2026, java.util.Calendar.NOVEMBER, 25, 14, 30, 0)
            set(java.util.Calendar.MILLISECOND, 0)
        }
        val nov25Ms = nov25Cal.timeInMillis

        val visits = listOf(
            com.example.data.model.PsaHomeVisit(
                id = "v1",
                customerName = "María Pérez",
                phone = "1155551111",
                address = "Av. Rivadavia 1200",
                neighborhood = "Caballito",
                scheduledDate = "2026-11-22",
                scheduledTime = "16:30"
            ),
            com.example.data.model.PsaHomeVisit(
                id = "v2",
                customerName = "Carlos Gómez",
                phone = "1166662222",
                address = "Calle Belgrano 400",
                neighborhood = "Palermo",
                scheduledDate = "2026-11-25",
                scheduledTime = "18:00"
            )
        )

        val leads = listOf(
            com.example.data.model.PsaContactLead(
                id = "l1",
                name = "María Elena López",
                phone = "1144443333",
                neighborhood = "Almagro",
                rescheduleDate = "2026-11-22",
                lastContactDateMs = nov22Ms,
                createdAtMs = nov22Ms
            ),
            com.example.data.model.PsaContactLead(
                id = "l2",
                name = "Lucas Martínez",
                phone = "1177778888",
                neighborhood = "Belgrano",
                rescheduleDate = "",
                lastContactDateMs = nov25Ms,
                createdAtMs = nov25Ms
            )
        )

        val postSale = listOf(
            PsaPostSaleInstallation(
                id = "ps1",
                customerName = "María Laura Díaz",
                customerPhone = "1199990000",
                purifierName = "PSA Senior 4",
                installedAtMs = nov22Ms - 86_400_000L * 365L,
                nextReplacementAtMs = nov22Ms
            ),
            PsaPostSaleInstallation(
                id = "ps2",
                customerName = "Roberto Sosa",
                customerPhone = "1122223333",
                purifierName = "PSA Quantum",
                installedAtMs = nov25Ms - 86_400_000L * 365L,
                nextReplacementAtMs = nov25Ms
            )
        )

        val uniqueDates = listOf(
            "2026-11-22" to "Domingo 22 Nov",
            "2026-11-25" to "Miércoles 25 Nov"
        )
        val displayMap = uniqueDates.toMap()

        // 1. Accent & case-insensitive name filtering across all models
        val mariaVisits = com.example.util.PsaAgendaSearchHelper.filterVisits(
            visits = visits,
            query = "maria",
            scope = com.example.util.AgendaSearchScope.NAME,
            displayDatesByKey = displayMap
        )
        assertEquals(listOf("v1"), mariaVisits.map { it.id })

        val mariaLeads = com.example.util.PsaAgendaSearchHelper.filterLeads(
            leads = leads,
            query = "MARIA",
            scope = com.example.util.AgendaSearchScope.NAME
        )
        assertEquals(listOf("l1"), mariaLeads.map { it.id })

        val mariaPostSale = com.example.util.PsaAgendaSearchHelper.filterPostSale(
            items = postSale,
            query = "maria",
            scope = com.example.util.AgendaSearchScope.NAME
        )
        assertEquals(listOf("ps1"), mariaPostSale.map { it.id })

        // 2. Multi-format date search: "22/11", "2026-11-22", "domingo", "nov"
        assertEquals(
            listOf("v1"),
            com.example.util.PsaAgendaSearchHelper.filterVisits(
                visits, "22/11", com.example.util.AgendaSearchScope.DATE, displayMap
            ).map { it.id }
        )
        assertEquals(
            listOf("v1"),
            com.example.util.PsaAgendaSearchHelper.filterVisits(
                visits, "2026-11-22", com.example.util.AgendaSearchScope.DATE, displayMap
            ).map { it.id }
        )
        assertEquals(
            listOf("v1"),
            com.example.util.PsaAgendaSearchHelper.filterVisits(
                visits, "domingo", com.example.util.AgendaSearchScope.DATE, displayMap
            ).map { it.id }
        )
        assertEquals(
            listOf("v1", "v2"),
            com.example.util.PsaAgendaSearchHelper.filterVisits(
                visits, "nov", com.example.util.AgendaSearchScope.DATE, displayMap
            ).map { it.id }
        )
        assertEquals(
            listOf("l1"),
            com.example.util.PsaAgendaSearchHelper.filterLeads(
                leads, "22/11", com.example.util.AgendaSearchScope.DATE
            ).map { it.id }
        )
        assertEquals(
            listOf("ps2"),
            com.example.util.PsaAgendaSearchHelper.filterPostSale(
                postSale, "25/11", com.example.util.AgendaSearchScope.DATE
            ).map { it.id }
        )
        assertEquals(
            listOf("2026-11-22"),
            com.example.util.PsaAgendaSearchHelper.filterUniqueDates(uniqueDates, "domingo").map { it.first }
        )

        // 3. Scope isolation (NAME ignores dates, DATE ignores names)
        assertTrue(
            com.example.util.PsaAgendaSearchHelper.filterVisits(
                visits, "22/11", com.example.util.AgendaSearchScope.NAME, displayMap
            ).isEmpty()
        )
        assertTrue(
            com.example.util.PsaAgendaSearchHelper.filterLeads(
                leads, "22/11", com.example.util.AgendaSearchScope.NAME
            ).isEmpty()
        )
        assertTrue(
            com.example.util.PsaAgendaSearchHelper.filterVisits(
                visits, "María", com.example.util.AgendaSearchScope.DATE, displayMap
            ).isEmpty()
        )
        assertTrue(
            com.example.util.PsaAgendaSearchHelper.filterLeads(
                leads, "Lucas", com.example.util.AgendaSearchScope.DATE
            ).isEmpty()
        )

        // 4. Combined ALL scope matches combined name + date tokens
        assertEquals(
            listOf("v1"),
            com.example.util.PsaAgendaSearchHelper.filterVisits(
                visits, "maria 22/11", com.example.util.AgendaSearchScope.ALL, displayMap
            ).map { it.id }
        )
        assertTrue(
            com.example.util.PsaAgendaSearchHelper.filterVisits(
                visits, "maria 25/11", com.example.util.AgendaSearchScope.ALL, displayMap
            ).isEmpty()
        )
    }

    @Test
    fun `licenseStatusFormatter validates custom days evaluates user and token expiration states and formats invites`() {
        val now = 1_760_000_000_000L
        val hourMs = 3_600_000L
        val dayMs = 24L * hourMs

        // Custom days sanitization (1..3650)
        assertEquals(null, com.example.data.model.LicenseStatusFormatter.sanitizeCustomDays(""))
        assertEquals(null, com.example.data.model.LicenseStatusFormatter.sanitizeCustomDays("0"))
        assertEquals(null, com.example.data.model.LicenseStatusFormatter.sanitizeCustomDays("3651"))
        assertEquals(null, com.example.data.model.LicenseStatusFormatter.sanitizeCustomDays("abc"))
        assertEquals(1, com.example.data.model.LicenseStatusFormatter.sanitizeCustomDays("1"))
        assertEquals(45, com.example.data.model.LicenseStatusFormatter.sanitizeCustomDays(" 45 "))
        assertEquals(3650, com.example.data.model.LicenseStatusFormatter.sanitizeCustomDays("3650"))

        // User license states: ACTIVE, EXPIRING_SOON, CRITICAL_24H, EXPIRED, BLOCKED
        val baseUser = UserAccount(
            id = "op_1",
            username = "Laura Ventas",
            token = "PSA-30D-AB12CD34",
            role = "OPERATOR",
            daysGranted = 30,
            validFromMs = now - (10L * dayMs),
            expiresAtMs = now + (20L * dayMs),
            email = "laura@gmail.com"
        )
        val activeSnap = com.example.data.model.LicenseStatusFormatter.evaluateUserLicense(baseUser, now)
        assertEquals(com.example.data.model.LicenseExpirationState.ACTIVE, activeSnap.state)
        assertEquals("ACTIVA", activeSnap.statusLabel)
        assertFalse(activeSnap.isRestricted)
        assertTrue(activeSnap.progressFraction in 0.6f..0.7f)

        val soonUser = baseUser.copy(expiresAtMs = now + (2L * dayMs) + (4L * hourMs))
        val soonSnap = com.example.data.model.LicenseStatusFormatter.evaluateUserLicense(soonUser, now)
        assertEquals(com.example.data.model.LicenseExpirationState.EXPIRING_SOON, soonSnap.state)
        assertEquals("POR VENCER", soonSnap.statusLabel)

        val criticalUser = baseUser.copy(expiresAtMs = now + (12L * hourMs))
        val criticalSnap = com.example.data.model.LicenseStatusFormatter.evaluateUserLicense(criticalUser, now)
        assertEquals(com.example.data.model.LicenseExpirationState.CRITICAL_24H, criticalSnap.state)
        assertEquals("VENCE HOY", criticalSnap.statusLabel)

        val expiredUser = baseUser.copy(expiresAtMs = now - (2L * dayMs) - (3L * hourMs))
        val expiredSnap = com.example.data.model.LicenseStatusFormatter.evaluateUserLicense(expiredUser, now)
        assertEquals(com.example.data.model.LicenseExpirationState.EXPIRED, expiredSnap.state)
        assertEquals("EXPIRADA", expiredSnap.statusLabel)
        assertTrue(expiredSnap.isRestricted)
        assertEquals(0f, expiredSnap.progressFraction, 0.001f)
        assertTrue(expiredSnap.countdownLabel.contains("2d 3h"))

        val blockedSnap = com.example.data.model.LicenseStatusFormatter.evaluateUserLicense(
            baseUser.copy(isBlocked = true),
            now
        )
        assertEquals(com.example.data.model.LicenseExpirationState.BLOCKED, blockedSnap.state)
        assertEquals("BLOQUEADA", blockedSnap.statusLabel)
        assertTrue(blockedSnap.isRestricted)

        // CloudAccessToken evaluation lifecycle
        val availToken = com.example.data.model.CloudAccessToken(
            tokenCode = "PSA-45D-1234ABCD",
            assignedUsername = "Laura Ventas",
            targetEmail = "laura@gmail.com",
            daysGranted = 45,
            maxDevices = 2,
            status = com.example.data.model.CloudAccessTokenStatus.AVAILABLE.name,
            createdAtMs = now - dayMs
        )
        val availEval = com.example.data.model.LicenseStatusFormatter.evaluateAccessToken(availToken, now)
        assertEquals(com.example.data.model.LicenseExpirationState.PENDING_ACTIVATION, availEval.state)
        assertEquals("DISPONIBLE", availEval.statusLabel)
        assertTrue(availEval.countdownLabel.contains("45"))

        val redeemedEval = com.example.data.model.LicenseStatusFormatter.evaluateAccessToken(
            availToken.copy(
                status = com.example.data.model.CloudAccessTokenStatus.REDEEMED.name,
                redeemedByEmail = "laura@gmail.com",
                redeemedAtMs = now - (43L * dayMs),
                expiresAtMs = now + (2L * dayMs)
            ),
            now
        )
        assertEquals(com.example.data.model.LicenseExpirationState.EXPIRING_SOON, redeemedEval.state)

        val revokedEval = com.example.data.model.LicenseStatusFormatter.evaluateAccessToken(
            availToken.copy(status = com.example.data.model.CloudAccessTokenStatus.REVOKED.name),
            now
        )
        assertEquals(com.example.data.model.LicenseExpirationState.REVOKED, revokedEval.state)
        assertTrue(revokedEval.isRestricted)

        val inviteText = com.example.data.repository.EzoRepository.buildAccessTokenInviteMessage(
            username = "Laura Ventas",
            tokenCode = "PSA-45D-1234ABCD",
            days = 45,
            maxDevices = 2,
            targetEmail = "laura@gmail.com",
            apkDownloadUrl = "https://github.com/Elizondo17/Descargar-Como-On/releases/latest/download/ComoOn-PSA.apk"
        )
        assertTrue(inviteText.contains("PSA-45D-1234ABCD"))
        assertTrue(inviteText.contains("45 días"))
        assertTrue(inviteText.contains("laura@gmail.com"))
        assertTrue(inviteText.contains("Canjear Token y Activar"))
    }
}


