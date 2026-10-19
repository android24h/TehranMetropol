package com.example.tehranmetropoll.presentation.route

import androidx.annotation.StringRes
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.createSavedStateHandle
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tehranmetropoll.MetroApplication
import com.example.tehranmetropoll.R
import com.example.tehranmetropoll.domain.model.FavoriteRoute
import com.example.tehranmetropoll.domain.model.LastTrainCheck
import com.example.tehranmetropoll.domain.model.LastTrainReminder
import com.example.tehranmetropoll.domain.model.MetroNetwork
import com.example.tehranmetropoll.domain.model.Route
import com.example.tehranmetropoll.domain.model.RoutePreference
import com.example.tehranmetropoll.domain.model.WomenCars
import com.example.tehranmetropoll.domain.usecase.CheckLastTrainUseCase
import com.example.tehranmetropoll.domain.usecase.EstimateCo2SavingUseCase
import com.example.tehranmetropoll.domain.usecase.FavoritesUseCase
import com.example.tehranmetropoll.domain.usecase.FindRouteUseCase
import com.example.tehranmetropoll.domain.usecase.GetWomenCarsUseCase
import com.example.tehranmetropoll.domain.usecase.LastTrainReminderUseCase
import com.example.tehranmetropoll.domain.usecase.ObserveConnectivityUseCase
import com.example.tehranmetropoll.domain.usecase.RouteResult
import com.example.tehranmetropoll.domain.usecase.StartTripUseCase
import com.example.tehranmetropoll.presentation.navigation.Destinations
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
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
    observeConnectivity: ObserveConnectivityUseCase,
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

    private val _events = Channel<RouteEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var lastTrainJob: Job? = null

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
            (result as? RouteResult.Found)?.let { watchLastTrain(it.route) }
        }
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
                    observeConnectivity = c.observeConnectivity,
                )
            }
        }
    }
}
