package com.tangem.domain.staking.fetcher

import com.tangem.domain.tokens.model.CryptoCurrency
import com.tangem.domain.tokens.model.Network
import com.tangem.domain.wallets.models.UserWalletId

/**
 * Params for fetchers of yield balance
 *
[REDACTED_AUTHOR]
 */
sealed interface YieldBalanceFetcherParams {

    /** User wallet ID */
    val userWalletId: UserWalletId

    /**
     * Params for fetching multiple yield balances
     *
     * @property userWalletId             user wallet ID
     * @property currencyIdWithNetworkMap map of currency ID to network
     */
    data class Multi(
        override val userWalletId: UserWalletId,
        val currencyIdWithNetworkMap: Map<CryptoCurrency.ID, Network>,
    ) : YieldBalanceFetcherParams

    /**
     * Params for fetching single yield balance
     *
     * @property userWalletId user wallet ID
     * @property currencyId   currency ID
     * @property network      network
     */
    data class Single(
        override val userWalletId: UserWalletId,
        val currencyId: CryptoCurrency.ID,
        val network: Network,
    ) : YieldBalanceFetcherParams
}