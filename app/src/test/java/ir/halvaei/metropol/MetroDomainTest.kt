package ir.halvaei.metropol

import ir.halvaei.metropol.data.source.MetroNetworkParser
import ir.halvaei.metropol.domain.model.MetroNetwork
import ir.halvaei.metropol.domain.model.RoutePreference
import ir.halvaei.metropol.domain.routing.RoutePlanner
import ir.halvaei.metropol.domain.routing.StationMatcher
import ir.halvaei.metropol.domain.usecase.PlanRideAlertsUseCase
import ir.halvaei.metropol.domain.usecase.RideProgress
import ir.halvaei.metropol.domain.usecase.TrackRideProgressUseCase
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class MetroDomainTest {

    private val network: MetroNetwork by lazy {
        MetroNetworkParser.parse(File("src/main/assets/metro_network.json").readText())
    }
    private val planner by lazy { RoutePlanner(network) }
    private val noon = 12 * 60

    @Test
    fun `network data is complete and consistent`() {
        val ids = network.stations.map { it.id }
        assertEquals("duplicate station ids", ids.size, ids.toSet().size)
        assertEquals(7, network.publicLines.size)
        assertTrue("expected the full network, got ${ids.size}", ids.size >= 140)
        network.stations.forEach { s ->
            assertTrue("${s.id} outside the Tehran region", s.location.lat in 35.3..36.0 && s.location.lng in 50.8..51.7)
            assertTrue("${s.id} missing Persian name", s.nameFa.isNotBlank())
        }
        network.lines.forEach { line ->
            assertEquals("${line.id} repeats a station", line.stationIds.size, line.stationIds.toSet().size)
        }
    }

    @Test
    fun `every station is reachable from every other`() {
        val origin = network.stations.first().id
        network.stations.drop(1).forEach { target ->
            assertNotNull("no route to ${target.id}", planner.plan(origin, target.id, RoutePreference.FASTEST, noon))
        }
    }

    @Test
    fun `sadeghieh to mirdamad gives direction and line change`() {
        val route = planner.plan("tehran_sadeghieh", "mirdamad", RoutePreference.FASTEST, noon)!!
        assertEquals("tehran_sadeghieh", route.origin.id)
        assertEquals("mirdamad", route.destination.id)
        assertTrue(route.transfers >= 1)
        assertEquals(1, route.legs.last().line.number)
        assertEquals("tajrish", route.legs.last().direction.id)
        assertTrue(route.totalMinutes in 20.0..70.0)
        // Legs chain together and times only increase.
        route.legs.zipWithNext().forEach { (a, b) -> assertEquals(a.alight.id, b.board.id) }
        route.allStops.zipWithNext().forEach { (a, b) -> assertTrue(b.offsetMinutes > a.offsetMinutes) }
    }

    @Test
    fun `same line trip has no transfers and the right direction`() {
        val route = planner.plan("tajrish", "shoush", RoutePreference.FASTEST, noon)!!
        assertEquals(0, route.transfers)
        assertEquals("kahrizak", route.legs.single().direction.id)
        assertEquals(19, route.stopCount)
    }

    @Test
    fun `fewest transfers never uses more changes than fastest`() {
        listOf("ghaem" to "golshahr", "farhangsara" to "azadegan", "kouhsar" to "meydan_basij").forEach { (o, d) ->
            val fast = planner.plan(o, d, RoutePreference.FASTEST, noon)!!
            val simple = planner.plan(o, d, RoutePreference.FEWEST_TRANSFERS, noon)!!
            assertTrue(simple.transfers <= fast.transfers)
            assertTrue(fast.totalMinutes <= simple.totalMinutes + 1e-6)
        }
    }

    @Test
    fun `airport trips use the line 1 branch`() {
        val route = planner.plan("tajrish", "ika_airport", RoutePreference.FASTEST, noon)!!
        assertEquals("L1A", route.legs.last().line.id)
        assertEquals("shahed_bagher_shahr", route.legs.last().board.id)
    }

    @Test
    fun `peak hours shorten waits`() {
        val peak = planner.plan("tajrish", "shoush", RoutePreference.FASTEST, 8 * 60)!!
        val late = planner.plan("tajrish", "shoush", RoutePreference.FASTEST, 22 * 60)!!
        assertTrue(peak.totalWaitMinutes < late.totalWaitMinutes)
    }

    @Test
    fun `same origin and destination yields no route`() {
        assertNull(planner.plan("mirdamad", "mirdamad", RoutePreference.FASTEST, noon))
    }

    @Test
    fun `search is bilingual and forgiving`() {
        val stations = network.stations
        assertEquals("tehran_sadeghieh", StationMatcher.search(stations, "sadegh").first().id)
        assertEquals("tehran_sadeghieh", StationMatcher.search(stations, "صادقیه").first().id)
        assertEquals("tehran_sadeghieh", StationMatcher.search(stations, "صادقيه").first().id) // Arabic yeh
        assertEquals("mirdamad", StationMatcher.search(stations, "میر داماد").first().id)
        assertEquals("mirdamad", StationMatcher.exact(stations, "MIRDAMAD")?.id)
        assertTrue(StationMatcher.search(stations, "zzzz").isEmpty())
    }

    @Test
    fun `ride progress walks through phases`() {
        val route = planner.plan("tehran_sadeghieh", "mirdamad", RoutePreference.FASTEST, noon)!!
        val track = TrackRideProgressUseCase()
        assertEquals(RideProgress.Phase.WAITING, track(route, 0.0).phase)
        val firstLeg = route.legs.first()
        assertEquals(RideProgress.Phase.RIDING, track(route, firstLeg.departureOffset + 1).phase)
        assertEquals(RideProgress.Phase.TRANSFERRING, track(route, firstLeg.arrivalOffset + 0.5).phase)
        val end = track(route, route.totalMinutes + 1)
        assertTrue(end.finished)
        assertEquals(route.allStops.lastIndex, end.currentStopIndex)
    }

    @Test
    fun `ride alerts fire before each change and the destination`() {
        val route = planner.plan("tehran_sadeghieh", "mirdamad", RoutePreference.FASTEST, noon)!!
        val alerts = PlanRideAlertsUseCase()(route)
        assertEquals(route.legs.size, alerts.size)
        assertEquals("mirdamad", alerts.last().station.id)
        alerts.zip(route.legs).forEach { (alert, leg) -> assertTrue(alert.fireAtOffsetMinutes < leg.arrivalOffset) }
    }
}
