package com.example.tehranmetropoll.data.trip

import android.content.Context
import androidx.core.content.edit
import com.example.tehranmetropoll.domain.model.RoutePreference
import com.example.tehranmetropoll.domain.model.TripSession
import com.example.tehranmetropoll.domain.repository.TripSessionRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** SharedPreferences keep the session readable synchronously when the service is restarted. */
class TripSessionRepositoryImpl(context: Context) : TripSessionRepository {
    private val prefs = context.applicationContext.getSharedPreferences("trip_session", Context.MODE_PRIVATE)
    private val _session = MutableStateFlow(read())
    override val session: StateFlow<TripSession?> = _session.asStateFlow()

    override fun save(session: TripSession) {
        prefs.edit {
            putString(ORIGIN, session.originId)
            putString(DESTINATION, session.destinationId)
            putString(PREFERENCE, session.preference.name)
            putInt(DEPARTURE, session.departureMinuteOfDay)
            putLong(ANCHOR_MILLIS, session.anchorEpochMillis)
            putFloat(ANCHOR_OFFSET, session.anchorOffsetMinutes.toFloat())
            putStringSet(HANDLED, session.handledAlertKeys)
        }
        _session.value = session
    }

    override fun clear() {
        prefs.edit { clear() }
        _session.value = null
    }

    private fun read(): TripSession? {
        val origin = prefs.getString(ORIGIN, null) ?: return null
        val destination = prefs.getString(DESTINATION, null) ?: return null
        return TripSession(
            originId = origin,
            destinationId = destination,
            preference = runCatching { RoutePreference.valueOf(prefs.getString(PREFERENCE, null)!!) }
                .getOrDefault(RoutePreference.FASTEST),
            departureMinuteOfDay = prefs.getInt(DEPARTURE, 0),
            anchorEpochMillis = prefs.getLong(ANCHOR_MILLIS, System.currentTimeMillis()),
            anchorOffsetMinutes = prefs.getFloat(ANCHOR_OFFSET, 0f).toDouble(),
            handledAlertKeys = prefs.getStringSet(HANDLED, emptySet()).orEmpty().toSet(),
        )
    }

    private companion object {
        const val ORIGIN = "origin"
        const val DESTINATION = "destination"
        const val PREFERENCE = "preference"
        const val DEPARTURE = "departure"
        const val ANCHOR_MILLIS = "anchor_millis"
        const val ANCHOR_OFFSET = "anchor_offset"
        const val HANDLED = "handled_alerts"
    }
}
