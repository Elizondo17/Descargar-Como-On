package com.example.service

import android.util.Log
import com.example.data.model.AlertSeverity
import com.example.data.model.CriticalCrmAlert
import com.example.util.CriticalNotificationHelper
import com.example.util.UnattendedAutomationLogger
import com.google.firebase.messaging.FirebaseMessaging
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import java.util.UUID

class EzoFirebaseMessagingService : FirebaseMessagingService() {

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        Log.i(TAG, "Nuevo token FCM recibido: $token")
        
        // Auto-suscribir inmediatamente al tema de alertas críticas
        try {
            FirebaseMessaging.getInstance().subscribeToTopic(CriticalCrmAlert.TOPIC_CRITICAL_ALERTS)
                .addOnCompleteListener { task ->
                    if (task.isSuccessful) {
                        Log.i(TAG, "Suscripción exitosa a topic: ${CriticalCrmAlert.TOPIC_CRITICAL_ALERTS}")
                    } else {
                        Log.w(TAG, "Fallo al suscribir a topic: ${task.exception?.message}")
                    }
                }
        } catch (e: Exception) {
            Log.e(TAG, "Error durante suscripción a topic", e)
        }
    }

    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        super.onMessageReceived(remoteMessage)
        Log.i(TAG, "Mensaje FCM recibido de: ${remoteMessage.from}")

        val data = remoteMessage.data
        val title = data["title"] 
            ?: remoteMessage.notification?.title 
            ?: "Aviso Crítico de EzoCRM"

        val body = data["body"] 
            ?: remoteMessage.notification?.body 
            ?: "Se ha emitido una alerta operativa desde el panel de superusuario."

        val severityStr = data["severity"] ?: "CRITICAL"
        val severity = try {
            AlertSeverity.valueOf(severityStr.uppercase())
        } catch (e: Exception) {
            AlertSeverity.CRITICAL
        }

        val requirePause = data["require_pause"]?.toBoolean() ?: (severity == AlertSeverity.CRITICAL)
        val sender = data["sender"] ?: "Superusuario"
        val alertId = data["id"] ?: UUID.randomUUID().toString()

        val alert = CriticalCrmAlert(
            id = alertId,
            title = title,
            body = body,
            severity = severity,
            requireAutomationPause = requirePause,
            timestamp = System.currentTimeMillis(),
            senderAdmin = sender
        )

        // Registrar en el log de diagnóstico desatendido
        UnattendedAutomationLogger.logEventSync(
            context = applicationContext,
            tag = "FCM-BROADCAST",
            recipient = "LOCAL_DEVICE",
            status = "PUSH_RECIBIDA: [${alert.severity}] ${alert.title}",
            exitedChatSuccessfully = true,
            details = "Requiere pausa: $requirePause | Emisor: $sender"
        )

        // Mostrar notificación Heads-Up de máxima prioridad
        CriticalNotificationHelper.showNotification(applicationContext, alert)
    }

    companion object {
        private const val TAG = "EzoFirebaseMessaging"

        fun subscribeToCriticalAlertsTopic() {
            try {
                FirebaseMessaging.getInstance().subscribeToTopic(CriticalCrmAlert.TOPIC_CRITICAL_ALERTS)
                    .addOnCompleteListener { task ->
                        if (task.isSuccessful) {
                            Log.i(TAG, "Suscrito activamente a ${CriticalCrmAlert.TOPIC_CRITICAL_ALERTS}")
                        }
                    }
            } catch (e: Exception) {
                Log.w(TAG, "No fue posible suscribir al topic: ${e.message}")
            }
        }
    }
}
