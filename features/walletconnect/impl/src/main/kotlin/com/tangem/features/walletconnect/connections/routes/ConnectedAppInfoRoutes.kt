package com.tangem.features.walletconnect.connections.routes

import com.tangem.core.decompose.navigation.Route
import kotlinx.serialization.Serializable

@Serializable
internal sealed class ConnectedAppInfoRoutes : Route {

    @Serializable
    data object AppInfo : ConnectedAppInfoRoutes()

    @Serializable
    data class VerifiedAlert(val appName: String) : ConnectedAppInfoRoutes()
}