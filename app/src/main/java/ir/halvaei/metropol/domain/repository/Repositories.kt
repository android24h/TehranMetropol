package ir.halvaei.metropol.domain.repository

import ir.halvaei.metropol.domain.model.AppLanguage
import ir.halvaei.metropol.domain.model.FavoriteRoute
import ir.halvaei.metropol.domain.model.LastTrainReminder
import ir.halvaei.metropol.domain.model.LatLng
import ir.halvaei.metropol.domain.model.MetroNetwork
import ir.halvaei.metropol.domain.model.MetroOperations
import ir.halvaei.metropol.domain.model.RecentTrip
import ir.halvaei.metropol.domain.model.ThemeMode
import ir.halvaei.metropol.domain.model.TripSession
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

interface MetroRepository {
    suspend fun getNetwork(): MetroNetwork
}

/** Timetable-like data (last trains, women-only cars) from assets/metro_operations.json. */
interface OperationsRepository {
    suspend fun getOperations(): MetroOperations
}

interface SettingsRepository {
    val themeMode: Flow<ThemeMode>
    val tutorialSeen: Flow<Boolean>
    val recentTrips: Flow<List<RecentTrip>>

    suspend fun setThemeMode(mode: ThemeMode)
    suspend fun setTutorialSeen()
    suspend fun addRecentTrip(trip: RecentTrip)
    suspend fun clearRecentTrips()
}

interface FavoritesRepository {
    val favorites: Flow<List<FavoriteRoute>>

    suspend fun save(favorite: FavoriteRoute)
    suspend fun remove(id: String)
}

interface LanguageRepository {
    fun current(): AppLanguage
    fun set(language: AppLanguage)

    /** Applies [AppLanguage.DEFAULT] the first time the app runs; later launches keep the user's choice. */
    fun applyDefaultOnFirstLaunch()
}

interface LocationRepository {
    /** Best effort current position; null when unavailable or permission is missing. */
    suspend fun currentLocation(): LatLng?
}

interface ConnectivityRepository {
    /** True while a network with internet access is available. */
    val isOnline: Flow<Boolean>
}

/** Road routing for a private car between two points. */
interface CarDirectionsRepository {
    /** Traffic-aware driving time in minutes; fails when offline or the service errors. */
    suspend fun drivingMinutes(from: LatLng, to: LatLng): Result<Double>
}

/** Persists the trip being tracked so the foreground service can survive process restarts. */
interface TripSessionRepository {
    val session: StateFlow<TripSession?>

    fun save(session: TripSession)
    fun clear()
}

/** Starts and stops the foreground service that follows a [TripSession]. */
interface TripTrackingController {
    fun start()
    fun stop()
}

interface LastTrainReminderScheduler {
    val active: Flow<LastTrainReminder?>

    fun schedule(reminder: LastTrainReminder)
    fun cancel()
}

/** Asks home-screen widgets to redraw after their data changed. */
fun interface WidgetUpdater {
    fun refresh()
}

/** Abstracts wall-clock access so routing stays deterministic in tests. */
fun interface Clock {
    /** Minute of the day in Tehran local time. */
    fun minuteOfDay(): Int

    fun nowMillis(): Long = System.currentTimeMillis()
}
