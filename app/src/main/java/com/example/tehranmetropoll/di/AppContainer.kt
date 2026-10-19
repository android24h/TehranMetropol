package com.example.tehranmetropoll.di

import android.content.Context
import com.example.tehranmetropoll.data.connectivity.ConnectivityRepositoryImpl
import com.example.tehranmetropoll.data.location.LocationRepositoryImpl
import com.example.tehranmetropoll.data.reminder.AlarmLastTrainReminderScheduler
import com.example.tehranmetropoll.data.repository.FavoritesRepositoryImpl
import com.example.tehranmetropoll.data.repository.LanguageRepositoryImpl
import com.example.tehranmetropoll.data.repository.MetroRepositoryImpl
import com.example.tehranmetropoll.data.repository.OperationsRepositoryImpl
import com.example.tehranmetropoll.data.repository.SettingsRepositoryImpl
import com.example.tehranmetropoll.data.time.TehranClock
import com.example.tehranmetropoll.data.trip.ServiceTripTrackingController
import com.example.tehranmetropoll.data.trip.TripNotifications
import com.example.tehranmetropoll.data.trip.TripSessionRepositoryImpl
import com.example.tehranmetropoll.domain.repository.Clock
import com.example.tehranmetropoll.domain.repository.ConnectivityRepository
import com.example.tehranmetropoll.domain.repository.FavoritesRepository
import com.example.tehranmetropoll.domain.repository.LanguageRepository
import com.example.tehranmetropoll.domain.repository.LocationRepository
import com.example.tehranmetropoll.domain.repository.MetroRepository
import com.example.tehranmetropoll.domain.repository.OperationsRepository
import com.example.tehranmetropoll.domain.repository.SettingsRepository
import com.example.tehranmetropoll.domain.repository.TripSessionRepository
import com.example.tehranmetropoll.domain.repository.TripTrackingController
import com.example.tehranmetropoll.domain.repository.WidgetUpdater
import com.example.tehranmetropoll.domain.usecase.AdvanceTripUseCase
import com.example.tehranmetropoll.domain.usecase.CheckLastTrainUseCase
import com.example.tehranmetropoll.domain.usecase.CorrectTripPositionUseCase
import com.example.tehranmetropoll.domain.usecase.EstimateCo2SavingUseCase
import com.example.tehranmetropoll.domain.usecase.EvaluateWakeAlertsUseCase
import com.example.tehranmetropoll.domain.usecase.FavoritesUseCase
import com.example.tehranmetropoll.domain.usecase.FindNearestStationUseCase
import com.example.tehranmetropoll.domain.usecase.FindRouteUseCase
import com.example.tehranmetropoll.domain.usecase.GetLanguageUseCase
import com.example.tehranmetropoll.domain.usecase.GetMetroNetworkUseCase
import com.example.tehranmetropoll.domain.usecase.GetWomenCarsUseCase
import com.example.tehranmetropoll.domain.usecase.LastTrainReminderUseCase
import com.example.tehranmetropoll.domain.usecase.ObserveActiveTripUseCase
import com.example.tehranmetropoll.domain.usecase.ObserveConnectivityUseCase
import com.example.tehranmetropoll.domain.usecase.ObserveThemeModeUseCase
import com.example.tehranmetropoll.domain.usecase.PlanRideAlertsUseCase
import com.example.tehranmetropoll.domain.usecase.RecentTripsUseCase
import com.example.tehranmetropoll.domain.usecase.SearchStationsUseCase
import com.example.tehranmetropoll.domain.usecase.SetLanguageUseCase
import com.example.tehranmetropoll.domain.usecase.SetThemeModeUseCase
import com.example.tehranmetropoll.domain.usecase.StartTripUseCase
import com.example.tehranmetropoll.domain.usecase.StopTripUseCase
import com.example.tehranmetropoll.domain.usecase.TrackRideProgressUseCase
import com.example.tehranmetropoll.domain.usecase.TutorialUseCase
import com.example.tehranmetropoll.widget.GlanceWidgetUpdater

/** Manual dependency graph – keeps the build free of annotation processors. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    // Data
    private val metroRepository: MetroRepository by lazy { MetroRepositoryImpl(appContext) }
    private val operationsRepository: OperationsRepository by lazy { OperationsRepositoryImpl(appContext) }
    private val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(appContext) }
    private val favoritesRepository: FavoritesRepository by lazy { FavoritesRepositoryImpl(appContext) }
    private val languageRepository: LanguageRepository by lazy { LanguageRepositoryImpl() }
    private val locationRepository: LocationRepository by lazy { LocationRepositoryImpl(appContext) }
    private val connectivityRepository: ConnectivityRepository by lazy { ConnectivityRepositoryImpl(appContext) }
    private val tripController: TripTrackingController by lazy { ServiceTripTrackingController(appContext) }
    private val widgetUpdater: WidgetUpdater by lazy { GlanceWidgetUpdater(appContext) }
    val tripSessions: TripSessionRepository by lazy { TripSessionRepositoryImpl(appContext) }
    val lastTrainReminderScheduler by lazy { AlarmLastTrainReminderScheduler(appContext) }
    val clock: Clock = TehranClock

    // Use cases
    val getMetroNetwork by lazy { GetMetroNetworkUseCase(metroRepository) }
    val searchStations by lazy { SearchStationsUseCase(metroRepository) }
    val findRoute by lazy { FindRouteUseCase(metroRepository, clock) }
    val findNearestStation by lazy { FindNearestStationUseCase(metroRepository, locationRepository) }
    val estimateCo2Saving by lazy { EstimateCo2SavingUseCase() }
    val observeThemeMode by lazy { ObserveThemeModeUseCase(settingsRepository) }
    val setThemeMode by lazy { SetThemeModeUseCase(settingsRepository) }
    val getLanguage by lazy { GetLanguageUseCase(languageRepository) }
    val setLanguage by lazy { SetLanguageUseCase(languageRepository) }
    val tutorial by lazy { TutorialUseCase(settingsRepository) }
    val recentTrips by lazy { RecentTripsUseCase(settingsRepository) }
    val favorites by lazy { FavoritesUseCase(favoritesRepository, widgetUpdater) }
    val observeConnectivity by lazy { ObserveConnectivityUseCase(connectivityRepository) }

    // Last train & women-only cars
    val checkLastTrain by lazy { CheckLastTrainUseCase(metroRepository, operationsRepository, clock) }
    val getWomenCars by lazy { GetWomenCarsUseCase(operationsRepository) }
    val lastTrainReminder by lazy { LastTrainReminderUseCase(lastTrainReminderScheduler, clock) }

    // Trip tracking ("wake me up")
    private val planRideAlerts by lazy { PlanRideAlertsUseCase() }
    val trackRideProgress by lazy { TrackRideProgressUseCase() }
    val advanceTrip by lazy { AdvanceTripUseCase(trackRideProgress, planRideAlerts, EvaluateWakeAlertsUseCase()) }
    val startTrip by lazy { StartTripUseCase(tripSessions, tripController, clock) }
    val stopTrip by lazy { StopTripUseCase(tripSessions, tripController) }
    val correctTripPosition by lazy { CorrectTripPositionUseCase(tripSessions, planRideAlerts, clock) }
    val observeActiveTrip by lazy { ObserveActiveTripUseCase(tripSessions, findRoute) }

    /** Widgets and notification channels carry text, so redraw them in the new language. */
    fun onLanguageChanged() {
        widgetUpdater.refresh()
        TripNotifications.ensureChannels(appContext)
    }
}
