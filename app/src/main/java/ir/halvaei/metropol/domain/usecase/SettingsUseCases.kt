package ir.halvaei.metropol.domain.usecase

import ir.halvaei.metropol.domain.model.AppLanguage
import ir.halvaei.metropol.domain.model.RecentTrip
import ir.halvaei.metropol.domain.model.ThemeMode
import ir.halvaei.metropol.domain.repository.LanguageRepository
import ir.halvaei.metropol.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow

class ObserveThemeModeUseCase(private val settings: SettingsRepository) {
    operator fun invoke(): Flow<ThemeMode> = settings.themeMode
}

class SetThemeModeUseCase(private val settings: SettingsRepository) {
    suspend operator fun invoke(mode: ThemeMode) = settings.setThemeMode(mode)
}

class GetLanguageUseCase(private val languages: LanguageRepository) {
    operator fun invoke(): AppLanguage = languages.current()
}

class SetLanguageUseCase(private val languages: LanguageRepository) {
    operator fun invoke(language: AppLanguage) = languages.set(language)
}

class ApplyDefaultLanguageUseCase(private val languages: LanguageRepository) {
    operator fun invoke() = languages.applyDefaultOnFirstLaunch()
}

class TutorialUseCase(private val settings: SettingsRepository) {
    val seen: Flow<Boolean> get() = settings.tutorialSeen
    suspend fun markSeen() = settings.setTutorialSeen()
}

class RecentTripsUseCase(private val settings: SettingsRepository) {
    val trips: Flow<List<RecentTrip>> get() = settings.recentTrips
    suspend fun add(originId: String, destinationId: String) =
        settings.addRecentTrip(RecentTrip(originId, destinationId))
    suspend fun clear() = settings.clearRecentTrips()
}
