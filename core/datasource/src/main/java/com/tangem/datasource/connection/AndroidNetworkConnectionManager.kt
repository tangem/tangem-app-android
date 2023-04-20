package com.tangem.datasource.connection

import android.app.Activity
import android.app.Application
import android.app.Application.ActivityLifecycleCallbacks
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.NetworkCapabilities
import android.os.Build
import android.os.Bundle
import com.tangem.utils.coroutines.CoroutineDispatcherProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import android.net.ConnectivityManager as AndroidConnectivityManager

/**
 * Network connection manager implementation using Android connectivity service
 *
 * @property context     application context for registration lifecycle callbacks
 * @property dispatchers coroutine dispatcher provider
 *
[REDACTED_AUTHOR]
 */
@Singleton
internal class AndroidNetworkConnectionManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dispatchers: CoroutineDispatcherProvider,
) : NetworkConnectionManager {

    private val _isOnline = MutableStateFlow(value = false)
    private val callbacks = NetworkConnectionManagerCallbacks()
    private val receiver = NetworkConnectionBroadcastReceiver()

    override val isOnline: Boolean get() = _isOnline.value
    override val isOnlineFlow: StateFlow<Boolean> = _isOnline

    init {
        (context as? Application)?.registerActivityLifecycleCallbacks(callbacks)
    }

    private inner class NetworkConnectionManagerCallbacks : ActivityLifecycleCallbacks {

        override fun onActivityCreated(activity: Activity, savedInstanceState: Bundle?) {
            val intentFilter = IntentFilter().apply {
                addAction(AndroidConnectivityManager.CONNECTIVITY_ACTION)
            }

            activity.registerReceiver(receiver, intentFilter)
        }

        override fun onActivityStarted(activity: Activity) = Unit
        override fun onActivityResumed(activity: Activity) = Unit
        override fun onActivityPaused(activity: Activity) = Unit
        override fun onActivityStopped(activity: Activity) = Unit
        override fun onActivitySaveInstanceState(activity: Activity, outState: Bundle) = Unit

        override fun onActivityDestroyed(activity: Activity) {
            activity.unregisterReceiver(receiver)
            (context as? Application)?.unregisterActivityLifecycleCallbacks(callbacks)
        }
    }

    private inner class NetworkConnectionBroadcastReceiver : BroadcastReceiver() {

        private val connectivityManager: android.net.ConnectivityManager? =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as? android.net.ConnectivityManager

        override fun onReceive(c: Context?, intent: Intent?) {
            val pendingResult = goAsync()

            CoroutineScope(SupervisorJob()).launch(dispatchers.io) {
                try {
                    val status = isConnected()

                    _isOnline.emit(value = status)
                    Timber.i("Connection status is ${if (status) "ONLINE" else "OFFLINE"}")
                } finally {
                    pendingResult.finish()
                }
            }
        }

        private fun isConnected(): Boolean {
            connectivityManager ?: return false

            return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                capabilities != null && capabilities.hasNetworkTransport()
            } else {
                val networkInfo = connectivityManager.activeNetworkInfo
                networkInfo != null && networkInfo.isConnectedOrConnecting
            }
        }

        private fun NetworkCapabilities.hasNetworkTransport(): Boolean {
            return hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET)
        }
    }
}