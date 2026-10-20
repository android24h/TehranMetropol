package ir.halvaei.metropol.data.trip

import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import android.os.PowerManager
import android.util.Log
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.ServiceCompat
import androidx.core.content.ContextCompat
import ir.halvaei.metropol.MetroApplication
import ir.halvaei.metropol.domain.model.Route
import ir.halvaei.metropol.domain.repository.TripTrackingController
import ir.halvaei.metropol.domain.usecase.ActiveTrip
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Follows a tracked trip in the foreground: keeps an ongoing notification with the next station
 * and fires wake-me-up alerts. Progress comes from the route's estimated times (GPS is useless
 * underground); the passenger can re-anchor it from the app with "I'm at this station".
 */
class TripTrackingService : Service() {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var trackingJob: Job? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private val container get() = (application as MetroApplication).container

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            container.stopTrip()
            return START_NOT_STICKY
        }
        if (!enterForeground()) {
            stopSelf()
            return START_NOT_STICKY
        }
        if (trackingJob?.isActive != true) {
            trackingJob = scope.launch { track() }
        }
        return START_STICKY
    }

    private fun enterForeground(): Boolean = try {
        TripNotifications.ensureChannels(this)
        ServiceCompat.startForeground(
            this,
            TripNotifications.ONGOING_ID,
            TripNotifications.starting(this),
            if (Build.VERSION.SDK_INT >= 34) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0,
        )
        true
    } catch (e: Exception) {
        // E.g. a sticky restart while the app is in the background on Android 12+.
        Log.w(TAG, "Could not start trip tracking in the foreground", e)
        false
    }

    private suspend fun track() {
        container.observeActiveTrip().collectLatest { trip ->
            if (trip == null) {
                finish()
                return@collectLatest
            }
            holdWakeLock(trip.route)
            runLoop(trip)
        }
    }

    private suspend fun runLoop(trip: ActiveTrip) {
        val stop = stopIntent()
        while (scope.isActive) {
            val tick = container.advanceTrip(trip.route, trip.session, System.currentTimeMillis())
            tick.due.forEach { TripNotifications.alert(this, it) }
            if (tick.ended) {
                container.stopTrip()
                return
            }
            @Suppress("MissingPermission")
            if (TripNotifications.canPost(this, TripNotifications.TRACKING_CHANNEL)) {
                NotificationManagerCompat.from(this).notify(
                    TripNotifications.ONGOING_ID,
                    TripNotifications.ongoing(this, trip.route, tick, stop, container.clock.minuteOfDay()),
                )
            }
            if (tick.session != trip.session) {
                // Saving re-emits the session, which restarts this loop with the new state.
                container.tripSessions.save(tick.session)
                return
            }
            delay(TICK_MS)
        }
    }

    private fun stopIntent(): PendingIntent = PendingIntent.getService(
        this, 1,
        Intent(this, TripTrackingService::class.java).setAction(ACTION_STOP),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT,
    )

    /** Ticks must keep running with the screen off, or the alert would come too late. */
    private fun holdWakeLock(route: Route) {
        val lock = wakeLock ?: (getSystemService(Context.POWER_SERVICE) as PowerManager)
            .newWakeLock(PowerManager.PARTIAL_WAKE_LOCK, WAKE_LOCK_TAG)
            .apply { setReferenceCounted(false) }
            .also { wakeLock = it }
        val timeoutMs = ((route.totalMinutes + WAKE_LOCK_MARGIN_MINUTES) * 60_000).toLong()
        lock.acquire(timeoutMs)
    }

    private fun finish() {
        wakeLock?.takeIf { it.isHeld }?.release()
        ServiceCompat.stopForeground(this, ServiceCompat.STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    override fun onDestroy() {
        wakeLock?.takeIf { it.isHeld }?.release()
        scope.cancel()
        super.onDestroy()
    }

    companion object {
        private const val TAG = "TripTrackingService"
        private const val ACTION_STOP = "ir.halvaei.metropol.action.STOP_TRIP"
        private const val WAKE_LOCK_TAG = "Metropool:trip"
        private const val WAKE_LOCK_MARGIN_MINUTES = 30
        private const val TICK_MS = 5_000L
    }
}

class ServiceTripTrackingController(context: Context) : TripTrackingController {
    private val appContext = context.applicationContext

    override fun start() {
        ContextCompat.startForegroundService(appContext, Intent(appContext, TripTrackingService::class.java))
    }

    override fun stop() {
        appContext.stopService(Intent(appContext, TripTrackingService::class.java))
    }
}
