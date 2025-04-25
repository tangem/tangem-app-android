package com.tangem.features.walletconnect.connections.routes

import kotlinx.serialization.Serializable

@Serializable
internal sealed class WcConnectionsBottomSheetRoutes {
    @Serializable
    data class AppInfo(val wcUrl: String) : WcConnectionsBottomSheetRoutes()
}