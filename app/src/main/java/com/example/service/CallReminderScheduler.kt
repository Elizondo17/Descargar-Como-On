package com.example.service

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.example.data.model.CallReminderEntity

object CallReminderScheduler {

    /**
     * Schedules an inexact/idle-safe call reminder via AlarmManager.setAndAllowWhileIdle,
     * avoiding SCHEDULE_EXACT_ALARM / USE_EXACT_ALARM and FOREGROUND_SERVICE for Google Play compliance.
     */
    fun scheduleReminder(context: Context, reminder: CallReminderEntity) {
        if (!reminder.isEnabled) {
            cancelReminder(context, reminder.id)
            return
        }
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val pendingIntent = buildPendingIntent(context, reminder)
        val safeTriggerAt = reminder.triggerAtMs.coerceAtLeast(System.currentTimeMillis() + 5_000L)

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, safeTriggerAt, pendingIntent)
            } else {
                alarmManager.set(AlarmManager.RTC_WAKEUP, safeTriggerAt, pendingIntent)
            }
        } catch (_: SecurityException) {
            // Safe fallback if system rejects alarm
        }
    }

    fun cancelReminder(context: Context, reminderId: String) {
        val alarmManager = context.getSystemService(Context.ALARM_SERVICE) as? AlarmManager ?: return
        val intent = Intent(context, CallReminderReceiver::class.java)
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            reminderId.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_NO_CREATE
        )
        if (pendingIntent != null) {
            alarmManager.cancel(pendingIntent)
            pendingIntent.cancel()
        }
    }

    private fun buildPendingIntent(context: Context, reminder: CallReminderEntity): PendingIntent {
        val intent = Intent(context, CallReminderReceiver::class.java).apply {
            putExtra(CallReminderReceiver.EXTRA_REMINDER_ID, reminder.id)
            putExtra(
                CallReminderReceiver.EXTRA_REMINDER_TITLE,
                "📞 ${reminder.title.ifBlank { "Recordatorio de llamadas PSA" }}"
            )
            putExtra(
                CallReminderReceiver.EXTRA_REMINDER_BODY,
                reminder.note.ifBlank { "Tu tanda programada está por comenzar. ¡Vamos por esa cita!" }
            )
        }
        return PendingIntent.getBroadcast(
            context,
            reminder.id.hashCode(),
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
    }
}
