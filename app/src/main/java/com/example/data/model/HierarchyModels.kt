package com.example.data.model

import androidx.annotation.Keep
import com.google.firebase.firestore.DocumentId
import com.google.firebase.firestore.IgnoreExtraProperties
import org.json.JSONArray
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

enum class ApprovalStatus {
    ACTIVE,
    PENDING_APPROVAL,
    REJECTED
}

enum class HierarchyRequestType {
    NEW_SUB_USER,
    LINK_EXISTING_USER
}

enum class HierarchyRequestStatus {
    PENDING,
    APPROVED,
    REJECTED
}

@Keep
@IgnoreExtraProperties
data class HierarchyRequest(
    @DocumentId
    val id: String = "",
    val requestType: String = HierarchyRequestType.NEW_SUB_USER.name,
    val requestedParentEmail: String = "",
    val requestedParentName: String = "",
    val targetEmail: String = "",
    val targetUsername: String = "",
    val suggestedDays: Int = 30,
    val maxDevices: Int = 1,
    val notes: String = "",
    val requestedByUid: String = "",
    val requestedByEmail: String = "",
    val status: String = HierarchyRequestStatus.PENDING.name,
    val resolvedByEmail: String = "",
    val createdAtMs: Long = 0L,
    val updatedAtMs: Long = 0L
) {
    val parsedType: HierarchyRequestType
        get() = try {
            HierarchyRequestType.valueOf(requestType)
        } catch (_: Exception) {
            HierarchyRequestType.NEW_SUB_USER
        }

    val parsedStatus: HierarchyRequestStatus
        get() = try {
            HierarchyRequestStatus.valueOf(status)
        } catch (_: Exception) {
            HierarchyRequestStatus.PENDING
        }

    val normalizedParentEmail: String
        get() = requestedParentEmail.trim().lowercase()

    val normalizedTargetEmail: String
        get() = targetEmail.trim().lowercase()
}

data class SellerActivityBucket(
    val dayKey: String = "",
    val label: String = "",
    val messagesSuccess: Int = 0,
    val messagesFailed: Int = 0,
    val callsCount: Int = 0
) {
    val totalEvents: Int
        get() = messagesSuccess + messagesFailed + callsCount
}

data class HierarchyTreeNode(
    val user: UserAccount,
    val stats: SellerPerformanceStats? = null,
    val relativeLevel: Int = 0,
    val children: List<HierarchyTreeNode> = emptyList(),
    val directSubCount: Int = 0,
    val totalSubtreeCount: Int = 0
)

data class FlattenedTreeRow(
    val node: HierarchyTreeNode,
    val isExpanded: Boolean,
    val hasChildren: Boolean,
    val matchesSearch: Boolean
)

data class ReParentCascadeUpdate(
    val user: UserAccount,
    val newParentEmail: String,
    val newChain: List<String>,
    val newDepth: Int
)

object HierarchyTreeBuilder {

    const val MAX_CHAIN_LENGTH = 10

    fun normalizeEmail(raw: String): String = raw.trim().lowercase()

    fun normalizeChain(raw: List<String>): List<String> =
        raw.map { normalizeEmail(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_CHAIN_LENGTH)

    fun serializeChain(chain: List<String>): String =
        normalizeChain(chain).joinToString("|")

    fun parseChain(serialized: String): List<String> =
        serialized.split("|")
            .map { normalizeEmail(it) }
            .filter { it.isNotBlank() }
            .distinct()
            .take(MAX_CHAIN_LENGTH)

    fun isSuperuserViewer(user: UserAccount): Boolean =
        user.role == "SUPERUSER"

    /**
     * Checks whether assigning [movingUserEmail] under [targetParentEmail] would create
     * a self-loop or cycle anywhere in the pyramid.
     */
    fun wouldCreateCycle(
        movingUserEmail: String,
        targetParentEmail: String,
        allUsers: List<UserAccount>
    ): Boolean {
        val moving = normalizeEmail(movingUserEmail)
        val targetParent = normalizeEmail(targetParentEmail)
        if (moving.isBlank() || targetParent.isBlank()) return false
        if (moving == targetParent) return true

        val usersByEmail = allUsers.associateBy { it.normalizedEmail }
        val visited = mutableSetOf<String>()
        var cursor: String? = targetParent
        var hops = 0
        while (!cursor.isNullOrBlank() && hops < MAX_CHAIN_LENGTH + 4) {
            if (cursor == moving) return true
            if (!visited.add(cursor)) return true
            val parentUser = usersByEmail[cursor] ?: break
            if (parentUser.supervisorChainEmails.any { it == moving }) return true
            cursor = parentUser.parentEmail.trim().lowercase().takeIf { it.isNotBlank() }
            hops++
        }
        return false
    }

    /**
     * Builds the parent -> children hierarchy forest and computes recursive subtree counts.
     * - Superuser views all root nodes (excluding superuser root self unless children exist under them).
     * - Manager views their own branch, bounded by [UserAccount.visibilityDepth]
     *   (0 = unlimited branch below manager, N = up to N levels below).
     */
    fun buildHierarchyForest(
        viewer: UserAccount,
        allUsers: List<UserAccount>,
        statsByEmail: Map<String, SellerPerformanceStats> = emptyMap(),
        enforceDepthLimit: Boolean = true
    ): List<HierarchyTreeNode> {
        val viewerEmail = viewer.normalizedEmail
        val usersByEmail = allUsers
            .filter { it.normalizedEmail.isNotBlank() }
            .associateBy { it.normalizedEmail }
            .toMutableMap()
        if (viewerEmail.isNotBlank() && !usersByEmail.containsKey(viewerEmail)) {
            usersByEmail[viewerEmail] = viewer
        }

        val childrenMap = linkedMapOf<String, MutableList<UserAccount>>()
        usersByEmail.values.forEach { account ->
            val email = account.normalizedEmail
            val parent = normalizeEmail(account.parentEmail)
            val resolvedParent = if (
                parent.isNotBlank() &&
                parent != email &&
                usersByEmail.containsKey(parent)
            ) {
                parent
            } else {
                ""
            }
            childrenMap.getOrPut(resolvedParent) { mutableListOf() }.add(account)
        }

        childrenMap.values.forEach { bucket ->
            bucket.sortWith(
                compareBy<UserAccount>(
                    { if (it.isPendingApproval) 0 else 1 },
                    { if (it.isBlocked) 1 else 0 },
                    { it.username.lowercase() }
                )
            )
        }

        fun buildNode(
            account: UserAccount,
            relativeLevel: Int,
            maxDepth: Int,
            visited: Set<String>
        ): HierarchyTreeNode {
            val email = account.normalizedEmail
            val nextVisited = visited + email
            val canGoDeeper = maxDepth == 0 || relativeLevel < maxDepth
            val directKids = if (canGoDeeper) {
                childrenMap[email].orEmpty().filter { it.normalizedEmail !in nextVisited }
            } else {
                emptyList()
            }
            val childNodes = directKids.map { child ->
                buildNode(child, relativeLevel + 1, maxDepth, nextVisited)
            }
            val totalBelow = childNodes.sumOf { 1 + it.totalSubtreeCount }
            return HierarchyTreeNode(
                user = account,
                stats = statsByEmail[email],
                relativeLevel = relativeLevel,
                children = childNodes,
                directSubCount = childNodes.size,
                totalSubtreeCount = totalBelow
            )
        }

        if (isSuperuserViewer(viewer) && !enforceDepthLimit) {
            val roots = childrenMap[""].orEmpty().filter { it.normalizedEmail != viewerEmail }
            val adminDirectKids = childrenMap[viewerEmail].orEmpty()
            val combinedRoots = (adminDirectKids + roots).distinctBy { it.normalizedEmail }
            return combinedRoots.map { rootUser ->
                buildNode(rootUser, relativeLevel = 0, maxDepth = 0, visited = setOf(viewerEmail))
            }
        }

        val maxDepth = if (enforceDepthLimit) viewer.visibilityDepth.coerceAtLeast(0) else 0
        val viewerDirectChildren = childrenMap[viewerEmail].orEmpty()
        return viewerDirectChildren.map { child ->
            buildNode(child, relativeLevel = 1, maxDepth = maxDepth, visited = setOf(viewerEmail))
        }
    }

    fun flattenForDisplay(
        roots: List<HierarchyTreeNode>,
        expandedEmails: Set<String>,
        searchQuery: String
    ): List<FlattenedTreeRow> {
        val query = searchQuery.trim().lowercase()
        val result = mutableListOf<FlattenedTreeRow>()

        fun subtreeMatches(node: HierarchyTreeNode): Boolean {
            if (query.isBlank()) return true
            val selfMatch = node.user.username.lowercase().contains(query) ||
                node.user.normalizedEmail.contains(query) ||
                node.user.notes.lowercase().contains(query)
            if (selfMatch) return true
            return node.children.any { subtreeMatches(it) }
        }

        fun visit(node: HierarchyTreeNode) {
            val email = node.user.normalizedEmail
            val selfMatches = query.isBlank() ||
                node.user.username.lowercase().contains(query) ||
                email.contains(query) ||
                node.user.notes.lowercase().contains(query)
            val anyChildMatches = node.children.any { subtreeMatches(it) }
            if (query.isNotBlank() && !selfMatches && !anyChildMatches) return

            val expanded = if (query.isNotBlank()) true else email in expandedEmails
            result.add(
                FlattenedTreeRow(
                    node = node,
                    isExpanded = expanded,
                    hasChildren = node.children.isNotEmpty(),
                    matchesSearch = selfMatches && query.isNotBlank()
                )
            )
            if (expanded) {
                node.children.forEach { visit(it) }
            }
        }

        roots.forEach { visit(it) }
        return result
    }

    /**
     * Collects all descendant user accounts currently visible to [viewer], strictly respecting [UserAccount.visibilityDepth].
     */
    fun getVisibleSubordinates(
        viewer: UserAccount,
        allUsers: List<UserAccount>
    ): List<UserAccount> {
        if (isSuperuserViewer(viewer)) {
            val viewerEmail = viewer.normalizedEmail
            return allUsers.filter { it.normalizedEmail.isNotBlank() && it.normalizedEmail != viewerEmail }
        }
        val forest = buildHierarchyForest(
            viewer = viewer,
            allUsers = allUsers,
            statsByEmail = emptyMap(),
            enforceDepthLimit = true
        )
        val collected = mutableListOf<UserAccount>()
        fun walk(node: HierarchyTreeNode) {
            collected.add(node.user)
            node.children.forEach { walk(it) }
        }
        forest.forEach { walk(it) }
        return collected.distinctBy { it.normalizedEmail }
    }

    /**
     * Computes updated parentEmail, supervisorChainEmails, and hierarchyDepth for [targetEmail]
     * and cascades the recalculated ancestor chain down to all its descendants.
     */
    fun computeReParentCascadeUpdates(
        targetEmail: String,
        newParentEmail: String,
        allUsers: List<UserAccount>
    ): List<ReParentCascadeUpdate> {
        val cleanTarget = normalizeEmail(targetEmail)
        val cleanNewParent = normalizeEmail(newParentEmail)
        if (cleanTarget.isBlank()) return emptyList()
        if (cleanNewParent.isNotBlank() && wouldCreateCycle(cleanTarget, cleanNewParent, allUsers)) {
            return emptyList()
        }

        val usersByEmail = allUsers.associateBy { it.normalizedEmail }
        val targetUser = usersByEmail[cleanTarget] ?: return emptyList()

        val parentUser = usersByEmail[cleanNewParent]
        val parentChain = if (cleanNewParent.isBlank()) {
            emptyList()
        } else if (parentUser != null) {
            normalizeChain(listOf(cleanNewParent) + parentUser.supervisorChainEmails)
        } else {
            listOf(cleanNewParent)
        }
        val rootDepth = if (cleanNewParent.isBlank()) {
            0
        } else if (parentUser != null) {
            (parentUser.hierarchyDepth + 1).coerceIn(1, 15)
        } else {
            1
        }

        val childrenByParent = allUsers
            .filter { it.normalizedEmail != cleanTarget }
            .groupBy { normalizeEmail(it.parentEmail) }

        val updates = mutableListOf<ReParentCascadeUpdate>()
        val visited = mutableSetOf<String>()

        fun traverse(
            account: UserAccount,
            assignedParent: String,
            assignedChain: List<String>,
            assignedDepth: Int
        ) {
            val email = account.normalizedEmail
            if (!visited.add(email)) return
            val updated = account.copy(
                parentEmail = assignedParent,
                supervisorChainSerialized = serializeChain(assignedChain),
                hierarchyDepth = assignedDepth.coerceIn(0, 15)
            )
            updates.add(
                ReParentCascadeUpdate(
                    user = updated,
                    newParentEmail = assignedParent,
                    newChain = assignedChain,
                    newDepth = assignedDepth.coerceIn(0, 15)
                )
            )
            childrenByParent[email].orEmpty().forEach { child ->
                val childChain = normalizeChain(listOf(email) + assignedChain)
                traverse(
                    account = child,
                    assignedParent = email,
                    assignedChain = childChain,
                    assignedDepth = (assignedDepth + 1).coerceIn(1, 15)
                )
            }
        }

        traverse(targetUser, cleanNewParent, parentChain, rootDepth)
        return updates
    }

    fun buildLast7DaysBuckets(
        deliveryTimestampsAndSuccess: List<Pair<Long, Boolean>>,
        callTimestamps: List<Long>,
        nowMs: Long = System.currentTimeMillis()
    ): List<SellerActivityBucket> {
        val dayFmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val labelFmt = SimpleDateFormat("dd/MM", Locale.getDefault())
        val orderedDays = mutableListOf<Pair<String, String>>()
        val cal = Calendar.getInstance().apply {
            timeInMillis = nowMs
            add(Calendar.DAY_OF_YEAR, -6)
        }
        repeat(7) {
            val d = cal.time
            orderedDays.add(dayFmt.format(d) to labelFmt.format(d))
            cal.add(Calendar.DAY_OF_YEAR, 1)
        }

        val successByDay = hashMapOf<String, Int>()
        val failedByDay = hashMapOf<String, Int>()
        deliveryTimestampsAndSuccess.forEach { (ts, isSuccess) ->
            if (ts > 0L) {
                val key = dayFmt.format(Date(ts))
                if (isSuccess) {
                    successByDay[key] = (successByDay[key] ?: 0) + 1
                } else {
                    failedByDay[key] = (failedByDay[key] ?: 0) + 1
                }
            }
        }

        val callsByDay = hashMapOf<String, Int>()
        callTimestamps.forEach { ts ->
            if (ts > 0L) {
                val key = dayFmt.format(Date(ts))
                callsByDay[key] = (callsByDay[key] ?: 0) + 1
            }
        }

        return orderedDays.map { (dayKey, label) ->
            SellerActivityBucket(
                dayKey = dayKey,
                label = label,
                messagesSuccess = successByDay[dayKey] ?: 0,
                messagesFailed = failedByDay[dayKey] ?: 0,
                callsCount = callsByDay[dayKey] ?: 0
            )
        }
    }

    fun serializeActivityBuckets(buckets: List<SellerActivityBucket>): String {
        return try {
            val arr = JSONArray()
            buckets.takeLast(7).forEach { b ->
                val obj = JSONObject()
                obj.put("d", b.dayKey)
                obj.put("l", b.label)
                obj.put("ok", b.messagesSuccess)
                obj.put("fail", b.messagesFailed)
                obj.put("call", b.callsCount)
                arr.put(obj)
            }
            arr.toString().take(2000)
        } catch (_: Exception) {
            "[]"
        }
    }

    fun parseActivityBuckets(json: String): List<SellerActivityBucket> {
        if (json.isBlank() || json == "[]") return emptyList()
        return try {
            val arr = JSONArray(json)
            val list = mutableListOf<SellerActivityBucket>()
            for (i in 0 until arr.length()) {
                val obj = arr.optJSONObject(i) ?: continue
                list.add(
                    SellerActivityBucket(
                        dayKey = obj.optString("d"),
                        label = obj.optString("l"),
                        messagesSuccess = obj.optInt("ok", 0),
                        messagesFailed = obj.optInt("fail", 0),
                        callsCount = obj.optInt("call", 0)
                    )
                )
            }
            list
        } catch (_: Exception) {
            emptyList()
        }
    }
}
