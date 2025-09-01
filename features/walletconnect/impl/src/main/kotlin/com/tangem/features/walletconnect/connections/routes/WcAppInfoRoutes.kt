package com.tangem.features.walletconnect.connections.routes

import androidx.compose.runtime.Immutable
import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
@Immutable
internal sealed class WcAppInfoRoutes : Route {
    @Serializable
    data object AppInfo : WcAppInfoRoutes()

    @Serializable
    data class SelectWallet(val selectedWalletId: UserWalletId) : WcAppInfoRoutes()

    @Serializable
    data class SelectNetworks(
        val missingRequiredNetworks: Set<Network>,
        val requiredNetworks: Set<Network>,
        val availableNetworks: Set<Network>,
        val enabledAvailableNetworks: Set<Network>,
        val notAddedNetworks: Set<Network>,
    ) : WcAppInfoRoutes()

    @Serializable
    sealed class Alert : WcAppInfoRoutes() {
        @Serializable data class Verified(val appName: String) : Alert()

        @Serializable data object UnknownDomain : Alert()

        @Serializable data object UnsafeDomain : Alert()

        @Serializable data object InvalidDomain : Alert()

        @Serializable data class UnsupportedDApp(val appName: String) : Alert()

        @Serializable data class UnsupportedNetwork(val appName: String) : Alert()

        @Serializable data object UriAlreadyUsed : Alert()

        @Serializable data object TimeoutException : Alert()
    }
}