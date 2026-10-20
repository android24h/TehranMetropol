package ir.halvaei.metropol.data.location

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.location.LocationManager
import androidx.core.content.ContextCompat
import androidx.core.location.LocationManagerCompat
import androidx.core.os.CancellationSignal
import ir.halvaei.metropol.domain.model.LatLng
import ir.halvaei.metropol.domain.repository.LocationRepository
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import kotlin.coroutines.resume

/** Platform LocationManager based provider – no Google Play Services dependency. */
class LocationRepositoryImpl(private val context: Context) : LocationRepository {

    private val manager = context.getSystemService(Context.LOCATION_SERVICE) as LocationManager

    private fun hasPermission(): Boolean = listOf(
        Manifest.permission.ACCESS_FINE_LOCATION,
        Manifest.permission.ACCESS_COARSE_LOCATION,
    ).any { ContextCompat.checkSelfPermission(context, it) == PackageManager.PERMISSION_GRANTED }

    @SuppressLint("MissingPermission")
    override suspend fun currentLocation(): LatLng? {
        if (!hasPermission()) return null
        val providers = manager.getProviders(true)
        val lastKnown = providers.mapNotNull { runCatching { manager.getLastKnownLocation(it) }.getOrNull() }
            .maxByOrNull { it.time }
        val fresh = if (lastKnown != null && System.currentTimeMillis() - lastKnown.time < FRESH_MS) {
            lastKnown
        } else {
            // Network location usually needs the internet, so offline we fall through to GPS.
            listOf(LocationManager.NETWORK_PROVIDER, LocationManager.GPS_PROVIDER)
                .filter { it in providers }
                .firstNotNullOfOrNull { withTimeoutOrNull(TIMEOUT_MS) { requestCurrent(it) } }
                ?: lastKnown
        }
        return fresh?.let { LatLng(it.latitude, it.longitude) }
    }

    @SuppressLint("MissingPermission")
    private suspend fun requestCurrent(provider: String): Location? =
        suspendCancellableCoroutine { cont ->
            val signal = CancellationSignal()
            cont.invokeOnCancellation { signal.cancel() }
            LocationManagerCompat.getCurrentLocation(
                manager, provider, signal, ContextCompat.getMainExecutor(context),
            ) { location -> if (cont.isActive) cont.resume(location) }
        }

    private companion object {
        const val FRESH_MS = 2 * 60 * 1000L
        const val TIMEOUT_MS = 10_000L
    }
}
