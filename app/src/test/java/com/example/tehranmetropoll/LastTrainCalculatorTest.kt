package com.example.tehranmetropoll

import com.example.tehranmetropoll.data.source.MetroNetworkParser
import com.example.tehranmetropoll.data.source.OperationsParser
import com.example.tehranmetropoll.domain.lasttrain.LastTrainCalculator
import com.example.tehranmetropoll.domain.lasttrain.ServiceDayResolver
import com.example.tehranmetropoll.domain.model.DayType
import com.example.tehranmetropoll.domain.model.DirectionOperations
import com.example.tehranmetropoll.domain.model.LastTrainCheck.Status
import com.example.tehranmetropoll.domain.model.LastTrainTimes
import com.example.tehranmetropoll.domain.model.MetroNetwork
import com.example.tehranmetropoll.domain.model.MetroOperations
import com.example.tehranmetropoll.domain.model.Route
import com.example.tehranmetropoll.domain.model.RoutePreference
import com.example.tehranmetropoll.domain.model.ServiceTime
import com.example.tehranmetropoll.domain.routing.RoutePlanner
import com.example.tehranmetropoll.domain.routing.TravelTimes
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.floor

class LastTrainCalculatorTest {

    private val network: MetroNetwork by lazy {
        MetroNetworkParser.parse(File("src/main/assets/metro_network.json").readText())
    }
    private val planner by lazy { RoutePlanner(network) }
    private val noon = 12 * 60

    private fun time(h: Int, m: Int = 0) = h * 60 + m

    /** Same last-train times for every direction of every line, unless overridden. */
    private fun operations(
        weekday: Int? = time(22),
        friday: Int? = time(21),
        holiday: Int? = time(20),
        verified: Boolean = true,
        holidays: Set<String> = emptySet(),
        overrides: Map<Pair<String, String>, LastTrainTimes> = emptyMap(),
    ): MetroOperations {
        val directions = network.lines.flatMap { line ->
            listOf(line.firstStationId, line.lastStationId).map { toward ->
                DirectionOperations(
                    lineId = line.id,
                    towardStationId = toward,
                    lastTrain = overrides[line.id to toward] ?: LastTrainTimes(weekday, friday, holiday, verified),
                    womenCars = null,
                )
            }
        }
        return MetroOperations(
            directions = directions,
            holidays = holidays,
            holidaysVerified = true,
            serviceDayStartMinute = time(3),
            stationEntryMinutes = 3.0,
            warningMinutes = 45,
            timeZoneId = "Asia/Tehran",
        )
    }

    private fun at(minute: Int, dayType: DayType = DayType.WEEKDAY) = ServiceTime(0L, "2026-10-14", dayType, minute)

    private fun route(from: String, to: String): Route = planner.plan(from, to, RoutePreference.FASTEST, noon)!!

    private fun lastPass(route: Route, legIndex: Int, departure: Int): Double {
        val leg = route.legs[legIndex]
        return departure + TravelTimes.minutesFromStartTerminal(network, leg.line, leg.direction.id, leg.board.id)!!
    }

    @Test
    fun `single leg from a terminal uses the terminal departure minus station entry`() {
        val route = route("tajrish", "mirdamad")
        assertEquals(1, route.legs.size)
        val check = LastTrainCalculator(network, operations()).check(route, at(time(20)))
        assertEquals(time(22) - 3, check.latestDepartureMinute)
        assertEquals(Status.SAFE, check.status)
        assertEquals(0, check.criticalLegIndex)
    }

    @Test
    fun `boarding mid-line adds running time from the starting terminal`() {
        val route = route("mirdamad", "shoush")
        assertEquals(1, route.legs.size)
        val expected = floor(lastPass(route, 0, time(22)) - 3).toInt()
        assertTrue("mid-line station should be later than the terminal", expected > time(22) - 3)
        assertEquals(expected, LastTrainCalculator(network, operations()).check(route, at(noon)).latestDepartureMinute)
    }

    @Test
    fun `status moves from safe to at risk to missed as time passes`() {
        val route = route("tajrish", "mirdamad")
        val calc = LastTrainCalculator(network, operations())
        val latest = time(22) - 3
        assertEquals(Status.SAFE, calc.check(route, at(latest - 46)).status)

        val atRisk = calc.check(route, at(latest - 15))
        assertEquals(Status.AT_RISK, atRisk.status)
        assertEquals(15, atRisk.minutesLeft)

        assertEquals(Status.AT_RISK, calc.check(route, at(latest)).status)
        val missed = calc.check(route, at(latest + 1))
        assertEquals(Status.MISSED, missed.status)
        assertEquals(-1, missed.minutesLeft)
    }

    @Test
    fun `an early last train on the second leg pulls the whole trip earlier`() {
        val route = route("tehran_sadeghieh", "mirdamad")
        assertEquals("expected exactly one line change", 2, route.legs.size)
        val (first, second) = route.legs
        val ops = operations(
            weekday = time(23, 30),
            overrides = mapOf((second.line.id to second.direction.id) to LastTrainTimes(time(21), time(21), time(21), true)),
        )
        val check = LastTrainCalculator(network, ops).check(route, at(noon))

        // By hand: to board the last leg-2 train, leave leg 1 early enough to ride to the
        // interchange and walk across.
        val latestBoardFirst = lastPass(route, 1, time(21)) - second.walkMinutes - first.rideMinutes
        assertTrue(latestBoardFirst < lastPass(route, 0, time(23, 30)))
        assertEquals(floor(latestBoardFirst - 3).toInt(), check.latestDepartureMinute)
        assertTrue(check.latestDepartureMinute!! < floor(lastPass(route, 0, time(23, 30)) - 3).toInt())
        assertEquals(1, check.criticalLegIndex)
    }

    @Test
    fun `an early last train on the first leg is the binding constraint`() {
        val route = route("tehran_sadeghieh", "mirdamad")
        val first = route.legs[0]
        val ops = operations(
            weekday = time(23, 30),
            overrides = mapOf((first.line.id to first.direction.id) to LastTrainTimes(time(20), time(20), time(20), true)),
        )
        val check = LastTrainCalculator(network, ops).check(route, at(noon))
        assertEquals(floor(lastPass(route, 0, time(20)) - 3).toInt(), check.latestDepartureMinute)
        assertEquals(0, check.criticalLegIndex)
    }

    @Test
    fun `friday and holiday timetables are used on those days`() {
        val route = route("tajrish", "mirdamad")
        val calc = LastTrainCalculator(network, operations(weekday = time(22), friday = time(21), holiday = time(20)))
        assertEquals(time(22) - 3, calc.check(route, at(noon, DayType.WEEKDAY)).latestDepartureMinute)
        assertEquals(time(21) - 3, calc.check(route, at(noon, DayType.FRIDAY)).latestDepartureMinute)
        assertEquals(time(20) - 3, calc.check(route, at(noon, DayType.HOLIDAY)).latestDepartureMinute)

        // The same moment (20:30) is fine on a weekday, tight on a Friday and too late on a holiday.
        assertEquals(Status.SAFE, calc.check(route, at(time(20, 30))).status)
        assertEquals(Status.AT_RISK, calc.check(route, at(time(20, 30), DayType.FRIDAY)).status)
        assertEquals(Status.MISSED, calc.check(route, at(time(20, 30), DayType.HOLIDAY)).status)
    }

    @Test
    fun `holidays without their own time follow the friday timetable`() {
        val route = route("tajrish", "mirdamad")
        val calc = LastTrainCalculator(network, operations(friday = time(21), holiday = null))
        assertEquals(time(21) - 3, calc.check(route, at(noon, DayType.HOLIDAY)).latestDepartureMinute)
    }

    @Test
    fun `transfer routes respect the day type on every leg`() {
        val route = route("tehran_sadeghieh", "mirdamad")
        val calc = LastTrainCalculator(network, operations(weekday = time(22), friday = time(21)))
        val weekday = calc.check(route, at(noon)).latestDepartureMinute!!
        val friday = calc.check(route, at(noon, DayType.FRIDAY)).latestDepartureMinute!!
        assertEquals(60, weekday - friday)
    }

    @Test
    fun `missing data makes the result unknown instead of guessing`() {
        val route = route("tehran_sadeghieh", "mirdamad")
        val leg = route.legs.last()
        val noFriday = operations(overrides = mapOf((leg.line.id to leg.direction.id) to LastTrainTimes(time(22), null, null, true)))
        val check = LastTrainCalculator(network, noFriday).check(route, at(noon, DayType.FRIDAY))
        assertEquals(Status.UNKNOWN, check.status)
        assertNull(check.latestDepartureMinute)

        assertEquals(Status.UNKNOWN, LastTrainCalculator(network, MetroOperations.EMPTY).check(route, at(noon)).status)
    }

    @Test
    fun `placeholder data is flagged as unverified`() {
        val route = route("tajrish", "mirdamad")
        assertFalse(LastTrainCalculator(network, operations(verified = true)).check(route, at(noon)).unverified)
        assertTrue(LastTrainCalculator(network, operations(verified = false)).check(route, at(noon)).unverified)
    }

    // --- Tehran service day ---

    private fun tehranMillis(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long =
        Calendar.getInstance(TimeZone.getTimeZone("Asia/Tehran")).apply {
            clear()
            set(year, month - 1, day, hour, minute)
        }.timeInMillis

    @Test
    fun `service day uses Tehran time and day types`() {
        val ops = operations(holidays = setOf("2026-10-14"))
        val wednesdayHoliday = ServiceDayResolver.resolve(tehranMillis(2026, 10, 14, 21, 0), ops)
        assertEquals(DayType.HOLIDAY, wednesdayHoliday.dayType)
        assertEquals(time(21), wednesdayHoliday.minuteOfServiceDay)

        val thursday = ServiceDayResolver.resolve(tehranMillis(2026, 10, 15, 8, 15), ops)
        assertEquals(DayType.WEEKDAY, thursday.dayType)
        assertEquals("2026-10-15", thursday.serviceDate)

        val friday = ServiceDayResolver.resolve(tehranMillis(2026, 10, 16, 22, 0), ops)
        assertEquals(DayType.FRIDAY, friday.dayType)
    }

    @Test
    fun `just after midnight still belongs to the previous service day`() {
        val ops = operations()
        // 00:30 on Saturday is still Friday night's service.
        val now = ServiceDayResolver.resolve(tehranMillis(2026, 10, 17, 0, 30), ops)
        assertEquals("2026-10-16", now.serviceDate)
        assertEquals(DayType.FRIDAY, now.dayType)
        assertEquals(24 * 60 + 30, now.minuteOfServiceDay)

        val check = LastTrainCalculator(network, ops).check(route("tajrish", "mirdamad"), now)
        assertEquals(Status.MISSED, check.status)
    }

    @Test
    fun `the phone time zone does not affect the result`() {
        val original = TimeZone.getDefault()
        try {
            TimeZone.setDefault(TimeZone.getTimeZone("America/Los_Angeles"))
            val now = ServiceDayResolver.resolve(tehranMillis(2026, 10, 16, 22, 0), operations())
            assertEquals(DayType.FRIDAY, now.dayType)
            assertEquals(time(22), now.minuteOfServiceDay)
        } finally {
            TimeZone.setDefault(original)
        }
    }

    // --- Bundled data file ---

    @Test
    fun `operations file covers every line and direction and parses`() {
        val ops = OperationsParser.parse(File("src/main/assets/metro_operations.json").readText())
        assertEquals("Asia/Tehran", ops.timeZoneId)
        network.lines.forEach { line ->
            listOf(line.firstStationId, line.lastStationId).forEach { toward ->
                val direction = ops.direction(line.id, toward)
                assertNotNull("${line.id} toward $toward missing", direction)
                val lastTrain = direction!!.lastTrain
                assertNotNull(lastTrain)
                DayType.entries.forEach { assertNotNull("${line.id}→$toward $it", lastTrain!!.departureFor(it)) }
                val cars = direction.womenCars
                assertNotNull("${line.id}→$toward women cars", cars)
                assertTrue(cars!!.womenOnlyCars.all { it in 1..cars.carsPerTrain })
            }
        }
    }

    @Test
    fun `times after midnight can be written past 24`() {
        assertEquals(24 * 60 + 15, OperationsParser.parseTime("24:15"))
        assertEquals(time(22, 5), OperationsParser.parseTime(" 22:05 "))
        assertNull(OperationsParser.parseTime("22.05"))
        assertNull(OperationsParser.parseTime(null))
    }
}
