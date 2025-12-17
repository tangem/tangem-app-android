package com.tangem.domain.staking.single

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.staking.StakingID

/**
 * Fetcher of staking balance
 *
[REDACTED_AUTHOR]
 */
interface SingleStakingBalanceFetcher : FlowFetcher<SingleStakingBalanceFetcher.Params> {

    /**
     * Params for fetching single staking balance
     *
     * @property userWalletId user wallet ID
     * @property stakingId    staking ID
     */
    data class Params(
        val userWalletId: UserWalletId,
        val stakingId: StakingID,
    )
}