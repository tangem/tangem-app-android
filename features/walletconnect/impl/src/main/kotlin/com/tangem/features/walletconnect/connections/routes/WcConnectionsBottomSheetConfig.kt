package com.tangem.features.walletconnect.connections.routes

import kotlinx.serialization.Serializable

@Serializable
internal sealed class WcConnectionsBottomSheetConfig {
    @Serializable
    data class AppInfo(val wcUrl: String) : WcConnectionsBottomSheetConfig()

    @Serializable
    data class ConnectedApp(val topic: String) : WcConnectionsBottomSheetConfig()
}