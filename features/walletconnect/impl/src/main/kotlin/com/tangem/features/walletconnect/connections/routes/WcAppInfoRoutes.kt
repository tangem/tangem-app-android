package com.tangem.features.walletconnect.connections.routes

import androidx.compose.runtime.Immutable
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.features.walletconnect.connections.components.AlertsComponent
import kotlinx.serialization.Serializable

@Serializable
@Immutable
internal sealed class WcAppInfoRoutes : TangemBottomSheetConfigContent {
    @Serializable
    data object AppInfo : WcAppInfoRoutes()

    @Serializable
    data object SelectWallet : WcAppInfoRoutes()

    @Serializable
    data object SelectNetworks : WcAppInfoRoutes()

    @Serializable
    data class Alert(val alertType: AlertsComponent.AlertType) : WcAppInfoRoutes()
}