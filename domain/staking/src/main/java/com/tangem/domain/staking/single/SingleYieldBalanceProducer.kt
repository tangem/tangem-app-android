package com.tangem.domain.staking.single

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Producer of yield balance for selected wallet [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface SingleYieldBalanceProducer : FlowProducer<YieldBalance> {

    data class Params(
        val userWalletId: UserWalletId,
        val currencyId: CryptoCurrency.ID,
        val network: Network,
    )

    interface Factory : FlowProducer.Factory<Params, SingleYieldBalanceProducer>
}