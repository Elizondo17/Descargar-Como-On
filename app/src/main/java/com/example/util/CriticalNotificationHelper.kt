package com.example.util

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Color
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.example.MainActivity
import com.example.data.model.AlertSeverity
import com.example.data.model.CriticalCrmAlert

object CriticalNotificationHelper {

    fun ensureNotificationChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CriticalCrmAlert.CHANNEL_ID,
                CriticalCrmAlert.CHANNEL_NAME,
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notificaciones push de máxima prioridad para alertas operativas y estado de EzoCRM"
                enableLights(true)
                lightColor = Color.RED
                enableVibration(true)
                vibrationPattern = longArrayOf(0, 300, 200, 300)
                lockscreenVisibility = NotificationCompat.VISIBILITY_PUBLIC
            }
            val notificationManager = context.getSystemService(Context.NOTIFICATION_SERVICE) as? NotificationManager
            notificationManager?.createNotificationChannel(channel)
        }
    }

    fun showNotification(context: Context, alert: CriticalCrmAlert) {
        ensureNotificationChannel(context)

        val systemNotificationId = alert.id.hashCode()
        val intent = Intent(context, MainActivity::class.java).apply {
            action = FcmNotificationPreferences.ACTION_OPEN_MAIN_BANNER
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
            putExtra(FcmNotificationPreferences.EXTRA_OPEN_FROM_FCM_BANNER, true)
            putExtra(FcmNotificationPreferences.EXTRA_OPEN_MAIN_SCREEN, true)
            putExtra(FcmNotificationPreferences.EXTRA_FCM_SYSTEM_NOTIFICATION_ID, systemNotificationId)
            putExtra(FcmNotificationPreferences.EXTRA_FCM_NOTIF_ID, alert.id)
            putExtra(FcmNotificationPreferences.EXTRA_FCM_TITLE, alert.title)
            putExtra(FcmNotificationPreferences.EXTRA_FCM_BODY, alert.body)
            putExtra(FcmNotificationPreferences.EXTRA_FCM_SENDER, alert.senderAdmin)
            putExtra("EXTRA_ALERT_ID", alert.id)
            putExtra("EXTRA_ALERT_TITLE", alert.title)
            putExtra("EXTRA_ALERT_BODY", alert.body)
            putExtra("EXTRA_ALERT_SEVERITY", alert.severity.name)
            putExtra("EXTRA_ALERT_REQUIRE_PAUSE", alert.requireAutomationPause)
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            systemNotificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val severityPrefix = when (alert.severity) {
            AlertSeverity.CRITICAL -> "🚨 [CRÍTICA] "
            AlertSeverity.MAINTENANCE -> "⚠️ [MANTENIMIENTO] "
            AlertSeverity.ANNOUNCEMENT -> "ℹ️ [AVISO] "
        }

        val formattedTitle = "$severityPrefix${alert.title}"

        val builder = NotificationCompat.Builder(context, CriticalCrmAlert.CHANNEL_ID)
            .setSmallIcon(android.R.drawable.stat_sys_warning)
            .setContentTitle(formattedTitle)
            .setContentText(alert.body)
            .setStyle(NotificationCompat.BigTextStyle().bigText("${alert.body}\n\nEmitido por: ${alert.senderAdmin}"))
            .setPriority(NotificationCompat.PRIORITY_MAX)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setOngoing(true)
            .setAutoCancel(false)
            .setContentIntent(pendingIntent)
            .setVibrate(longArrayOf(0, 300, 200, 300))
            .setDefaults(NotificationCompat.DEFAULT_ALL)

        try {
            val notificationManager = NotificationManagerCompat.from(context)
            notificationManager.notify(alert.id.hashCode(), builder.build())
        } catch (e: SecurityException) {
            android.util.Log.e("CriticalNotifHelper", "Permission denied to post notification", e)
        } catch (e: Exception) {
            android.util.Log.e("CriticalNotifHelper", "Error posting notification", e)
        }
    }
}
