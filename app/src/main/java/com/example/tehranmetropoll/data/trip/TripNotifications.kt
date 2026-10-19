package com.example.tehranmetropoll.data.trip

import android.Manifest
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.example.tehranmetropoll.R
import com.example.tehranmetropoll.core.AppLinks
import com.example.tehranmetropoll.core.isPersianLocale
import com.example.tehranmetropoll.core.localizeDigits
import com.example.tehranmetropoll.core.withAppLocale
import com.example.tehranmetropoll.domain.model.RideAlert
import com.example.tehranmetropoll.domain.model.Route
import com.example.tehranmetropoll.domain.usecase.TripTick
import java.util.Locale
import kotlin.math.ceil

/** Notification channels and builders for trip tracking and wake-me-up alerts. */
object TripNotifications {
    const val TRACKING_CHANNEL = "trip_tracking"
    const val WAKE_CHANNEL = "wake_alerts"
    const val REMINDER_CHANNEL = "last_train_reminders"
    const val ONGOING_ID = 4100
    private const val ALERT_ID_BASE = 4200
    private const val LEGACY_CHANNEL = "ride_alerts"

    private val VIBRATION = longArrayOf(0, 500, 200, 500, 200, 800)

    fun ensureChannels(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val ctx = context.withAppLocale()
        val manager = ctx.getSystemService(NotificationManager::class.java)
        manager.deleteNotificationChannel(LEGACY_CHANNEL)
        manager.createNotificationChannel(
            NotificationChannel(TRACKING_CHANNEL, ctx.getString(R.string.trip_channel_name), NotificationManager.IMPORTANCE_LOW).apply {
                description = ctx.getString(R.string.trip_channel_description)
                setShowBadge(false)
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(WAKE_CHANNEL, ctx.getString(R.string.alert_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                description = ctx.getString(R.string.alert_channel_description)
                enableVibration(true)
                vibrationPattern = VIBRATION
                setSound(
                    RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION),
                    AudioAttributes.Builder()
                        .setUsage(AudioAttributes.USAGE_NOTIFICATION_EVENT)
                        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                        .build(),
                )
            },
        )
        manager.createNotificationChannel(
            NotificationChannel(REMINDER_CHANNEL, ctx.getString(R.string.reminder_channel_name), NotificationManager.IMPORTANCE_HIGH).apply {
                description = ctx.getString(R.string.reminder_channel_description)
            },
        )
    }

    private fun openRide(context: Context): PendingIntent = PendingIntent.getActivity(
        context, 0, AppLinks.intent(context, AppLinks.rideUri),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    fun starting(context: Context): Notification {
        val ctx = context.withAppLocale()
        return NotificationCompat.Builder(ctx, TRACKING_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_metro)
            .setContentTitle(ctx.getString(R.string.trip_notif_starting))
            .setOngoing(true)
            .setSilent(true)
            .setContentIntent(openRide(ctx))
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    fun ongoing(context: Context, route: Route, tick: TripTick, stopIntent: PendingIntent, nowMinuteOfDay: Int): Notification {
        val ctx = context.withAppLocale()
        val persian = ctx.isPersianLocale
        val next = tick.nextStop
        val title = if (next != null) {
            ctx.getString(R.string.trip_notif_next, next.station.name(persian))
        } else {
            ctx.getString(R.string.trip_notif_arriving, route.destination.name(persian))
        }
        val remaining = ceil(tick.progress.remainingMinutes).toInt()
        val arrival = clock(nowMinuteOfDay + remaining).localizeDigits(persian)
        val leg = route.legs[tick.progress.currentLegIndex]
        val body = ctx.getString(
            R.string.trip_notif_body,
            remaining.toString().localizeDigits(persian),
            route.destination.name(persian),
            arrival,
        )
        return NotificationCompat.Builder(ctx, TRACKING_CHANNEL)
            .setSmallIcon(R.drawable.ic_stat_metro)
            .setContentTitle(title)
            .setContentText(body)
            .setSubText(ctx.getString(R.string.line_number, leg.line.number.toString().localizeDigits(persian)))
            .setColor(leg.line.colorArgb.toInt())
            .setProgress(1000, (tick.progress.fraction * 1000).toInt(), false)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setSilent(true)
            .setCategory(NotificationCompat.CATEGORY_NAVIGATION)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .setContentIntent(openRide(ctx))
            .addAction(0, ctx.getString(R.string.trip_notif_stop), stopIntent)
            .setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            .build()
    }

    /**
     * Posts the wake-me-up alert. When notifications are blocked the phone still vibrates and
     * plays the notification sound, so a sleeping passenger is not left without a warning.
     */
    fun alert(context: Context, alert: RideAlert) {
        val ctx = context.withAppLocale()
        val persian = ctx.isPersianLocale
        val station = alert.station.name(persian)
        val (title, body) = when (alert.type) {
            RideAlert.Type.TRANSFER -> ctx.getString(R.string.alert_transfer_title, station) to ctx.getString(
                R.string.alert_transfer_body,
                (alert.nextLine?.number ?: 0).toString().localizeDigits(persian),
                alert.nextDirection?.name(persian).orEmpty(),
            )
            RideAlert.Type.ARRIVAL -> ctx.getString(R.string.alert_arrival_title, station) to
                ctx.getString(R.string.alert_arrival_body)
        }
        if (canPost(ctx, WAKE_CHANNEL)) {
            val notification = NotificationCompat.Builder(ctx, WAKE_CHANNEL)
                .setSmallIcon(R.drawable.ic_stat_metro)
                .setContentTitle(title)
                .setContentText(body)
                .setStyle(NotificationCompat.BigTextStyle().bigText(body))
                .setPriority(NotificationCompat.PRIORITY_MAX)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
                // Pre-Oreo devices take sound and vibration from the notification itself.
                .setVibrate(VIBRATION)
                .setSound(RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))
                .setAutoCancel(true)
                .setContentIntent(openRide(ctx))
                .build()
            @Suppress("MissingPermission")
            NotificationManagerCompat.from(ctx).notify(ALERT_ID_BASE + alert.legIndex, notification)
        } else {
            vibrate(ctx)
            runCatching {
                RingtoneManager.getRingtone(ctx, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION))?.play()
            }
        }
    }

    fun canPost(context: Context, channelId: String): Boolean {
        if (Build.VERSION.SDK_INT >= 33 &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            return false
        }
        val manager = NotificationManagerCompat.from(context)
        if (!manager.areNotificationsEnabled()) return false
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = manager.getNotificationChannel(channelId)
            if (channel != null && channel.importance == NotificationManager.IMPORTANCE_NONE) return false
        }
        return true
    }

    private fun vibrate(context: Context) {
        val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java) ?: return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(VIBRATION, -1))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(VIBRATION, -1)
        }
    }

    fun clock(minuteOfDay: Int): String {
        val m = ((minuteOfDay % 1440) + 1440) % 1440
        return String.format(Locale.US, "%02d:%02d", m / 60, m % 60)
    }
}
