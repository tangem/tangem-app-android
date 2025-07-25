package com.tangem.domain.staking.single

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingID

/**
 * Fetcher of yield balance
 *
[REDACTED_AUTHOR]
 */
interface SingleYieldBalanceFetcher : FlowFetcher<SingleYieldBalanceFetcher.Params> {

    /**
     * Params for fetching single yield balance
     *
     * @property userWalletId user wallet ID
     * @property stakingId    staking ID
     */
    data class Params(
        val userWalletId: UserWalletId,
        val stakingId: StakingID,
    )
}