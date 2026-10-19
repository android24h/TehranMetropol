package com.example.tehranmetropoll.domain.model

/** Which timetable applies on a given service day. */
enum class DayType { WEEKDAY, FRIDAY, HOLIDAY }

/**
 * Last train of one line in one direction. Times are minutes after midnight of the service day
 * (values past 1440 mean after midnight) and describe the departure from the starting terminal.
 */
data class LastTrainTimes(
    val weekday: Int?,
    val friday: Int?,
    val holiday: Int?,
    val verified: Boolean,
) {
    /** Holidays fall back to the Friday timetable when no holiday time is given. */
    fun departureFor(dayType: DayType): Int? = when (dayType) {
        DayType.WEEKDAY -> weekday
        DayType.FRIDAY -> friday
        DayType.HOLIDAY -> holiday ?: friday
    }
}

/** Women-only cars, numbered from the front of the train in its direction of travel (1 = first car). */
data class WomenCars(
    val carsPerTrain: Int,
    val womenOnlyCars: Set<Int>,
    val verified: Boolean,
) {
    enum class Position { FRONT, REAR, FRONT_AND_REAR, MIDDLE, MIXED }

    val position: Position
        get() {
            val n = carsPerTrain.toDouble()
            val third = n / 3.0
            val cars = womenOnlyCars
            fun inFront(car: Int) = car <= third
            fun inRear(car: Int) = car > n - third
            return when {
                cars.isEmpty() -> Position.MIXED
                cars.none { inFront(it) || inRear(it) } -> Position.MIDDLE
                cars.all { it <= n / 2 } -> Position.FRONT
                cars.all { it > n / 2 } -> Position.REAR
                cars.all { inFront(it) || inRear(it) } -> Position.FRONT_AND_REAR
                else -> Position.MIXED
            }
        }
}

data class DirectionOperations(
    val lineId: String,
    /** Terminal station id the train is heading to. */
    val towardStationId: String,
    val lastTrain: LastTrainTimes?,
    val womenCars: WomenCars?,
)

class MetroOperations(
    directions: List<DirectionOperations>,
    val holidays: Set<String>,
    val holidaysVerified: Boolean,
    /** Minute of the calendar day at which a new service day begins (earlier times count as the previous day). */
    val serviceDayStartMinute: Int,
    val stationEntryMinutes: Double,
    val warningMinutes: Int,
    val timeZoneId: String,
) {
    private val byKey = directions.associateBy { it.lineId to it.towardStationId }

    fun direction(lineId: String, towardStationId: String): DirectionOperations? = byKey[lineId to towardStationId]

    companion object {
        val EMPTY = MetroOperations(emptyList(), emptySet(), false, 3 * 60, 3.0, 45, "Asia/Tehran")
    }
}

/** A point in time expressed in the metro's own service day. */
data class ServiceTime(
    val epochMillis: Long,
    /** Calendar date (YYYY-MM-DD) the service day started on. */
    val serviceDate: String,
    val dayType: DayType,
    /** Minutes since midnight of [serviceDate]; may exceed 1440 shortly after midnight. */
    val minuteOfServiceDay: Int,
)

data class LastTrainCheck(
    val status: Status,
    /** Latest time to enter the origin station, in minutes of the service day. */
    val latestDepartureMinute: Int? = null,
    /** Minutes from now until [latestDepartureMinute]; negative when it has passed. */
    val minutesLeft: Int? = null,
    val dayType: DayType? = null,
    /** True when any timetable value used is still a placeholder. */
    val unverified: Boolean = false,
    /** The leg whose last train is the binding constraint. */
    val criticalLegIndex: Int? = null,
) {
    enum class Status { SAFE, AT_RISK, MISSED, UNKNOWN }
}

data class FavoriteRoute(
    val id: String,
    val label: String,
    val originId: String,
    val destinationId: String,
    val preference: RoutePreference,
)

data class LastTrainReminder(
    val originId: String,
    val destinationId: String,
    val preference: RoutePreference,
    val triggerAtMillis: Long,
    /** Latest departure as minute of the service day, for the notification text. */
    val latestDepartureMinute: Int,
    val leadMinutes: Int,
)
