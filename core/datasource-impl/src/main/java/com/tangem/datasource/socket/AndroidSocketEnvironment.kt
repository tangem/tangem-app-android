package com.tangem.datasource.socket

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.ProcessLifecycleOwner
import com.tangem.core.socket.SocketEnvironment
import com.tangem.utils.coroutines.AppCoroutineScope
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import com.tangem.utils.logging.TangemLogger
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.shareIn
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class AndroidSocketEnvironment @Inject constructor(
    @ApplicationContext private val context: Context,
    dispatchers: CoroutineDispatcherProvider,
    appScope: AppCoroutineScope,
) : SocketEnvironment {

    private val sharing = SharingStarted.WhileSubscribed(replayExpirationMillis = 0)

    private val foreground: Flow<Boolean> = callbackFlow {
        val lifecycle = ProcessLifecycleOwner.get().lifecycle
        val observer = LifecycleEventObserver { _, _ ->
            trySend(lifecycle.currentState.isAtLeast(Lifecycle.State.STARTED))
        }
        lifecycle.addObserver(observer)
        awaitClose { lifecycle.removeObserver(observer) }
    }
        .flowOn(dispatchers.mainImmediate)
        .distinctUntilChanged()
        .shareIn(appScope, sharing, replay = 1)

    private val network: Flow<String?> = observeDefaultNetwork().shareIn(appScope, sharing, replay = 1)

    override fun isForeground(): Flow<Boolean> = foreground

    override fun defaultNetwork(): Flow<String?> = network

    private fun observeDefaultNetwork(): Flow<String?> {
        val connectivityManager = ContextCompat.getSystemService(context, ConnectivityManager::class.java)
            ?: return flowOf(UNKNOWN_NETWORK)

        return callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {
                private var isBlocked = false

                override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
                    trySend(networkId(network, capabilities, isBlocked))
                }

                override fun onBlockedStatusChanged(network: Network, blocked: Boolean) {
                    isBlocked = blocked
                    trySend(networkId(network, connectivityManager.capabilitiesOrNull(network), blocked))
                }

                override fun onLost(network: Network) {
                    trySend(connectivityManager.activeNetworkId().takeUnless { it == network.toString() })
                }
            }
            trySend(connectivityManager.activeNetworkId())
            val isRegistered = try {
                connectivityManager.registerDefaultNetworkCallback(callback)
                true
            } catch (e: RuntimeException) {
                TangemLogger.e("Socket network callback not registered, assuming online", e)
                trySend(UNKNOWN_NETWORK)
                false
            }
            awaitClose {
                if (isRegistered) {
                    runCatching { connectivityManager.unregisterNetworkCallback(callback) }
                }
            }
        }.distinctUntilChanged()
    }

    private fun ConnectivityManager.activeNetworkId(): String? = try {
        activeNetwork?.let { network -> networkId(network, getNetworkCapabilities(network), isBlocked = false) }
    } catch (e: SecurityException) {
        TangemLogger.e("Socket network state unreadable, assuming online", e)
        UNKNOWN_NETWORK
    }

    private fun ConnectivityManager.capabilitiesOrNull(network: Network): NetworkCapabilities? =
        runCatching { getNetworkCapabilities(network) }.getOrNull()

    private fun networkId(network: Network, capabilities: NetworkCapabilities?, isBlocked: Boolean): String? {
        val hasInternet = capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET) == true
        return if (isBlocked || !hasInternet) null else network.toString()
    }

    private companion object {
        const val UNKNOWN_NETWORK = "unknown"
    }
}