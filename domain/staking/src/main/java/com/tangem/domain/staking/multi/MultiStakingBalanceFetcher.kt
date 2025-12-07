package com.tangem.domain.staking.multi

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.staking.StakingID

/**
 * Fetcher of staking balances
 *
[REDACTED_AUTHOR]
 */
interface MultiStakingBalanceFetcher : FlowFetcher<MultiStakingBalanceFetcher.Params> {

    /**
     * Params for fetching multiple staking balances
     *
     * @property userWalletId user wallet ID
     * @property stakingIds   map of currency ID to network
     */
    data class Params(
        val userWalletId: UserWalletId,
        val stakingIds: Set<StakingID>,
    ) {

        override fun toString(): String {
            return """
                MultiStakingBalanceFetcher.Params(
                    userWalletId = $userWalletId,
                    stakingIds: ${stakingIds.joinToString()}
                )
            """.trimIndent()
        }
    }
}