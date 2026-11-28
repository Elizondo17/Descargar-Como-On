package com.example.util

import android.content.Context
import android.content.Intent
import android.util.Log
import com.example.data.model.CriticalCrmAlert
import com.example.data.model.TeamNotification
import com.example.data.model.TeamNotificationPriority
import com.example.data.model.TeamNotificationTargetMode
import com.example.data.model.TeamNotificationType
import com.google.firebase.messaging.FirebaseMessaging
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

sealed class FcmRoutedMessage {
    data class Critical(
        val id: String,
        val title: String,
        val body: String,
        val severityName: String,
        val requirePause: Boolean,
        val sender: String
    ) : FcmRoutedMessage()

    data class Personalized(
        val id: String,
        val type: TeamNotificationType,
        val priority: TeamNotificationPriority,
        val title: String,
        val body: String,
        val sender: String,
        val actionUrl: String
    ) : FcmRoutedMessage()
}

data class FcmChannelSettings(
    val notifyCatalogUpdates: Boolean = true,
    val notifyAssignedContacts: Boolean = true
)

object FcmPayloadRouter {
    fun classifyAndCheck(
        data: Map<String, String>,
        fallbackTitle: String?,
        fallbackBody: String?,
        catalogEnabled: Boolean,
        assignedContactsEnabled: Boolean
    ): FcmRoutedMessage? {
        val rawCategory = data["category"]
            ?: data["type"]
            ?: data["notification_type"]
            ?: ""
        val normalizedCategory = rawCategory.trim().uppercase()
        val id = data["id"]?.takeIf { it.isNotBlank() }
            ?: data["notifId"]?.takeIf { it.isNotBlank() }
            ?: "fcm_${System.currentTimeMillis()}"
        val sender = data["sender"]
            ?: data["senderName"]
            ?: "Superusuario"
        val actionUrl = data["actionUrl"].orEmpty()

        val isExplicitCritical = normalizedCategory == "CRITICAL_ALERT" ||
            normalizedCategory == "CRITICAL" ||
            (normalizedCategory.isBlank() && !data["severity"].isNullOrBlank()) ||
            data["require_pause"]?.equals("true", ignoreCase = true) == true

        if (isExplicitCritical) {
            return FcmRoutedMessage.Critical(
                id = id,
                title = data["title"] ?: fallbackTitle ?: "Aviso Crítico de Como On",
                body = data["body"] ?: fallbackBody ?: "Se ha emitido una alerta operativa desde el panel de superusuario.",
                severityName = data["severity"] ?: "CRITICAL",
                requirePause = data["require_pause"]?.toBooleanStrictOrNull() ?: true,
                sender = sender
            )
        }

        val resolvedType = when (normalizedCategory) {
            "CATALOG", "CATALOG_UPDATE", "PRICING_UPDATE" -> TeamNotificationType.CATALOG_UPDATE
            "ASSIGNED_CONTACT", "NEW_CONTACT", "LEAD_ASSIGNED" -> TeamNotificationType.ASSIGNED_CONTACT
            "APP_UPDATE" -> TeamNotificationType.APP_UPDATE
            "SUPERUSER_DIRECT", "DIRECT", "PERSONALIZED" -> TeamNotificationType.SUPERUSER_DIRECT
            "LEADER_COACHING", "COACHING" -> TeamNotificationType.LEADER_COACHING
            else -> TeamNotificationType.SUPERUSER_GLOBAL
        }

        if (resolvedType == TeamNotificationType.CATALOG_UPDATE && !catalogEnabled) {
            return null
        }
        if (resolvedType == TeamNotificationType.ASSIGNED_CONTACT && !assignedContactsEnabled) {
            return null
        }

        val priority = try {
            val rawPrio = data["priority"]?.trim()?.uppercase().orEmpty()
            if (rawPrio.isNotBlank()) TeamNotificationPriority.valueOf(rawPrio) else TeamNotificationPriority.IMPORTANT
        } catch (_: Exception) {
            TeamNotificationPriority.IMPORTANT
        }

        val defaultTitle = when (resolvedType) {
            TeamNotificationType.CATALOG_UPDATE -> "Actualización de Catálogo PSA"
            TeamNotificationType.ASSIGNED_CONTACT -> "Nuevo Contacto Asignado"
            TeamNotificationType.APP_UPDATE -> "Nueva Versión Disponible"
            TeamNotificationType.LEADER_COACHING -> "Mensaje de tu Líder"
            TeamNotificationType.SUPERUSER_DIRECT, TeamNotificationType.SUPERUSER_GLOBAL -> "Aviso Personalizado PSA"
        }

        return FcmRoutedMessage.Personalized(
            id = id,
            type = resolvedType,
            priority = priority,
            title = data["title"]?.takeIf { it.isNotBlank() } ?: fallbackTitle ?: defaultTitle,
            body = data["body"]?.takeIf { it.isNotBlank() } ?: fallbackBody ?: "Abre la app para revisar el aviso completo.",
            sender = sender,
            actionUrl = actionUrl
        )
    }

    fun toBannerNotification(
        routed: FcmRoutedMessage,
        createdAtMs: Long = System.currentTimeMillis()
    ): TeamNotification {
        return when (routed) {
            is FcmRoutedMessage.Critical -> TeamNotification(
                id = routed.id,
                type = TeamNotificationType.SUPERUSER_GLOBAL.name,
                priority = TeamNotificationPriority.URGENT.name,
                targetMode = TeamNotificationTargetMode.ALL.name,
                senderName = routed.sender.ifBlank { "Superusuario" },
                title = routed.title.ifBlank { "Aviso Crítico PSA" },
                body = routed.body,
                createdAtMs = createdAtMs
            )
            is FcmRoutedMessage.Personalized -> TeamNotification(
                id = routed.id,
                type = routed.type.name,
                priority = routed.priority.name,
                targetMode = TeamNotificationTargetMode.ALL.name,
                senderName = routed.sender.ifBlank { "Superusuario" },
                title = routed.title,
                body = routed.body,
                actionUrl = routed.actionUrl,
                createdAtMs = createdAtMs
            )
        }
    }

    fun extractBannerFromTapIntent(
        intent: Intent?,
        catalogEnabled: Boolean,
        assignedContactsEnabled: Boolean
    ): TeamNotification? {
        if (intent == null) return null

        val action = intent.action.orEmpty()
        val openedFromExplicitFlag =
            intent.getBooleanExtra(FcmNotificationPreferences.EXTRA_OPEN_FROM_FCM_BANNER, false) ||
                intent.getBooleanExtra(FcmNotificationPreferences.EXTRA_OPEN_MAIN_SCREEN, false) ||
                action == FcmNotificationPreferences.ACTION_OPEN_MAIN_BANNER ||
                action == FcmNotificationPreferences.ACTION_OPEN_MAIN_SCREEN

        val explicitNotifId = intent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_NOTIF_ID)
            ?.takeIf { it.isNotBlank() }
            ?: intent.getStringExtra("OPEN_TEAM_NOTIF_ID")?.takeIf { it.isNotBlank() }

        val alertId = intent.getStringExtra("EXTRA_ALERT_ID")?.takeIf { it.isNotBlank() }
        if (alertId != null) {
            val severity = intent.getStringExtra("EXTRA_ALERT_SEVERITY").orEmpty()
            val mappedPriority = if (severity.equals("ANNOUNCEMENT", ignoreCase = true)) {
                TeamNotificationPriority.IMPORTANT
            } else {
                TeamNotificationPriority.URGENT
            }
            return TeamNotification(
                id = alertId,
                type = TeamNotificationType.SUPERUSER_GLOBAL.name,
                priority = mappedPriority.name,
                targetMode = TeamNotificationTargetMode.ALL.name,
                senderName = intent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_SENDER)
                    ?.takeIf { it.isNotBlank() }
                    ?: "Superusuario",
                title = intent.getStringExtra("EXTRA_ALERT_TITLE")
                    ?.takeIf { it.isNotBlank() }
                    ?: intent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_TITLE)
                    ?: "Aviso Crítico",
                body = intent.getStringExtra("EXTRA_ALERT_BODY")
                    ?.takeIf { it.isNotBlank() }
                    ?: intent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_BODY)
                    ?: "",
                createdAtMs = System.currentTimeMillis()
            )
        }

        if (openedFromExplicitFlag || explicitNotifId != null) {
            val hasExplicitTitle = !intent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_TITLE).isNullOrBlank()
            val hasExplicitBody = !intent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_BODY).isNullOrBlank()
            if (hasExplicitTitle || hasExplicitBody || explicitNotifId != null) {
                return TeamNotification(
                    id = explicitNotifId
                        ?: intent.getStringExtra("id")?.takeIf { it.isNotBlank() }
                        ?: intent.getStringExtra("google.message_id")?.takeIf { it.isNotBlank() }
                        ?: "fcm_tap_${System.currentTimeMillis()}",
                    type = intent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_TYPE)
                        ?.takeIf { it.isNotBlank() }
                        ?: intent.getStringExtra("category")?.takeIf { it.isNotBlank() }
                        ?: TeamNotificationType.SUPERUSER_GLOBAL.name,
                    priority = intent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_PRIORITY)
                        ?.takeIf { it.isNotBlank() }
                        ?: intent.getStringExtra("priority")?.takeIf { it.isNotBlank() }
                        ?: TeamNotificationPriority.IMPORTANT.name,
                    targetMode = TeamNotificationTargetMode.ALL.name,
                    senderName = intent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_SENDER)
                        ?.takeIf { it.isNotBlank() }
                        ?: intent.getStringExtra("sender")?.takeIf { it.isNotBlank() }
                        ?: "Superusuario",
                    title = intent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_TITLE)
                        ?.takeIf { it.isNotBlank() }
                        ?: intent.getStringExtra("title")?.takeIf { it.isNotBlank() }
                        ?: "Aviso Importante PSA",
                    body = intent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_BODY)
                        ?.takeIf { it.isNotBlank() }
                        ?: intent.getStringExtra("body")?.takeIf { it.isNotBlank() }
                        ?: "Tienes una actualización reciente disponible.",
                    actionUrl = intent.getStringExtra(FcmNotificationPreferences.EXTRA_FCM_ACTION_URL)
                        ?.takeIf { it.isNotBlank() }
                        ?: intent.getStringExtra("actionUrl").orEmpty(),
                    createdAtMs = System.currentTimeMillis()
                )
            }
        }

        // Background FCM notification-tray tap puts FCM `data` map directly as String extras on launch Intent
        val hasFcmDataExtras = !intent.getStringExtra("google.message_id").isNullOrBlank() ||
            !intent.getStringExtra("notifId").isNullOrBlank() ||
            !intent.getStringExtra("category").isNullOrBlank() ||
            (!intent.getStringExtra("id").isNullOrBlank() && !intent.getStringExtra("title").isNullOrBlank())

        if (!hasFcmDataExtras) return null

        val dataMap = buildMap<String, String> {
            listOf(
                "id", "notifId", "google.message_id", "category", "type", "notification_type",
                "priority", "title", "body", "sender", "senderName", "actionUrl", "severity", "require_pause"
            ).forEach { key ->
                intent.getStringExtra(key)?.let { value ->
                    if (value.isNotBlank()) put(key, value)
                }
            }
            if (!containsKey("id") && containsKey("google.message_id")) {
                put("id", getValue("google.message_id"))
            }
        }

        val routed = classifyAndCheck(
            data = dataMap,
            fallbackTitle = dataMap["title"],
            fallbackBody = dataMap["body"],
            catalogEnabled = catalogEnabled,
            assignedContactsEnabled = assignedContactsEnabled
        ) ?: return null

        return toBannerNotification(routed)
    }
}

object FcmNotificationPreferences {
    private const val TAG = "FcmNotifPrefs"
    private const val PREFS_NAME = "psa_fcm_preferences"
    private const val KEY_NOTIFY_CATALOG = "notify_catalog_updates"
    private const val KEY_NOTIFY_CONTACTS = "notify_assigned_contacts"
    private const val KEY_FCM_TOKEN = "cached_fcm_token"
    private const val KEY_FCM_TOKEN_UPDATED_AT = "cached_fcm_token_updated_at"

    private const val KEY_PERSISTENT_BANNER_ID = "persistent_fcm_banner_id"
    private const val KEY_PERSISTENT_BANNER_TYPE = "persistent_fcm_banner_type"
    private const val KEY_PERSISTENT_BANNER_PRIORITY = "persistent_fcm_banner_priority"
    private const val KEY_PERSISTENT_BANNER_SENDER = "persistent_fcm_banner_sender"
    private const val KEY_PERSISTENT_BANNER_TITLE = "persistent_fcm_banner_title"
    private const val KEY_PERSISTENT_BANNER_BODY = "persistent_fcm_banner_body"
    private const val KEY_PERSISTENT_BANNER_ACTION_URL = "persistent_fcm_banner_action_url"
    private const val KEY_PERSISTENT_BANNER_CREATED_AT = "persistent_fcm_banner_created_at"

    const val TOPIC_CATALOG_UPDATES = "psa_catalog_updates"
    const val TOPIC_ASSIGNED_CONTACTS = "psa_assigned_contacts"
    const val TOPIC_PERSONALIZED_ALERTS = "psa_personalized_alerts"

    const val ACTION_OPEN_MAIN_BANNER = "OPEN_MAIN_BANNER"
    const val ACTION_OPEN_MAIN_SCREEN = "OPEN_MAIN_SCREEN"

    const val EXTRA_OPEN_FROM_FCM_BANNER = "OPEN_FROM_FCM_BANNER"
    const val EXTRA_OPEN_MAIN_SCREEN = "OPEN_MAIN_HOME_SCREEN"
    const val EXTRA_FCM_SYSTEM_NOTIFICATION_ID = "FCM_SYSTEM_NOTIFICATION_ID"
    const val EXTRA_FCM_NOTIF_ID = "FCM_NOTIF_ID"
    const val EXTRA_FCM_TITLE = "FCM_BANNER_TITLE"
    const val EXTRA_FCM_BODY = "FCM_BANNER_BODY"
    const val EXTRA_FCM_TYPE = "FCM_BANNER_TYPE"
    const val EXTRA_FCM_SENDER = "FCM_BANNER_SENDER"
    const val EXTRA_FCM_PRIORITY = "FCM_BANNER_PRIORITY"
    const val EXTRA_FCM_ACTION_URL = "FCM_BANNER_ACTION_URL"

    private val _persistentFcmBanner = MutableStateFlow<TeamNotification?>(null)
    val persistentFcmBanner: StateFlow<TeamNotification?> = _persistentFcmBanner.asStateFlow()

    fun ensurePersistentBannerLoaded(context: Context): TeamNotification? {
        val loaded = getPersistentFcmBanner(context)
        if (_persistentFcmBanner.value != loaded) {
            _persistentFcmBanner.value = loaded
        }
        return loaded
    }

    fun savePersistentFcmBanner(context: Context, notification: TeamNotification) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_PERSISTENT_BANNER_ID, notification.id)
            .putString(KEY_PERSISTENT_BANNER_TYPE, notification.type)
            .putString(KEY_PERSISTENT_BANNER_PRIORITY, notification.priority)
            .putString(KEY_PERSISTENT_BANNER_SENDER, notification.senderName)
            .putString(KEY_PERSISTENT_BANNER_TITLE, notification.title)
            .putString(KEY_PERSISTENT_BANNER_BODY, notification.body)
            .putString(KEY_PERSISTENT_BANNER_ACTION_URL, notification.actionUrl)
            .putLong(KEY_PERSISTENT_BANNER_CREATED_AT, notification.createdAtMs)
            .commit()
        _persistentFcmBanner.value = notification
    }

    fun getPersistentFcmBanner(context: Context): TeamNotification? {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val id = prefs.getString(KEY_PERSISTENT_BANNER_ID, null)?.takeIf { it.isNotBlank() } ?: return null
        return TeamNotification(
            id = id,
            type = prefs.getString(KEY_PERSISTENT_BANNER_TYPE, TeamNotificationType.SUPERUSER_GLOBAL.name)
                ?: TeamNotificationType.SUPERUSER_GLOBAL.name,
            priority = prefs.getString(KEY_PERSISTENT_BANNER_PRIORITY, TeamNotificationPriority.IMPORTANT.name)
                ?: TeamNotificationPriority.IMPORTANT.name,
            targetMode = TeamNotificationTargetMode.ALL.name,
            senderName = prefs.getString(KEY_PERSISTENT_BANNER_SENDER, "Superusuario") ?: "Superusuario",
            title = prefs.getString(KEY_PERSISTENT_BANNER_TITLE, "").orEmpty(),
            body = prefs.getString(KEY_PERSISTENT_BANNER_BODY, "").orEmpty(),
            actionUrl = prefs.getString(KEY_PERSISTENT_BANNER_ACTION_URL, "").orEmpty(),
            createdAtMs = prefs.getLong(KEY_PERSISTENT_BANNER_CREATED_AT, 0L)
        )
    }

    fun clearPersistentFcmBanner(context: Context, notifId: String? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val currentId = prefs.getString(KEY_PERSISTENT_BANNER_ID, null)
        if (notifId == null || currentId == notifId) {
            prefs.edit()
                .remove(KEY_PERSISTENT_BANNER_ID)
                .remove(KEY_PERSISTENT_BANNER_TYPE)
                .remove(KEY_PERSISTENT_BANNER_PRIORITY)
                .remove(KEY_PERSISTENT_BANNER_SENDER)
                .remove(KEY_PERSISTENT_BANNER_TITLE)
                .remove(KEY_PERSISTENT_BANNER_BODY)
                .remove(KEY_PERSISTENT_BANNER_ACTION_URL)
                .remove(KEY_PERSISTENT_BANNER_CREATED_AT)
                .commit()
            _persistentFcmBanner.value = null
        }
    }

    fun getSettings(context: Context): FcmChannelSettings {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        return FcmChannelSettings(
            notifyCatalogUpdates = prefs.getBoolean(KEY_NOTIFY_CATALOG, true),
            notifyAssignedContacts = prefs.getBoolean(KEY_NOTIFY_CONTACTS, true)
        )
    }

    fun isCatalogUpdatesEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_NOTIFY_CATALOG, true)

    fun setCatalogUpdatesEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFY_CATALOG, enabled)
            .apply()
    }

    fun isAssignedContactsEnabled(context: Context): Boolean =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getBoolean(KEY_NOTIFY_CONTACTS, true)

    fun setAssignedContactsEnabled(context: Context, enabled: Boolean) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putBoolean(KEY_NOTIFY_CONTACTS, enabled)
            .apply()
    }

    fun getCachedToken(context: Context): String =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getString(KEY_FCM_TOKEN, "").orEmpty()

    fun getCachedTokenUpdatedAtMs(context: Context): Long =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE).getLong(KEY_FCM_TOKEN_UPDATED_AT, 0L)

    fun saveCachedToken(context: Context, token: String, updatedAtMs: Long = System.currentTimeMillis()) {
        if (token.isBlank()) return
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_FCM_TOKEN, token.trim())
            .putLong(KEY_FCM_TOKEN_UPDATED_AT, updatedAtMs)
            .apply()
    }

    fun syncTopicSubscriptions(context: Context) {
        val settings = getSettings(context)
        runCatching {
            val messaging = FirebaseMessaging.getInstance()
            messaging.subscribeToTopic(CriticalCrmAlert.TOPIC_CRITICAL_ALERTS)
            messaging.subscribeToTopic(TOPIC_PERSONALIZED_ALERTS)
            if (settings.notifyCatalogUpdates) {
                messaging.subscribeToTopic(TOPIC_CATALOG_UPDATES)
            } else {
                messaging.unsubscribeFromTopic(TOPIC_CATALOG_UPDATES)
            }
            if (settings.notifyAssignedContacts) {
                messaging.subscribeToTopic(TOPIC_ASSIGNED_CONTACTS)
            } else {
                messaging.unsubscribeFromTopic(TOPIC_ASSIGNED_CONTACTS)
            }
        }.onFailure { err ->
            Log.d(TAG, "FCM topic sync skipped: ${err.message}")
        }
    }

    fun fetchCurrentFcmToken(context: Context, onResult: (String) -> Unit) {
        runCatching {
            FirebaseMessaging.getInstance().token
                .addOnSuccessListener { token ->
                    if (!token.isNullOrBlank()) {
                        saveCachedToken(context, token)
                        onResult(token)
                    }
                }
                .addOnFailureListener { err ->
                    Log.d(TAG, "FCM token fetch failed: ${err.message}")
                }
        }.onFailure { err ->
            Log.d(TAG, "FCM unavailable: ${err.message}")
        }
    }
}
