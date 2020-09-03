package com.tangem.tap.network

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.ConnectivityManager
import org.rekotlin.Action
import org.rekotlin.Store

/**
[REDACTED_AUTHOR]
 */
class NetworkConnectivity(
        private val store: Store<*>,
        context: Context
) {
    private val connectivityManager = context.applicationContext.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val receiver = object : BroadcastReceiver() {
        override fun onReceive(c: Context?, intent: Intent?) {
            store.dispatch(NetworkStateChanged(isOnlineOrConnecting()))
        }
    }

    init {
        val intentFilter = IntentFilter()
        intentFilter.addAction(ConnectivityManager.CONNECTIVITY_ACTION)
        context.registerReceiver(receiver, intentFilter)
        store.dispatch(NetworkStateChanged(isOnlineOrConnecting()))
    }


    fun isOnlineOrConnecting(): Boolean {
        val netInfo = connectivityManager.activeNetworkInfo
        return netInfo != null && netInfo.isConnectedOrConnecting
    }
}

data class NetworkStateChanged(val isOnline: Boolean) : Action