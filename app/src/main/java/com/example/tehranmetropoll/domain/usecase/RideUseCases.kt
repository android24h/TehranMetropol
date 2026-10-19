package com.example.tehranmetropoll.domain.usecase

import com.example.tehranmetropoll.domain.model.RideAlert
import com.example.tehranmetropoll.domain.model.Route
import com.example.tehranmetropoll.domain.model.RouteLeg
import com.example.tehranmetropoll.domain.model.RouteStop
import com.example.tehranmetropoll.domain.model.TripSession
import com.example.tehranmetropoll.domain.repository.Clock
import com.example.tehranmetropoll.domain.repository.TripSessionRepository
import com.example.tehranmetropoll.domain.repository.TripTrackingController
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.mapLatest

/** Where the passenger is expected to be, given minutes elapsed since the ride started. */
data class RideProgress(
    val elapsedMinutes: Double,
    val remainingMinutes: Double,
    val fraction: Float,
    /** Index into [Route.allStops] of the last station reached (or the origin while waiting). */
    val currentStopIndex: Int,
    val currentLegIndex: Int,
    val phase: Phase,
    val finished: Boolean,
) {
    enum class Phase { WAITING, RIDING, TRANSFERRING, ARRIVED }
}

class TrackRideProgressUseCase {
    operator fun invoke(route: Route, elapsedMinutes: Double): RideProgress {
        val total = route.totalMinutes
        val elapsed = elapsedMinutes.coerceAtLeast(0.0)
        val stops: List<RouteStop> = route.allStops
        val currentStopIndex = stops.indexOfLast { it.offsetMinutes <= elapsed }.coerceAtLeast(0)

        val legIndex = route.legs.indexOfFirst { elapsed < it.arrivalOffset }
            .let { if (it == -1) route.legs.lastIndex else it }
        val leg: RouteLeg = route.legs[legIndex]
        val phase = when {
            elapsed >= total -> RideProgress.Phase.ARRIVED
            elapsed < leg.departureOffset && legIndex == 0 -> RideProgress.Phase.WAITING
            elapsed < leg.departureOffset -> RideProgress.Phase.TRANSFERRING
            else -> RideProgress.Phase.RIDING
        }
        return RideProgress(
            elapsedMinutes = elapsed,
            remainingMinutes = (total - elapsed).coerceAtLeast(0.0),
            fraction = (elapsed / total).toFloat().coerceIn(0f, 1f),
            currentStopIndex = currentStopIndex,
            currentLegIndex = legIndex,
            phase = phase,
            finished = phase == RideProgress.Phase.ARRIVED,
        )
    }
}

class PlanRideAlertsUseCase {
    /**
     * One alert per leg, fired when the train reaches the stop just before the station where the
     * passenger gets off (an interchange or the destination). On a one-stop leg that is the
     * boarding station, so the alert fires as the train departs.
     */
    operator fun invoke(route: Route): List<RideAlert> = route.legs.mapIndexed { index, leg ->
        val next = route.legs.getOrNull(index + 1)
        val trigger = leg.stops[leg.stops.size - 2]
        RideAlert(
            type = if (next == null) RideAlert.Type.ARRIVAL else RideAlert.Type.TRANSFER,
            legIndex = index,
            station = leg.alight,
            triggerStation = trigger.station,
            fireAtOffsetMinutes = trigger.offsetMinutes,
            targetOffsetMinutes = leg.arrivalOffset,
            nextLine = next?.line,
            nextDirection = next?.direction,
        )
    }
}

data class AlertDecision(val due: List<RideAlert>, val handledKeys: Set<String>)

class EvaluateWakeAlertsUseCase {
    /**
     * Alerts whose trigger station has been reached are due. An alert whose target station is
     * already well behind us is marked handled without firing – there is nothing to wake up for.
     */
    operator fun invoke(alerts: List<RideAlert>, elapsedMinutes: Double, handled: Set<String>): AlertDecision {
        val due = mutableListOf<RideAlert>()
        val updated = handled.toMutableSet()
        for (alert in alerts) {
            if (alert.key in handled) continue
            when {
                elapsedMinutes >= alert.targetOffsetMinutes + LATE_TOLERANCE_MINUTES -> updated += alert.key
                elapsedMinutes >= alert.fireAtOffsetMinutes -> {
                    due += alert
                    updated += alert.key
                }
            }
        }
        return AlertDecision(due, updated)
    }

    private companion object {
        /** A slightly late check (e.g. the device was busy) still fires the alert. */
        const val LATE_TOLERANCE_MINUTES = 1.0
    }
}

data class TripTick(
    val progress: RideProgress,
    val due: List<RideAlert>,
    val session: TripSession,
    /** The next station the train will reach, or null once arrived. */
    val nextStop: RouteStop?,
    /** True once the trip is over and tracking should stop. */
    val ended: Boolean,
)

/** One step of the tracking loop: where are we, which alerts fire now, is the trip over? */
class AdvanceTripUseCase(
    private val trackProgress: TrackRideProgressUseCase,
    private val planAlerts: PlanRideAlertsUseCase,
    private val evaluateAlerts: EvaluateWakeAlertsUseCase,
) {
    operator fun invoke(route: Route, session: TripSession, nowMillis: Long): TripTick {
        val elapsed = session.elapsedMinutes(nowMillis)
        val progress = trackProgress(route, elapsed)
        val decision = evaluateAlerts(planAlerts(route), elapsed, session.handledAlertKeys)
        return TripTick(
            progress = progress,
            due = decision.due,
            session = session.copy(handledAlertKeys = decision.handledKeys),
            nextStop = route.allStops.getOrNull(progress.currentStopIndex + 1),
            ended = elapsed >= route.totalMinutes + ARRIVAL_GRACE_MINUTES,
        )
    }

    companion object {
        /** Keep tracking briefly after the estimated arrival in case the train runs late. */
        const val ARRIVAL_GRACE_MINUTES = 3.0
    }
}

data class ActiveTrip(val session: TripSession, val route: Route)

class StartTripUseCase(
    private val sessions: TripSessionRepository,
    private val controller: TripTrackingController,
    private val clock: Clock,
) {
    operator fun invoke(route: Route) {
        sessions.save(
            TripSession(
                originId = route.origin.id,
                destinationId = route.destination.id,
                preference = route.preference,
                departureMinuteOfDay = route.departureMinuteOfDay,
                anchorEpochMillis = clock.nowMillis(),
                anchorOffsetMinutes = 0.0,
            ),
        )
        controller.start()
    }
}

class StopTripUseCase(
    private val sessions: TripSessionRepository,
    private val controller: TripTrackingController,
) {
    operator fun invoke() {
        sessions.clear()
        controller.stop()
    }
}

class CorrectTripPositionUseCase(
    private val sessions: TripSessionRepository,
    private val planAlerts: PlanRideAlertsUseCase,
    private val clock: Clock,
) {
    /** "I'm at this station": re-anchor the estimate to when the train reaches [stopIndex]. */
    operator fun invoke(route: Route, stopIndex: Int) {
        val session = sessions.session.value ?: return
        sessions.save(corrected(session, route, stopIndex, clock.nowMillis()))
    }

    fun corrected(session: TripSession, route: Route, stopIndex: Int, nowMillis: Long): TripSession {
        val offset = route.allStops[stopIndex.coerceIn(0, route.allStops.lastIndex)].offsetMinutes
        val alerts = planAlerts(route)
        // Alerts for stations already reached are done; moving backwards re-arms the ones ahead.
        val handled = session.handledAlertKeys +
            alerts.filter { it.targetOffsetMinutes <= offset }.map { it.key } -
            alerts.filter { it.fireAtOffsetMinutes > offset }.map { it.key }.toSet()
        return session.copy(anchorEpochMillis = nowMillis, anchorOffsetMinutes = offset, handledAlertKeys = handled)
    }
}

class ObserveActiveTripUseCase(
    private val sessions: TripSessionRepository,
    private val findRoute: FindRouteUseCase,
) {
    @OptIn(ExperimentalCoroutinesApi::class)
    operator fun invoke(): Flow<ActiveTrip?> {
        var cachedKey: List<Any>? = null
        var cachedRoute: Route? = null
        return sessions.session.mapLatest { session ->
            if (session == null) return@mapLatest null
            val key = listOf(session.originId, session.destinationId, session.preference, session.departureMinuteOfDay)
            val route = cachedRoute.takeIf { key == cachedKey }
                ?: (findRoute(session.originId, session.destinationId, session.preference, session.departureMinuteOfDay)
                    as? RouteResult.Found)?.route
                    ?.also { cachedKey = key; cachedRoute = it }
            route?.let { ActiveTrip(session, it) }
        }
    }
}
