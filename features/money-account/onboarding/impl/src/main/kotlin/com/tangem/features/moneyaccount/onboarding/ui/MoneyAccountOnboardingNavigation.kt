package com.tangem.features.moneyaccount.onboarding.ui

import com.tangem.domain.models.wallet.UserWalletId
import kotlinx.serialization.Serializable

@Serializable
internal sealed class MoneyAccountOnboardingNavigation {

    @Serializable
    data class WalletSelector(
        val walletsIds: List<UserWalletId>,
    ) : MoneyAccountOnboardingNavigation()
}