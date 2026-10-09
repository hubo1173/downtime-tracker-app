package com.foodpack.downtime.reminders

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.foodpack.downtime.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

/** Fires at the reminder time: posts a notification, then schedules tomorrow's. */
class ReminderReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val kind = intent.getStringExtra("kind") ?: return
        val (id, title, text) = if (kind == ReminderScheduler.KIND_HANDOVER) {
            Triple(2001, "Morning handover", "Send the Start of Shift handover before 7am.")
        } else {
            Triple(2002, "Break times", "Set Team A, B and C break times before 6:30am.")
        }

        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            nm.createNotificationChannel(
                NotificationChannel("reminders", "Shift reminders", NotificationManager.IMPORTANCE_HIGH)
            )
        }
        val open = PendingIntent.getActivity(
            context, id, Intent(context, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(context, "reminders")
            .setSmallIcon(android.R.drawable.ic_popup_reminder)
            .setContentTitle(title)
            .setContentText(text)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        if (NotificationManagerCompat.from(context).areNotificationsEnabled()) {
            try { NotificationManagerCompat.from(context).notify(id, notification) } catch (_: SecurityException) {}
        }

        val pending = goAsync()
        CoroutineScope(Dispatchers.IO).launch {
            try { ReminderScheduler.scheduleAll(context.applicationContext) } finally { pending.finish() }
        }
    }
}
