package com.tangem.domain.staking.fetcher

import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Params for fetchers of staking balance
 *
[REDACTED_AUTHOR]
 */
sealed interface StakingBalanceFetcherParams {

    /** User wallet ID */
    val userWalletId: UserWalletId

    /**
     * Params for fetching multiple staking balances
     *
     * @property userWalletId             user wallet ID
     * @property currencyIdWithNetworkMap map of currency ID to network
     */
    data class Multi(
        override val userWalletId: UserWalletId,
        val currencyIdWithNetworkMap: Map<CryptoCurrency.ID, Network>,
    ) : StakingBalanceFetcherParams

    /**
     * Params for fetching single staking balance
     *
     * @property userWalletId user wallet ID
     * @property currencyId   currency ID
     * @property network      network
     */
    data class Single(
        override val userWalletId: UserWalletId,
        val currencyId: CryptoCurrency.ID,
        val network: Network,
    ) : StakingBalanceFetcherParams
}