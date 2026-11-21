package com.example.data.repository

import android.content.Context
import com.example.data.model.AlertSeverity
import com.example.data.model.CriticalCrmAlert
import com.example.util.CriticalNotificationHelper
import com.example.util.UnattendedAutomationLogger
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.UUID

class FcmBroadcastRepository private constructor() {

    private val _broadcastHistory = MutableStateFlow<List<CriticalCrmAlert>>(emptyList())
    val broadcastHistory: StateFlow<List<CriticalCrmAlert>> = _broadcastHistory.asStateFlow()

    fun broadcastAlert(
        context: Context,
        title: String,
        body: String,
        severity: AlertSeverity,
        requirePause: Boolean,
        adminName: String = "Superusuario"
    ): Result<CriticalCrmAlert> {
        return try {
            val alert = CriticalCrmAlert(
                id = UUID.randomUUID().toString(),
                title = title.trim(),
                body = body.trim(),
                severity = severity,
                requireAutomationPause = requirePause,
                timestamp = System.currentTimeMillis(),
                senderAdmin = adminName,
                topic = CriticalCrmAlert.TOPIC_CRITICAL_ALERTS
            )

            // 1. Transmitir en la nube a todos los dispositivos de operadores
            kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO).launch {
                try {
                    CloudSyncRepository.getInstance(context).broadcastAlertToCloud(alert)
                } catch (e: Exception) {
                    e.printStackTrace()
                }
            }

            // Mostrar notificación Heads-Up local
            CriticalNotificationHelper.showNotification(context, alert)

            // 2. Registrar en historial en memoria para auditoría del superusuario
            _broadcastHistory.value = listOf(alert) + _broadcastHistory.value

            // 3. Registrar en archivo de logs desatendidos
            UnattendedAutomationLogger.logEventSync(
                context = context,
                tag = "FCM_DISPATCH",
                recipient = "ALL_DEVICES",
                status = "DIFUSION_PUSH_EXITOSA [${severity.name}]",
                exitedChatSuccessfully = true,
                details = "Título: '${alert.title}' | Topic: ${alert.topic} | Requiere pausa: $requirePause"
            )

            Result.success(alert)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    companion object {
        @Volatile
        private var instance: FcmBroadcastRepository? = null

        fun getInstance(): FcmBroadcastRepository {
            return instance ?: synchronized(this) {
                instance ?: FcmBroadcastRepository().also { instance = it }
            }
        }
    }
}
