package ir.halvaei.metropol.data.directions

import android.util.Log
import ir.halvaei.metropol.BuildConfig
import ir.halvaei.metropol.data.dto.NeshanDirectionDto
import ir.halvaei.metropol.domain.model.LatLng
import ir.halvaei.metropol.domain.repository.CarDirectionsRepository
import com.google.gson.Gson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL
import java.util.Locale

/**
 * Traffic-aware car routing from the Neshan Direction API. The key comes from local.properties
 * via BuildConfig and is only ever sent in the Api-Key header – never put in URLs or logs.
 */
class NeshanDirectionsRepository(
    private val apiKey: String = BuildConfig.NESHAN_API_KEY,
    private val gson: Gson = Gson(),
) : CarDirectionsRepository {

    override suspend fun drivingMinutes(from: LatLng, to: LatLng): Result<Double> = withContext(Dispatchers.IO) {
        runCatching {
            check(apiKey.isNotBlank()) { "NESHAN_API_KEY is missing from local.properties" }
            val url = URL(
                String.format(
                    Locale.US, "%s?type=car&origin=%.6f,%.6f&destination=%.6f,%.6f",
                    ENDPOINT, from.lat, from.lng, to.lat, to.lng,
                ),
            )
            val connection = url.openConnection() as HttpURLConnection
            try {
                connection.connectTimeout = TIMEOUT_MS
                connection.readTimeout = TIMEOUT_MS
                connection.setRequestProperty("Api-Key", apiKey)
                connection.setRequestProperty("Accept", "application/json")
                val code = connection.responseCode
                if (code != HttpURLConnection.HTTP_OK) throw IOException("Neshan direction HTTP $code")
                val body = connection.inputStream.bufferedReader().use { it.readText() }
                val seconds = gson.fromJson(body, NeshanDirectionDto::class.java)
                    ?.routes?.firstOrNull()
                    ?.legs?.mapNotNull { it.duration?.value }
                    ?.takeIf { it.isNotEmpty() }
                    ?.sum()
                    ?: throw IOException("Neshan direction response has no duration")
                seconds / 60.0
            } finally {
                connection.disconnect()
            }
        }.onFailure { Log.w(TAG, "Car time unavailable: ${it.javaClass.simpleName}: ${it.message}") }
    }

    private companion object {
        const val TAG = "NeshanDirections"
        const val ENDPOINT = "https://api.neshan.org/v4/direction"
        const val TIMEOUT_MS = 10_000
    }
}
