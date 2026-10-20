package ir.halvaei.metropol.domain.lasttrain

import ir.halvaei.metropol.domain.model.DayType
import ir.halvaei.metropol.domain.model.LastTrainCheck
import ir.halvaei.metropol.domain.model.MetroNetwork
import ir.halvaei.metropol.domain.model.MetroOperations
import ir.halvaei.metropol.domain.model.Route
import ir.halvaei.metropol.domain.model.ServiceTime
import ir.halvaei.metropol.domain.routing.TravelTimes
import java.util.Calendar
import java.util.Locale
import java.util.TimeZone
import kotlin.math.floor

/** Maps a wall-clock instant to the metro's service day in the operator's time zone (Tehran). */
object ServiceDayResolver {

    fun resolve(epochMillis: Long, operations: MetroOperations): ServiceTime {
        val calendar = Calendar.getInstance(TimeZone.getTimeZone(operations.timeZoneId), Locale.US)
        calendar.timeInMillis = epochMillis
        var minute = calendar.get(Calendar.HOUR_OF_DAY) * 60 + calendar.get(Calendar.MINUTE)
        if (minute < operations.serviceDayStartMinute) {
            // Shortly after midnight still belongs to the previous day's service.
            calendar.add(Calendar.DAY_OF_MONTH, -1)
            minute += MINUTES_PER_DAY
        }
        val date = String.format(
            Locale.US, "%04d-%02d-%02d",
            calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH) + 1, calendar.get(Calendar.DAY_OF_MONTH),
        )
        val dayType = when {
            date in operations.holidays -> DayType.HOLIDAY
            calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY -> DayType.FRIDAY
            else -> DayType.WEEKDAY
        }
        return ServiceTime(epochMillis, date, dayType, minute)
    }

    const val MINUTES_PER_DAY = 24 * 60
}

/**
 * Works out the latest moment a passenger can enter the origin station and still finish the
 * route on today's last trains, including every line change.
 *
 * For each leg the last train passes the boarding station at
 * `last departure from the starting terminal + running time to that station`. Walking backwards
 * from the final leg, an earlier leg must be boarded early enough to ride to the interchange and
 * walk to the next platform before that next deadline.
 */
class LastTrainCalculator(
    private val network: MetroNetwork,
    private val operations: MetroOperations,
) {

    fun check(route: Route, now: ServiceTime): LastTrainCheck {
        val legs = route.legs
        if (legs.isEmpty()) return LastTrainCheck(LastTrainCheck.Status.UNKNOWN)
        // An unverified holiday list means today's day type itself may be wrong.
        var unverified = !operations.holidaysVerified

        // Time the last train of each leg leaves that leg's boarding station.
        val lastPass = DoubleArray(legs.size)
        legs.forEachIndexed { index, leg ->
            val times = operations.direction(leg.line.id, leg.direction.id)?.lastTrain
                ?: return unknown(now)
            val departure = times.departureFor(now.dayType) ?: return unknown(now)
            val running = TravelTimes.minutesFromStartTerminal(network, leg.line, leg.direction.id, leg.board.id)
                ?: return unknown(now)
            if (!times.verified) unverified = true
            lastPass[index] = departure + running
        }

        var latestBoard = lastPass.last()
        var critical = legs.lastIndex
        for (i in legs.lastIndex - 1 downTo 0) {
            val toCatchNext = latestBoard - legs[i + 1].walkMinutes - legs[i].rideMinutes
            if (lastPass[i] <= toCatchNext) {
                latestBoard = lastPass[i]
                critical = i
            } else {
                latestBoard = toCatchNext
            }
        }

        val latestDeparture = floor(latestBoard - operations.stationEntryMinutes).toInt()
        val minutesLeft = latestDeparture - now.minuteOfServiceDay
        val status = when {
            minutesLeft < 0 -> LastTrainCheck.Status.MISSED
            minutesLeft <= operations.warningMinutes -> LastTrainCheck.Status.AT_RISK
            else -> LastTrainCheck.Status.SAFE
        }
        return LastTrainCheck(
            status = status,
            latestDepartureMinute = latestDeparture,
            minutesLeft = minutesLeft,
            dayType = now.dayType,
            unverified = unverified,
            criticalLegIndex = critical,
        )
    }

    private fun unknown(now: ServiceTime) = LastTrainCheck(LastTrainCheck.Status.UNKNOWN, dayType = now.dayType)
}
