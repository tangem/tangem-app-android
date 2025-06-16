package com.tangem.domain.staking.single

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.staking.fetcher.YieldBalanceFetcherParams
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Fetcher of yield balance
 *
[REDACTED_AUTHOR]
 */
interface SingleYieldBalanceFetcher : FlowFetcher<YieldBalanceFetcherParams.Single> {

    /**
     * Params for fetching single yield balance
     *
     * @property userWalletId user wallet ID
     * @property currencyId   currency ID
     * @property network      network
     */
    data class Params(
        val userWalletId: UserWalletId,
        val currencyId: CryptoCurrency.ID,
        val network: Network,
    )
}