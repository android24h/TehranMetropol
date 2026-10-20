package ir.halvaei.metropol.data.connectivity

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import ir.halvaei.metropol.domain.repository.ConnectivityRepository
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.conflate
import kotlinx.coroutines.flow.distinctUntilChanged

class ConnectivityRepositoryImpl(context: Context) : ConnectivityRepository {
    private val manager = context.applicationContext.getSystemService(ConnectivityManager::class.java)

    override val isOnline: Flow<Boolean> = callbackFlow {
        fun current(): Boolean = manager.getNetworkCapabilities(manager.activeNetwork)
            ?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true

        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) { trySend(current()) }
            override fun onLost(network: Network) { trySend(current()) }
            override fun onCapabilitiesChanged(network: Network, caps: NetworkCapabilities) { trySend(current()) }
        }
        trySend(current())
        val registered = runCatching { manager.registerDefaultNetworkCallback(callback) }.isSuccess
        awaitClose { if (registered) runCatching { manager.unregisterNetworkCallback(callback) } }
    }.conflate().distinctUntilChanged()
}
