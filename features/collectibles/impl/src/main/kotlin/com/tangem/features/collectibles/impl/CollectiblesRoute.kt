package com.tangem.features.collectibles.impl

import com.tangem.core.decompose.navigation.Route
import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal sealed interface CollectiblesRoute : Route {

    @Serializable
    data class Onboarding(val userWalletId: UserWalletId) : CollectiblesRoute

    @Serializable
    data object Stories : CollectiblesRoute

    @Serializable
    data class Main(val userWalletId: UserWalletId) : CollectiblesRoute

    data class MachineDetails(val externalId: String) : CollectiblesRoute
}