package com.example.tehranmetropoll.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.tehranmetropoll.domain.model.RecentTrip
import com.example.tehranmetropoll.domain.model.ThemeMode
import com.example.tehranmetropoll.domain.repository.SettingsRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "metropool_settings")

class SettingsRepositoryImpl(context: Context) : SettingsRepository {
    private val store = context.applicationContext.dataStore

    override val themeMode: Flow<ThemeMode> = store.data.map { prefs ->
        prefs[THEME]?.let { runCatching { ThemeMode.valueOf(it) }.getOrNull() } ?: ThemeMode.SYSTEM
    }

    override val tutorialSeen: Flow<Boolean> = store.data.map { it[TUTORIAL_SEEN] ?: false }

    override val recentTrips: Flow<List<RecentTrip>> = store.data.map { prefs ->
        prefs[RECENT].orEmpty().split(TRIP_SEPARATOR).mapNotNull { entry ->
            val parts = entry.split(PAIR_SEPARATOR)
            if (parts.size == 2) RecentTrip(parts[0], parts[1]) else null
        }
    }

    override suspend fun setThemeMode(mode: ThemeMode) {
        store.edit { it[THEME] = mode.name }
    }

    override suspend fun setTutorialSeen() {
        store.edit { it[TUTORIAL_SEEN] = true }
    }

    override suspend fun addRecentTrip(trip: RecentTrip) {
        store.edit { prefs ->
            val current = prefs[RECENT].orEmpty().split(TRIP_SEPARATOR).filter { it.isNotBlank() }
            val entry = "${trip.originId}$PAIR_SEPARATOR${trip.destinationId}"
            prefs[RECENT] = (listOf(entry) + current.filterNot { it == entry })
                .take(MAX_RECENT)
                .joinToString(TRIP_SEPARATOR)
        }
    }

    override suspend fun clearRecentTrips() {
        store.edit { it.remove(RECENT) }
    }

    private companion object {
        val THEME = stringPreferencesKey("theme_mode")
        val TUTORIAL_SEEN = booleanPreferencesKey("tutorial_seen")
        val RECENT = stringPreferencesKey("recent_trips")
        const val TRIP_SEPARATOR = ";"
        const val PAIR_SEPARATOR = ">"
        const val MAX_RECENT = 5
    }
}
