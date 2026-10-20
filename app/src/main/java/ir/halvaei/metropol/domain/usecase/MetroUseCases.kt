package ir.halvaei.metropol.domain.usecase

import ir.halvaei.metropol.domain.model.LatLng
import ir.halvaei.metropol.domain.model.MetroNetwork
import ir.halvaei.metropol.domain.model.Route
import ir.halvaei.metropol.domain.model.RoutePreference
import ir.halvaei.metropol.domain.model.Station
import ir.halvaei.metropol.domain.repository.Clock
import ir.halvaei.metropol.domain.repository.LocationRepository
import ir.halvaei.metropol.domain.repository.MetroRepository
import ir.halvaei.metropol.domain.routing.Geo
import ir.halvaei.metropol.domain.routing.RoutePlanner
import ir.halvaei.metropol.domain.routing.StationMatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class GetMetroNetworkUseCase(private val repository: MetroRepository) {
    suspend operator fun invoke(): MetroNetwork = repository.getNetwork()
}

class SearchStationsUseCase(private val repository: MetroRepository) {
    suspend operator fun invoke(query: String, limit: Int = 8): List<Station> =
        StationMatcher.search(repository.getNetwork().stations, query, limit)

    suspend fun exactMatch(query: String): Station? =
        StationMatcher.exact(repository.getNetwork().stations, query)
}

sealed interface RouteResult {
    data class Found(val route: Route, val network: MetroNetwork) : RouteResult
    data object SameStation : RouteResult
    data object NotFound : RouteResult
}

class FindRouteUseCase(
    private val repository: MetroRepository,
    private val clock: Clock,
) {
    private val mutex = Mutex()
    private var planner: Pair<MetroNetwork, RoutePlanner>? = null

    private suspend fun planner(network: MetroNetwork): RoutePlanner = mutex.withLock {
        planner?.takeIf { it.first === network }?.second
            ?: RoutePlanner(network).also { planner = network to it }
    }

    suspend operator fun invoke(
        originId: String,
        destinationId: String,
        preference: RoutePreference = RoutePreference.FASTEST,
        departureMinuteOfDay: Int = clock.minuteOfDay(),
    ): RouteResult {
        if (originId == destinationId) return RouteResult.SameStation
        val network = repository.getNetwork()
        val route = planner(network).plan(originId, destinationId, preference, departureMinuteOfDay)
        return route?.let { RouteResult.Found(it, network) } ?: RouteResult.NotFound
    }
}

data class NearestStation(val station: Station, val distanceKm: Double, val userLocation: LatLng)

class FindNearestStationUseCase(
    private val repository: MetroRepository,
    private val locationRepository: LocationRepository,
) {
    suspend operator fun invoke(): NearestStation? {
        val here = locationRepository.currentLocation() ?: return null
        return repository.getNetwork().stations
            .map { it to Geo.distanceKm(here, it.location) }
            .minByOrNull { it.second }
            ?.let { (station, km) -> NearestStation(station, km, here) }
    }
}

/** Rough CO₂ saved by riding instead of driving a private car the same distance (kg). */
class EstimateCo2SavingUseCase {
    operator fun invoke(route: Route): Double =
        route.distanceKm * ROAD_DETOUR_FACTOR * (CAR_KG_PER_KM - METRO_KG_PER_KM)

    private companion object {
        const val ROAD_DETOUR_FACTOR = 1.3
        const val CAR_KG_PER_KM = 0.17
        const val METRO_KG_PER_KM = 0.03
    }
}
