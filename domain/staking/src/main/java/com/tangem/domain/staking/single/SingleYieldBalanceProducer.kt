package com.tangem.domain.staking.single

import com.tangem.domain.core.flow.FlowProducer
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.staking.model.stakekit.YieldBalance
import com.tangem.domain.models.wallet.UserWalletId

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
    ) {

        override fun toString(): String {
            return """
                SingleYieldBalanceProducer.Params(
                    userWalletId = $userWalletId,
                    currencyId = $currencyId,
                    network = $network
                )
            """.trimIndent()
        }
    }

    interface Factory : FlowProducer.Factory<Params, SingleYieldBalanceProducer>
}