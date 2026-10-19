package com.example.tehranmetropoll.presentation.ride

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider.AndroidViewModelFactory.Companion.APPLICATION_KEY
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.example.tehranmetropoll.MetroApplication
import com.example.tehranmetropoll.domain.model.Route
import com.example.tehranmetropoll.domain.repository.Clock
import com.example.tehranmetropoll.domain.usecase.CorrectTripPositionUseCase
import com.example.tehranmetropoll.domain.usecase.ObserveActiveTripUseCase
import com.example.tehranmetropoll.domain.usecase.RideProgress
import com.example.tehranmetropoll.domain.usecase.StopTripUseCase
import com.example.tehranmetropoll.domain.usecase.TrackRideProgressUseCase
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

data class RideUiState(
    val loading: Boolean = true,
    val route: Route? = null,
    val progress: RideProgress? = null,
    /** True while the tracking service follows this trip. */
    val tracking: Boolean = false,
)

/** Mirrors the trip followed by the tracking service; the service keeps running when this screen closes. */
class RideViewModel(
    observeActiveTrip: ObserveActiveTripUseCase,
    private val trackProgress: TrackRideProgressUseCase,
    private val stopTrip: StopTripUseCase,
    private val correctPosition: CorrectTripPositionUseCase,
    private val clock: Clock,
) : ViewModel() {

    private val _state = MutableStateFlow(RideUiState())
    val state: StateFlow<RideUiState> = _state.asStateFlow()

    init {
        viewModelScope.launch {
            observeActiveTrip().collectLatest { trip ->
                if (trip == null) {
                    // Tracking ended (arrived or stopped); keep showing the finished trip if we had one.
                    _state.update { s ->
                        val route = s.route
                        s.copy(
                            loading = false,
                            tracking = false,
                            progress = route?.let { trackProgress(it, it.totalMinutes) },
                        )
                    }
                    return@collectLatest
                }
                _state.update { it.copy(loading = false, route = trip.route, tracking = true) }
                while (isActive) {
                    val progress = trackProgress(trip.route, trip.session.elapsedMinutes(clock.nowMillis()))
                    _state.update { it.copy(progress = progress) }
                    delay(1_000)
                }
            }
        }
    }

    /** "I'm at this station": re-anchors the estimate; confirming the destination ends the trip. */
    fun onAtStation(stopIndex: Int) {
        val route = _state.value.route ?: return
        if (!_state.value.tracking) return
        if (stopIndex >= route.allStops.lastIndex) stopTrip() else correctPosition(route, stopIndex)
    }

    fun onStop() = stopTrip()

    companion object {
        val Factory = viewModelFactory {
            initializer {
                val c = (this[APPLICATION_KEY] as MetroApplication).container
                RideViewModel(c.observeActiveTrip, c.trackRideProgress, c.stopTrip, c.correctTripPosition, c.clock)
            }
        }
    }
}
