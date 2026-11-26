package com.example

import android.content.Context
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.PsaContactLead
import com.example.data.model.PsaHomeVisit
import com.example.data.model.PsaPostSaleInstallation
import com.example.data.model.PsaPricingConfig
import com.example.data.repository.AgendaCloudSyncGateway
import com.example.data.repository.EzoRepository
import com.example.data.repository.PsaFinancingConfig
import com.example.ui.components.PsaWelcomeTutorialDialog
import com.example.ui.theme.MyApplicationTheme
import com.example.util.PsaCountryRegistry
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class PsaWelcomeTutorialTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `welcome tutorial flag defaults to false and persists changes`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.getSharedPreferences("como_on_auth_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()

        val db = Room.inMemoryDatabaseBuilder(context, AppDatabase::class.java)
            .allowMainThreadQueries()
            .build()

        try {
            val repo = EzoRepository(
                database = db,
                context = context,
                cloudGatewayOverride = object : AgendaCloudSyncGateway {
                    override fun currentOwnerUid(): String = "test-uid"
                    override suspend fun upsertVisit(ownerUid: String, visit: PsaHomeVisit): Result<Unit> = Result.success(Unit)
                    override suspend fun deleteVisit(ownerUid: String, visitId: String): Result<Unit> = Result.success(Unit)
                    override suspend fun upsertPostSale(ownerUid: String, record: PsaPostSaleInstallation): Result<Unit> = Result.success(Unit)
                    override suspend fun deletePostSale(ownerUid: String, recordId: String): Result<Unit> = Result.success(Unit)
                    override suspend fun upsertContact(ownerUid: String, lead: PsaContactLead): Result<Unit> = Result.success(Unit)
                    override suspend fun deleteContact(ownerUid: String, contactId: String): Result<Unit> = Result.success(Unit)
                }
            )

            assertFalse(repo.hasSeenWelcomeTutorial())
            repo.saveHasSeenWelcomeTutorial(true)
            assertTrue(repo.hasSeenWelcomeTutorial())
            repo.saveHasSeenWelcomeTutorial(false)
            assertFalse(repo.hasSeenWelcomeTutorial())
        } finally {
            db.close()
        }
    }

    @Test
    fun `welcome tutorial walks through water demo, quote simulator and launches chosen section`() {
        var demoLaunchedCount = 0
        var budgetLaunchedCount = 0
        var dismissedCount = 0

        composeTestRule.setContent {
            MyApplicationTheme {
                PsaWelcomeTutorialDialog(
                    pricingConfig = PsaPricingConfig(),
                    financingConfig = PsaFinancingConfig(),
                    activeCountry = PsaCountryRegistry.defaultCountry(),
                    onStartDemo = { demoLaunchedCount++ },
                    onStartBudget = { budgetLaunchedCount++ },
                    onDismiss = { dismissedCount++ }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("welcome_tutorial_dialog").assertIsDisplayed()
        composeTestRule.onNodeWithText("Paso 1 de 4 • Guía Interactiva").assertIsDisplayed()

        // Step 1 -> Step 2 (Demo de Agua)
        composeTestRule.onNodeWithTag("tutorial_btn_next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Paso 2 de 4 • Guía Interactiva").assertIsDisplayed()

        // Interactive Well Water toggle
        composeTestRule.onNodeWithTag("tutorial_source_well").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tutorial_hardness_chip_5").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Interactive Tap Water + Chlorine chip 3.0 ppm
        composeTestRule.onNodeWithTag("tutorial_source_tap").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tutorial_chlorine_chip_3.0").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("3.0 ppm • Concentración crítica").performScrollTo().assertIsDisplayed()

        // Step 2 -> Step 3 (Cotizador)
        composeTestRule.onNodeWithTag("tutorial_btn_next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Paso 3 de 4 • Guía Interactiva").assertIsDisplayed()

        // Interactive quote plan switch: 12 Cuotas + advance toggle + Contado
        composeTestRule.onNodeWithTag("tutorial_quote_plan_12").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tutorial_quote_advance_toggle").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tutorial_quote_plan_1").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("tutorial_quote_result_card").performScrollTo().assertIsDisplayed()

        // Step 3 -> Step 4 (Final Launch actions)
        composeTestRule.onNodeWithTag("tutorial_btn_next").performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("Paso 4 de 4 • Guía Interactiva").assertIsDisplayed()

        composeTestRule.onNodeWithTag("tutorial_btn_go_demo").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("tutorial_btn_go_budget").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("tutorial_btn_skip").performClick()

        assertEquals(1, demoLaunchedCount)
        assertEquals(1, budgetLaunchedCount)
        assertEquals(1, dismissedCount)
    }
}
