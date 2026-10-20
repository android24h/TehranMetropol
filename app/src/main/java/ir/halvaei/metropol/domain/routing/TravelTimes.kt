package ir.halvaei.metropol.domain.routing

import ir.halvaei.metropol.domain.model.MetroLine
import ir.halvaei.metropol.domain.model.MetroNetwork

/** Running-time model shared by route planning and last-train calculations. */
object TravelTimes {
    private const val DWELL_MINUTES = 0.5
    private const val MIN_SEGMENT_MINUTES = 1.2

    fun segmentKm(network: MetroNetwork, line: MetroLine, i: Int): Double = Geo.distanceKm(
        network.station(line.stationIds[i]).location,
        network.station(line.stationIds[i + 1]).location,
    )

    /** Running time between station i and i+1 of [line], including the dwell. */
    fun segmentMinutes(network: MetroNetwork, line: MetroLine, i: Int): Double =
        (segmentKm(network, line, i) / line.speedKmh * 60.0 + DWELL_MINUTES).coerceAtLeast(MIN_SEGMENT_MINUTES)

    /**
     * Minutes a train heading toward [towardStationId] needs from its starting terminal to reach
     * [stationId]. Null when the station is not on the line or the direction is not a terminal.
     */
    fun minutesFromStartTerminal(
        network: MetroNetwork,
        line: MetroLine,
        towardStationId: String,
        stationId: String,
    ): Double? {
        val index = line.stationIds.indexOf(stationId)
        if (index < 0) return null
        val range = when (towardStationId) {
            line.lastStationId -> 0 until index
            line.firstStationId -> index until line.stationIds.lastIndex
            else -> return null
        }
        return range.sumOf { segmentMinutes(network, line, it) }
    }
}
