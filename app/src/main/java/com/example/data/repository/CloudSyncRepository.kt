package com.example.data.repository

import android.content.Context
import android.util.Log
import com.example.data.model.AlertSeverity
import com.example.data.model.CriticalCrmAlert
import com.example.data.model.RoleVisibilityConfig
import com.example.data.model.SalesKnowledgeItem
import com.example.data.model.UserAccount
import com.example.util.CriticalNotificationHelper
import com.example.util.UnattendedAutomationLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.util.concurrent.TimeUnit

class CloudSyncRepository private constructor(private val context: Context) {

    companion object {
        private const val TAG = "CloudSyncRepository"
        // Universal Cloud Sync Endpoint on Cloud Run
        const val DEFAULT_BASE_URL = "https://ais-dev-6nuqwfkrgmgfzmljmo2arz-684101465763.us-east1.run.app/api/sync"

        @Volatile
        private var INSTANCE: CloudSyncRepository? = null

        fun getInstance(context: Context): CloudSyncRepository {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: CloudSyncRepository(context.applicationContext).also { INSTANCE = it }
            }
        }
    }

    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()

    private val jsonMediaType = "application/json; charset=utf-8".toMediaType()

    private val _isSyncing = MutableStateFlow(false)
    val isSyncing: StateFlow<Boolean> = _isSyncing.asStateFlow()

    private val _lastAlerts = MutableStateFlow<List<CriticalCrmAlert>>(emptyList())
    val lastAlerts: StateFlow<List<CriticalCrmAlert>> = _lastAlerts.asStateFlow()

    private var syncJob: Job? = null
    private var lastSeenAlertTimestamp: Long = System.currentTimeMillis() - 60_000L

    fun startContinuousSync(
        scope: CoroutineScope,
        usernameProvider: () -> String?,
        onUserUpdated: suspend (isBlocked: Boolean, newExpiresAtMs: Long, newDays: Int) -> Unit,
        onEmergencyPauseRequired: () -> Unit,
        onKnowledgeSynced: (suspend (List<SalesKnowledgeItem>) -> Unit)? = null,
        onRoleMatrixSynced: (suspend (RoleVisibilityConfig) -> Unit)? = null
    ) {
        syncJob?.cancel()
        syncJob = scope.launch(Dispatchers.IO) {
            var loopIndex = 0
            while (isActive) {
                try {
                    val currentUsername = usernameProvider()?.trim()
                    if (!currentUsername.isNullOrBlank()) {
                        syncCurrentUserStatus(currentUsername, onUserUpdated)
                    }
                    syncAlerts(onEmergencyPauseRequired)

                    // Synchronize shared sales memory across team devices every 4 iterations (~16s)
                    if (onKnowledgeSynced != null && loopIndex % 4 == 0) {
                        val remoteKnowledge = fetchSalesKnowledgeFromCloud()
                        if (remoteKnowledge.isNotEmpty()) {
                            onKnowledgeSynced(remoteKnowledge)
                        }
                    }

                    // Synchronize role matrix configuration every 2 iterations (~8s)
                    if (onRoleMatrixSynced != null && loopIndex % 2 == 0) {
                        val matrix = fetchRoleMatrixFromCloud()
                        if (matrix != null) {
                            onRoleMatrixSynced(matrix)
                        }
                    }
                    loopIndex++
                } catch (e: Exception) {
                    Log.w(TAG, "Sync loop notice: ${e.message}")
                }
                // Poll every 4 seconds for rapid real-time block and status propagation
                delay(4000L)
            }
        }
    }

    fun stopContinuousSync() {
        syncJob?.cancel()
        syncJob = null
    }

    suspend fun syncCurrentUserStatus(
        username: String,
        onUserUpdated: suspend (isBlocked: Boolean, newExpiresAtMs: Long, newDays: Int) -> Unit
    ) = withContext(Dispatchers.IO) {
        val cleanUser = username.trim().lowercase()
        val url = "$DEFAULT_BASE_URL/users/$cleanUser"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    if (bodyStr.isNotBlank()) {
                        val obj = JSONObject(bodyStr)
                        val isBlocked = obj.optBoolean("isBlocked", false)
                        val expiresAtMs = obj.optLong("expiresAtMs", 0L)
                        val daysGranted = obj.optInt("daysGranted", 0)

                        onUserUpdated(isBlocked, expiresAtMs, daysGranted)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error fetching user status: ${e.message}")
        }
    }

    suspend fun pushUserUpdateToCloud(user: UserAccount): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = "$DEFAULT_BASE_URL/users/update"
        val json = JSONObject().apply {
            put("id", user.id)
            put("username", user.username.trim().lowercase())
            put("role", user.role)
            put("token", user.token)
            put("validFromMs", user.validFromMs)
            put("expiresAtMs", user.expiresAtMs)
            put("daysGranted", user.daysGranted)
            put("isBlocked", user.isBlocked)
            put("canAccessLegacyModules", user.canAccessLegacyModules)
            put("notes", user.notes)
        }

        val request = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    UnattendedAutomationLogger.logEventSync(
                        context = context,
                        tag = "CLOUD_SYNC_USER_OK",
                        recipient = user.username,
                        status = "BLOQUEO=${user.isBlocked} | DIAS=${user.daysGranted}",
                        exitedChatSuccessfully = true
                    )
                    Result.success(true)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun broadcastAlertToCloud(alert: CriticalCrmAlert): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = "$DEFAULT_BASE_URL/alerts"
        val json = JSONObject().apply {
            put("id", alert.id)
            put("title", alert.title)
            put("body", alert.body)
            put("severity", alert.severity.name)
            put("requirePause", alert.requireAutomationPause)
            put("timestamp", alert.timestamp)
            put("sender", alert.senderAdmin)
        }

        val request = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    // Also trigger local alert for sender
                    CriticalNotificationHelper.showNotification(context, alert)
                    Result.success(true)
                } else {
                    Result.failure(Exception("HTTP ${response.code}"))
                }
            }
        } catch (e: Exception) {
            // If offline, still show local alert
            CriticalNotificationHelper.showNotification(context, alert)
            Result.failure(e)
        }
    }

    suspend fun syncAlerts(onEmergencyPauseRequired: () -> Unit) = withContext(Dispatchers.IO) {
        val url = "$DEFAULT_BASE_URL/alerts"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    if (bodyStr.isNotBlank()) {
                        val array = JSONArray(bodyStr)
                        val alertList = mutableListOf<CriticalCrmAlert>()
                        for (i in 0 until array.length()) {
                            val obj = array.optJSONObject(i) ?: continue
                            val ts = obj.optLong("timestamp", 0L)
                            val sevStr = obj.optString("severity", "CRITICAL")
                            val sev = try { AlertSeverity.valueOf(sevStr) } catch (_: Exception) { AlertSeverity.CRITICAL }
                            val requirePause = obj.optBoolean("requirePause", sev == AlertSeverity.CRITICAL)

                            val alert = CriticalCrmAlert(
                                id = obj.optString("id"),
                                title = obj.optString("title"),
                                body = obj.optString("body"),
                                severity = sev,
                                requireAutomationPause = requirePause,
                                timestamp = ts,
                                senderAdmin = obj.optString("sender", "Superusuario")
                            )
                            alertList.add(alert)

                            // If this is a brand new alert received after lastSeenAlertTimestamp, fire notification!
                            if (ts > lastSeenAlertTimestamp) {
                                lastSeenAlertTimestamp = ts
                                CriticalNotificationHelper.showNotification(context, alert)
                                if (requirePause) {
                                    onEmergencyPauseRequired()
                                }
                            }
                        }
                        _lastAlerts.value = alertList
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error syncing alerts: ${e.message}")
        }
    }

    suspend fun pushSalesKnowledgeToCloud(item: SalesKnowledgeItem): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = "$DEFAULT_BASE_URL/sales_knowledge"
        val json = JSONObject().apply {
            put("id", item.id)
            put("category", item.category)
            put("triggerContext", item.triggerContext)
            put("winningContent", item.winningContent)
            put("successScore", item.successScore)
            put("isQuestion", item.isQuestion)
        }

        val request = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()

        try {
            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchSalesKnowledgeFromCloud(): List<SalesKnowledgeItem> = withContext(Dispatchers.IO) {
        val url = "$DEFAULT_BASE_URL/sales_knowledge"
        val request = Request.Builder()
            .url(url)
            .get()
            .build()

        val list = mutableListOf<SalesKnowledgeItem>()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    if (bodyStr.isNotBlank()) {
                        val array = JSONArray(bodyStr)
                        for (i in 0 until array.length()) {
                            val obj = array.optJSONObject(i) ?: continue
                            val id = obj.optString("id")
                            val content = obj.optString("winningContent")
                            if (id.isNotBlank() && content.isNotBlank()) {
                                list.add(
                                    SalesKnowledgeItem(
                                        id = id,
                                        category = obj.optString("category", "GENERAL_WINNING"),
                                        triggerContext = obj.optString("triggerContext", ""),
                                        winningContent = content,
                                        successScore = obj.optInt("successScore", 1),
                                        usageCount = obj.optInt("usageCount", 1),
                                        isQuestion = obj.optBoolean("isQuestion", false),
                                        createdAt = obj.optLong("createdAt", System.currentTimeMillis())
                                    )
                                )
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error fetching sales knowledge from cloud: ${e.message}")
        }
        list
    }

    suspend fun fetchAllUsersFromCloud(): List<UserAccount> = withContext(Dispatchers.IO) {
        val url = "$DEFAULT_BASE_URL/users"
        val request = Request.Builder().url(url).get().build()
        val list = mutableListOf<UserAccount>()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    if (bodyStr.isNotBlank()) {
                        val array = JSONArray(bodyStr)
                        for (i in 0 until array.length()) {
                            val obj = array.optJSONObject(i) ?: continue
                            val username = obj.optString("username").trim()
                            if (username.isBlank()) continue
                            list.add(
                                UserAccount(
                                    id = obj.optString("id", "user-${System.currentTimeMillis()}-$i"),
                                    username = username,
                                    role = obj.optString("role", "OPERATOR"),
                                    token = obj.optString("token", ""),
                                    validFromMs = obj.optLong("validFromMs", System.currentTimeMillis()),
                                    expiresAtMs = obj.optLong("expiresAtMs", System.currentTimeMillis() + 86400000L * 15),
                                    daysGranted = obj.optInt("daysGranted", 15),
                                    isBlocked = obj.optBoolean("isBlocked", false),
                                    canAccessLegacyModules = obj.optBoolean("canAccessLegacyModules", false),
                                    notes = obj.optString("notes", "")
                                )
                            )
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error fetching all users from cloud: ${e.message}")
        }
        list
    }

    suspend fun deleteUserFromCloud(username: String): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = "$DEFAULT_BASE_URL/users/delete"
        val json = JSONObject().apply {
            put("username", username.trim().lowercase())
        }
        val request = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    suspend fun fetchRoleMatrixFromCloud(): RoleVisibilityConfig? = withContext(Dispatchers.IO) {
        val url = "$DEFAULT_BASE_URL/role_matrix"
        val request = Request.Builder().url(url).get().build()
        try {
            client.newCall(request).execute().use { response ->
                if (response.isSuccessful) {
                    val bodyStr = response.body?.string().orEmpty()
                    if (bodyStr.isNotBlank()) {
                        val obj = JSONObject(bodyStr)
                        return@withContext RoleVisibilityConfig(
                            opPsaEnabled = obj.optBoolean("opPsaEnabled", true),
                            opScriptsEnabled = obj.optBoolean("opScriptsEnabled", false),
                            opAutomationEnabled = obj.optBoolean("opAutomationEnabled", false),
                            opDashboardEnabled = obj.optBoolean("opDashboardEnabled", false),
                            opAiSalesEnabled = obj.optBoolean("opAiSalesEnabled", false),
                            opPricingEditEnabled = obj.optBoolean("opPricingEditEnabled", false),
                            suPsaEnabled = obj.optBoolean("suPsaEnabled", true),
                            suScriptsEnabled = obj.optBoolean("suScriptsEnabled", true),
                            suAutomationEnabled = obj.optBoolean("suAutomationEnabled", true),
                            suDashboardEnabled = obj.optBoolean("suDashboardEnabled", true),
                            suSuperuserEnabled = obj.optBoolean("suSuperuserEnabled", true)
                        )
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Error fetching role matrix: ${e.message}")
        }
        null
    }

    suspend fun pushRoleMatrixToCloud(config: RoleVisibilityConfig): Result<Boolean> = withContext(Dispatchers.IO) {
        val url = "$DEFAULT_BASE_URL/role_matrix"
        val json = JSONObject().apply {
            put("opPsaEnabled", config.opPsaEnabled)
            put("opScriptsEnabled", config.opScriptsEnabled)
            put("opAutomationEnabled", config.opAutomationEnabled)
            put("opDashboardEnabled", config.opDashboardEnabled)
            put("opAiSalesEnabled", config.opAiSalesEnabled)
            put("opPricingEditEnabled", config.opPricingEditEnabled)
            put("suPsaEnabled", config.suPsaEnabled)
            put("suScriptsEnabled", config.suScriptsEnabled)
            put("suAutomationEnabled", config.suAutomationEnabled)
            put("suDashboardEnabled", config.suDashboardEnabled)
            put("suSuperuserEnabled", config.suSuperuserEnabled)
        }
        val request = Request.Builder()
            .url(url)
            .post(json.toString().toRequestBody(jsonMediaType))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                Result.success(response.isSuccessful)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
