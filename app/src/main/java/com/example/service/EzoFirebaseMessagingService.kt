package com.example.service

import android.util.Log
import com.example.data.model.AlertSeverity
import com.example.data.model.CriticalCrmAlert
import com.example.data.repository.FirestoreTeamRepository
import com.example.util.CriticalNotificationHelper
import com.example.util.FcmNotificationPreferences
import com.example.util.FcmPayloadRouter
import com.example.util.FcmRoutedMessage
import com.example.util.UnattendedAutomationLogger
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class EzoFirebaseMessagingService : FirebaseMessagingService() {

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "Nuevo token FCM recibido")
        FcmNotificationPreferences.saveCachedToken(applicationContext, token)
        FcmNotificationPreferences.syncTopicSubscriptions(applicationContext)

        serviceScope.launch {
            runCatching {
                FirestoreTeamRepository(applicationContext).syncOwnFcmSettingsIfSignedIn(token = token)
            }.onFailure { err ->
                Log.d(TAG, "Sync diferido de token FCM: ${err.message}")
            }
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "Mensaje FCM recibido de: ${remoteMessage.from}")

        val settings = FcmNotificationPreferences.getSettings(applicationContext)
        val routed = FcmPayloadRouter.classifyAndCheck(
            data = remoteMessage.data,
            fallbackTitle = remoteMessage.notification?.title,
            fallbackBody = remoteMessage.notification?.body,
            catalogEnabled = settings.notifyCatalogUpdates,
            assignedContactsEnabled = settings.notifyAssignedContacts
        )

        if (routed == null) {
            Log.i(TAG, "Notificación FCM silenciada por preferencias del vendedor.")
            return
        }

        val persistentBanner = FcmPayloadRouter.toBannerNotification(routed)
        FcmNotificationPreferences.savePersistentFcmBanner(applicationContext, persistentBanner)

        when (routed) {
            is FcmRoutedMessage.Critical -> {
                val severity = try {
                    AlertSeverity.valueOf(routed.severityName.uppercase())
                } catch (_: Exception) {
                    AlertSeverity.CRITICAL
                }
                val alert = CriticalCrmAlert(
                    id = routed.id,
                    title = routed.title,
                    body = routed.body,
                    severity = severity,
                    requireAutomationPause = routed.requirePause,
                    timestamp = System.currentTimeMillis(),
                    senderAdmin = routed.sender
                )
                UnattendedAutomationLogger.logEventSync(
                    context = applicationContext,
                    tag = "FCM-BROADCAST",
                    recipient = "LOCAL_DEVICE",
                    status = "PUSH_RECIBIDA: [${alert.severity}] ${alert.title}",
                    exitedChatSuccessfully = true,
                    details = "Requiere pausa: ${routed.requirePause} | Emisor: ${routed.sender}"
                )
                CriticalNotificationHelper.showNotification(applicationContext, alert)
            }
            is FcmRoutedMessage.Personalized -> {
                NotificationHelper(applicationContext).showFcmPersonalizedNotification(
                    notifId = routed.id,
                    title = routed.title,
                    body = routed.body,
                    type = routed.type,
                    priority = routed.priority,
                    senderName = routed.sender,
                    actionUrl = routed.actionUrl
                )
            }
        }
    }

    companion object {
        private const val TAG = "EzoFirebaseMessaging"

        fun subscribeToCriticalAlertsTopic(context: android.content.Context? = null) {
            if (context != null) {
                FcmNotificationPreferences.syncTopicSubscriptions(context)
            } else {
                runCatching {
                    com.google.firebase.messaging.FirebaseMessaging.getInstance()
                        .subscribeToTopic(CriticalCrmAlert.TOPIC_CRITICAL_ALERTS)
                }
            }
        }
    }
}
