package com.example.tehranmetropoll.domain.routing

import com.example.tehranmetropoll.domain.model.MetroLine
import com.example.tehranmetropoll.domain.model.MetroNetwork
import com.example.tehranmetropoll.domain.model.Route
import com.example.tehranmetropoll.domain.model.RouteLeg
import com.example.tehranmetropoll.domain.model.RoutePreference
import com.example.tehranmetropoll.domain.model.RouteStop
import com.example.tehranmetropoll.domain.model.ServiceStatus
import java.util.PriorityQueue

/**
 * Time-aware shortest path search over (station, line) states.
 *
 * Riding between adjacent stations costs the running time derived from distance and line speed
 * plus a dwell; boarding a line costs half its current headway; changing lines adds the walk
 * inside the interchange. [RoutePreference.FEWEST_TRANSFERS] adds a heavy virtual penalty per
 * change so the search prefers simpler trips, while reported times stay real.
 */
class RoutePlanner(private val network: MetroNetwork) {

    private data class Node(val stationId: String, val lineId: String)

    private val indexInLine: Map<String, Map<String, Int>> = network.lines.associate { line ->
        line.id to line.stationIds.withIndex().associate { it.value to it.index }
    }

    /** segmentMinutes[lineId][i] = running time between station i and i+1. */
    private val segmentMinutes: Map<String, DoubleArray> = network.lines.associate { line ->
        line.id to DoubleArray(line.stationIds.size - 1) { i -> TravelTimes.segmentMinutes(network, line, i) }
    }

    private fun segmentKm(line: MetroLine, i: Int): Double = TravelTimes.segmentKm(network, line, i)

    fun plan(
        originId: String,
        destinationId: String,
        preference: RoutePreference,
        departureMinuteOfDay: Int,
    ): Route? {
        if (originId == destinationId) return null
        val origin = network.stationOrNull(originId) ?: return null
        val destination = network.stationOrNull(destinationId) ?: return null

        val status = ServiceSchedule.status(departureMinuteOfDay)
        val multiplier = ServiceSchedule.headwayMultiplier(status)
        fun headway(line: MetroLine) = line.headwayMinutes * multiplier
        fun wait(line: MetroLine) = headway(line) / 2.0
        val transferPenalty = if (preference == RoutePreference.FEWEST_TRANSFERS) 30.0 else 0.0

        val cost = HashMap<Node, Double>()
        val previous = HashMap<Node, Node>()
        val queue = PriorityQueue<Pair<Node, Double>>(compareBy { it.second })

        fun relax(from: Node?, to: Node, newCost: Double) {
            if (newCost < (cost[to] ?: Double.MAX_VALUE)) {
                cost[to] = newCost
                if (from != null) previous[to] = from
                queue.add(to to newCost)
            }
        }

        network.linesAt(origin.id).forEach { relax(null, Node(origin.id, it.id), wait(it)) }

        var target: Node? = null
        while (queue.isNotEmpty()) {
            val (node, nodeCost) = queue.poll()!!
            if (nodeCost > (cost[node] ?: Double.MAX_VALUE)) continue
            if (node.stationId == destination.id) {
                target = node
                break
            }
            val line = network.line(node.lineId)
            val index = indexInLine.getValue(line.id).getValue(node.stationId)
            val segments = segmentMinutes.getValue(line.id)

            if (index > 0) {
                relax(node, Node(line.stationIds[index - 1], line.id), nodeCost + segments[index - 1])
            }
            if (index < line.stationIds.lastIndex) {
                relax(node, Node(line.stationIds[index + 1], line.id), nodeCost + segments[index])
            }
            for (other in network.linesAt(node.stationId)) {
                if (other.id == line.id) continue
                val change = network.transferWalkAt(node.stationId) + wait(other) + transferPenalty
                relax(node, Node(node.stationId, other.id), nodeCost + change)
            }
        }

        val end = target ?: return null
        val path = generateSequence(end) { previous[it] }.toList().asReversed()
        return buildRoute(path, preference, departureMinuteOfDay, status, ::wait, ::headway)
    }

    private fun buildRoute(
        path: List<Node>,
        preference: RoutePreference,
        departureMinuteOfDay: Int,
        status: ServiceStatus,
        wait: (MetroLine) -> Double,
        headway: (MetroLine) -> Double,
    ): Route {
        // Split the state path into runs on the same service.
        val runs = mutableListOf<MutableList<Node>>()
        for (node in path) {
            if (runs.isEmpty() || runs.last().first().lineId != node.lineId) runs.add(mutableListOf())
            runs.last().add(node)
        }
        // A run of length one is a change made at the very first or last station; drop it.
        val legsNodes = runs.filter { it.size > 1 }

        var clock = 0.0
        var distance = 0.0
        val legs = legsNodes.mapIndexed { legIndex, nodes ->
            val line = network.line(nodes.first().lineId)
            val indices = indexInLine.getValue(line.id)
            val segments = segmentMinutes.getValue(line.id)
            val walk = if (legIndex == 0) 0.0 else network.transferWalkAt(nodes.first().stationId)
            val waitMinutes = wait(line)
            clock += walk + waitMinutes

            val stops = ArrayList<RouteStop>(nodes.size)
            var rideMinutes = 0.0
            nodes.forEachIndexed { i, node ->
                if (i > 0) {
                    val a = indices.getValue(nodes[i - 1].stationId)
                    val b = indices.getValue(node.stationId)
                    val segment = segments[minOf(a, b)]
                    clock += segment
                    rideMinutes += segment
                    distance += segmentKm(line, minOf(a, b))
                }
                stops.add(RouteStop(network.station(node.stationId), clock))
            }

            val forward = indices.getValue(nodes.last().stationId) > indices.getValue(nodes.first().stationId)
            RouteLeg(
                line = line,
                stops = stops,
                direction = network.station(if (forward) line.lastStationId else line.firstStationId),
                walkMinutes = walk,
                waitMinutes = waitMinutes,
                effectiveHeadwayMinutes = headway(line),
                rideMinutes = rideMinutes,
            )
        }

        return Route(
            origin = legs.first().board,
            destination = legs.last().alight,
            legs = legs,
            totalMinutes = clock,
            distanceKm = distance,
            departureMinuteOfDay = departureMinuteOfDay,
            serviceStatus = status,
            preference = preference,
        )
    }
}
