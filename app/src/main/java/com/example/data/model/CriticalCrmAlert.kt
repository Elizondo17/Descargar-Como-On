package com.example.data.model

import java.util.UUID

enum class AlertSeverity {
    CRITICAL,      // Alerta roja: Fallo crítico / Detención inmediata
    MAINTENANCE,   // Alerta ámbar: Ventana de mantenimiento programada
    ANNOUNCEMENT   // Alerta índigo: Actualización o aviso general
}

data class CriticalCrmAlert(
    val id: String = UUID.randomUUID().toString(),
    val title: String,
    val body: String,
    val severity: AlertSeverity = AlertSeverity.CRITICAL,
    val requireAutomationPause: Boolean = false,
    val timestamp: Long = System.currentTimeMillis(),
    val senderAdmin: String = "Superusuario",
    val topic: String = TOPIC_CRITICAL_ALERTS
) {
    companion object {
        const val TOPIC_CRITICAL_ALERTS = "crm_critical_alerts"
        const val CHANNEL_ID = "crm_critical_alerts_channel"
        const val CHANNEL_NAME = "Alertas Críticas CRM"
    }
}
