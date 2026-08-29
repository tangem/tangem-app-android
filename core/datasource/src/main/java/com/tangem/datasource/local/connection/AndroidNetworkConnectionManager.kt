package com.tangem.datasource.local.connection

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import androidx.core.content.ContextCompat
import com.tangem.core.local.connection.NetworkConnectionManager
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject

internal class AndroidNetworkConnectionManager @Inject constructor(
    @ApplicationContext private val applicationContext: Context,
) : NetworkConnectionManager {

    override val isOnline: Boolean
        get() = isNetworkAvailable()

    private fun isNetworkAvailable(): Boolean {
        val connectivityManager = ContextCompat.getSystemService(applicationContext, ConnectivityManager::class.java)

        // Network that is currently in use (several networks can be presented at the same time)
        val defaultNetwork: Network = connectivityManager?.activeNetwork ?: return false
        val networkCapabilities: NetworkCapabilities =
            connectivityManager.getNetworkCapabilities(defaultNetwork) ?: return false

        return networkCapabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_INTERNET)
    }
}