package com.tangem.features.walletconnect.connections.routes

import androidx.compose.runtime.Immutable
import com.tangem.core.decompose.navigation.Route
import com.tangem.core.ui.components.bottomsheets.TangemBottomSheetConfigContent
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWalletId
import com.tangem.features.walletconnect.connections.components.AlertsComponent
import kotlinx.serialization.Serializable

@Serializable
@Immutable
internal sealed class WcAppInfoRoutes : TangemBottomSheetConfigContent, Route {
    @Serializable
    data object AppInfo : WcAppInfoRoutes()

    @Serializable
    data class SelectWallet(val selectedWalletId: UserWalletId) : WcAppInfoRoutes()

    @Serializable
    data class SelectNetworks(
        val missingRequiredNetworks: Set<Network>,
        val requiredNetworks: Set<Network>,
        val availableNetworks: Set<Network>,
        val enabledAvailableNetworks: Set<Network.RawID>,
        val notAddedNetworks: Set<Network>,
    ) : WcAppInfoRoutes()

    @Serializable
    data class Alert(val alertType: AlertsComponent.AlertType) : WcAppInfoRoutes()
}