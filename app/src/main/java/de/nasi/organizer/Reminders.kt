package de.nasi.organizer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import java.time.LocalDateTime
import java.time.ZoneId

/** Plant echte Android-Erinnerungen für alle Aufgaben mit Erinnerungszeit */
object Reminders {
    const val ACTION_REMIND = "de.nasi.organizer.REMIND"
    const val ACTION_DONE = "de.nasi.organizer.DONE"
    private const val CHANNEL = "reminders"

    fun rescheduleAll(ctx: Context) {
        val am = ctx.getSystemService(AlarmManager::class.java) ?: return
        val prefs = ctx.getSharedPreferences("alarms", Context.MODE_PRIVATE)
        prefs.getStringSet("ids", emptySet())?.forEach { am.cancel(alarmIntent(ctx, it, "")) }

        val s = Store.state(ctx)
        val scheduled = HashSet<String>()
        if (s != null) {
            val now = System.currentTimeMillis()
            for (t in Store.tasks(s)) {
                val r = Store.str(t, "remind")
                if (r.length < 16 || t.optBoolean("done")) continue
                val at = try {
                    LocalDateTime.parse(r.substring(0, 16)).atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
                } catch (e: Exception) { continue }
                if (at <= now) continue
                val id = Store.str(t, "id")
                val pi = alarmIntent(ctx, id, Store.str(t, "title"))
                try {
                    if (Build.VERSION.SDK_INT < 31 || am.canScheduleExactAlarms()) {
                        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                    } else {
                        am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                    }
                    scheduled.add(id)
                } catch (_: SecurityException) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi)
                    scheduled.add(id)
                }
            }
        }
        prefs.edit().putStringSet("ids", scheduled).apply()
    }

    private fun alarmIntent(ctx: Context, id: String, title: String): PendingIntent {
        val i = Intent(ctx, ReminderReceiver::class.java)
            .setAction(ACTION_REMIND)
            .setData(Uri.parse("organizer://task/" + Uri.encode(id)))
            .putExtra("id", id)
            .putExtra("title", title)
        return PendingIntent.getBroadcast(ctx, id.hashCode(), i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
    }

    fun show(ctx: Context, id: String, title: String) {
        val nm = ctx.getSystemService(NotificationManager::class.java) ?: return
        if (nm.getNotificationChannel(CHANNEL) == null) {
            nm.createNotificationChannel(
                NotificationChannel(CHANNEL, "Erinnerungen", NotificationManager.IMPORTANCE_HIGH).apply {
                    description = "Erinnerungen an deine Aufgaben"
                    enableVibration(true)
                }
            )
        }
        val open = PendingIntent.getActivity(
            ctx, id.hashCode(),
            Intent(ctx, MainActivity::class.java)
                .setData(Uri.parse("organizer://open/" + Uri.encode(id)))
                .putExtra(MainActivity.EXTRA_TASK, id)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val done = PendingIntent.getBroadcast(
            ctx, id.hashCode() + 1,
            Intent(ctx, ReminderReceiver::class.java)
                .setAction(ACTION_DONE)
                .setData(Uri.parse("organizer://done/" + Uri.encode(id)))
                .putExtra("id", id),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        val n = android.app.Notification.Builder(ctx, CHANNEL)
            .setSmallIcon(R.drawable.ic_notif)
            .setColor(0xFFA8141C.toInt())
            .setContentTitle(title)
            .setContentText("Erinnerung")
            .setContentIntent(open)
            .setAutoCancel(true)
            .addAction(android.app.Notification.Action.Builder(null, "Erledigt", done).build())
            .build()
        try { nm.notify(id.hashCode(), n) } catch (_: SecurityException) {}
    }

    fun cancelNotification(ctx: Context, id: String) {
        ctx.getSystemService(NotificationManager::class.java)?.cancel(id.hashCode())
    }
}

class ReminderReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        val id = intent.getStringExtra("id") ?: return
        when (intent.action) {
            Reminders.ACTION_REMIND -> {
                val s = Store.state(ctx)
                val t = s?.let { Store.find(it, id) } ?: return
                if (t.optBoolean("done")) return
                Store.markFired(ctx, id)
                Reminders.show(ctx, id, Store.str(t, "title").ifEmpty { intent.getStringExtra("title") ?: "Erinnerung" })
            }
            Reminders.ACTION_DONE -> {
                Store.toggle(ctx, id, onlyComplete = true)
                Reminders.cancelNotification(ctx, id)
            }
        }
    }
}

/** Nach Neustart, Update oder Datumswechsel: Erinnerungen neu planen und Widget aktualisieren */
class BootReceiver : BroadcastReceiver() {
    override fun onReceive(ctx: Context, intent: Intent) {
        Reminders.rescheduleAll(ctx)
        TodoWidget.refresh(ctx)
    }
}
