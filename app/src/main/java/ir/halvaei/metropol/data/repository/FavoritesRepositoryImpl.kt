package ir.halvaei.metropol.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import ir.halvaei.metropol.data.dto.FavoriteDto
import ir.halvaei.metropol.domain.model.FavoriteRoute
import ir.halvaei.metropol.domain.model.RoutePreference
import ir.halvaei.metropol.domain.repository.FavoritesRepository
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.favoritesStore: DataStore<Preferences> by preferencesDataStore(name = "metropool_favorites")

class FavoritesRepositoryImpl(context: Context) : FavoritesRepository {
    private val store = context.applicationContext.favoritesStore
    private val gson = Gson()
    private val listType = object : TypeToken<List<FavoriteDto>>() {}.type

    override val favorites: Flow<List<FavoriteRoute>> = store.data.map { decode(it[FAVORITES]) }

    override suspend fun save(favorite: FavoriteRoute) {
        store.edit { prefs ->
            val current = decode(prefs[FAVORITES])
            val updated = if (current.any { it.id == favorite.id }) {
                current.map { if (it.id == favorite.id) favorite else it }
            } else {
                current + favorite
            }
            prefs[FAVORITES] = encode(updated)
        }
    }

    override suspend fun remove(id: String) {
        store.edit { prefs -> prefs[FAVORITES] = encode(decode(prefs[FAVORITES]).filterNot { it.id == id }) }
    }

    private fun decode(json: String?): List<FavoriteRoute> {
        if (json.isNullOrBlank()) return emptyList()
        val dtos: List<FavoriteDto> = runCatching<List<FavoriteDto>> { gson.fromJson(json, listType) }.getOrNull().orEmpty()
        return dtos.mapNotNull { dto ->
            val origin = dto.originId ?: return@mapNotNull null
            val destination = dto.destinationId ?: return@mapNotNull null
            FavoriteRoute(
                id = dto.id ?: "$origin>$destination",
                label = dto.label.orEmpty(),
                originId = origin,
                destinationId = destination,
                preference = dto.preference?.let { runCatching { RoutePreference.valueOf(it) }.getOrNull() }
                    ?: RoutePreference.FASTEST,
            )
        }
    }

    private fun encode(list: List<FavoriteRoute>): String = gson.toJson(
        list.map { FavoriteDto(it.id, it.label, it.originId, it.destinationId, it.preference.name) },
    )

    private companion object {
        val FAVORITES = stringPreferencesKey("favorites")
    }
}
