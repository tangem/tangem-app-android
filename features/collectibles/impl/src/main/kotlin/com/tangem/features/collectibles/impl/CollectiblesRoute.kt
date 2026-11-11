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

    @Serializable
    data class MachineDetails(val externalId: String) : CollectiblesRoute

    /** @property reopenTxId a paid pack left unopened: the screen opens it at once instead of offering a purchase */
    @Serializable
    data class ConfirmOrder(
        val machineId: String,
        val isTurboEnabled: Boolean,
        val reopenTxId: String? = null,
    ) : CollectiblesRoute

    @Serializable
    data object PackOpenFailed : CollectiblesRoute
}