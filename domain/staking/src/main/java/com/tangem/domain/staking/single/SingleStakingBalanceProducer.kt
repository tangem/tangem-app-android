package com.tangem.domain.staking.single

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.wallet.UserWalletId
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.staking.StakingID

/**
 * Producer of staking balance for selected wallet [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface SingleStakingBalanceProducer : FlowProducer<StakingBalance> {

    data class Params(
        val userWalletId: UserWalletId,
        val stakingId: StakingID,
    ) {

        override fun toString(): String {
            return """
                SingleStakingBalanceProducer.Params(
                    userWalletId = $userWalletId,
                    stakingId = $stakingId,
                )
            """.trimIndent()
        }
    }

    interface Factory : FlowProducer.Factory<Params, SingleStakingBalanceProducer>
}