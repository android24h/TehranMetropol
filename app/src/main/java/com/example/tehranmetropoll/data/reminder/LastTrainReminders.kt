package com.example.tehranmetropoll.data.reminder

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.edit
import com.example.tehranmetropoll.MetroApplication
import com.example.tehranmetropoll.R
import com.example.tehranmetropoll.core.AppLinks
import com.example.tehranmetropoll.core.isPersianLocale
import com.example.tehranmetropoll.core.localizeDigits
import com.example.tehranmetropoll.core.withAppLocale
import com.example.tehranmetropoll.data.trip.TripNotifications
import com.example.tehranmetropoll.domain.model.LastTrainReminder
import com.example.tehranmetropoll.domain.model.MetroNetwork
import com.example.tehranmetropoll.domain.model.RoutePreference
import com.example.tehranmetropoll.domain.repository.LastTrainReminderScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * One pending "leave now for the last train" reminder at a time, delivered by AlarmManager so it
 * fires on time even in Doze. Exact alarms are used when the system allows them; otherwise the
 * alarm may arrive a few minutes late, which is why the lead time is chosen by the user.
 */
class AlarmLastTrainReminderScheduler(context: Context) : LastTrainReminderScheduler {
    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("last_train_reminder", Context.MODE_PRIVATE)
    private val alarms = appContext.getSystemService(AlarmManager::class.java)
    private val _active = MutableStateFlow(read()?.takeIf { it.triggerAtMillis > System.currentTimeMillis() })
    override val active: StateFlow<LastTrainReminder?> = _active.asStateFlow()

    override fun schedule(reminder: LastTrainReminder) {
        val pending = pendingIntent()
        val exact = Build.VERSION.SDK_INT < Build.VERSION_CODES.S || alarms.canScheduleExactAlarms()
        if (exact) {
            alarms.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pending)
        } else {
            alarms.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, reminder.triggerAtMillis, pending)
        }
        write(reminder)
        _active.value = reminder
    }

    override fun cancel() {
        alarms.cancel(pendingIntent())
        clearStored()
    }

    /** Called by the receiver once the reminder has been delivered. */
    fun consume(): LastTrainReminder? = read().also { clearStored() }

    private fun clearStored() {
        prefs.edit { clear() }
        _active.value = null
    }

    private fun pendingIntent(): PendingIntent = PendingIntent.getBroadcast(
        appContext, 0,
        Intent(appContext, LastTrainReminderReceiver::class.java),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    private fun write(r: LastTrainReminder) = prefs.edit {
        putString(ORIGIN, r.originId)
        putString(DESTINATION, r.destinationId)
        putString(PREFERENCE, r.preference.name)
        putLong(TRIGGER, r.triggerAtMillis)
        putInt(LATEST, r.latestDepartureMinute)
        putInt(LEAD, r.leadMinutes)
    }

    private fun read(): LastTrainReminder? {
        val origin = prefs.getString(ORIGIN, null) ?: return null
        val destination = prefs.getString(DESTINATION, null) ?: return null
        return LastTrainReminder(
            originId = origin,
            destinationId = destination,
            preference = runCatching { RoutePreference.valueOf(prefs.getString(PREFERENCE, null)!!) }
                .getOrDefault(RoutePreference.FASTEST),
            triggerAtMillis = prefs.getLong(TRIGGER, 0),
            latestDepartureMinute = prefs.getInt(LATEST, 0),
            leadMinutes = prefs.getInt(LEAD, 0),
        )
    }

    private companion object {
        const val ORIGIN = "origin"
        const val DESTINATION = "destination"
        const val PREFERENCE = "preference"
        const val TRIGGER = "trigger"
        const val LATEST = "latest"
        const val LEAD = "lead"
    }
}

class LastTrainReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val container = (context.applicationContext as MetroApplication).container
        val reminder = container.lastTrainReminderScheduler.consume() ?: return
        val pending = goAsync()
        CoroutineScope(Dispatchers.Default).launch {
            try {
                notify(context, reminder, runCatching { container.getMetroNetwork() }.getOrNull())
            } finally {
                pending.finish()
            }
        }
    }

    private fun notify(context: Context, reminder: LastTrainReminder, network: MetroNetwork?) {
        val ctx = context.withAppLocale()
        val persian = ctx.isPersianLocale
        TripNotifications.ensureChannels(ctx)
        if (!TripNotifications.canPost(ctx, TripNotifications.REMINDER_CHANNEL)) return

        val origin = network?.stationOrNull(reminder.originId)?.name(persian) ?: reminder.originId
        val destination = network?.stationOrNull(reminder.destinationId)?.name(persian) ?: reminder.destinationId
        val body = ctx.getString(
            R.string.reminder_body,
            TripNotifications.clock(reminder.latestDepartureMinute).localizeDigits(persian),
            origin,
            destination,
        )
        val open = PendingIntent.getActivity(
            ctx, 2,
            AppLinks.intent(ctx, AppLinks.routeUri(reminder.originId, reminder.destinationId, reminder.preference)),
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
        )
        val notification = NotificationCompat.Builder(ctx, TripNotifications.REMINDER_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_metro)
            .setContentTitle(ctx.getString(R.string.reminder_title))
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setAutoCancel(true)
            .setContentIntent(open)
            .build()
        @Suppress("MissingPermission")
        NotificationManagerCompat.from(ctx).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val NOTIFICATION_ID = 4300
    }
}
