package com.tangem.domain.staking.multi

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Fetcher of yields balances
 *
[REDACTED_AUTHOR]
 */
interface MultiYieldBalanceFetcher : FlowFetcher<MultiYieldBalanceFetcher.Params> {

    /**
     * Params for fetching multiple yield balances
     *
     * @property userWalletId             user wallet ID
     * @property currencyIdWithNetworkMap map of currency ID to network
     */
    data class Params(
        val userWalletId: UserWalletId,
        val currencyIdWithNetworkMap: Map<CryptoCurrency.ID, Network>,
    )
}