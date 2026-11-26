package com.example.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent

class CallReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val reminderId = intent.getStringExtra(EXTRA_REMINDER_ID).orEmpty().ifBlank { "default_call" }
        val title = intent.getStringExtra(EXTRA_REMINDER_TITLE).orEmpty()
        val body = intent.getStringExtra(EXTRA_REMINDER_BODY).orEmpty()
        NotificationHelper(context.applicationContext).showCallReminderNotification(
            reminderId = reminderId,
            title = title,
            body = body
        )
    }

    companion object {
        const val EXTRA_REMINDER_ID = "extra_reminder_id"
        const val EXTRA_REMINDER_TITLE = "extra_reminder_title"
        const val EXTRA_REMINDER_BODY = "extra_reminder_body"
    }
}
