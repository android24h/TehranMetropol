package ir.halvaei.metropol.domain.model

enum class RoutePreference { FASTEST, FEWEST_TRANSFERS }

enum class ServiceStatus { NORMAL, PEAK, LATE_NIGHT, CLOSED }

/** A stop on a route with its estimated time, measured in minutes from the moment the trip starts. */
data class RouteStop(
    val station: Station,
    val offsetMinutes: Double,
)

data class RouteLeg(
    val line: MetroLine,
    /** Boarding station through alighting station, inclusive. */
    val stops: List<RouteStop>,
    /** Terminal station the train is heading to – what the platform signs show. */
    val direction: Station,
    /** Walking time inside the station before boarding (0 for the first leg). */
    val walkMinutes: Double,
    /** Expected platform wait (half the current headway). */
    val waitMinutes: Double,
    val effectiveHeadwayMinutes: Double,
    val rideMinutes: Double,
) {
    val board: Station get() = stops.first().station
    val alight: Station get() = stops.last().station
    val stopCount: Int get() = stops.size - 1
    val departureOffset: Double get() = stops.first().offsetMinutes
    val arrivalOffset: Double get() = stops.last().offsetMinutes
}

data class Route(
    val origin: Station,
    val destination: Station,
    val legs: List<RouteLeg>,
    val totalMinutes: Double,
    val distanceKm: Double,
    val departureMinuteOfDay: Int,
    val serviceStatus: ServiceStatus,
    val preference: RoutePreference,
) {
    val transfers: Int get() = legs.size - 1
    val stopCount: Int get() = legs.sumOf { it.stopCount }
    val totalWaitMinutes: Double get() = legs.sumOf { it.waitMinutes }
    val arrivalMinuteOfDay: Int get() = (departureMinuteOfDay + totalMinutes.toInt()) % (24 * 60)

    /** Every station passed, in order, with transfer stations listed once. */
    val allStops: List<RouteStop>
        get() = legs.flatMapIndexed { index, leg -> if (index == 0) leg.stops else leg.stops.drop(1) }
}

data class RecentTrip(val originId: String, val destinationId: String)

enum class ThemeMode { SYSTEM, LIGHT, DARK }

enum class AppLanguage(val tag: String) {
    ENGLISH("en"), PERSIAN("fa");

    companion object {
        /** Used on first launch regardless of the phone's language. */
        val DEFAULT = PERSIAN
    }
}

/**
 * A "wake me up" moment: fired when the train reaches [triggerStation], the stop just before
 * the station where the passenger must get off ([station]).
 */
data class RideAlert(
    val type: Type,
    val legIndex: Int,
    val station: Station,
    val triggerStation: Station,
    /** Minutes from trip start when the train is expected at [triggerStation]. */
    val fireAtOffsetMinutes: Double,
    /** Minutes from trip start when the train is expected at [station]; past this the alert is stale. */
    val targetOffsetMinutes: Double,
    /** For transfers: the line to change to. */
    val nextLine: MetroLine? = null,
    val nextDirection: Station? = null,
) {
    enum class Type { TRANSFER, ARRIVAL }

    val key: String get() = "$legIndex:${station.id}"
}

/**
 * A tracked trip. Progress is estimated from the timetable: at [anchorEpochMillis] the passenger
 * was [anchorOffsetMinutes] into the route. Tapping "I'm at this station" moves the anchor.
 */
data class TripSession(
    val originId: String,
    val destinationId: String,
    val preference: RoutePreference,
    /** Departure minute the route was planned for, so the same route can be rebuilt later. */
    val departureMinuteOfDay: Int,
    val anchorEpochMillis: Long,
    val anchorOffsetMinutes: Double,
    /** Alerts already shown (or skipped because they were passed). */
    val handledAlertKeys: Set<String> = emptySet(),
) {
    fun elapsedMinutes(nowMillis: Long): Double =
        anchorOffsetMinutes + (nowMillis - anchorEpochMillis) / 60_000.0
}
