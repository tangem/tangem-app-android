package com.tangem.domain.networks.single

import com.tangem.domain.core.flow.FlowFetcher
import com.tangem.domain.models.currency.CryptoCurrency
import com.tangem.domain.models.network.Network
import com.tangem.domain.models.wallet.UserWalletId

/**
 * Fetcher of network status [Network] for wallet with [UserWalletId]
 *
[REDACTED_AUTHOR]
 */
interface SingleNetworkStatusFetcher : FlowFetcher<SingleNetworkStatusFetcher.Params> {

    /**
     * Params
     *
     * @property userWalletId user wallet id
     * @property network      network whose status is fetched
     * @property extraTokens  additional tokens to fetch balances for, beyond the wallet's added currencies
     */
    data class Params(
        val userWalletId: UserWalletId,
        val network: Network,
        val extraTokens: Set<CryptoCurrency.Token> = emptySet(),
    )
}