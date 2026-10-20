package ir.halvaei.metropol.core

import android.content.Context
import android.content.Intent
import android.net.Uri
import ir.halvaei.metropol.MainActivity
import ir.halvaei.metropol.domain.model.RoutePreference

/** In-app destinations that notifications and the home-screen widget can open directly. */
sealed interface AppLink {
    data class Route(val originId: String, val destinationId: String, val preference: RoutePreference) : AppLink
    data object Ride : AppLink
}

object AppLinks {
    private const val SCHEME = "metropool"
    private const val HOST_ROUTE = "route"
    private const val HOST_RIDE = "ride"
    private const val PARAM_PREFERENCE = "pref"

    fun routeUri(originId: String, destinationId: String, preference: RoutePreference): Uri =
        Uri.Builder().scheme(SCHEME).authority(HOST_ROUTE)
            .appendPath(originId).appendPath(destinationId)
            .appendQueryParameter(PARAM_PREFERENCE, preference.name)
            .build()

    val rideUri: Uri = Uri.Builder().scheme(SCHEME).authority(HOST_RIDE).build()

    /** Explicit intent for [MainActivity]; the URI keeps PendingIntents for different routes distinct. */
    fun intent(context: Context, uri: Uri): Intent =
        Intent(context, MainActivity::class.java)
            .setAction(Intent.ACTION_VIEW)
            .setData(uri)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)

    fun parse(uri: Uri?): AppLink? {
        if (uri == null || uri.scheme != SCHEME) return null
        return when (uri.host) {
            HOST_RIDE -> AppLink.Ride
            HOST_ROUTE -> {
                val segments = uri.pathSegments
                if (segments.size < 2) return null
                val preference = uri.getQueryParameter(PARAM_PREFERENCE)
                    ?.let { runCatching { RoutePreference.valueOf(it) }.getOrNull() }
                    ?: RoutePreference.FASTEST
                AppLink.Route(segments[0], segments[1], preference)
            }
            else -> null
        }
    }
}
