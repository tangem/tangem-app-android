package com.tangem.domain.networks.multi

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Fetcher of network status [Network] for multi-currency wallet with [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface MultiNetworkStatusFetcher : FlowFetcher<MultiNetworkStatusFetcher.Params> {

    /**
     * Params
     *
     * @property userWalletId user wallet id
     * @property networks     networks whose statuses are fetched
     * @property extraTokens  additional tokens to fetch balances for, beyond the wallet's added currencies
     */
    data class Params(
        val userWalletId: UserWalletId,
        val networks: Set<Network>,
        val extraTokens: Set<CryptoCurrency.Token> = emptySet(),
    )
}