package com.tangem.features.earn.staking.impl

import com.tangem.common.routing.AppRoute
import com.tangem.core.ui.extensions.TextReference
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.earn.EarnProductId
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingIntegrationID
import com.tangem.features.earn.product.api.session.EarnAction

internal val POOLED_STAKING_ID = EarnProductId("staking")

/** Any click opens the staking screen, so the action carries only a caption and an id, not what to open. */
internal data class PooledStakingAction(override val label: TextReference) : EarnAction {

    override val id: String = "open_staking"
}

internal fun stakingRoute(userWalletId: UserWalletId, currency: CryptoCurrency): AppRoute.Staking = AppRoute.Staking(
    userWalletId = userWalletId,
    cryptoCurrency = currency,
    integrationId = StakingIntegrationID.P2PEthPool,
)