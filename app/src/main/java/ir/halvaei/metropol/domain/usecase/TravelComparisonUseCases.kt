package ir.halvaei.metropol.domain.usecase

import ir.halvaei.metropol.domain.model.Route
import ir.halvaei.metropol.domain.repository.CarDirectionsRepository
import kotlin.math.ceil

/** Driving time between the route's origin and destination stations, in whole minutes. */
class EstimateCarMinutesUseCase(private val repository: CarDirectionsRepository) {
    suspend operator fun invoke(route: Route): Result<Int> =
        repository.drivingMinutes(route.origin.location, route.destination.location)
            .map { ceil(it).toInt().coerceAtLeast(1) }
}
