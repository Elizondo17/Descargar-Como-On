package com.example

import androidx.test.core.app.ApplicationProvider
import com.example.data.model.ApprovalStatus
import com.example.data.model.HierarchyTreeBuilder
import com.example.data.model.SellerPerformanceStats
import com.example.data.model.UserAccount
import com.example.util.AccessLockReason
import com.example.util.LicenseSecurityGuard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HierarchyAndAnalyticsTest {

    private fun makeUser(
        email: String,
        username: String,
        role: String = "OPERATOR",
        parentEmail: String = "",
        chain: List<String> = emptyList(),
        depth: Int = 0,
        approvalStatus: ApprovalStatus = ApprovalStatus.ACTIVE,
        canManageSubUsers: Boolean = false,
        visibilityDepth: Int = 0,
        isBlocked: Boolean = false
    ): UserAccount {
        val now = System.currentTimeMillis()
        return UserAccount(
            id = email,
            username = username,
            token = "GOOGLE-AUTH",
            role = role,
            daysGranted = 30,
            validFromMs = now,
            expiresAtMs = now + 30L * 86_400_000L,
            isBlocked = isBlocked,
            email = email,
            parentEmail = parentEmail,
            supervisorChainSerialized = HierarchyTreeBuilder.serializeChain(chain),
            hierarchyDepth = depth,
            approvalStatus = approvalStatus.name,
            canManageSubUsers = canManageSubUsers,
            visibilityDepth = visibilityDepth
        )
    }

    @Test
    fun `cycle detection blocks self loops and ancestor re-parenting`() {
        val leader = makeUser("leader@psa.com", "Leader", canManageSubUsers = true)
        val mid = makeUser("mid@psa.com", "Mid", parentEmail = "leader@psa.com", chain = listOf("leader@psa.com"), depth = 1)
        val leaf = makeUser("leaf@psa.com", "Leaf", parentEmail = "mid@psa.com", chain = listOf("mid@psa.com", "leader@psa.com"), depth = 2)
        val all = listOf(leader, mid, leaf)

        assertTrue(HierarchyTreeBuilder.wouldCreateCycle("leader@psa.com", "leader@psa.com", all))
        assertTrue(HierarchyTreeBuilder.wouldCreateCycle("leader@psa.com", "mid@psa.com", all))
        assertTrue(HierarchyTreeBuilder.wouldCreateCycle("leader@psa.com", "leaf@psa.com", all))
        assertFalse(HierarchyTreeBuilder.wouldCreateCycle("leaf@psa.com", "leader@psa.com", all))
    }

    @Test
    fun `visibility depth strictly bounds visible pyramid levels while 0 allows full branch`() {
        val unlimitedLeader = makeUser("leader@psa.com", "Leader", canManageSubUsers = true, visibilityDepth = 0)
        val direct = makeUser("n1@psa.com", "N1", parentEmail = "leader@psa.com", chain = listOf("leader@psa.com"), depth = 1, canManageSubUsers = true)
        val grandChild = makeUser("n2@psa.com", "N2", parentEmail = "n1@psa.com", chain = listOf("n1@psa.com", "leader@psa.com"), depth = 2)
        val greatGrandChild = makeUser("n3@psa.com", "N3", parentEmail = "n2@psa.com", chain = listOf("n2@psa.com", "n1@psa.com", "leader@psa.com"), depth = 3)
        val all = listOf(unlimitedLeader, direct, grandChild, greatGrandChild)

        val fullVisible = HierarchyTreeBuilder.getVisibleSubordinates(unlimitedLeader, all).map { it.normalizedEmail }
        assertEquals(listOf("n1@psa.com", "n2@psa.com", "n3@psa.com"), fullVisible)

        val depth1Leader = unlimitedLeader.copy(visibilityDepth = 1)
        val oneLevel = HierarchyTreeBuilder.getVisibleSubordinates(depth1Leader, all).map { it.normalizedEmail }
        assertEquals(listOf("n1@psa.com"), oneLevel)

        val depth2Leader = unlimitedLeader.copy(visibilityDepth = 2)
        val twoLevels = HierarchyTreeBuilder.getVisibleSubordinates(depth2Leader, all).map { it.normalizedEmail }
        assertEquals(listOf("n1@psa.com", "n2@psa.com"), twoLevels)
    }

    @Test
    fun `re-parent cascade recalculates parent chain and depths across subtree`() {
        val directorA = makeUser("dira@psa.com", "Director A", canManageSubUsers = true)
        val directorB = makeUser("dirb@psa.com", "Director B", canManageSubUsers = true)
        val supervisor = makeUser("sup@psa.com", "Supervisor", parentEmail = "dira@psa.com", chain = listOf("dira@psa.com"), depth = 1, canManageSubUsers = true)
        val rep = makeUser("rep@psa.com", "Rep", parentEmail = "sup@psa.com", chain = listOf("sup@psa.com", "dira@psa.com"), depth = 2)
        val all = listOf(directorA, directorB, supervisor, rep)

        val cascade = HierarchyTreeBuilder.computeReParentCascadeUpdates(
            targetEmail = "sup@psa.com",
            newParentEmail = "dirb@psa.com",
            allUsers = all
        )
        assertEquals(2, cascade.size)
        val supUpdate = cascade.first { it.user.normalizedEmail == "sup@psa.com" }
        assertEquals("dirb@psa.com", supUpdate.newParentEmail)
        assertEquals(listOf("dirb@psa.com"), supUpdate.newChain)
        assertEquals(1, supUpdate.newDepth)

        val repUpdate = cascade.first { it.user.normalizedEmail == "rep@psa.com" }
        assertEquals("sup@psa.com", repUpdate.newParentEmail)
        assertEquals(listOf("sup@psa.com", "dirb@psa.com"), repUpdate.newChain)
        assertEquals(2, repUpdate.newDepth)
    }

    @Test
    fun `search flattening automatically expands branches matching query`() {
        val admin = makeUser("admin@psa.com", "Admin", role = "SUPERUSER")
        val parent = makeUser("lider@psa.com", "Carlos Líder", canManageSubUsers = true)
        val child = makeUser("ana@psa.com", "Ana Torres", parentEmail = "lider@psa.com", chain = listOf("lider@psa.com"), depth = 1)
        val forest = HierarchyTreeBuilder.buildHierarchyForest(
            viewer = admin,
            allUsers = listOf(parent, child),
            enforceDepthLimit = false
        )

        val collapsedRows = HierarchyTreeBuilder.flattenForDisplay(forest, expandedEmails = emptySet(), searchQuery = "")
        assertEquals(1, collapsedRows.size)

        val searchedRows = HierarchyTreeBuilder.flattenForDisplay(forest, expandedEmails = emptySet(), searchQuery = "Torres")
        assertEquals(2, searchedRows.size)
        assertTrue(searchedRows[1].matchesSearch)
    }

    @Test
    fun `pending approval user is locked until superuser activates account`() {
        val pendingUser = makeUser(
            email = "nuevo@psa.com",
            username = "Nuevo",
            approvalStatus = ApprovalStatus.PENDING_APPROVAL
        )
        assertFalse(pendingUser.isActive)
        assertTrue(pendingUser.isPendingApproval)

        val eval = LicenseSecurityGuard.evaluateAccess(ApplicationProvider.getApplicationContext(), pendingUser)
        assertTrue(eval.isLocked)
        assertEquals(AccessLockReason.PENDING_SUPERUSER_APPROVAL, eval.reason)
    }

    @Test
    fun `seller stats rates and 7 day activity buckets serialize and parse accurately`() {
        val now = System.currentTimeMillis()
        val buckets = HierarchyTreeBuilder.buildLast7DaysBuckets(
            deliveryTimestampsAndSuccess = listOf(
                now to true,
                now to true,
                now to false
            ),
            callTimestamps = listOf(now, now - 86_400_000L),
            nowMs = now
        )
        assertEquals(7, buckets.size)
        assertEquals(2, buckets.last().messagesSuccess)
        assertEquals(1, buckets.last().messagesFailed)
        assertEquals(1, buckets.last().callsCount)

        val json = HierarchyTreeBuilder.serializeActivityBuckets(buckets)
        val stats = SellerPerformanceStats(
            sellerName = "Ana",
            sellerEmail = "ana@psa.com",
            messagesSuccess = 8,
            messagesFailed = 2,
            messagesTotal = 10,
            callsAttempted = 5,
            callsConnected = 3,
            recentActivityJson = json
        )
        assertEquals(80f, stats.messagingSuccessRate, 0.01f)
        assertEquals(20f, stats.messagingFailureRate, 0.01f)
        assertEquals(60f, stats.callConnectionRate, 0.01f)
        assertEquals(7, stats.parsedRecentBuckets.size)
        assertEquals(2, stats.parsedRecentBuckets.last().messagesSuccess)
    }
}
