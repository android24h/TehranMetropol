package ir.halvaei.metropol.presentation.route

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ir.halvaei.metropol.MetroApplication
import ir.halvaei.metropol.R
import ir.halvaei.metropol.domain.model.FavoriteRoute
import ir.halvaei.metropol.domain.model.LastTrainCheck
import ir.halvaei.metropol.domain.model.LastTrainReminder
import ir.halvaei.metropol.domain.model.MetroNetwork
import ir.halvaei.metropol.domain.model.Route
import ir.halvaei.metropol.domain.model.RoutePreference
import ir.halvaei.metropol.domain.model.WomenCars
import ir.halvaei.metropol.domain.usecase.CheckLastTrainUseCase
import ir.halvaei.metropol.domain.usecase.EstimateCarMinutesUseCase
import ir.halvaei.metropol.domain.usecase.EstimateCo2SavingUseCase
import ir.halvaei.metropol.domain.usecase.FavoritesUseCase
import ir.halvaei.metropol.domain.usecase.FindRouteUseCase
import ir.halvaei.metropol.domain.usecase.GetWomenCarsUseCase
import ir.halvaei.metropol.domain.usecase.LastTrainReminderUseCase
import ir.halvaei.metropol.domain.usecase.ObserveConnectivityUseCase
import ir.halvaei.metropol.domain.usecase.RouteResult
import ir.halvaei.metropol.domain.usecase.StartTripUseCase
import ir.halvaei.metropol.presentation.navigation.Destinations
import ir.halvaei.metropol.presentation.util.ceilMinutes
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

sealed interface RouteUiState {
    data object Loading : RouteUiState
    data class Success(
        val route: Route,
        val network: MetroNetwork,
        val co2SavedKg: Double,
        /** Women-only car layout per leg (null where the data file has none). */
        val womenCars: List<WomenCars?> = emptyList(),
    ) : RouteUiState
    data object NotFound : RouteUiState
}

/** Car half of the metro-vs-car comparison. */
sealed interface CarTimeState {
    data object Loading : CarTimeState
    data class Ready(val minutes: Int) : CarTimeState
    data object Offline : CarTimeState
    data object Failed : CarTimeState
}

data class TravelComparison(val metroMinutes: Int, val car: CarTimeState)

sealed interface RouteEvent {
    data class Message(@StringRes val text: Int, val minuteOfDay: Int? = null) : RouteEvent
    data object TripStarted : RouteEvent
}

/** Shared by the route summary and the route map screens; both receive the same arguments. */
class RouteViewModel(
    private val handle: SavedStateHandle,
    private val findRoute: FindRouteUseCase,
    private val estimateCo2: EstimateCo2SavingUseCase,
    private val getWomenCars: GetWomenCarsUseCase,
    private val checkLastTrain: CheckLastTrainUseCase,
    private val reminders: LastTrainReminderUseCase,
    private val favorites: FavoritesUseCase,
    private val startTrip: StartTripUseCase,
    private val estimateCarMinutes: EstimateCarMinutesUseCase,
    private val observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    private val originId: String = checkNotNull(handle[Destinations.ARG_ORIGIN])
    private val destinationId: String = checkNotNull(handle[Destinations.ARG_DESTINATION])

    private val _preference = MutableStateFlow(
        handle.get<String>(Destinations.ARG_PREFERENCE)
            ?.let { runCatching { RoutePreference.valueOf(it) }.getOrNull() }
            ?: RoutePreference.FASTEST,
    )
    val preference: StateFlow<RoutePreference> = _preference.asStateFlow()

    private val _state = MutableStateFlow<RouteUiState>(RouteUiState.Loading)
    val state: StateFlow<RouteUiState> = _state.asStateFlow()

    /** Re-evaluated every half minute so the countdown stays current while the screen is open. */
    private val _lastTrain = MutableStateFlow<LastTrainCheck?>(null)
    val lastTrain: StateFlow<LastTrainCheck?> = _lastTrain.asStateFlow()

    val favorite: StateFlow<FavoriteRoute?> = favorites.favorites
        .map { list -> list.firstOrNull { it.id == favorites.idFor(originId, destinationId) } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    /** The pending reminder, if it belongs to this trip. */
    val reminder: StateFlow<LastTrainReminder?> = reminders.active
        .map { r -> r?.takeIf { it.originId == originId && it.destinationId == destinationId } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    val isOnline: StateFlow<Boolean> = observeConnectivity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _comparison = MutableStateFlow<TravelComparison?>(null)
    val comparison: StateFlow<TravelComparison?> = _comparison.asStateFlow()

    private val _events = Channel<RouteEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lastTrainJob: Job? = null
    private var carJob: Job? = null

    init {
        load()
    }

    fun onPreferenceChange(preference: RoutePreference) {
        if (preference == _preference.value) return
        _preference.value = preference
        handle[Destinations.ARG_PREFERENCE] = preference.name
        load()
    }

    private fun load() {
        viewModelScope.launch {
            val result = findRoute(originId, destinationId, _preference.value)
            _state.value = when (result) {
                is RouteResult.Found -> RouteUiState.Success(
                    route = result.route,
                    network = result.network,
                    co2SavedKg = estimateCo2(result.route),
                    womenCars = getWomenCars(result.route),
                )
                RouteResult.NotFound, RouteResult.SameStation -> RouteUiState.NotFound
            }
            (result as? RouteResult.Found)?.let {
                watchLastTrain(it.route)
                compareWithCar(it.route)
            }
        }
    }

    /**
     * Metro time is the route's own total, rounded exactly like the header, so the two always
     * match. The car time only depends on the two stations, so it is fetched once and retried
     * whenever the device comes back online.
     */
    private fun compareWithCar(route: Route) {
        val metroMinutes = route.totalMinutes.ceilMinutes()
        _comparison.update { TravelComparison(metroMinutes, it?.car ?: CarTimeState.Loading) }
        if (carJob != null) return
        carJob = viewModelScope.launch {
            observeConnectivity().collectLatest { online ->
                if (_comparison.value?.car is CarTimeState.Ready) return@collectLatest
                if (!online) {
                    setCarTime(CarTimeState.Offline)
                    return@collectLatest
                }
                setCarTime(CarTimeState.Loading)
                val car = estimateCarMinutes(route).fold(
                    onSuccess = { CarTimeState.Ready(it) },
                    onFailure = { CarTimeState.Failed },
                )
                setCarTime(car)
            }
        }
    }

    private fun setCarTime(car: CarTimeState) {
        _comparison.update { it?.copy(car = car) }
    }

    private fun watchLastTrain(route: Route) {
        lastTrainJob?.cancel()
        lastTrainJob = viewModelScope.launch {
            while (isActive) {
                _lastTrain.value = checkLastTrain(route)
                delay(LAST_TRAIN_REFRESH_MS)
            }
        }
    }

    private val currentRoute: Route? get() = (_state.value as? RouteUiState.Success)?.route

    fun reminderLeads(): List<Int> = _lastTrain.value?.let(reminders::availableLeads).orEmpty()

    fun onSetReminder(leadMinutes: Int) {
        val route = currentRoute ?: return
        val check = _lastTrain.value ?: return
        val scheduled = reminders.schedule(route, check, leadMinutes)
        viewModelScope.launch {
            _events.send(
                if (scheduled) {
                    RouteEvent.Message(R.string.reminder_scheduled, check.latestDepartureMinute?.minus(leadMinutes))
                } else {
                    RouteEvent.Message(R.string.reminder_too_late)
                },
            )
        }
    }

    fun onCancelReminder() = reminders.cancel()

    fun onSaveFavorite(label: String) {
        viewModelScope.launch {
            favorites.save(label, originId, destinationId, _preference.value)
            _events.send(RouteEvent.Message(R.string.favorite_saved))
        }
    }

    fun onRemoveFavorite() {
        viewModelScope.launch {
            favorites.remove(favorites.idFor(originId, destinationId))
            _events.send(RouteEvent.Message(R.string.favorite_removed))
        }
    }

    fun onStartTrip() {
        val route = currentRoute ?: return
        startTrip(route)
        viewModelScope.launch { _events.send(RouteEvent.TripStarted) }
    }

    companion object {
        private const val LAST_TRAIN_REFRESH_MS = 30_000L

        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as MetroApplication).container
                RouteViewModel(
                    handle = createSavedStateHandle(),
                    findRoute = c.findRoute,
                    estimateCo2 = c.estimateCo2Saving,
                    getWomenCars = c.getWomenCars,
                    checkLastTrain = c.checkLastTrain,
                    reminders = c.lastTrainReminder,
                    favorites = c.favorites,
                    startTrip = c.startTrip,
                    estimateCarMinutes = c.estimateCarMinutes,
                    observeConnectivity = c.observeConnectivity,
                )
            }
        }
    }
}
