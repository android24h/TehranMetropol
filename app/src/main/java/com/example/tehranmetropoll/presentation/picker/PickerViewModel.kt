package com.example.tehranmetropoll.presentation.picker

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
import com.example.tehranmetropoll.domain.model.LatLng
import com.example.tehranmetropoll.domain.model.MetroNetwork
import com.example.tehranmetropoll.domain.model.Station
import com.example.tehranmetropoll.domain.usecase.FindNearestStationUseCase
import com.example.tehranmetropoll.domain.usecase.GetMetroNetworkUseCase
import com.example.tehranmetropoll.domain.usecase.ObserveConnectivityUseCase
import com.example.tehranmetropoll.presentation.navigation.Destinations
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class PickerUiState(
    val network: MetroNetwork? = null,
    val selected: Station? = null,
    val origin: Station? = null,
    val destination: Station? = null,
    val locating: Boolean = false,
    val userLocation: LatLng? = null,
)

sealed interface PickerEvent {
    data class Message(@StringRes val text: Int, val station: Station? = null, val distanceKm: Double = 0.0) : PickerEvent
    data class Focus(val station: Station) : PickerEvent
}

class PickerViewModel(
    handle: SavedStateHandle,
    private val getNetwork: GetMetroNetworkUseCase,
    private val findNearest: FindNearestStationUseCase,
    observeConnectivity: ObserveConnectivityUseCase,
) : ViewModel() {

    val isOnline: StateFlow<Boolean> = observeConnectivity()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    private val _state = MutableStateFlow(PickerUiState())
    val state: StateFlow<PickerUiState> = _state.asStateFlow()

    private val _events = Channel<PickerEvent>(Channel.BUFFERED)
    val events = _events.receiveAsFlow()

    init {
        val originId = handle.get<String>(Destinations.ARG_ORIGIN)
        val destinationId = handle.get<String>(Destinations.ARG_DESTINATION)
        viewModelScope.launch {
            val net = getNetwork()
            _state.update {
                it.copy(
                    network = net,
                    origin = net.stationOrNull(originId),
                    destination = net.stationOrNull(destinationId),
                )
            }
        }
    }

    fun onStationTapped(station: Station) = _state.update { it.copy(selected = station) }

    fun onDismissSelection() = _state.update { it.copy(selected = null) }

    fun onSetOrigin() = _state.update { s ->
        s.copy(origin = s.selected, destination = s.destination.takeUnless { it?.id == s.selected?.id }, selected = null)
    }

    fun onSetDestination() = _state.update { s ->
        s.copy(destination = s.selected, origin = s.origin.takeUnless { it?.id == s.selected?.id }, selected = null)
    }

    fun onLocate() {
        if (_state.value.locating) return
        viewModelScope.launch {
            _state.update { it.copy(locating = true) }
            val nearest = findNearest()
            _state.update {
                it.copy(locating = false, userLocation = nearest?.userLocation, selected = nearest?.station ?: it.selected)
            }
            if (nearest == null) {
                _events.send(PickerEvent.Message(R.string.location_unavailable))
            } else {
                _events.send(PickerEvent.Focus(nearest.station))
                _events.send(PickerEvent.Message(R.string.nearest_station_found, nearest.station, nearest.distanceKm))
            }
        }
    }

    fun onLocationDenied() {
        viewModelScope.launch { _events.send(PickerEvent.Message(R.string.location_denied)) }
    }

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as MetroApplication).container
                PickerViewModel(createSavedStateHandle(), c.getMetroNetwork, c.findNearestStation, c.observeConnectivity)
            }
        }
    }
}
