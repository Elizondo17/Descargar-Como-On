package com.example.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.R

class NotificationHelper(private val context: Context) {

    companion object {
        const val CHANNEL_ID = "ezo_crm_automation"
        const val CHANNEL_NAME = "EzoCRM Automatizaciones"
        const val CHANNEL_DESC = "Notificaciones en tiempo real de envíos, retrasos y fallos"

        const val CHANNEL_TEAM_UPDATES = "como_on_team_updates"
        const val CHANNEL_ANNOUNCEMENTS = "como_on_team_announcements"
        const val CHANNEL_CALL_REMINDERS = "como_on_call_reminders"
    }

    init {
        createNotificationChannels()
    }

    private fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager ?: return

            val defaultChannel = NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = CHANNEL_DESC
                enableVibration(true)
            }

            val updatesChannel = NotificationChannel(
                CHANNEL_TEAM_UPDATES,
                "Actualizaciones Como On",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Alertas cuando el Administrador publica una nueva versión"
                enableVibration(true)
            }

            val announcementsChannel = NotificationChannel(
                CHANNEL_ANNOUNCEMENTS,
                "Avisos y Equipo PSA",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos del Administrador y mensajes de aliento de tu líder de equipo"
                enableVibration(true)
            }

            val remindersChannel = NotificationChannel(
                CHANNEL_CALL_REMINDERS,
                "Recordatorios de Llamadas",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Avisos programados para realizar bloques de llamadas o contactar en horario pico"
                enableVibration(true)
            }

            manager.createNotificationChannels(
                listOf(defaultChannel, updatesChannel, announcementsChannel, remindersChannel)
            )
        }
    }

    fun showSuccessNotification(recipient: String, delaySeconds: Int, snippet: String) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("✅ Mensaje entregado a $recipient")
            .setContentText("Pausa anti-spam: ${delaySeconds}s • \"$snippet\"")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Entregado con éxito a $recipient.\nTiempo entre mensajes: ${delaySeconds}s\nContenido: $snippet"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(recipient.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission might be pending
        }
    }

    fun showFailureNotification(recipient: String, reason: String) {
        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("⚠️ Fallo en envío a $recipient")
            .setContentText("Motivo: $reason")
            .setStyle(NotificationCompat.BigTextStyle().bigText("Error al enviar a $recipient.\nMotivo: $reason"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(recipient.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission might be pending
        }
    }

    fun showCampaignSummaryNotification(totalSent: Int, totalFailed: Int) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            1001,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val summaryText = if (totalFailed == 0) {
            "Se enviaron los $totalSent mensajes programados con éxito."
        } else {
            "Enviados: $totalSent con éxito • Omitidos/reintentar: $totalFailed."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("🎉 Campaña Finalizada")
            .setContentText(summaryText)
            .setStyle(NotificationCompat.BigTextStyle().bigText("Campaña de envío en Como On concluida.\n$summaryText"))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(88888, notification)
        } catch (_: SecurityException) {
            // Permission might be pending
        }
    }

    fun showPostSaleReplacementNotification(
        recordId: String,
        customerName: String,
        purifierName: String,
        daysRemaining: Int,
        isOverdue: Boolean
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_PSA_POST_SALE", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            recordId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val title = if (isOverdue) {
            "🔧 Recambio PSA vencido: $customerName"
        } else {
            "🔔 Recambio PSA en $daysRemaining días: $customerName"
        }
        val body = if (isOverdue) {
            "$purifierName cumplió su ciclo de filtro. Toca para coordinar el recambio por WhatsApp."
        } else {
            "$purifierName vence pronto (faltan $daysRemaining días). Agenda el servicio posventa."
        }

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                ("postsale_$recordId").hashCode(),
                notification
            )
        } catch (_: SecurityException) {
            // Permission not granted yet
        }
    }

    fun showTeamNotificationAlert(
        notifId: String,
        title: String,
        body: String,
        isAppUpdate: Boolean = false,
        senderName: String = ""
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_TEAM_NOTIF_ID", notifId)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            ("team_notif_$notifId").hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val channelId = if (isAppUpdate) CHANNEL_TEAM_UPDATES else CHANNEL_ANNOUNCEMENTS
        val displayTitle = if (senderName.isNotBlank() && !isAppUpdate) {
            "$title • $senderName"
        } else {
            title
        }

        val notification = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(displayTitle)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                ("team_notif_$notifId").hashCode(),
                notification
            )
        } catch (_: SecurityException) {
            // Permission not granted yet
        }
    }

    fun showCallReminderNotification(
        reminderId: String,
        title: String,
        body: String
    ) {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("OPEN_PSA_CONTACTS", true)
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            ("call_rem_$reminderId").hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_CALL_REMINDERS)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title.ifBlank { "📞 Momento de Llamadas PSA" })
            .setContentText(body.ifBlank { "Es tu horario programado para contactar clientes y agendar demos." })
            .setStyle(
                NotificationCompat.BigTextStyle().bigText(
                    body.ifBlank { "Es tu horario programado para contactar clientes y agendar demos." }
                )
            )
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        try {
            NotificationManagerCompat.from(context).notify(
                ("call_rem_$reminderId").hashCode(),
                notification
            )
        } catch (_: SecurityException) {
            // Permission not granted yet
        }
    }

    fun cancelAllNotifications() {
        try {
            val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            manager?.cancelAll()
            NotificationManagerCompat.from(context).cancelAll()
        } catch (_: Exception) {
            // Safe fallback
        }
    }
}
