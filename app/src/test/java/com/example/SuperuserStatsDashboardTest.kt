package com.example

import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.layout.Column
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.example.data.model.HierarchyTreeBuilder
import com.example.data.model.SellerActivityBucket
import com.example.data.model.SellerPerformanceStats
import com.example.ui.components.SuperuserConversionBasis
import com.example.ui.components.SuperuserStatsDashboard
import com.example.ui.components.SuperuserStatsSortMode
import com.example.ui.components.computeSuperuserDashboardSummary
import com.example.ui.theme.MyApplicationTheme
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class SuperuserStatsDashboardTest {

    @get:Rule
    val composeTestRule = createComposeRule()

    private fun sampleSellers(): List<SellerPerformanceStats> {
        val bucketA = HierarchyTreeBuilder.serializeActivityBuckets(
            listOf(
                SellerActivityBucket("2026-11-24", "24/11", messagesSuccess = 12, messagesFailed = 3, demosCount = 2),
                SellerActivityBucket("2026-11-25", "25/11", messagesSuccess = 18, messagesFailed = 2, demosCount = 3)
            )
        )
        val bucketB = HierarchyTreeBuilder.serializeActivityBuckets(
            listOf(
                SellerActivityBucket("2026-11-24", "24/11", messagesSuccess = 8, messagesFailed = 2, demosCount = 1),
                SellerActivityBucket("2026-11-25", "25/11", messagesSuccess = 20, messagesFailed = 0, demosCount = 2)
            )
        )
        return listOf(
            SellerPerformanceStats(
                ownerUid = "uid_ana",
                sellerName = "Ana Gomez",
                sellerEmail = "ana@psa.test",
                totalContacts = 20,
                totalVisits = 12,
                completedDemos = 8,
                closedSales = 4,
                messagesSuccess = 30,
                messagesFailed = 5,
                messagesTotal = 35,
                recentActivityJson = bucketA
            ),
            SellerPerformanceStats(
                ownerUid = "uid_bruno",
                sellerName = "Bruno Diaz",
                sellerEmail = "bruno@psa.test",
                totalContacts = 40,
                totalVisits = 15,
                completedDemos = 10,
                closedSales = 4,
                messagesSuccess = 45,
                messagesFailed = 15,
                messagesTotal = 60,
                recentActivityJson = bucketB
            ),
            SellerPerformanceStats(
                ownerUid = "uid_zero",
                sellerName = "Carla Nueva",
                sellerEmail = "carla@psa.test",
                totalContacts = 0,
                totalVisits = 0,
                completedDemos = 0,
                closedSales = 0,
                messagesSuccess = 0,
                messagesFailed = 0,
                messagesTotal = 0
            )
        )
    }

    @Test
    fun `computeSuperuserDashboardSummary calculates rates, sorting, and buckets safely`() {
        val emptySummary = computeSuperuserDashboardSummary(emptyList())
        assertEquals(0, emptySummary.totalMessagesSent)
        assertEquals(0f, emptySummary.globalMessageSuccessRatePct, 0.01f)
        assertEquals(0f, emptySummary.globalConversionRatePct, 0.01f)

        val portfolioSummary = computeSuperuserDashboardSummary(
            stats = sampleSellers(),
            sortMode = SuperuserStatsSortMode.CONVERSION,
            conversionBasis = SuperuserConversionBasis.PORTFOLIO
        )
        assertEquals(95, portfolioSummary.totalMessagesSent)
        assertEquals(75, portfolioSummary.totalMessagesSuccess)
        assertEquals(20, portfolioSummary.totalMessagesFailed)
        assertEquals(75f * 100f / 95f, portfolioSummary.globalMessageSuccessRatePct, 0.05f)
        // Ana: 4/20 = 20%, Bruno: 4/40 = 10%, Carla: 0%
        assertEquals("uid_ana", portfolioSummary.sellers.first().raw.ownerUid)
        assertEquals(20f, portfolioSummary.sellers[0].portfolioConversionRatePct, 0.05f)
        assertEquals(50f, portfolioSummary.sellers[0].demoConversionRatePct, 0.05f)
        assertEquals(10f, portfolioSummary.sellers[1].portfolioConversionRatePct, 0.05f)
        assertEquals(0f, portfolioSummary.sellers[2].portfolioConversionRatePct, 0.01f)
        // Global portfolio conversion: 8 sales / 60 contacts = 13.33%
        assertEquals(8f * 100f / 60f, portfolioSummary.globalConversionRatePct, 0.05f)
        // Aggregated 7-day buckets: 2026-11-25 has 18+20=38 ok, 3+2=5 demos
        val nov25 = portfolioSummary.recent7DayBuckets.first { it.dayKey == "2026-11-25" }
        assertEquals(38, nov25.messagesSuccess)
        assertEquals(5, nov25.demosCount)

        val byMessages = computeSuperuserDashboardSummary(
            stats = sampleSellers(),
            sortMode = SuperuserStatsSortMode.MESSAGES,
            conversionBasis = SuperuserConversionBasis.DEMOS
        )
        assertEquals("uid_bruno", byMessages.sellers.first().raw.ownerUid)
        // Global demo conversion: 8 sales / 18 demos = 44.44%
        assertEquals(8f * 100f / 18f, byMessages.globalConversionRatePct, 0.05f)
    }

    @Test
    fun `SuperuserStatsDashboard shows empty state and triggers Firestore refresh`() {
        var refreshCount = 0
        composeTestRule.setContent {
            MyApplicationTheme {
                SuperuserStatsDashboard(
                    stats = emptyList(),
                    isCloudConnected = true,
                    onRefreshStats = { refreshCount++ }
                )
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("superuser_stats_dashboard").assertIsDisplayed()
        composeTestRule.onNodeWithTag("superuser_dashboard_empty_state").assertIsDisplayed()
        composeTestRule.onNodeWithTag("superuser_dashboard_empty_refresh_btn").performClick()
        composeTestRule.waitForIdle()
        assertEquals(1, refreshCount)
    }

    @Test
    fun `SuperuserStatsDashboard renders charts, switches basis, and opens seller detail tooltip`() {
        var refreshCount = 0
        composeTestRule.setContent {
            MyApplicationTheme {
                Column(modifier = Modifier.verticalScroll(rememberScrollState())) {
                    SuperuserStatsDashboard(
                        stats = sampleSellers(),
                        isCloudConnected = true,
                        onRefreshStats = { refreshCount++ }
                    )
                }
            }
        }

        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("superuser_kpi_messages_total").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("95").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("superuser_messages_chart_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("superuser_messages_chart_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("superuser_conversion_chart_card").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("superuser_conversion_chart_canvas").assertIsDisplayed()
        composeTestRule.onNodeWithTag("superuser_trend_chart_canvas").performScrollTo().assertIsDisplayed()

        // Switch conversion basis to Demos
        composeTestRule.onNodeWithTag("superuser_basis_demos").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithText("44.4%").performScrollTo().assertIsDisplayed()

        // Tap ranking row for Ana to inspect detailed tooltip
        composeTestRule.onNodeWithTag("superuser_ranking_row_uid_ana").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        composeTestRule.onNodeWithTag("superuser_seller_detail_tooltip").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("ana@psa.test").performScrollTo().assertIsDisplayed()

        // Trigger header refresh button
        composeTestRule.onNodeWithTag("superuser_dashboard_refresh_btn").performScrollTo().performClick()
        composeTestRule.waitForIdle()
        assertTrue(refreshCount >= 1)
    }
}
