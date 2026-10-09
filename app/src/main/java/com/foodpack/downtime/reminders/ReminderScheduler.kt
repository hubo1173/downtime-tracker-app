package com.foodpack.downtime.reminders

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import com.foodpack.downtime.data.SettingsStore
import kotlinx.coroutines.flow.first
import java.util.Calendar

/** Schedules the two daily reminders (handover, break times) with AlarmManager. */
object ReminderScheduler {

    const val KIND_HANDOVER = "handover"
    const val KIND_BREAKS = "breaks"

    fun parse(time: String, fallback: Pair<Int, Int>): Pair<Int, Int> {
        val m = Regex("^([01]?\\d|2[0-3])[:.]([0-5]\\d)$").matchEntire(time.trim()) ?: return fallback
        return m.groupValues[1].toInt() to m.groupValues[2].toInt()
    }

    private fun pending(context: Context, kind: String): PendingIntent =
        PendingIntent.getBroadcast(
            context,
            if (kind == KIND_HANDOVER) 1001 else 1002,
            Intent(context, ReminderReceiver::class.java).putExtra("kind", kind),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

    private fun nextTrigger(hour: Int, minute: Int): Long {
        val cal = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, hour); set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        if (cal.timeInMillis <= System.currentTimeMillis() + 1000) cal.add(Calendar.DAY_OF_YEAR, 1)
        return cal.timeInMillis
    }

    private fun scheduleOne(context: Context, kind: String, time: String, fallback: Pair<Int, Int>) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val (h, m) = parse(time, fallback)
        val at = nextTrigger(h, m)
        val pi = pending(context, kind)
        val canExact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || am.canScheduleExactAlarms()
        if (canExact) am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
        else am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
    }

    fun cancelAll(context: Context) {
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pending(context, KIND_HANDOVER))
        am.cancel(pending(context, KIND_BREAKS))
    }

    /** Re-reads the saved settings and (re)schedules both reminders for their next occurrence. */
    suspend fun scheduleAll(context: Context) {
        val settings = SettingsStore(context)
        if (!settings.remindersEnabled.first()) {
            cancelAll(context)
            return
        }
        scheduleOne(context, KIND_HANDOVER, settings.handoverReminderTime.first(), 6 to 45)
        scheduleOne(context, KIND_BREAKS, settings.breaksReminderTime.first(), 6 to 15)
    }
}
