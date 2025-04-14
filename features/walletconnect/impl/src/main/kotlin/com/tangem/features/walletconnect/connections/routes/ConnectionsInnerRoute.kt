package com.tangem.features.walletconnect.connections.routes

import kotlinx.serialization.Serializable

@Serializable
internal sealed class ConnectionsInnerRoute {
    @Serializable
    data object Connections : ConnectionsInnerRoute()

    @Serializable
    data object QrScan : ConnectionsInnerRoute()
}