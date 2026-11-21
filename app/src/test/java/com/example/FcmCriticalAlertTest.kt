package com.example

import android.app.NotificationManager
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.AlertSeverity
import com.example.data.model.CriticalCrmAlert
import com.example.data.repository.FcmBroadcastRepository
import com.example.util.CriticalNotificationHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class FcmCriticalAlertTest {

    private lateinit var context: Context
    private lateinit var notificationManager: NotificationManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
    }

    @Test
    fun testAlertModelDefaults() {
        val alert = CriticalCrmAlert(
            title = "Mantenimiento EzoCRM",
            body = "Pausar envíos durante 1 hora",
            severity = AlertSeverity.CRITICAL,
            requireAutomationPause = true,
            senderAdmin = "Admin-payito"
        )

        assertEquals("Mantenimiento EzoCRM", alert.title)
        assertEquals(AlertSeverity.CRITICAL, alert.severity)
        assertTrue(alert.requireAutomationPause)
        assertEquals(CriticalCrmAlert.TOPIC_CRITICAL_ALERTS, alert.topic)
        assertEquals("crm_critical_alerts", alert.topic)
    }

    @Test
    fun testEnsureNotificationChannelCreated() {
        CriticalNotificationHelper.ensureNotificationChannel(context)

        val channel = notificationManager.getNotificationChannel(CriticalCrmAlert.CHANNEL_ID)
        assertNotNull("El canal de notificaciones críticas debe existir", channel)
        assertEquals(NotificationManager.IMPORTANCE_HIGH, channel.importance)
        assertEquals(CriticalCrmAlert.CHANNEL_NAME, channel.name)
        assertTrue(channel.shouldVibrate())
    }

    @Test
    fun testShowNotificationPostsToManager() {
        val alert = CriticalCrmAlert(
            title = "Fallo en Servidor",
            body = "Detener inmediatamente todos los scripts desatendidos",
            severity = AlertSeverity.CRITICAL,
            requireAutomationPause = true,
            senderAdmin = "Admin-payito"
        )

        CriticalNotificationHelper.showNotification(context, alert)

        val activeNotifications = notificationManager.activeNotifications
        assertTrue("Debe existir al menos una notificación activa", activeNotifications.isNotEmpty())
        
        val notification = activeNotifications.firstOrNull { it.id == alert.id.hashCode() }
        assertNotNull("La notificación de alerta debe coincidir en ID", notification)
    }

    @Test
    fun testBroadcastRepositoryRecordsHistory() {
        val repository = FcmBroadcastRepository.getInstance()

        val result = repository.broadcastAlert(
            context = context,
            title = "Aviso Programado",
            body = "Actualización de catálogo de ofertas",
            severity = AlertSeverity.MAINTENANCE,
            requirePause = false,
            adminName = "Admin-payito"
        )

        assertTrue(result.isSuccess)
        val history = repository.broadcastHistory.value
        assertTrue("El historial debe contener la alerta transmitida", history.isNotEmpty())
        assertEquals("Aviso Programado", history.first().title)
        assertEquals(AlertSeverity.MAINTENANCE, history.first().severity)
    }
}
