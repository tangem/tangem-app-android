package com.tangem.domain.staking.single

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.staking.model.StakingID
import com.tangem.domain.staking.model.stakekit.YieldBalance

/**
 * Producer of yield balance for selected wallet [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface SingleYieldBalanceProducer : FlowProducer<YieldBalance> {

    data class Params(
        val userWalletId: UserWalletId,
        val stakingId: StakingID,
    ) {

        override fun toString(): String {
            return """
                SingleYieldBalanceProducer.Params(
                    userWalletId = $userWalletId,
                    stakingId = $stakingId,
                )
            """.trimIndent()
        }
    }

    interface Factory : FlowProducer.Factory<Params, SingleYieldBalanceProducer>
}