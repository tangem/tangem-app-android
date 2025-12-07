package com.tangem.domain.staking.multi

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.staking.StakingBalance
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Producer of all staking balances for selected wallet [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface MultiStakingBalanceProducer : FlowProducer<Set<StakingBalance>> {

    data class Params(val userWalletId: UserWalletId)

    interface Factory : FlowProducer.Factory<Params, MultiStakingBalanceProducer>
}