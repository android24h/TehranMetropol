package com.example.tehranmetropoll.domain.usecase

import com.example.tehranmetropoll.domain.lasttrain.LastTrainCalculator
import com.example.tehranmetropoll.domain.lasttrain.ServiceDayResolver
import com.example.tehranmetropoll.domain.model.FavoriteRoute
import com.example.tehranmetropoll.domain.model.LastTrainCheck
import com.example.tehranmetropoll.domain.model.LastTrainReminder
import com.example.tehranmetropoll.domain.model.Route
import com.example.tehranmetropoll.domain.model.RoutePreference
import com.example.tehranmetropoll.domain.model.WomenCars
import com.example.tehranmetropoll.domain.repository.Clock
import com.example.tehranmetropoll.domain.repository.ConnectivityRepository
import com.example.tehranmetropoll.domain.repository.FavoritesRepository
import com.example.tehranmetropoll.domain.repository.LastTrainReminderScheduler
import com.example.tehranmetropoll.domain.repository.MetroRepository
import com.example.tehranmetropoll.domain.repository.OperationsRepository
import com.example.tehranmetropoll.domain.repository.WidgetUpdater
import kotlinx.coroutines.flow.Flow

class CheckLastTrainUseCase(
    private val metro: MetroRepository,
    private val operations: OperationsRepository,
    private val clock: Clock,
) {
    suspend operator fun invoke(route: Route): LastTrainCheck {
        val ops = operations.getOperations()
        val now = ServiceDayResolver.resolve(clock.nowMillis(), ops)
        return LastTrainCalculator(metro.getNetwork(), ops).check(route, now)
    }
}

class GetWomenCarsUseCase(private val operations: OperationsRepository) {
    /** Women-only car layout for each leg of [route], or null where no data exists. */
    suspend operator fun invoke(route: Route): List<WomenCars?> {
        val ops = operations.getOperations()
        return route.legs.map { ops.direction(it.line.id, it.direction.id)?.womenCars }
    }
}

class FavoritesUseCase(
    private val repository: FavoritesRepository,
    private val widgets: WidgetUpdater,
) {
    val favorites: Flow<List<FavoriteRoute>> get() = repository.favorites

    suspend fun save(label: String, originId: String, destinationId: String, preference: RoutePreference) {
        repository.save(FavoriteRoute(idFor(originId, destinationId), label.trim(), originId, destinationId, preference))
        widgets.refresh()
    }

    suspend fun remove(id: String) {
        repository.remove(id)
        widgets.refresh()
    }

    fun idFor(originId: String, destinationId: String): String = "$originId>$destinationId"
}

class LastTrainReminderUseCase(
    private val scheduler: LastTrainReminderScheduler,
    private val clock: Clock,
) {
    val active: Flow<LastTrainReminder?> get() = scheduler.active

    /** Lead times (minutes before the latest departure) that are still in the future. */
    fun availableLeads(check: LastTrainCheck): List<Int> {
        val left = check.minutesLeft ?: return emptyList()
        return LEAD_OPTIONS.filter { it < left }
    }

    fun schedule(route: Route, check: LastTrainCheck, leadMinutes: Int): Boolean {
        val left = check.minutesLeft ?: return false
        val latest = check.latestDepartureMinute ?: return false
        if (leadMinutes >= left) return false
        scheduler.schedule(
            LastTrainReminder(
                originId = route.origin.id,
                destinationId = route.destination.id,
                preference = route.preference,
                triggerAtMillis = clock.nowMillis() + (left - leadMinutes) * 60_000L,
                latestDepartureMinute = latest,
                leadMinutes = leadMinutes,
            ),
        )
        return true
    }

    fun cancel() = scheduler.cancel()

    companion object {
        val LEAD_OPTIONS = listOf(10, 15, 30, 60)
    }
}

class ObserveConnectivityUseCase(private val connectivity: ConnectivityRepository) {
    operator fun invoke(): Flow<Boolean> = connectivity.isOnline
}
