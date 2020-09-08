package com.tangem.tap.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import org.rekotlin.Action
import org.rekotlin.Store
import java.lang.ref.WeakReference

/**
* [REDACTED_AUTHOR]
 */
class NetworkConnectivity(
        private val store: Store<*>,
        context: Context
) {

    private val wContext: WeakReference<Context> = WeakReference(context)

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            store.dispatch(NetworkStateChanged(isOnlineOrConnecting()))
        }
    }

    init {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        wContext.get()?.registerReceiver(receiver, intentFilter)
        store.dispatch(NetworkStateChanged(isOnlineOrConnecting()))
    }


    fun isOnlineOrConnecting(): Boolean {
        val connectivityManager = getConnectivityManager() ?: return false

        return if (Build.VERSION.SDK_INT >= 23) {
            val capabilities =
                    connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
            (capabilities != null) &&
                    (capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))

        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }

    private fun getConnectivityManager(): ConnectivityManager? {
        return wContext.get()?.applicationContext
                ?.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
    }

    companion object {
        private lateinit var instance: NetworkConnectivity

        fun createInstance(store: Store<*>, context: Context): NetworkConnectivity {
            instance = NetworkConnectivity(store, context)
            return instance
        }

        fun getInstance(): NetworkConnectivity = instance
    }
}

data class NetworkStateChanged(val isOnline: Boolean) : Action
