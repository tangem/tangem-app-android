package com.tangem.features.tangempay.ui

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal sealed class TangemPayOnboardingNavigation {

    @Serializable
    data class WalletSelector(
        val walletsIds: List<UserWalletId>,
    ) : TangemPayOnboardingNavigation()
}