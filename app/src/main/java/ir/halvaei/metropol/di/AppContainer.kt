package ir.halvaei.metropol.di

import android.content.Context
import ir.halvaei.metropol.data.connectivity.ConnectivityRepositoryImpl
import ir.halvaei.metropol.data.directions.NeshanDirectionsRepository
import ir.halvaei.metropol.data.location.LocationRepositoryImpl
import ir.halvaei.metropol.data.reminder.AlarmLastTrainReminderScheduler
import ir.halvaei.metropol.data.repository.FavoritesRepositoryImpl
import ir.halvaei.metropol.data.repository.LanguageRepositoryImpl
import ir.halvaei.metropol.data.repository.MetroRepositoryImpl
import ir.halvaei.metropol.data.repository.OperationsRepositoryImpl
import ir.halvaei.metropol.data.repository.SettingsRepositoryImpl
import ir.halvaei.metropol.data.time.TehranClock
import ir.halvaei.metropol.data.trip.ServiceTripTrackingController
import ir.halvaei.metropol.data.trip.TripNotifications
import ir.halvaei.metropol.data.trip.TripSessionRepositoryImpl
import ir.halvaei.metropol.domain.repository.CarDirectionsRepository
import ir.halvaei.metropol.domain.repository.Clock
import ir.halvaei.metropol.domain.repository.ConnectivityRepository
import ir.halvaei.metropol.domain.repository.FavoritesRepository
import ir.halvaei.metropol.domain.repository.LanguageRepository
import ir.halvaei.metropol.domain.repository.LocationRepository
import ir.halvaei.metropol.domain.repository.MetroRepository
import ir.halvaei.metropol.domain.repository.OperationsRepository
import ir.halvaei.metropol.domain.repository.SettingsRepository
import ir.halvaei.metropol.domain.repository.TripSessionRepository
import ir.halvaei.metropol.domain.repository.TripTrackingController
import ir.halvaei.metropol.domain.repository.WidgetUpdater
import ir.halvaei.metropol.domain.usecase.AdvanceTripUseCase
import ir.halvaei.metropol.domain.usecase.ApplyDefaultLanguageUseCase
import ir.halvaei.metropol.domain.usecase.CheckLastTrainUseCase
import ir.halvaei.metropol.domain.usecase.CorrectTripPositionUseCase
import ir.halvaei.metropol.domain.usecase.EstimateCarMinutesUseCase
import ir.halvaei.metropol.domain.usecase.EstimateCo2SavingUseCase
import ir.halvaei.metropol.domain.usecase.EvaluateWakeAlertsUseCase
import ir.halvaei.metropol.domain.usecase.FavoritesUseCase
import ir.halvaei.metropol.domain.usecase.FindNearestStationUseCase
import ir.halvaei.metropol.domain.usecase.FindRouteUseCase
import ir.halvaei.metropol.domain.usecase.GetLanguageUseCase
import ir.halvaei.metropol.domain.usecase.GetMetroNetworkUseCase
import ir.halvaei.metropol.domain.usecase.GetWomenCarsUseCase
import ir.halvaei.metropol.domain.usecase.LastTrainReminderUseCase
import ir.halvaei.metropol.domain.usecase.ObserveActiveTripUseCase
import ir.halvaei.metropol.domain.usecase.ObserveConnectivityUseCase
import ir.halvaei.metropol.domain.usecase.ObserveThemeModeUseCase
import ir.halvaei.metropol.domain.usecase.PlanRideAlertsUseCase
import ir.halvaei.metropol.domain.usecase.RecentTripsUseCase
import ir.halvaei.metropol.domain.usecase.SearchStationsUseCase
import ir.halvaei.metropol.domain.usecase.SetLanguageUseCase
import ir.halvaei.metropol.domain.usecase.SetThemeModeUseCase
import ir.halvaei.metropol.domain.usecase.StartTripUseCase
import ir.halvaei.metropol.domain.usecase.StopTripUseCase
import ir.halvaei.metropol.domain.usecase.TrackRideProgressUseCase
import ir.halvaei.metropol.domain.usecase.TutorialUseCase
import ir.halvaei.metropol.widget.GlanceWidgetUpdater

/** Manual dependency graph – keeps the build free of annotation processors. */
class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    // Data
    private val metroRepository: MetroRepository by lazy { MetroRepositoryImpl(appContext) }
    private val operationsRepository: OperationsRepository by lazy { OperationsRepositoryImpl(appContext) }
    private val settingsRepository: SettingsRepository by lazy { SettingsRepositoryImpl(appContext) }
    private val favoritesRepository: FavoritesRepository by lazy { FavoritesRepositoryImpl(appContext) }
    private val languageRepository: LanguageRepository by lazy { LanguageRepositoryImpl(appContext) }
    private val locationRepository: LocationRepository by lazy { LocationRepositoryImpl(appContext) }
    private val connectivityRepository: ConnectivityRepository by lazy { ConnectivityRepositoryImpl(appContext) }
    private val carDirectionsRepository: CarDirectionsRepository by lazy { NeshanDirectionsRepository() }
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
    val applyDefaultLanguage by lazy { ApplyDefaultLanguageUseCase(languageRepository) }
    val tutorial by lazy { TutorialUseCase(settingsRepository) }
    val recentTrips by lazy { RecentTripsUseCase(settingsRepository) }
    val favorites by lazy { FavoritesUseCase(favoritesRepository, widgetUpdater) }
    val observeConnectivity by lazy { ObserveConnectivityUseCase(connectivityRepository) }
    val estimateCarMinutes by lazy { EstimateCarMinutesUseCase(carDirectionsRepository) }

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
