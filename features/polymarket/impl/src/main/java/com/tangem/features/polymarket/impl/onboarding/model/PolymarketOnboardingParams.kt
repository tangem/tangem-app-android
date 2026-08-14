package com.tangem.features.polymarket.impl.onboarding.model

import com.tangem.domain.models.wallet.UserWalletId

/**
 * Input of the entry gate.
 *
 * @property userWalletId the wallet `PolymarketRoute.Entry` settled — may differ from the wallet the feature
 *  was opened with when the caller left the choice to the user.
 */
internal data class PolymarketOnboardingParams(
    val userWalletId: UserWalletId,
)