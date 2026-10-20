package ir.halvaei.metropol

import ir.halvaei.metropol.data.source.MetroNetworkParser
import ir.halvaei.metropol.domain.model.MetroNetwork
import ir.halvaei.metropol.domain.model.RideAlert
import ir.halvaei.metropol.domain.model.Route
import ir.halvaei.metropol.domain.model.RoutePreference
import ir.halvaei.metropol.domain.model.TripSession
import ir.halvaei.metropol.domain.repository.TripSessionRepository
import ir.halvaei.metropol.domain.routing.RoutePlanner
import ir.halvaei.metropol.domain.usecase.AdvanceTripUseCase
import ir.halvaei.metropol.domain.usecase.CorrectTripPositionUseCase
import ir.halvaei.metropol.domain.usecase.EvaluateWakeAlertsUseCase
import ir.halvaei.metropol.domain.usecase.PlanRideAlertsUseCase
import ir.halvaei.metropol.domain.usecase.TrackRideProgressUseCase
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import kotlin.math.ceil

/** Station-progress logic behind the "wake me up" trip tracking. */
class WakeAlertTest {

    private val network: MetroNetwork by lazy {
        MetroNetworkParser.parse(File("src/main/assets/metro_network.json").readText())
    }
    private val planner by lazy { RoutePlanner(network) }
    private val plan = PlanRideAlertsUseCase()
    private val evaluate = EvaluateWakeAlertsUseCase()
    private val advance = AdvanceTripUseCase(TrackRideProgressUseCase(), plan, evaluate)
    private val correct = CorrectTripPositionUseCase(FakeSessions(), plan) { 0 }

    /** Sadeghieh → Mirdamad needs one line change. */
    private val transferRoute: Route by lazy {
        planner.plan("tehran_sadeghieh", "mirdamad", RoutePreference.FASTEST, 12 * 60)!!.also {
            assertEquals(2, it.legs.size)
        }
    }

    private fun session(route: Route, anchorOffset: Double = 0.0, handled: Set<String> = emptySet()) = TripSession(
        originId = route.origin.id,
        destinationId = route.destination.id,
        preference = route.preference,
        departureMinuteOfDay = route.departureMinuteOfDay,
        anchorEpochMillis = 0L,
        anchorOffsetMinutes = anchorOffset,
        handledAlertKeys = handled,
    )

    /** Rounded up so "at minute x" never lands a fraction of a millisecond before x. */
    private fun millis(minutes: Double) = ceil(minutes * 60_000).toLong()

    @Test
    fun `alerts fire one station before each change and before the destination`() {
        val route = transferRoute
        val alerts = plan(route)
        assertEquals(2, alerts.size)

        val (transfer, arrival) = alerts
        assertEquals(RideAlert.Type.TRANSFER, transfer.type)
        assertEquals(route.legs[0].alight.id, transfer.station.id)
        assertEquals(route.legs[0].stops.dropLast(1).last().station.id, transfer.triggerStation.id)
        assertEquals(route.legs[1].line.id, transfer.nextLine?.id)

        assertEquals(RideAlert.Type.ARRIVAL, arrival.type)
        assertEquals("mirdamad", arrival.station.id)
        assertEquals(route.legs[1].stops.dropLast(1).last().station.id, arrival.triggerStation.id)
        assertNull(arrival.nextLine)

        alerts.forEach { assertTrue(it.fireAtOffsetMinutes < it.targetOffsetMinutes) }
    }

    @Test
    fun `on a one-stop trip the alert fires as the train leaves the origin`() {
        val route = planner.plan("tajrish", "gheytariyeh", RoutePreference.FASTEST, 12 * 60)!!
        assertEquals(1, route.stopCount)
        val alert = plan(route).single()
        assertEquals("tajrish", alert.triggerStation.id)
        assertEquals(route.legs[0].departureOffset, alert.fireAtOffsetMinutes, 1e-9)
    }

    @Test
    fun `an alert is due once its trigger station is reached and fires only once`() {
        val alerts = plan(transferRoute)
        val first = alerts.first()

        val before = evaluate(alerts, first.fireAtOffsetMinutes - 0.1, emptySet())
        assertTrue(before.due.isEmpty())
        assertTrue(before.handledKeys.isEmpty())

        val due = evaluate(alerts, first.fireAtOffsetMinutes, emptySet())
        assertEquals(listOf(first), due.due)

        val again = evaluate(alerts, first.fireAtOffsetMinutes + 0.2, due.handledKeys)
        assertTrue(again.due.isEmpty())
    }

    @Test
    fun `a slightly late check still fires but a long-passed alert is skipped silently`() {
        val alerts = plan(transferRoute)
        val first = alerts.first()

        val late = evaluate(alerts, first.targetOffsetMinutes + 0.5, emptySet())
        assertTrue(first in late.due)

        val passed = evaluate(alerts, first.targetOffsetMinutes + 5, emptySet())
        assertFalse(first in passed.due)
        assertTrue(first.key in passed.handledKeys)
    }

    @Test
    fun `progress follows the estimated times from the anchor`() {
        val route = transferRoute
        val stops = route.allStops
        val s = session(route)
        val target = stops[3]

        val tick = advance(route, s, millis(target.offsetMinutes + 0.01))
        assertEquals(3, tick.progress.currentStopIndex)
        assertEquals(stops[4].station.id, tick.nextStop?.station?.id)
        assertFalse(tick.ended)
    }

    @Test
    fun `tracking ends shortly after the estimated arrival`() {
        val route = transferRoute
        val s = session(route)
        val atArrival = advance(route, s, millis(route.totalMinutes))
        assertTrue(atArrival.progress.finished)
        assertNull(atArrival.nextStop)
        assertFalse("keeps a grace period in case the train is late", atArrival.ended)

        assertTrue(advance(route, s, millis(route.totalMinutes + AdvanceTripUseCase.ARRIVAL_GRACE_MINUTES)).ended)
    }

    @Test
    fun `advancing records fired alerts in the session`() {
        val route = transferRoute
        val first = plan(route).first()
        val tick = advance(route, session(route), millis(first.fireAtOffsetMinutes))
        assertEquals(listOf(first), tick.due)
        assertTrue(first.key in tick.session.handledAlertKeys)

        val next = advance(route, tick.session, millis(first.fireAtOffsetMinutes + 0.1))
        assertTrue(next.due.isEmpty())
    }

    @Test
    fun `I'm at this station re-anchors progress to that stop`() {
        val route = transferRoute
        val stops = route.allStops
        val now = millis(2.0)
        // The timetable thinks we're near the start, but the passenger says they are further on.
        val corrected = correct.corrected(session(route), route, 5, now)
        assertEquals(stops[5].offsetMinutes, corrected.elapsedMinutes(now), 1e-9)

        val tick = advance(route, corrected, now + millis(0.01))
        assertEquals(5, tick.progress.currentStopIndex)

        // A minute later the estimate keeps running from the corrected position.
        assertEquals(stops[5].offsetMinutes + 1, corrected.elapsedMinutes(now + millis(1.0)), 1e-9)
    }

    @Test
    fun `jumping past an interchange does not fire its stale alert`() {
        val route = transferRoute
        val transfer = plan(route).first()
        val afterInterchange = route.allStops.indexOfFirst { it.offsetMinutes > transfer.targetOffsetMinutes }
        val corrected = correct.corrected(session(route), route, afterInterchange, 0L)
        assertTrue(transfer.key in corrected.handledAlertKeys)
        assertTrue(advance(route, corrected, 0L).due.none { it.key == transfer.key })
    }

    @Test
    fun `correcting to the trigger station fires that alert`() {
        val route = transferRoute
        val arrival = plan(route).last()
        val triggerIndex = route.allStops.indexOfLast { it.station.id == arrival.triggerStation.id }
        val corrected = correct.corrected(session(route), route, triggerIndex, 0L)
        assertTrue(advance(route, corrected, 0L).due.any { it.key == arrival.key })
    }

    @Test
    fun `moving back before a fired alert re-arms it`() {
        val route = transferRoute
        val transfer = plan(route).first()
        val fired = session(route, handled = setOf(transfer.key))
        val corrected = correct.corrected(fired, route, 0, 0L)
        assertFalse(transfer.key in corrected.handledAlertKeys)
    }

    private class FakeSessions : TripSessionRepository {
        private val state = MutableStateFlow<TripSession?>(null)
        override val session: StateFlow<TripSession?> = state
        override fun save(session: TripSession) { state.value = session }
        override fun clear() { state.value = null }
    }
}
