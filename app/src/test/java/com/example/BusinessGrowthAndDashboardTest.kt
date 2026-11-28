package com.example

import android.content.Context
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.click
import androidx.compose.ui.test.hasTestTag
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.test.performScrollToNode
import androidx.compose.ui.test.performTextInput
import androidx.compose.ui.test.performTouchInput
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.BusinessInviteEntity
import com.example.data.model.BusinessInviteStatus
import com.example.data.model.DeliveryLog
import com.example.data.model.DeliveryStats
import com.example.data.model.HourlyEffectivenessBucket
import com.example.data.model.PsaContactInteraction
import com.example.data.model.PsaPricingConfig
import com.example.data.model.SellerPerformanceStats
import com.example.ui.components.DashboardAnalyticsChartsSection
import com.example.ui.components.PhoneContactPickerDialog
import com.example.ui.components.PsaCareerEnterpriseComponent
import com.example.ui.screens.psa.PsaBudgetTabContent
import com.example.ui.screens.psa.PsaBusinessInvitesSection
import com.example.ui.screens.psa.PsaBusinessPresentationSection
import com.example.ui.screens.psa.PsaClientDemoSection
import com.example.ui.theme.MyApplicationTheme
import com.example.ui.viewmodel.buildHourlyEffectivenessBuckets
import com.example.util.BusinessPresentationEngine
import com.example.util.BusinessPresentationSessionState
import com.example.util.CareerRankId
import com.example.util.CashflowQuadrant
import com.example.util.PhoneContactEntry
import com.example.util.PresentationBlock
import com.example.util.PsaCountryRegistry
import com.example.util.WeeklyPlanMode
import com.example.util.WeeklyProspectingPlanState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import java.util.Calendar
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class BusinessGrowthAndDashboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun millisAtHour(hour: Int): Long {
        return Calendar.getInstance().apply {
            set(2026, Calendar.NOVEMBER, 26, hour, 15, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }

    @Test
    fun `buildHourlyEffectivenessBuckets aggregates messages and calls by hour`() {
        val logs = listOf(
            DeliveryLog(id = 1, recipientId = "111", recipientPreview = "A", crmFolder = "PSA", scriptTitle = "Demo", scriptSnippet = "Hola", status = "SUCCESS", timestamp = millisAtHour(10)),
            DeliveryLog(id = 2, recipientId = "112", recipientPreview = "B", crmFolder = "PSA", scriptTitle = "Demo", scriptSnippet = "Hola", status = "FAILED", timestamp = millisAtHour(10)),
            DeliveryLog(id = 3, recipientId = "113", recipientPreview = "C", crmFolder = "PSA", scriptTitle = "Demo", scriptSnippet = "Hola", status = "SUCCESS", timestamp = millisAtHour(18))
        )
        val interactions = listOf(
            PsaContactInteraction(
                id = "i1",
                leadId = "c1",
                leadName = "C",
                channel = "CALL",
                result = "SUCCESS",
                dateKey = "2026-11-26",
                timestampMs = millisAtHour(18)
            ),
            PsaContactInteraction(
                id = "i2",
                leadId = "c2",
                leadName = "D",
                channel = "CALL",
                result = "NO_ANSWER",
                dateKey = "2026-11-26",
                timestampMs = millisAtHour(18)
            )
        )

        val buckets = buildHourlyEffectivenessBuckets(logs, interactions)
        assertEquals(14, buckets.size) // 8..21 inclusive
        val h10 = buckets.first { it.hour == 10 }
        assertEquals(2, h10.totalMessages)
        assertEquals(1, h10.successMessages)
        assertEquals(1, h10.failedMessages)
        assertEquals(50f, h10.messageSuccessRate, 0.1f)

        val h18 = buckets.first { it.hour == 18 }
        assertEquals(1, h18.totalMessages)
        assertEquals(2, h18.totalCalls)
        assertEquals(1, h18.connectedCalls)
        assertEquals(2f / 3f * 100f, h18.combinedEffectivenessRate, 0.2f)
    }

    @Test
    fun `dashboard analytics displays hourly effectiveness chart, filters, and tooltip`() {
        val buckets = (8..21).map { h ->
            when (h) {
                10 -> HourlyEffectivenessBucket(hour = h, totalMessages = 8, successMessages = 6, failedMessages = 2, totalCalls = 2, connectedCalls = 1)
                19 -> HourlyEffectivenessBucket(hour = h, totalMessages = 5, successMessages = 5, failedMessages = 0, totalCalls = 4, connectedCalls = 4)
                else -> HourlyEffectivenessBucket(hour = h)
            }
        }
        val sellers = listOf(
            SellerPerformanceStats(
                ownerUid = "u1",
                sellerName = "Ana",
                sellerEmail = "ana@test.com",
                messagesSuccess = 11,
                messagesFailed = 2,
                messagesTotal = 13,
                callsAttempted = 6,
                callsConnected = 5
            )
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    DashboardAnalyticsChartsSection(
                        teamStats = sellers,
                        localBuckets = emptyList(),
                        deliveryStats = DeliveryStats(totalCount = 13, successCount = 11, failedCount = 2),
                        currentUserEmail = "ana@test.com",
                        canViewSubordinates = true,
                        onRefreshBoard = {},
                        hourlyBuckets = buckets
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("dashboard_hourly_effectiveness_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("hourly_filter_messages").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("hourly_filter_calls").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("hourly_filter_all").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("analytics_hourly_effectiveness_chart").performScrollTo()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("analytics_hourly_effectiveness_chart").performTouchInput {
            click(Offset(width * 0.28f, height * 0.5f))
        }
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("hourly_tooltip_card").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `budget calculator defaults cash discount to 0 percent and applies manual discount`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val pricing = PsaPricingConfig()
        val country = PsaCountryRegistry.defaultCountry()
        val fullListPriceFormatted = PsaCountryRegistry.currencyNumberFormat(country).format(pricing.senior4Price)

        composeTestRule.setContent {
            MyApplicationTheme {
                PsaBudgetTabContent(
                    context = context,
                    viewModel = null,
                    pricingConfig = pricing,
                    activeCountry = country,
                    onOpenPricingSettings = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("psa_budget_lazy_list")
            .performScrollToNode(hasTestTag("budget_cash_discount_chip_10"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("budget_cash_discount_input").assertIsDisplayed()
        composeTestRule.onNodeWithTag("budget_cash_total_value").assertTextEquals("Total: $fullListPriceFormatted")

        composeTestRule.onNodeWithTag("budget_cash_discount_chip_10").performClick()
        composeTestRule.waitForIdle()
        val tenPctOffFormatted = PsaCountryRegistry.currencyNumberFormat(country).format(pricing.senior4Price * 0.90)
        composeTestRule.onNodeWithTag("budget_cash_total_value").assertTextEquals("Total: $tenPctOffFormatted")
    }

    @Test
    fun `client demonstration screen hides AI quick assistant buttons`() {
        composeTestRule.setContent {
            MyApplicationTheme {
                PsaClientDemoSection(
                    pricingConfig = PsaPricingConfig(),
                    activeCountry = PsaCountryRegistry.defaultCountry(),
                    onOpenAiAssistant = {}
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("demo_open_psa_ai_btn").assertDoesNotExist()
    }

    @Test
    fun `phone contact picker filters contacts by name and phone and returns selection`() {
        var selected: PhoneContactEntry? = null
        val sampleContacts = listOf(
            PhoneContactEntry(id = "c1", name = "María Gómez", phone = "+5491122334455"),
            PhoneContactEntry(id = "c2", name = "Carlos Méndez", phone = "+5491199887766")
        )

        composeTestRule.setContent {
            MyApplicationTheme {
                PhoneContactPickerDialog(
                    onSelectContact = { selected = it },
                    onDismiss = {},
                    contactsLoaderOverride = { sampleContacts }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("phone_contact_row_c1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("phone_contact_row_c2").assertIsDisplayed()
        composeTestRule.onNodeWithTag("phone_contact_search_input").performTextInput("Carlos")
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("phone_contact_row_c1").assertDoesNotExist()
        composeTestRule.onNodeWithTag("phone_contact_row_c2").performClick()
        assertEquals("Carlos Méndez", selected?.name)
    }

    @Test
    fun `business invites and guided 3 step business presentation complete recruit flow`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val invite = BusinessInviteEntity(
            id = "inv1",
            name = "Sofía Álvarez",
            phone = "+5491144332211",
            email = "sofia@gmail.com",
            status = BusinessInviteStatus.CONFIRMED.name,
            presentationDate = "2026-11-28",
            presentationTime = "19:00",
            reminderEnabled = true
        )
        var presentedInvite: BusinessInviteEntity? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                PsaBusinessInvitesSection(
                    context = context,
                    invites = listOf(invite),
                    sellerDisplayName = "Leo",
                    onSaveInvite = {},
                    onDeleteInvite = {},
                    onStartPresentationForInvite = { presentedInvite = it },
                    onSubmitTeamRecruit = { _, _, _, _, _ -> }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("psa_business_invites_screen")
            .performScrollToNode(hasTestTag("business_invite_card_inv1"))
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("business_invite_card_inv1").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_invite_present_inv1").performScrollTo().performClick()
        assertEquals("inv1", presentedInvite?.id)
    }

    @Test
    fun `guided business presentation advances slides to earnings calculator and submits recruit`() {
        val invite = BusinessInviteEntity(
            id = "inv2",
            name = "Luciano Paz",
            phone = "+5491177665544",
            email = "luciano@gmail.com"
        )
        var submittedName = ""
        var submittedEmail = ""
        var submittedInviteId: String? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                PsaBusinessPresentationSection(
                    pricingConfig = PsaPricingConfig(),
                    activeCountry = PsaCountryRegistry.defaultCountry(),
                    activeInvite = invite,
                    onSubmitQuickRecruit = { inviteId, name, email, _, _, _ ->
                        submittedInviteId = inviteId
                        submittedName = name
                        submittedEmail = email
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("biz_pres_slide_card").assertIsDisplayed()
        repeat(22) {
            composeTestRule.onNodeWithTag("btn_slide_next").performScrollTo().performClick()
            composeTestRule.waitForIdle()
        }
        composeTestRule.onNodeWithTag("btn_slides_to_calculator").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("biz_pres_calculator_panel").assertIsDisplayed()
        composeTestRule.onNodeWithTag("biz_calc_monthly_total").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_calc_to_signup").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("biz_pres_quick_signup_card").assertIsDisplayed()
        composeTestRule.onNodeWithTag("btn_biz_pres_submit_recruit").performScrollTo().performClick()
        assertTrue(submittedInviteId == "inv2")
        assertEquals("Luciano Paz", submittedName)
        assertEquals("luciano@gmail.com", submittedEmail)
    }

    @Test
    fun `all 23 presentation slides and 5 blocks preserve official prompts and recalculate with loaded catalog`() {
        assertEquals(23, BusinessPresentationEngine.allSlides.size)
        assertEquals(5, PresentationBlock.entries.size)
        assertTrue(BusinessPresentationEngine.allSlides.any { it.questionPrompt.contains("cuadrante") })
        assertTrue(BusinessPresentationEngine.allSlides.any { it.questionPrompt.contains("De 1 a 10") })

        val baselineAr = BusinessPresentationEngine.calculateDynamicMetrics(
            pricingConfig = PsaCountryRegistry.defaultPricingForCountry("AR"),
            activeCountry = PsaCountryRegistry.getByCode("AR")
        )
        val doubledCatalog = PsaCountryRegistry.defaultPricingForCountry("AR").copy(senior4Price = 1_900_000.0)
        val updatedAr = BusinessPresentationEngine.calculateDynamicMetrics(
            pricingConfig = doubledCatalog,
            activeCountry = PsaCountryRegistry.getByCode("AR")
        )
        assertEquals(baselineAr.personalOneProductProfit * 2.0, updatedAr.personalOneProductProfit, 2000.0)
        assertEquals(baselineAr.networkTenDistTenProductsProfit * 2.0, updatedAr.networkTenDistTenProductsProfit, 20000.0)

        val careerRows = BusinessPresentationEngine.buildCareerTableForActiveCatalog(
            pricingConfig = doubledCatalog,
            activeCountry = PsaCountryRegistry.getByCode("AR")
        )
        assertEquals(9, careerRows.size)
        assertTrue(careerRows.first { it.countryCode == "AR" }.isDynamicFromCatalog)

        val msg = BusinessPresentationEngine.buildWhatsAppMaterialMessage(
            inviteName = "Luciano",
            sellerName = "Leo",
            selectedMaterials = BusinessPresentationEngine.shareableMaterials.take(2),
            ratingScore = 9,
            followUpDate = "2026-12-03",
            followUpTime = "18:30"
        )
        assertTrue(msg.contains("Luciano"))
        assertTrue(msg.contains("9/10"))
        assertTrue(msg.contains("2026-12-03"))

        val notes = BusinessPresentationEngine.buildSummaryNotesForInvite(
            BusinessPresentationSessionState(
                currentQuadrant = CashflowQuadrant.E,
                desiredQuadrant = CashflowQuadrant.D,
                closingScore = 9
            )
        )
        assertTrue(notes.contains("E->D"))
        assertTrue(notes.contains("9/10"))
    }

    @Test
    fun `interactive presentation block jumps save thermometer followup and transfer summary to recruit`() {
        val invite = BusinessInviteEntity(
            id = "inv3",
            name = "Sofía Méndez",
            phone = "+5491155556666",
            email = "sofia@gmail.com"
        )
        var savedFollowUp: BusinessInviteEntity? = null
        var submittedNotes = ""

        composeTestRule.setContent {
            MyApplicationTheme {
                PsaBusinessPresentationSection(
                    pricingConfig = PsaPricingConfig(),
                    activeCountry = PsaCountryRegistry.defaultCountry(),
                    activeInvite = invite,
                    sellerDisplayName = "Leo",
                    onSaveInviteFollowUp = { savedFollowUp = it },
                    onSubmitQuickRecruit = { _, _, _, _, _, notes ->
                        submittedNotes = notes
                    }
                )
            }
        }
        composeTestRule.waitForIdle()

        // Jump to slide 2 (E-D-A-I Quadrant) and select current quadrant E
        composeTestRule.onNodeWithTag("btn_slide_next").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("quadrant_e").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Jump to Block 5 (Vision & Close), advance to Thermometer (index 21 = 2 steps ahead)
        composeTestRule.onNodeWithTag("block_tab_close").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        repeat(2) {
            composeTestRule.onNodeWithTag("btn_slide_next").performScrollTo().performClick()
            composeTestRule.waitForIdle()
        }

        composeTestRule.onNodeWithTag("thermometer_score_9").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("input_followup_date").performScrollTo().performTextInput("2026-12-05")
        composeTestRule.onNodeWithTag("btn_save_second_meeting").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        assertEquals("2026-12-05", savedFollowUp?.presentationDate)
        assertTrue(savedFollowUp?.notes?.contains("9/10") == true)

        // Jump straight to Quick Signup from Thermometer and verify notes carry summary
        composeTestRule.onNodeWithTag("btn_thermometer_to_signup").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("btn_biz_pres_submit_recruit").performScrollTo().performClick()
        assertTrue(submittedNotes.contains("E->D"))
        assertTrue(submittedNotes.contains("9/10"))
    }

    @Test
    fun `career enterprise summary recalculates awards and levels from loaded catalog and weekly pace`() {
        val countryAr = PsaCountryRegistry.getByCode("AR")
        val baseCatalog = PsaCountryRegistry.defaultPricingForCountry("AR")
        val doubledCatalog = baseCatalog.copy(senior4Price = baseCatalog.senior4Price * 2.0)

        val halfTimePlan = WeeklyProspectingPlanState.forMode(WeeklyPlanMode.HALF_TIME)
        val fullTimePlan = WeeklyProspectingPlanState.forMode(WeeklyPlanMode.FULL_TIME)

        val baseHalfSummary = BusinessPresentationEngine.calculateCareerEnterpriseSummary(
            pricingConfig = baseCatalog,
            activeCountry = countryAr,
            planState = halfTimePlan
        )
        val doubledHalfSummary = BusinessPresentationEngine.calculateCareerEnterpriseSummary(
            pricingConfig = doubledCatalog,
            activeCountry = countryAr,
            planState = halfTimePlan
        )
        val baseFullSummary = BusinessPresentationEngine.calculateCareerEnterpriseSummary(
            pricingConfig = baseCatalog,
            activeCountry = countryAr,
            planState = fullTimePlan
        )

        assertEquals(7, baseHalfSummary.ranks.size)
        assertEquals(5, baseHalfSummary.milestoneAwards.size)
        assertEquals(150, baseHalfSummary.milestoneAwards.first().pbPoints)
        assertEquals(40000, baseHalfSummary.milestoneAwards.last().pbPoints)

        // Doubling the loaded catalog price doubles awards and rank incomes proportionally
        assertEquals(
            baseHalfSummary.unitDirectProfit * 2.0,
            doubledHalfSummary.unitDirectProfit,
            2000.0
        )
        assertEquals(
            baseHalfSummary.milestoneAwards[1].calculatedAmount * 2.0,
            doubledHalfSummary.milestoneAwards[1].calculatedAmount,
            5000.0
        )

        // Switching to Full Time boosts monthly closures and shortens estimated time to target rank
        assertEquals(16, baseHalfSummary.monthlyClosesProjected)
        assertEquals(24, baseFullSummary.monthlyClosesProjected)
        assertTrue(baseFullSummary.projectedMonthlyPersonalIncome > baseHalfSummary.projectedMonthlyPersonalIncome)
        assertTrue(baseFullSummary.estimatedMonthsToTargetRank <= baseHalfSummary.estimatedMonthsToTargetRank)

        // Selecting another catalog item recalculates against that catalog item price
        val quantumPlan = halfTimePlan.copy(selectedReferencePurifierId = "quantum", targetRank = CareerRankId.CE)
        val quantumSummary = BusinessPresentationEngine.calculateCareerEnterpriseSummary(
            pricingConfig = baseCatalog,
            activeCountry = countryAr,
            planState = quantumPlan
        )
        assertEquals("quantum", quantumSummary.referencePurifierId)
        assertEquals(baseCatalog.quantumPrice, quantumSummary.referenceUnitPrice, 1.0)
        assertTrue(quantumSummary.unitDirectProfit > baseHalfSummary.unitDirectProfit)
        assertEquals(CareerRankId.CE, quantumSummary.targetAward.linkedRank)
    }

    @Test
    fun `career enterprise component switches Medio Tiempo and Full Tiempo adjusts goals and saves`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val country = PsaCountryRegistry.defaultCountry()
        val pricing = PsaCountryRegistry.defaultPricingForCountry(country.code)
        var lastSavedPlan: WeeklyProspectingPlanState? = null

        composeTestRule.setContent {
            MyApplicationTheme {
                var planState by remember {
                    mutableStateOf(WeeklyProspectingPlanState.forMode(WeeklyPlanMode.HALF_TIME))
                }
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    PsaCareerEnterpriseComponent(
                        pricingConfig = pricing,
                        activeCountry = country,
                        planState = planState,
                        onPlanStateChange = { planState = it },
                        onSavePlan = { saved ->
                            lastSavedPlan = saved
                            WeeklyProspectingPlanState.saveToPrefs(context, saved)
                        }
                    )
                }
            }
        }
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("career_enterprise_component").assertIsDisplayed()
        composeTestRule.onNodeWithTag("career_award_card_pb_600").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("career_enterprise_rank_CE").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        // Default Half Time goals: 4 Demos, 2 PNs
        composeTestRule.onNodeWithTag("goal_val_demos").performScrollTo().assertTextEquals("4")
        composeTestRule.onNodeWithTag("goal_val_pns").performScrollTo().assertTextEquals("2")

        // Switch to Full Time -> official 6 Demos, 5 PNs
        composeTestRule.onNodeWithTag("weekly_plan_toggle_full_time").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("goal_val_demos").performScrollTo().assertTextEquals("6")
        composeTestRule.onNodeWithTag("goal_val_pns").performScrollTo().assertTextEquals("5")

        // Switch back to Medio Tiempo and customize Demos & PNs
        composeTestRule.onNodeWithTag("weekly_plan_toggle_half_time").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("goal_inc_demos").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("goal_inc_pns").performScrollTo().performClick()
        composeTestRule.waitForIdle()

        composeTestRule.onNodeWithTag("goal_val_demos").performScrollTo().assertTextEquals("5")
        composeTestRule.onNodeWithTag("goal_val_pns").performScrollTo().assertTextEquals("3")

        // Save plan & confirm SharedPreferences persistence
        composeTestRule.onNodeWithTag("btn_save_weekly_plan").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("weekly_plan_saved_banner").performScrollTo().assertIsDisplayed()

        assertEquals(WeeklyPlanMode.HALF_TIME, lastSavedPlan?.mode)
        assertEquals(5, lastSavedPlan?.demosPerWeek)
        assertEquals(3, lastSavedPlan?.pnsPerWeek)
        assertEquals(CareerRankId.CE, lastSavedPlan?.targetRank)

        val persisted = WeeklyProspectingPlanState.loadFromPrefs(context)
        assertEquals(5, persisted.demosPerWeek)
        assertEquals(3, persisted.pnsPerWeek)
        assertEquals(CareerRankId.CE, persisted.targetRank)

        // Reset restores official Medio Tiempo defaults (4 Demos, 2 PNs)
        composeTestRule.onNodeWithTag("btn_reset_weekly_goals").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("goal_val_demos").performScrollTo().assertTextEquals("4")
        composeTestRule.onNodeWithTag("goal_val_pns").performScrollTo().assertTextEquals("2")
    }
}
