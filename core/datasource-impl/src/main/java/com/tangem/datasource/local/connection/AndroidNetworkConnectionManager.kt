package com.tangem.datasource.local.connection

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.tangem.core.local.connection.NetworkConnectionManager
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import javax.inject.Inject

internal class AndroidNetworkConnectionManager @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
) : NetworkConnectionManager {

    override val isOnlineSync: Boolean
        get() = isNetworkAvailable()

    override val isOnline: Flow<Boolean> = callbackFlow {
        val connectivityManager = connectivityManager()
        val callback = object : ConnectivityManager.NetworkCallback() {
            override fun onAvailable(network: Network) {
                trySend(isNetworkAvailable())
            }

            override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                trySend(isNetworkAvailable())
            }

            override fun onCapabilitiesChanged(network: Network, networkCapabilities: NetworkCapabilities) {
                trySend(isNetworkAvailable())
            }

            override fun onLost(network: Network) {
                trySend(connectivityManager?.activeNetwork != network && isNetworkAvailable())
            }
        }

        trySend(isNetworkAvailable())

        val isRegistered = try {
            connectivityManager?.registerDefaultNetworkCallback(callback) != null
        } catch (e: RuntimeException) {
            TangemLogger.e("Network callback not registered, connection changes are not observed", e)
            false
        }

        awaitClose {
            if (isRegistered) runCatching { connectivityManager?.unregisterNetworkCallback(callback) }
        }
    }.distinctUntilChanged()

    private fun connectivityManager(): ConnectivityManager? =
        ContextCompat.getSystemService(applicationContext, ConnectivityManager::class.java)

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = connectivityManager()

        // Network that is currently in use (several networks can be presented at the same time)
        val defaultNetwork: Network = connectivityManager?.activeNetwork ?: return false
        val networkCapabilities: NetworkCapabilities =
            connectivityManager.getNetworkCapabilities(defaultNetwork) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}