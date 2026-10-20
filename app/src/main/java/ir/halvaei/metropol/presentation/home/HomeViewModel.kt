package ir.halvaei.metropol.presentation.home

import androidx.annotation.StringRes
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import ir.halvaei.metropol.MetroApplication
import ir.halvaei.metropol.R
import ir.halvaei.metropol.domain.model.FavoriteRoute
import ir.halvaei.metropol.domain.model.MetroLine
import ir.halvaei.metropol.domain.model.MetroNetwork
import ir.halvaei.metropol.domain.model.RoutePreference
import ir.halvaei.metropol.domain.model.Station
import ir.halvaei.metropol.domain.usecase.FavoritesUseCase
import ir.halvaei.metropol.domain.usecase.GetMetroNetworkUseCase
import ir.halvaei.metropol.domain.usecase.RecentTripsUseCase
import ir.halvaei.metropol.domain.usecase.SearchStationsUseCase
import ir.halvaei.metropol.domain.usecase.TutorialUseCase
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

enum class StationField { ORIGIN, DESTINATION }

data class LineSummary(val line: MetroLine, val first: Station, val last: Station)

data class FavoriteItem(val favorite: FavoriteRoute, val origin: Station, val destination: Station)

data class HomeUiState(
    val originQuery: String = "",
    val destinationQuery: String = "",
    val origin: Station? = null,
    val destination: Station? = null,
    val activeField: StationField? = null,
    val suggestions: List<Station> = emptyList(),
    val recentTrips: List<Pair<Station, Station>> = emptyList(),
    val favorites: List<FavoriteItem> = emptyList(),
    val lines: List<LineSummary> = emptyList(),
    val stationCount: Int = 0,
    val tutorialVisible: Boolean = false,
    /** The metro lines section starts collapsed and expands on tap. */
    val linesExpanded: Boolean = false,
) {
    val canSearch: Boolean
        get() = (origin != null || originQuery.isNotBlank()) &&
            (destination != null || destinationQuery.isNotBlank())
}

sealed interface HomeEvent {
    data class OpenRoute(
        val originId: String,
        val destinationId: String,
        val preference: RoutePreference = RoutePreference.FASTEST,
    ) : HomeEvent
    data class Message(@StringRes val text: Int) : HomeEvent
}

class HomeViewModel(
    private val getNetwork: GetMetroNetworkUseCase,
    private val searchStations: SearchStationsUseCase,
    private val recentTrips: RecentTripsUseCase,
    private val favorites: FavoritesUseCase,
    private val tutorial: TutorialUseCase,
) : ViewModel() {

    private val _state = MutableStateFlow(HomeUiState())
    val state: StateFlow<HomeUiState> = _state.asStateFlow()

    private val _events = Channel<HomeEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    private var network: MetroNetwork? = null
    private var searchJob: Job? = null

    init {
        viewModelScope.launch {
            val net = getNetwork().also { network = it }
            _state.update {
                it.copy(
                    lines = net.publicLines.map { l ->
                        LineSummary(l, net.station(l.firstStationId), net.station(l.lastStationId))
                    },
                    stationCount = net.stations.size,
                )
            }
            recentTrips.trips.collect { trips ->
                _state.update { s ->
                    s.copy(recentTrips = trips.mapNotNull { t ->
                        val o = net.stationOrNull(t.originId)
                        val d = net.stationOrNull(t.destinationId)
                        if (o != null && d != null) o to d else null
                    })
                }
            }
        }
        viewModelScope.launch {
            val net = getNetwork()
            favorites.favorites.collect { list ->
                _state.update { s ->
                    s.copy(favorites = list.mapNotNull { f ->
                        val o = net.stationOrNull(f.originId)
                        val d = net.stationOrNull(f.destinationId)
                        if (o != null && d != null) FavoriteItem(f, o, d) else null
                    })
                }
            }
        }
        viewModelScope.launch {
            if (!tutorial.seen.first()) _state.update { it.copy(tutorialVisible = true) }
        }
    }

    fun onQueryChange(field: StationField, text: String) {
        _state.update {
            when (field) {
                StationField.ORIGIN -> it.copy(originQuery = text, origin = null, activeField = field)
                StationField.DESTINATION -> it.copy(destinationQuery = text, destination = null, activeField = field)
            }
        }
        searchJob?.cancel()
        if (text.isBlank()) {
            _state.update { it.copy(suggestions = emptyList()) }
            return
        }
        searchJob = viewModelScope.launch {
            delay(120)
            val results = searchStations(text)
            _state.update { if (it.activeField == field) it.copy(suggestions = results) else it }
        }
    }

    fun onFieldFocused(field: StationField?) {
        _state.update {
            if (field == null) it.copy(activeField = null, suggestions = emptyList())
            else it.copy(activeField = field, suggestions = if (it.activeField == field) it.suggestions else emptyList())
        }
    }

    fun onSuggestionSelected(station: Station) {
        val field = _state.value.activeField ?: return
        setStation(field, station)
    }

    private fun setStation(field: StationField, station: Station?) {
        _state.update {
            val updated = when (field) {
                StationField.ORIGIN -> it.copy(origin = station, originQuery = "")
                StationField.DESTINATION -> it.copy(destination = station, destinationQuery = "")
            }
            updated.copy(suggestions = emptyList(), activeField = null)
        }
    }

    fun onClear(field: StationField) {
        setStation(field, null)
    }

    fun onSwap() {
        _state.update {
            it.copy(
                origin = it.destination, destination = it.origin,
                originQuery = it.destinationQuery, destinationQuery = it.originQuery,
                suggestions = emptyList(), activeField = null,
            )
        }
    }

    fun onPickedFromMap(originId: String?, destinationId: String?) {
        val net = network ?: return
        net.stationOrNull(originId)?.let { setStation(StationField.ORIGIN, it) }
        net.stationOrNull(destinationId)?.let { setStation(StationField.DESTINATION, it) }
    }

    fun onRecentTrip(origin: Station, destination: Station) {
        setStation(StationField.ORIGIN, origin)
        setStation(StationField.DESTINATION, destination)
        onFindRoute()
    }

    fun onFavorite(item: FavoriteItem) {
        viewModelScope.launch {
            send(HomeEvent.OpenRoute(item.origin.id, item.destination.id, item.favorite.preference))
        }
    }

    fun onRemoveFavorite(item: FavoriteItem) {
        viewModelScope.launch { favorites.remove(item.favorite.id) }
    }

    fun onClearRecent() {
        viewModelScope.launch { recentTrips.clear() }
    }

    fun onFindRoute() {
        viewModelScope.launch {
            val s = _state.value
            val origin = s.origin ?: searchStations.exactMatch(s.originQuery)
                ?: return@launch send(HomeEvent.Message(R.string.error_pick_origin))
            val destination = s.destination ?: searchStations.exactMatch(s.destinationQuery)
                ?: return@launch send(HomeEvent.Message(R.string.error_pick_destination))
            if (origin.id == destination.id) return@launch send(HomeEvent.Message(R.string.error_same_station))

            setStation(StationField.ORIGIN, origin)
            setStation(StationField.DESTINATION, destination)
            recentTrips.add(origin.id, destination.id)
            send(HomeEvent.OpenRoute(origin.id, destination.id))
        }
    }

    fun onToggleLines() = _state.update { it.copy(linesExpanded = !it.linesExpanded) }

    fun showTutorial() = _state.update { it.copy(tutorialVisible = true) }

    fun dismissTutorial() {
        _state.update { it.copy(tutorialVisible = false) }
        viewModelScope.launch { tutorial.markSeen() }
    }

    private suspend fun send(event: HomeEvent) = _events.send(event)

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as MetroApplication).container
                HomeViewModel(c.getMetroNetwork, c.searchStations, c.recentTrips, c.favorites, c.tutorial)
            }
        }
    }
}
